package dev.shura.core.match;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.arena.ArenaCopy;
import dev.shura.core.extra.LobbyItems;
import dev.shura.core.extra.MessageService;
import dev.shura.core.extra.PotionApplicator;
import dev.shura.core.kit.Kit;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

/**
 * A team (N-vs-N) match. Used by party-vs-party duels and party-split fights.
 * <p>
 * RED spawns at the arena's spawn A, BLUE at spawn B. A round ends when every
 * member of one team is eliminated; the surviving team takes the round. First
 * team to {@link MatchFormat#getWinsRequired()} rounds wins the match.
 */
public class TeamMatch {

    private final ShuraCore plugin;
    private final UUID matchId;
    private final Map<Team, List<MatchPlayer>> teams = new EnumMap<>(Team.class);
    private final Map<UUID, Team> teamByPlayer = new HashMap<>();
    private final Kit kit;
    private final Arena arena;
    private final ArenaCopy arenaCopy;
    private final MatchType type;
    private final MatchFormat format;
    private final Set<UUID> spectators = new HashSet<>();
    private final EnumMap<Team, Integer> roundsWon = new EnumMap<>(Team.class);

    private MatchState state = MatchState.WAITING;
    private int currentRound = 1;
    private long startTime;
    private BukkitTask countdownTask;

    public TeamMatch(ShuraCore plugin, List<Player> red, List<Player> blue, Kit kit,
                     Arena arena, ArenaCopy copy, MatchType type, MatchFormat format) {
        this.plugin = plugin;
        this.matchId = UUID.randomUUID();
        this.kit = kit;
        this.arena = arena;
        this.arenaCopy = copy;
        this.type = type;
        this.format = format;

        List<MatchPlayer> redPlayers = new ArrayList<>();
        for (Player p : red) {
            redPlayers.add(new MatchPlayer(p));
            teamByPlayer.put(p.getUniqueId(), Team.RED);
        }
        List<MatchPlayer> bluePlayers = new ArrayList<>();
        for (Player p : blue) {
            bluePlayers.add(new MatchPlayer(p));
            teamByPlayer.put(p.getUniqueId(), Team.BLUE);
        }
        teams.put(Team.RED, redPlayers);
        teams.put(Team.BLUE, bluePlayers);
        roundsWon.put(Team.RED, 0);
        roundsWon.put(Team.BLUE, 0);
    }

    // ==================== LIFECYCLE ====================

    public void start() {
        state = MatchState.STARTING;
        forEachOnlinePlayer((player, team) -> {
            preparePlayer(player);
            player.teleport(spawnFor(team));
        });
        startCountdown();
    }

    private void preparePlayer(Player p) {
        p.getInventory().clear();
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.setGameMode(GameMode.SURVIVAL);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.setFireTicks(0);
        p.setExp(0f);
        p.setLevel(0);
        kit.applyTo(p);
    }

