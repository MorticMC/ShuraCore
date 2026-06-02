package dev.shura.core.arena;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Arena {

    private final String id;
    private String name;
    private String world;
    private Location pos1;
    private Location pos2;
    private Location spawnA;
    private Location spawnB;
    private String kitId;
    private boolean enabled;
    private final List<ArenaCopy> copies = new ArrayList<>();
    private final Set<String> boundKitIds = new HashSet<>();
    private int autoCloneCount = 2; // Default to 2 clones

    public Arena(String id, String name, String world) {
        this.id = id;
        this.name = name;
        this.world = world;
        this.enabled = true;
    }

    public boolean isFullyConfigured() {
        return pos1 != null && pos2 != null && spawnA != null && spawnB != null;
    }

    public boolean hasAvailableCopy() {
        return copies.stream().anyMatch(c -> !c.isInUse());
    }

    public ArenaCopy getAvailableCopy() {
        return copies.stream().filter(c -> !c.isInUse()).findFirst().orElse(null);
    }

    public void addCopy(ArenaCopy copy) { copies.add(copy); }
    public void removeCopy(ArenaCopy copy) { copies.remove(copy); }

    // Dimensions
    public int getWidth() {
        if (pos1 == null || pos2 == null) return 0;
        return Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1;
    }

    public int getHeight() {
        if (pos1 == null || pos2 == null) return 0;
        return Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1;
    }

    public int getLength() {
        if (pos1 == null || pos2 == null) return 0;
        return Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }
    public Location getPos1() { return pos1; }
    public void setPos1(Location pos1) { this.pos1 = pos1; }
    public Location getPos2() { return pos2; }
    public void setPos2(Location pos2) { this.pos2 = pos2; }
    public Location getSpawnA() { return spawnA; }
    public void setSpawnA(Location spawnA) { this.spawnA = spawnA; }
    public Location getSpawnB() { return spawnB; }
    public void setSpawnB(Location spawnB) { this.spawnB = spawnB; }
    public String getKitId() { return kitId; }
    public void setKitId(String kitId) { this.kitId = kitId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<ArenaCopy> getCopies() { return copies; }
    public Set<String> getBoundKitIds() { return boundKitIds; }
    public void addBoundKit(String kitId) { boundKitIds.add(kitId); }
    public void removeBoundKit(String kitId) { boundKitIds.remove(kitId); }
    public boolean isBoundToKit(String kitId) { return boundKitIds.contains(kitId); }
    public int getAutoCloneCount() { return autoCloneCount; }
    public void setAutoCloneCount(int count) { this.autoCloneCount = Math.max(1, Math.min(10, count)); }
}
