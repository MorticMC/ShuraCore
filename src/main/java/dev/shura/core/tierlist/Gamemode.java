package dev.shura.core.tierlist;

import org.bukkit.Material;

public class Gamemode {

    private final String key;
    private final String name;
    private final Material material;
    private final String fullId;
    private final int slot;
    private final String queueType;

    public Gamemode(String key, String name, Material material, String fullId, int slot, String queueType) {
        this.key = key;
        this.name = name;
        this.material = material;
        this.fullId = fullId;
        this.slot = slot;
        this.queueType = queueType;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public Material getMaterial() { return material; }
    public String getFullId() { return fullId; }
    public int getSlot() { return slot; }
    public String getQueueType() { return queueType; }
    public boolean isRanked() { return "ranked".equalsIgnoreCase(queueType) || "both".equalsIgnoreCase(queueType); }
    public boolean isUnranked() { return "unranked".equalsIgnoreCase(queueType) || "both".equalsIgnoreCase(queueType); }
}
