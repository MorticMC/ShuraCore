package dev.shura.core.queue;

import java.util.UUID;

public class QueueEntry {

    private final UUID playerUuid;
    private final String playerName;
    private final String tierlistId;
    private final int elo;
    private final long joinTime;
    private final boolean premium;
    private int eloRange;

    private static final int BASE_RANGE = 100;
    private static final int EXPAND_AMOUNT = 50;
    private static final int EXPAND_INTERVAL_SECONDS = 10;
    private static final int MAX_RANGE = 600;

    public QueueEntry(UUID playerUuid, String playerName, String tierlistId, int elo, boolean premium) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.tierlistId = tierlistId;
        this.elo = elo;
        this.premium = premium;
        this.joinTime = System.currentTimeMillis();
        this.eloRange = BASE_RANGE;
    }

    // Called every matchmaking tick to expand range over time
    public void tickRange() {
        long secondsWaiting = (System.currentTimeMillis() - joinTime) / 1000;
        int expansions = (int) (secondsWaiting / EXPAND_INTERVAL_SECONDS);
        eloRange = Math.min(BASE_RANGE + (expansions * EXPAND_AMOUNT), MAX_RANGE);
    }

    public boolean canMatch(QueueEntry other) {
        if (!this.tierlistId.equals(other.tierlistId)) return false;
        if (this.premium != other.premium) return false;
        int eloDiff = Math.abs(this.elo - other.elo);
        return eloDiff <= this.eloRange || eloDiff <= other.eloRange;
    }

    public long getWaitSeconds() {
        return (System.currentTimeMillis() - joinTime) / 1000;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getTierlistId() { return tierlistId; }
    public int getElo() { return elo; }
    public boolean isPremium() { return premium; }
    public long getJoinTime() { return joinTime; }
    public int getEloRange() { return eloRange; }
}
