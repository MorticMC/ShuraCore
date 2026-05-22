package dev.shura.core.arena;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ArenaManager {

    private static final int COPY_SPACING = 200; // blocks between copies in duel world
    private static final int MIN_COPIES = 2;

    private final ShuraCore plugin;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private final ArenaReset arenaReset;
    private final ArenaWand arenaWand;

    // Tracks next available grid position for copy placement
    private int nextCopyIndex = 0;

    public ArenaManager(ShuraCore plugin) {
        this.plugin = plugin;
        this.arenaReset = new ArenaReset(plugin);
        this.arenaWand = new ArenaWand();
        loadAll();
    }

    private void loadAll() {
        plugin.getDatabaseService().query("SELECT * FROM arenas", null, rs -> {
            List<Arena> loaded = new ArrayList<>();
            try {
                while (rs.next()) loaded.add(deserialize(rs));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load arenas.", e);
            }
            return loaded;
        }).thenAccept(list -> {
            if (list == null) return;
            list.forEach(a -> arenas.put(a.getId(), a));
            Bukkit.getScheduler().runTask(plugin, this::initializeCopies);
        });
    }

    private void initializeCopies() {
        for (Arena arena : arenas.values()) {
            if (!arena.isEnabled() || !arena.isFullyConfigured()) continue;
            while (arena.getCopies().size() < MIN_COPIES) {
                generateCopy(arena);
            }
        }
    }

    public ArenaCopy getOrCreateCopy(Arena arena) {
        ArenaCopy available = arena.getAvailableCopy();
        if (available != null) {
            available.claim();
            return available;
        }
        // All copies in use — generate a new one
        ArenaCopy newCopy = generateCopy(arena);
        if (newCopy != null) newCopy.claim();
        return newCopy;
    }

    private ArenaCopy generateCopy(Arena arena) {
        World duelWorld = Bukkit.getWorld("shura_duels");
        if (duelWorld == null) {
            plugin.getLogger().warning("shura_duels world not found! Cannot generate arena copy.");
            return null;
        }

        // Grid layout: copies placed at fixed X/Z offsets
        int gridX = (nextCopyIndex % 10) * COPY_SPACING;
        int gridZ = (nextCopyIndex / 10) * COPY_SPACING;
        nextCopyIndex++;

        Location origin = new Location(duelWorld, gridX, 64, gridZ);

        // Calculate offset spawn points relative to origin
        Location arenaPos1 = arena.getPos1();
        int offsetX = origin.getBlockX() - arenaPos1.getBlockX();
        int offsetZ = origin.getBlockZ() - arenaPos1.getBlockZ();

        Location copySpawnA = arena.getSpawnA().clone().add(offsetX, 0, offsetZ);
        Location copySpawnB = arena.getSpawnB().clone().add(offsetX, 0, offsetZ);

        ArenaCopy copy = new ArenaCopy(arena.getId(), origin, copySpawnA, copySpawnB);
        arena.addCopy(copy);

        // Paste arena blocks at origin (sync — runs on main thread)
        pasteArena(arena, origin, duelWorld);

        // Capture snapshot after paste
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                arenaReset.captureSnapshot(copy, arena), 5L);

        return copy;
    }

    private void pasteArena(Arena arena, Location origin, World targetWorld) {
        Location pos1 = arena.getPos1();
        Location pos2 = arena.getPos2();
        World sourceWorld = pos1.getWorld();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        int offsetX = origin.getBlockX() - minX;
        int offsetY = origin.getBlockY() - minY;
        int offsetZ = origin.getBlockZ() - minZ;

        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    targetWorld.getBlockAt(x + offsetX, y + offsetY, z + offsetZ)
                            .setBlockData(sourceWorld.getBlockAt(x, y, z).getBlockData().clone(), false);
    }

    public void resetArena(ArenaCopy copy) {
        arenaReset.reset(copy, () -> {
            Arena arena = arenas.get(copy.getArenaId());
            if (arena == null) return;
            // If total copies exceed minimum and this copy is no longer needed, remove it
            if (arena.getCopies().size() > MIN_COPIES) {
                arena.removeCopy(copy);
                arenaReset.removeSnapshot(copy.getCopyId());
            }
        });
    }

    public void saveArena(Arena arena) {
        ArenaValidator.ValidationResult result = ArenaValidator.validate(arena);
        if (!result.valid()) {
            plugin.getLogger().warning("Arena " + arena.getName() + " failed validation: " + result.errors());
            return;
        }

        plugin.getDatabaseService().updateAsync(
                "INSERT INTO arenas (id, name, world, pos1, pos2, spawn_a, spawn_b, kit_id, copies, enabled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, '[]', ?) ON CONFLICT(id) DO UPDATE SET " +
                "name=excluded.name, world=excluded.world, pos1=excluded.pos1, pos2=excluded.pos2, " +
                "spawn_a=excluded.spawn_a, spawn_b=excluded.spawn_b, kit_id=excluded.kit_id, enabled=excluded.enabled",
                stmt -> {
                    stmt.setString(1, arena.getId());
                    stmt.setString(2, arena.getName());
                    stmt.setString(3, arena.getWorld());
                    stmt.setString(4, LocationUtil.serialize(arena.getPos1()));
                    stmt.setString(5, LocationUtil.serialize(arena.getPos2()));
                    stmt.setString(6, LocationUtil.serialize(arena.getSpawnA()));
                    stmt.setString(7, LocationUtil.serialize(arena.getSpawnB()));
                    stmt.setString(8, arena.getKitId());
                    stmt.setBoolean(9, arena.isEnabled());
                });
        arenas.put(arena.getId(), arena);
    }

    public void deleteArena(String id) {
        Arena arena = arenas.remove(id);
        if (arena != null) arena.getCopies().forEach(c -> arenaReset.removeSnapshot(c.getCopyId()));
        plugin.getDatabaseService().updateAsync("DELETE FROM arenas WHERE id = ?",
                stmt -> stmt.setString(1, id));
    }

    public Arena getArena(String id) { return arenas.get(id); }

    public Arena getArenaByName(String name) {
        return arenas.values().stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public List<Arena> getAvailableArenas() {
        return arenas.values().stream()
                .filter(a -> a.isEnabled() && a.isFullyConfigured())
                .toList();
    }

    public Arena getAvailableArenaForKit(String kitId) {
        return arenas.values().stream()
                .filter(a -> a.isEnabled() && a.isFullyConfigured()
                        && (a.getKitId() == null || a.getKitId().equals(kitId))
                        && a.hasAvailableCopy())
                .findFirst().orElse(null);
    }

    public Collection<Arena> getAllArenas() { return Collections.unmodifiableCollection(arenas.values()); }
    public ArenaWand getArenaWand() { return arenaWand; }

    private Arena deserialize(ResultSet rs) throws SQLException {
        Arena arena = new Arena(rs.getString("id"), rs.getString("name"), rs.getString("world"));
        arena.setPos1(LocationUtil.deserialize(rs.getString("pos1")));
        arena.setPos2(LocationUtil.deserialize(rs.getString("pos2")));
        arena.setSpawnA(LocationUtil.deserialize(rs.getString("spawn_a")));
        arena.setSpawnB(LocationUtil.deserialize(rs.getString("spawn_b")));
        arena.setKitId(rs.getString("kit_id"));
        arena.setEnabled(rs.getBoolean("enabled"));
        return arena;
    }
}
