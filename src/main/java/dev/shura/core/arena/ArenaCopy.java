package dev.shura.core.arena;

import org.bukkit.Location;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ArenaCopy {

    private final String copyId;
    private final String arenaId;
    private final Location spawnA;
    private final Location spawnB;
    private final Location origin; // min-corner where this copy was pasted
    private final AtomicBoolean inUse = new AtomicBoolean(false);
    private volatile boolean ready; // blocks finished pasting

    public ArenaCopy(String arenaId, Location origin, Location spawnA, Location spawnB) {
        this.copyId = UUID.randomUUID().toString();
        this.arenaId = arenaId;
        this.origin = origin;
        this.spawnA = spawnA;
        this.spawnB = spawnB;
    }

    /** Atomically claims this copy. Returns false if it was already claimed (prevents double-booking). */
    public boolean claim() { return inUse.compareAndSet(false, true); }
    public void release() { inUse.set(false); }

    public String getCopyId() { return copyId; }
    public String getArenaId() { return arenaId; }
    public Location getSpawnA() { return spawnA; }
    public Location getSpawnB() { return spawnB; }
    public Location getOrigin() { return origin; }
    public boolean isInUse() { return inUse.get(); }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
}
