package dev.shura.core.queue;

import java.util.UUID;

public class QueueEntry {

    private final UUID playerUuid;
    private final String playerName;
    private final String tierlistId;
    private final long joinTime;
    private final boolean premium;

    public QueueEntry(UUID playerUuid, String playerName, String tierlistId, boolean premium) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.tierlistId = tierlistId;
        this.premium = premium;
        this.joinTime = System.currentTimeMillis();
    }

    public void tickRange() {
        // No-op, kept for compatibility
    }

    public boolean canMatch(QueueEntry other) {
        if (!this.tierlistId.equals(other.tierlistId)) return false;
        if (this.premium != other.premium) return false;
        return true;
    }

    public long getWaitSeconds() {
        return (System.currentTimeMillis() - joinTime) / 1000;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getTierlistId() { return tierlistId; }
    public boolean isPremium() { return premium; }
    public long getJoinTime() { return joinTime; }
}
