package dev.shura.core.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

public class BlockSnapshot {

    private final Map<Long, BlockData> snapshot = new HashMap<>();
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final World world;

    public BlockSnapshot(World world, Location pos1, Location pos2) {
        this.world = world;
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public void capture() {
        snapshot.clear();
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    snapshot.put(encode(x, y, z), world.getBlockAt(x, y, z).getBlockData().clone());
    }

    // Restores blocks in chunks to avoid TPS spikes
    public void restore(Runnable onComplete, org.bukkit.plugin.Plugin plugin) {
        if (snapshot.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        var entries = snapshot.entrySet().iterator();
        final int BLOCKS_PER_TICK = 1000;

        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int count = 0;
            while (entries.hasNext() && count < BLOCKS_PER_TICK) {
                var entry = entries.next();
                long key = entry.getKey();
                int x = decodeX(key);
                int y = decodeY(key);
                int z = decodeZ(key);
                Block block = world.getBlockAt(x, y, z);
                block.setBlockData(entry.getValue(), false);
                count++;
            }
            if (!entries.hasNext()) {
                task.cancel();
                if (onComplete != null) onComplete.run();
            }
        }, 0L, 1L);
    }

    private long encode(int x, int y, int z) {
        return ((long)(x - minX) << 24 | (long)(y - minY) << 12 | (long)(z - minZ));
    }

    private int decodeX(long key) { return (int)((key >> 24) & 0xFFF) + minX; }
    private int decodeY(long key) { return (int)((key >> 12) & 0xFFF) + minY; }
    private int decodeZ(long key) { return (int)(key & 0xFFF) + minZ; }

    public int size() { return snapshot.size(); }
    public boolean isCaptured() { return !snapshot.isEmpty(); }
}
