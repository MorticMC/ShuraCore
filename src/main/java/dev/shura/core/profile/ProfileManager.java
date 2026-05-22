package dev.shura.core.profile;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.shura.core.ShuraCore;
import dev.shura.core.database.DatabaseService;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ProfileManager {

    private final ShuraCore plugin;
    private final DatabaseService db;
    private final Map<UUID, Profile> cache = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public ProfileManager(ShuraCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseService();
    }

    public CompletableFuture<Profile> loadProfile(UUID uuid, String name) {
        return db.query("SELECT * FROM profiles WHERE uuid = ?",
                stmt -> stmt.setString(1, uuid.toString()),
                rs -> {
                    try {
                        if (rs.next()) {
                            return deserialize(rs);
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to load profile: " + uuid, e);
                    }
                    return null;
                }).thenCompose(profile -> {
                    if (profile != null) {
                        profile.setName(name);
                        cache.put(uuid, profile);
                        return CompletableFuture.completedFuture(profile);
                    }
                    return createProfile(uuid, name);
                });
    }

    private CompletableFuture<Profile> createProfile(UUID uuid, String name) {
        Profile profile = new Profile(uuid, name);
        cache.put(uuid, profile);
        return db.update("INSERT INTO profiles (uuid, name, tierlist_elo, tierlist_matches, stats, last_seen) VALUES (?, ?, '{}', '{}', '{}', ?)",
                stmt -> {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, name);
                    stmt.setLong(3, System.currentTimeMillis());
                }).thenApply(i -> profile);
    }

    public void saveProfile(Profile profile) {
        String eloJson = gson.toJson(profile.getTierlistElo());
        String matchesJson = gson.toJson(profile.getTierlistMatches());
        String statsJson = gson.toJson(buildStatsMap(profile));

        db.updateAsync("UPDATE profiles SET name = ?, tierlist_elo = ?, tierlist_matches = ?, stats = ?, last_seen = ? WHERE uuid = ?",
                stmt -> {
                    stmt.setString(1, profile.getName());
                    stmt.setString(2, eloJson);
                    stmt.setString(3, matchesJson);
                    stmt.setString(4, statsJson);
                    stmt.setLong(5, System.currentTimeMillis());
                    stmt.setString(6, profile.getUuid().toString());
                });
    }

    public void unloadProfile(UUID uuid) {
        Profile profile = cache.get(uuid);
        if (profile != null) {
            saveProfile(profile);
            cache.remove(uuid);
        }
    }

    public Profile getProfile(UUID uuid) {
        return cache.get(uuid);
    }

    public Profile getProfile(Player player) {
        return cache.get(player.getUniqueId());
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public Collection<Profile> getCachedProfiles() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public CompletableFuture<List<Profile>> getTopByTierlist(String tierlistId, int limit) {
        return db.query("SELECT * FROM profiles ORDER BY JSON_EXTRACT(tierlist_elo, '$." + tierlistId + "') DESC LIMIT ?",
                stmt -> stmt.setInt(1, limit),
                rs -> {
                    List<Profile> profiles = new ArrayList<>();
                    try {
                        while (rs.next()) profiles.add(deserialize(rs));
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to fetch top profiles.", e);
                    }
                    return profiles;
                });
    }

    private Profile deserialize(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        Profile profile = new Profile(uuid, rs.getString("name"));

        Map<String, Integer> eloMap = gson.fromJson(rs.getString("tierlist_elo"),
                new TypeToken<Map<String, Integer>>() {}.getType());
        Map<String, Integer> matchesMap = gson.fromJson(rs.getString("tierlist_matches"),
                new TypeToken<Map<String, Integer>>() {}.getType());
        Map<String, Object> statsMap = gson.fromJson(rs.getString("stats"),
                new TypeToken<Map<String, Object>>() {}.getType());

        if (eloMap != null) profile.getTierlistElo().putAll(eloMap);
        if (matchesMap != null) profile.getTierlistMatches().putAll(matchesMap);
        if (statsMap != null) applyStats(profile, statsMap);

        profile.setLastSeen(rs.getLong("last_seen"));
        return profile;
    }

    private Map<String, Object> buildStatsMap(Profile p) {
        Map<String, Object> map = new HashMap<>();
        map.put("wins", p.getTotalWins());
        map.put("losses", p.getTotalLosses());
        map.put("streak", p.getCurrentStreak());
        map.put("bestStreak", p.getBestStreak());
        map.put("scoreboardEnabled", p.isScoreboardEnabled());
        map.put("tabEnabled", p.isTabEnabled());
        map.put("partyInvitesEnabled", p.isPartyInvitesEnabled());
        map.put("duelRequestsEnabled", p.isDuelRequestsEnabled());
        return map;
    }

    private void applyStats(Profile profile, Map<String, Object> stats) {
        profile.setTotalWins(((Number) stats.getOrDefault("wins", 0)).intValue());
        profile.setTotalLosses(((Number) stats.getOrDefault("losses", 0)).intValue());
        profile.setCurrentStreak(((Number) stats.getOrDefault("streak", 0)).intValue());
        profile.setBestStreak(((Number) stats.getOrDefault("bestStreak", 0)).intValue());
        profile.setScoreboardEnabled((Boolean) stats.getOrDefault("scoreboardEnabled", true));
        profile.setTabEnabled((Boolean) stats.getOrDefault("tabEnabled", true));
        profile.setPartyInvitesEnabled((Boolean) stats.getOrDefault("partyInvitesEnabled", true));
        profile.setDuelRequestsEnabled((Boolean) stats.getOrDefault("duelRequestsEnabled", true));
    }
}
