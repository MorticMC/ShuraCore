package dev.shura.core.arena;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import dev.shura.core.ShuraCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Thin wrapper around the WorldEdit / FastAsyncWorldEdit API.
 * <p>
 * All FAWE usage lives here so the rest of the plugin stays decoupled from the
 * WorldEdit classpath. We capture exactly ONE in-memory clipboard per arena
 * (lazily, on first paste) and re-use it for every copy and every regen. This
 * is what makes regen O(blocks) on FAWE's async pipeline instead of the old
 * block-by-block main-thread loop, and removes the per-copy BlockSnapshot that
 * used to leak memory for every clone.
 */
public class SchematicService {

    private final ShuraCore plugin;
    // arenaId -> cached source clipboard
    private final Map<String, Clipboard> clipboards = new ConcurrentHashMap<>();

    public SchematicService(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        try {
            Class.forName("com.sk89q.worldedit.WorldEdit");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean hasClipboard(String arenaId) {
        return clipboards.containsKey(arenaId);
    }

    public void invalidate(String arenaId) {
        clipboards.remove(arenaId);
    }

    public void invalidateAll() {
        clipboards.clear();
    }

    /** Returns the cached clipboard for the arena, capturing it from the source region if needed. */
    public Clipboard getOrCapture(Arena arena) {
        Clipboard cached = clipboards.get(arena.getId());
        if (cached != null) return cached;
        Clipboard captured = capture(arena);
        if (captured != null) clipboards.put(arena.getId(), captured);
        return captured;
    }

    private Clipboard capture(Arena arena) {
        Location p1 = arena.getPos1();
        Location p2 = arena.getPos2();
        if (p1 == null || p2 == null || p1.getWorld() == null) return null;

        World weWorld = BukkitAdapter.adapt(p1.getWorld());
        BlockVector3 min = BlockVector3.at(
                Math.min(p1.getBlockX(), p2.getBlockX()),
                Math.min(p1.getBlockY(), p2.getBlockY()),
                Math.min(p1.getBlockZ(), p2.getBlockZ()));
        BlockVector3 max = BlockVector3.at(
                Math.max(p1.getBlockX(), p2.getBlockX()),
                Math.max(p1.getBlockY(), p2.getBlockY()),
                Math.max(p1.getBlockZ(), p2.getBlockZ()));

        CuboidRegion region = new CuboidRegion(weWorld, min, max);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(min);

        try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
            ForwardExtentCopy copy = new ForwardExtentCopy(session, region, clipboard, region.getMinimumPoint());
            copy.setCopyingEntities(false);
            copy.setCopyingBiomes(false);
            Operations.complete(copy);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to capture arena clipboard for " + arena.getId(), t);
            return null;
        }
        return clipboard;
    }

    /**
     * Pastes the arena's clipboard so its minimum corner lands at {@code targetMinCorner}.
     * The heavy edit runs off the main thread (FAWE supports async edits); {@code onComplete}
     * is always invoked back on the main thread.
     */
    public void pasteAsync(Arena arena, Location targetMinCorner, Runnable onComplete) {
        Clipboard clipboard = getOrCapture(arena);
        if (clipboard == null || targetMinCorner == null || targetMinCorner.getWorld() == null) {
            runSync(onComplete);
            return;
        }

        final World weWorld = BukkitAdapter.adapt(targetMinCorner.getWorld());
        final BlockVector3 to = BlockVector3.at(
                targetMinCorner.getBlockX(), targetMinCorner.getBlockY(), targetMinCorner.getBlockZ());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(session)
                        .to(to)
                        .ignoreAirBlocks(false)
                        .copyEntities(false)
                        .build();
                Operations.complete(operation);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Failed to paste arena " + arena.getId(), t);
            }
            runSync(onComplete);
        });
    }

    private void runSync(Runnable runnable) {
        if (runnable == null) return;
        if (Bukkit.isPrimaryThread()) runnable.run();
        else Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
