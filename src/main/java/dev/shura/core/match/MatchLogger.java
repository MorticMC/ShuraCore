package dev.shura.core.match;

import dev.shura.core.ShuraCore;

import java.util.UUID;

public class MatchLogger {

    private final ShuraCore plugin;

    public MatchLogger(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void log(Match match, UUID winnerUuid, UUID loserUuid) {
        plugin.getDatabaseService().updateAsync(
                "INSERT INTO match_history (id, tierlist_id, kit_id, match_type, winner_uuid, loser_uuid, " +
                "duration, played_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                stmt -> {
                    stmt.setString(1, match.getMatchId().toString());
                    stmt.setString(2, match.getTierlistId());
                    stmt.setString(3, match.getKit().getId());
                    stmt.setString(4, match.getType().name());
                    stmt.setString(5, winnerUuid.toString());
                    stmt.setString(6, loserUuid.toString());
                    stmt.setLong(7, match.getDuration());
                    stmt.setLong(8, System.currentTimeMillis());
                });
    }
}
