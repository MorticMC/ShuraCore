package dev.shura.core.arena;

import org.bukkit.Location;

import java.util.UUID;

public class ArenaCopy {

    private final String copyId;
    private final String arenaId;
    private final Location spawnA;
    private final Location spawnB;
    private final Location origin; // where this copy was pasted
    private boolean inUse;

    public ArenaCopy(String arenaId, Location origin, Location spawnA, Location spawnB) {
        this.copyId = UUID.randomUUID().toString();
        this.arenaId = arenaId;
        this.origin = origin;
        this.spawnA = spawnA;
        this.spawnB = spawnB;
        this.inUse = false;
    }

    public void claim() { this.inUse = true; }
    public void release() { this.inUse = false; }

    public String getCopyId() { return copyId; }
    public String getArenaId() { return arenaId; }
    public Location getSpawnA() { return spawnA; }
    public Location getSpawnB() { return spawnB; }
    public Location getOrigin() { return origin; }
    public boolean isInUse() { return inUse; }
}
