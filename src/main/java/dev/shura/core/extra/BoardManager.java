package dev.shura.core.extra;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.ScoreboardSoundsConfig;
import dev.shura.core.match.Match;
import dev.shura.core.profile.Profile;
import dev.shura.core.queue.QueueEntry;
import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;
import dev.shura.core.extra.MessageService;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BoardManager {

    private final ShuraCore plugin;
    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();

    public BoardManager(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void createBoard(Player player) {
        FastBoard board = new FastBoard(player);
        boards.put(player.getUniqueId(), board);
        updateLobby(player);
    }

    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null && !board.isDeleted()) board.delete();
    }

    public void updateLobby(Player player) {
        FastBoard board = boards.get(player.getUniqueId());
        if (board == null || board.isDeleted()) return;

        ScoreboardSoundsConfig cfg = plugin.getScoreboardSoundsConfig();
        if (!cfg.isBoardEnabled("lobby")) { board.delete(); return; }

        Profile profile = plugin.getProfileManager().getProfile(player);
        String wins   = profile != null ? String.valueOf(profile.getTotalWins())   : "0";
        String losses = profile != null ? String.valueOf(profile.getTotalLosses()) : "0";

        Map<String, String> placeholders = Map.of(
                "{players}", String.valueOf(player.getServer().getOnlinePlayers().size()),
                "{matches}", String.valueOf(plugin.getMatchManager().getActiveMatches().size()),
                "{wins}",    wins,
                "{losses}",  losses,
                "{server}",  cfg.serverName()
        );

        board.updateTitle(cfg.boardTitle("lobby"));
        board.updateLines(buildLines(cfg.boardLines("lobby"), placeholders));
    }

    public void updateMatch(Match match) {
        updateMatchBoard(match.getMatchPlayerA().getUuid(), match);
        updateMatchBoard(match.getMatchPlayerB().getUuid(), match);
    }

    private void updateMatchBoard(UUID uuid, Match match) {
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player == null) return;
        FastBoard board = boards.get(uuid);
        if (board == null || board.isDeleted()) return;

        ScoreboardSoundsConfig cfg = plugin.getScoreboardSoundsConfig();
        if (!cfg.isBoardEnabled("match")) { board.delete(); return; }

        var self     = match.getMatchPlayer(uuid);
        var opponent = match.getOpponent(uuid);
        if (self == null || opponent == null) return;

        Map<String, String> placeholders = Map.of(
                "{kit}",            match.getKit().getName(),
                "{format}",         match.getFormat().getDisplay(),
                "{your_wins}",      String.valueOf(self.getRoundsWon()),
                "{opponent}",       opponent.getName(),
                "{opponent_wins}",  String.valueOf(opponent.getRoundsWon()),
                "{round}",          String.valueOf(match.getCurrentRound()),
                "{server}",         cfg.serverName()
        );

        board.updateTitle(cfg.boardTitle("match"));
        board.updateLines(buildLines(cfg.boardLines("match"), placeholders));
    }

    /** Restores lobby board when not in queue/match; otherwise keeps the active board. */
    public void refreshForPlayer(Player player) {
        Profile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null && profile.isInMatch()) return;

        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            var entries = plugin.getQueueManager().getEntries(player.getUniqueId());
            if (!entries.isEmpty()) {
                updateQueue(player, entries.iterator().next());
                return;
            }
        }
        updateLobby(player);
    }

    public void updateQueue(Player player, QueueEntry entry) {
        FastBoard board = boards.get(player.getUniqueId());
        if (board == null || board.isDeleted()) return;

        ScoreboardSoundsConfig cfg = plugin.getScoreboardSoundsConfig();
        if (!cfg.isBoardEnabled("queue")) { board.delete(); return; }

        Map<String, String> placeholders = Map.of(
                "{tierlist}", entry.getTierlistId(),
                "{waiting}",  String.valueOf(entry.getWaitSeconds()),
                "{server}",   cfg.serverName()
        );

        board.updateTitle(cfg.boardTitle("queue"));
        board.updateLines(buildLines(cfg.boardLines("queue"), placeholders));
    }

    private List<Component> buildLines(List<String> lines, Map<String, String> placeholders) {
        List<Component> components = new ArrayList<>();
        for (String line : lines) {
            for (Map.Entry<String, String> entry : placeholders.entrySet())
                line = line.replace(entry.getKey(), entry.getValue());
            components.add(MessageService.colorizeComponent(line));
        }
        return components;
    }

    public boolean hasBoard(UUID uuid) { return boards.containsKey(uuid); }
}
