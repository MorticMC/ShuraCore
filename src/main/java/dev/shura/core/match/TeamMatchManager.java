package dev.shura.core.match;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.arena.ArenaCopy;
import dev.shura.core.kit.Kit;
import dev.shura.core.party.Party;
import dev.shura.core.profile.Profile;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamMatchManager {

    private final ShuraCore plugin;
    private final Map<UUID, TeamMatch> matches = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerMatchMap = new ConcurrentHashMap<>();

    public TeamMatchManager(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public TeamMatch createMatch(List<Player> red, List<Player> blue, Kit kit, Arena arena,
                                 ArenaCopy copy, MatchType type, MatchFormat format) {
        TeamMatch match = new TeamMatch(plugin, red, blue, kit, arena, copy, type, format);
        matches.put(match.getMatchId(), match);

        for (UUID uuid : match.getAllPlayers()) {
            playerMatchMap.put(uuid, match.getMatchId());
            Profile profile = plugin.getProfileManager().getProfile(uuid);
            if (profile != null) profile.setInMatch(true);
        }

        // Tag the involved parties so /party spectate works
        for (UUID uuid : match.getAllPlayers()) {
            Party party = plugin.getPartyManager().getParty(uuid);
            if (party != null) party.setMatchId(match.getMatchId());
        }

        match.start();
        return match;
    }

    public void removeMatch(UUID matchId) {
        TeamMatch match = matches.remove(matchId);
        if (match == null) return;
        for (UUID uuid : match.getAllPlayers()) {
            playerMatchMap.remove(uuid);
            Profile profile = plugin.getProfileManager().getProfile(uuid);
            if (profile != null) profile.setInMatch(false);
            Party party = plugin.getPartyManager().getParty(uuid);
            if (party != null) party.setMatchId(null);
        }
    }

    public TeamMatch getMatch(UUID matchId) { return matches.get(matchId); }

    public TeamMatch getMatchByPlayer(UUID playerUuid) {
        UUID matchId = playerMatchMap.get(playerUuid);
        return matchId != null ? matches.get(matchId) : null;
    }

    public boolean isInMatch(UUID playerUuid) { return playerMatchMap.containsKey(playerUuid); }

    public void endAllMatches() {
        new ArrayList<>(matches.values()).forEach(TeamMatch::forceEnd);
        matches.clear();
        playerMatchMap.clear();
    }

    public Collection<TeamMatch> getActiveMatches() {
        return Collections.unmodifiableCollection(matches.values());
    }
}
