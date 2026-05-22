package dev.shura.core.arena;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.BlockSnapshot;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaReset {

    private final ShuraCore plugin;
    // copyId -> snapshot
    private final Map<String, BlockSnapshot> snapshots = new ConcurrentHashMap<>();

    public ArenaReset(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void captureSnapshot(ArenaCopy copy, Arena arena) {
        World world = org.bukkit.Bukkit.getWorld("shura_duels");
        if (world == null) return;

        // Offset the arena bounds to the copy's origin
        Location origin = copy.getOrigin();
        Location arenaPos1 = arena.getPos1();
        Location arenaPos2 = arena.getPos2();

        int offsetX = origin.getBlockX() - arenaPos1.getBlockX();
        int offsetZ = origin.getBlockZ() - arenaPos1.getBlockZ();

        Location copyPos1 = arenaPos1.clone().add(offsetX, 0, offsetZ);
        Location copyPos2 = arenaPos2.clone().add(offsetX, 0, offsetZ);

        BlockSnapshot snapshot = new BlockSnapshot(world, copyPos1, copyPos2);
        snapshot.capture();
        snapshots.put(copy.getCopyId(), snapshot);
    }

    public void reset(ArenaCopy copy, Runnable onComplete) {
        BlockSnapshot snapshot = snapshots.get(copy.getCopyId());
        if (snapshot == null || !snapshot.isCaptured()) {
            copy.release();
            if (onComplete != null) onComplete.run();
            return;
        }
        snapshot.restore(() -> {
            copy.release();
            if (onComplete != null) onComplete.run();
        }, plugin);
    }

    public void removeSnapshot(String copyId) {
        snapshots.remove(copyId);
    }

    public boolean hasSnapshot(String copyId) {
        return snapshots.containsKey(copyId);
    }
}