    private void startCountdown() {
        state = MatchState.COUNTDOWN;
        final int[] remaining = {5};
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (remaining[0] <= 0) {
                if (countdownTask != null) countdownTask.cancel();
                forEachOnlinePlayer((p, t) -> SoundUtil.playCountdownFinal(p));
                beginRound();
                return;
            }
            String msg = "&aMatch starting in &e" + remaining[0] + "&a...";
            broadcast(MessageService.colorizeComponent(msg));
            forEachOnlinePlayer((p, t) -> SoundUtil.playCountdownTick(p));
            remaining[0]--;
        }, 0L, 20L);
    }

    private void beginRound() {
        state = MatchState.IN_PROGRESS;
        startTime = System.currentTimeMillis();
        forEachOnlinePlayer((p, t) -> PotionApplicator.apply(p, kit));
        broadcast(MessageService.colorizeComponent("&a&lFIGHT!"));
        sendScoreTitle();
    }

    // ==================== COMBAT ====================

    public void handleDeath(Player died) {
        if (state != MatchState.IN_PROGRESS) return;
        MatchPlayer mp = getMatchPlayer(died.getUniqueId());
        if (mp == null || !mp.isAlive()) return;

        mp.setAlive(false);
        Team team = teamByPlayer.get(died.getUniqueId());

        // Fake death — keep them in the arena as a corpse-spectator until the round ends
        Bukkit.getScheduler().runTask(plugin, () -> {
            died.setHealth(died.getMaxHealth());
            died.setFoodLevel(20);
            died.setFireTicks(0);
            died.setGameMode(GameMode.SPECTATOR);
        });

        broadcast(Component.text(died.getName(), team.getColor())
                .append(Component.text(" has been eliminated.", NamedTextColor.GRAY)));

        if (isTeamEliminated(team)) {
            roundWon(team.other());
        }
    }

    public void handleLeave(Player left) {
        if (state == MatchState.FINISHED) return;
        MatchPlayer mp = getMatchPlayer(left.getUniqueId());
        if (mp == null) return;
        mp.setAlive(false);
        mp.setDisconnected(true);
        Team team = teamByPlayer.get(left.getUniqueId());

        broadcast(Component.text(left.getName(), team.getColor())
                .append(Component.text(" left the match.", NamedTextColor.GRAY)));

        if (state == MatchState.IN_PROGRESS && isTeamEliminated(team)) {
            roundWon(team.other());
        } else if (bothTeamsHaveNoOnlinePlayers()) {
            forceEnd();
            plugin.getTeamMatchManager().removeMatch(matchId);
        }
    }

    private void roundWon(Team winner) {
        if (state == MatchState.ENDING || state == MatchState.FINISHED) return;
        state = MatchState.ENDING;
        roundsWon.merge(winner, 1, Integer::sum);

        broadcast(Component.text("Team " + winner.getDisplay(), winner.getColor())
                .append(Component.text(" won round " + currentRound + "! ", NamedTextColor.YELLOW))
                .append(Component.text("(" + roundsWon.get(Team.RED) + " - " + roundsWon.get(Team.BLUE) + ")", NamedTextColor.GRAY)));
        showRoundTitle(winner);

        if (roundsWon.get(winner) >= format.getWinsRequired()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> endMatch(winner), 40L);
        } else {
            currentRound++;
            Bukkit.getScheduler().runTaskLater(plugin, this::resetRound, 40L);
        }
    }

    private void resetRound() {
        if (state == MatchState.FINISHED) return;
        state = MatchState.STARTING;
        for (List<MatchPlayer> list : teams.values()) {
            for (MatchPlayer mp : list) {
                if (!mp.isDisconnected()) mp.setAlive(true);
            }
        }
        forEachOnlinePlayer((player, team) -> {
            if (getMatchPlayer(player.getUniqueId()).isAlive()) {
                preparePlayer(player);
                player.teleport(spawnFor(team));
            }
        });
        startCountdown();
    }

    private void endMatch(Team winner) {
        state = MatchState.ENDING;

        forEachOnlinePlayer((player, team) -> {
            boolean won = team == winner;
            String title = won ? "&a&lVICTORY" : "&c&lDEFEAT";
            player.showTitle(Title.title(
                    MessageService.colorizeComponent(title),
                    MessageService.colorizeComponent("&7Team " + winner.getDisplay() + " wins " +
                            roundsWon.get(Team.RED) + " - " + roundsWon.get(Team.BLUE)),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))));
            if (won) SoundUtil.playMatchWin(player);
            else SoundUtil.playMatchLoss(player);
        });

        Bukkit.getScheduler().runTaskLater(plugin, this::finish, 100L);
    }

    private void finish() {
        state = MatchState.FINISHED;

        // Remove spectators first
        new HashSet<>(spectators).forEach(uuid -> {
            Player spec = Bukkit.getPlayer(uuid);
            if (spec != null) plugin.getSpectatorManager().removeSpectator(spec);
        });

        forEachOnlinePlayer((player, team) -> {
            MatchPlayer mp = getMatchPlayer(player.getUniqueId());
            mp.restore(player);
            plugin.getSpawnManager().teleportToSpawn(player);
            LobbyItems.give(plugin, player);
            plugin.getTabManager().update(player);
        });

        plugin.getArenaManager().resetArena(arenaCopy);
        plugin.getTeamMatchManager().removeMatch(matchId);
    }

    public void forceEnd() {
        if (countdownTask != null) countdownTask.cancel();
        state = MatchState.FINISHED;
        forEachOnlinePlayer((player, team) -> {
            MatchPlayer mp = getMatchPlayer(player.getUniqueId());
            mp.restore(player);
        });
        plugin.getArenaManager().resetArena(arenaCopy);
    }

    // ==================== TITLES / SCOREBOARD ====================

    private void showRoundTitle(Team winner) {
        forEachOnlinePlayer((player, team) -> {
            boolean won = team == winner;
            player.showTitle(Title.title(
                    MessageService.colorizeComponent(won ? "&aRound won!" : "&cRound lost"),
                    MessageService.colorizeComponent("&7" + roundsWon.get(Team.RED) + " - " + roundsWon.get(Team.BLUE)),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(300))));
        });
    }

    private void sendScoreTitle() {
        Component bar = Component.text("Team Red ", Team.RED.getColor())
                .append(Component.text(roundsWon.get(Team.RED) + " - " + roundsWon.get(Team.BLUE), NamedTextColor.GRAY))
                .append(Component.text(" Team Blue", Team.BLUE.getColor()));
        forEachOnlinePlayer((player, team) -> player.sendActionBar(bar));
    }

    // ==================== HELPERS ====================

    private interface PlayerTeamConsumer { void accept(Player player, Team team); }

    private void forEachOnlinePlayer(PlayerTeamConsumer consumer) {
        for (Map.Entry<Team, List<MatchPlayer>> e : teams.entrySet()) {
            for (MatchPlayer mp : e.getValue()) {
                Player p = Bukkit.getPlayer(mp.getUuid());
                if (p != null && p.isOnline()) consumer.accept(p, e.getKey());
            }
        }
    }

    private Location spawnFor(Team team) {
        return team == Team.RED ? arenaCopy.getSpawnA() : arenaCopy.getSpawnB();
    }

    private boolean isTeamEliminated(Team team) {
        return teams.get(team).stream().noneMatch(MatchPlayer::isAlive);
    }

    private boolean bothTeamsHaveNoOnlinePlayers() {
        return teams.values().stream().flatMap(List::stream)
                .noneMatch(mp -> {
                    Player p = Bukkit.getPlayer(mp.getUuid());
                    return p != null && p.isOnline() && !mp.isDisconnected();
                });
    }

    public void broadcast(Component message) {
        forEachOnlinePlayer((player, team) -> player.sendMessage(message));
        spectators.forEach(uuid -> {
            Player spec = Bukkit.getPlayer(uuid);
            if (spec != null) spec.sendMessage(message);
        });
    }

    public MatchPlayer getMatchPlayer(UUID uuid) {
        Team team = teamByPlayer.get(uuid);
        if (team == null) return null;
        return teams.get(team).stream().filter(mp -> mp.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    public boolean hasPlayer(UUID uuid) { return teamByPlayer.containsKey(uuid); }
    public Team getTeam(UUID uuid) { return teamByPlayer.get(uuid); }

    public boolean hasSpectator(UUID uuid) { return spectators.contains(uuid); }
    public void addSpectator(UUID uuid) { spectators.add(uuid); }
    public void removeSpectator(UUID uuid) { spectators.remove(uuid); }
    public Set<UUID> getSpectators() { return Collections.unmodifiableSet(spectators); }

    public Set<UUID> getAllPlayers() { return Collections.unmodifiableSet(teamByPlayer.keySet()); }

    public UUID getMatchId() { return matchId; }
    public Kit getKit() { return kit; }
    public Arena getArena() { return arena; }
    public ArenaCopy getArenaCopy() { return arenaCopy; }
    public MatchType getType() { return type; }
    public MatchFormat getFormat() { return format; }
    public MatchState getState() { return state; }
    public int getCurrentRound() { return currentRound; }
}
