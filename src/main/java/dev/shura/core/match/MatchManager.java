package dev.shura.core.match;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.arena.ArenaCopy;
import dev.shura.core.gui.MatchEndGui;
import dev.shura.core.kit.Kit;
import dev.shura.core.rank.RankManager;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatchManager {

    private final ShuraCore plugin;
    private final Map<UUID, Match> matches = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerMatchMap = new ConcurrentHashMap<>();
    private final RankManager rankManager;
    private final MatchLogger matchLogger;

    public MatchManager(ShuraCore plugin) {
        this.plugin = plugin;
        this.rankManager = new RankManager(plugin);
        this.matchLogger = new MatchLogger(plugin);
    }

    public Match createMatch(Player a, Player b, Kit kit, Arena arena, ArenaCopy copy,
                             MatchType type, MatchFormat format, String tierlistId) {
        Match match = new Match(plugin, a, b, kit, arena, copy, type, format, tierlistId);
        matches.put(match.getMatchId(), match);
        playerMatchMap.put(a.getUniqueId(), match.getMatchId());
        playerMatchMap.put(b.getUniqueId(), match.getMatchId());

        plugin.getProfileManager().getProfile(a).setInMatch(true);
        plugin.getProfileManager().getProfile(b).setInMatch(true);

        // Set matchId on party if players are in a party
        var partyA = plugin.getPartyManager().getParty(a.getUniqueId());
        var partyB = plugin.getPartyManager().getParty(b.getUniqueId());
        if (partyA != null && partyA.isMember(b.getUniqueId())) {
            partyA.setMatchId(match.getMatchId());
        } else if (partyB != null && partyB.isMember(a.getUniqueId())) {
            partyB.setMatchId(match.getMatchId());
        }

        match.start();
        return match;
    }

    public void removeMatch(UUID matchId) {
        Match match = matches.remove(matchId);
        if (match == null) return;
        playerMatchMap.remove(match.getMatchPlayerA().getUuid());
        playerMatchMap.remove(match.getMatchPlayerB().getUuid());

        setNotInMatch(match.getMatchPlayerA().getUuid());
        setNotInMatch(match.getMatchPlayerB().getUuid());

        // Clear matchId on party
        var partyA = plugin.getPartyManager().getParty(match.getMatchPlayerA().getUuid());
        var partyB = plugin.getPartyManager().getParty(match.getMatchPlayerB().getUuid());
        if (partyA != null) partyA.setMatchId(null);
        if (partyB != null) partyB.setMatchId(null);
    }

    private void setNotInMatch(UUID uuid) {
        var profile = plugin.getProfileManager().getProfile(uuid);
        if (profile != null) profile.setInMatch(false);
    }

    public Match getMatch(UUID matchId) {
        return matches.get(matchId);
    }

    public Match getMatchByPlayer(UUID playerUuid) {
        UUID matchId = playerMatchMap.get(playerUuid);
        return matchId != null ? matches.get(matchId) : null;
    }

    public boolean isInMatch(UUID playerUuid) {
        return playerMatchMap.containsKey(playerUuid);
    }

    public void endAllMatches() {
        new ArrayList<>(matches.values()).forEach(Match::forceEnd);
        matches.clear();
        playerMatchMap.clear();
    }

    public void logMatch(Match match, UUID winnerUuid, UUID loserUuid) {
        matchLogger.log(match, winnerUuid, loserUuid);
    }

    public void showMatchEndGui(Match match, MatchPlayer winner, MatchPlayer loser) {
        Player winnerPlayer = org.bukkit.Bukkit.getPlayer(winner.getUuid());
        Player loserPlayer = org.bukkit.Bukkit.getPlayer(loser.getUuid());
        if (winnerPlayer != null) new MatchEndGui(plugin, match, winner, loser, true).open(winnerPlayer);
        if (loserPlayer != null) new MatchEndGui(plugin, match, winner, loser, false).open(loserPlayer);
    }

    public void checkRankChange(Player player, int oldElo, int newElo, String tierlistId) {
    }

    public Collection<Match> getActiveMatches() {
        return Collections.unmodifiableCollection(matches.values());
    }

    public RankManager getRankManager() { return rankManager; }
}
