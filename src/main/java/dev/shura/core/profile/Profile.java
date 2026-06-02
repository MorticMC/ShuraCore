package dev.shura.core.profile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Profile {

    private final UUID uuid;
    private String name;

    // Per-tierlist match count — key: tierlist ID, value: match count
    private final Map<String, Integer> tierlistMatches = new HashMap<>();

    // Global stats
    private int totalWins;
    private int totalLosses;
    private int currentStreak;
    private int bestStreak;
    private long lastSeen;

    // Transient state — not persisted
    private boolean inMatch;
    private boolean inQueue;
    private boolean vanished;
    private boolean godMode;
    private boolean staffChat;

    // Persistent settings
    private boolean scoreboardEnabled = true;
    private boolean tabEnabled = true;
    private boolean partyInvitesEnabled = true;
    private boolean duelRequestsEnabled = true;

    public Profile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.lastSeen = System.currentTimeMillis();
    }

    public int getMatches(String tierlistId) {
        return tierlistMatches.getOrDefault(tierlistId, 0);
    }

    public void incrementMatches(String tierlistId) {
        tierlistMatches.put(tierlistId, getMatches(tierlistId) + 1);
    }

    public void recordWin() {
        totalWins++;
        currentStreak = Math.max(0, currentStreak) + 1;
        if (currentStreak > bestStreak) bestStreak = currentStreak;
    }

    public void recordLoss() {
        totalLosses++;
        currentStreak = Math.min(0, currentStreak) - 1;
    }

    public double getWinRate() {
        int total = totalWins + totalLosses;
        return total == 0 ? 0.0 : (double) totalWins / total * 100;
    }

    // Getters & Setters
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, Integer> getTierlistMatches() { return tierlistMatches; }
    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }
    public int getTotalLosses() { return totalLosses; }
    public void setTotalLosses(int totalLosses) { this.totalLosses = totalLosses; }
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public int getBestStreak() { return bestStreak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    public boolean isInMatch() { return inMatch; }
    public void setInMatch(boolean inMatch) { this.inMatch = inMatch; }
    public boolean isInQueue() { return inQueue; }
    public void setInQueue(boolean inQueue) { this.inQueue = inQueue; }
    public boolean isVanished() { return vanished; }
    public void setVanished(boolean vanished) { this.vanished = vanished; }
    public boolean isGodMode() { return godMode; }
    public void setGodMode(boolean godMode) { this.godMode = godMode; }
    public boolean isStaffChat() { return staffChat; }
    public void setStaffChat(boolean staffChat) { this.staffChat = staffChat; }
    public boolean isScoreboardEnabled() { return scoreboardEnabled; }
    public void setScoreboardEnabled(boolean scoreboardEnabled) { this.scoreboardEnabled = scoreboardEnabled; }
    public boolean isTabEnabled() { return tabEnabled; }
    public void setTabEnabled(boolean tabEnabled) { this.tabEnabled = tabEnabled; }
    public boolean isPartyInvitesEnabled() { return partyInvitesEnabled; }
    public void setPartyInvitesEnabled(boolean partyInvitesEnabled) { this.partyInvitesEnabled = partyInvitesEnabled; }
    public boolean isDuelRequestsEnabled() { return duelRequestsEnabled; }
    public void setDuelRequestsEnabled(boolean duelRequestsEnabled) { this.duelRequestsEnabled = duelRequestsEnabled; }
}
