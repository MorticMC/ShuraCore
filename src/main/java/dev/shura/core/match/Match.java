package dev.shura.core.match;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.arena.ArenaCopy;
import dev.shura.core.event.*;
import dev.shura.core.kit.Kit;
import dev.shura.core.potion.PotionApplicator;
import dev.shura.core.profile.Profile;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Match {

    private final ShuraCore plugin;
    private final UUID matchId;
    private final MatchPlayer playerA;
    private final MatchPlayer playerB;
    private final Kit kit;
    private final Arena arena;
    private final ArenaCopy arenaCopy;
    private final MatchType type;
    private final MatchFormat format;
    private final String tierlistId; // null for unranked/duel
    private final Set<UUID> spectators = new HashSet<>();

    private MatchState state;
    private int currentRound;
    private long startTime;
    private BukkitTask countdownTask;

    public Match(ShuraCore plugin, Player a, Player b, Kit kit, Arena arena,
                 ArenaCopy copy, MatchType type, MatchFormat format, String tierlistId) {
        this.plugin = plugin;
        this.matchId = UUID.randomUUID();
        this.playerA = new MatchPlayer(a);
        this.playerB = new MatchPlayer(b);
        this.kit = kit;
        this.arena = arena;
        this.arenaCopy = copy;
        this.type = type;
        this.format = format;
        this.tierlistId = tierlistId;
        this.state = MatchState.WAITING;
        this.currentRound = 1;
    }

    public void start() {
        state = MatchState.STARTING;
        teleportPlayers();
        preparePlayersForMatch();
        startCountdown();
        Bukkit.getPluginManager().callEvent(new MatchStartEvent(this));
    }

    private void teleportPlayers() {
        Player a = getPlayerA();
        Player b = getPlayerB();
        if (a != null) a.teleport(arenaCopy.getSpawnA());
        if (b != null) b.teleport(arenaCopy.getSpawnB());
    }

    private void preparePlayersForMatch() {
        for (MatchPlayer mp : List.of(playerA, playerB)) {
            Player p = Bukkit.getPlayer(mp.getUuid());
            if (p == null) continue;
            p.getInventory().clear();
            p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.setGameMode(GameMode.SURVIVAL);
            p.setAllowFlight(false);
            kit.applyTo(p);
        }
    }

    private void startCountdown() {
        int seconds = plugin.getConfig().getInt("settings.countdown-seconds", 5);
        final int[] remaining = {seconds};

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (remaining[0] <= 0) {
                countdownTask.cancel();
                beginRound();
                return;
            }
            broadcastMatch(Component.text("Match starting in ", NamedTextColor.YELLOW)
                    .append(Component.text(remaining[0], NamedTextColor.AQUA))
                    .append(Component.text("...", NamedTextColor.YELLOW)));
            SoundUtil.playCountdownTick(getPlayerA(), remaining[0]);
            SoundUtil.playCountdownTick(getPlayerB(), remaining[0]);
            remaining[0]--;
        }, 0L, 20L);
    }

    private void beginRound() {
        state = MatchState.IN_PROGRESS;
        startTime = System.currentTimeMillis();

        Player a = getPlayerA();
        Player b = getPlayerB();

        // Apply potion effects and remove potions from inventory
        if (a != null) PotionApplicator.apply(a, kit);
        if (b != null) PotionApplicator.apply(b, kit);

        SoundUtil.playMatchStart(a);
        SoundUtil.playMatchStart(b);

        broadcastMatch(Component.text("Round " + currentRound + " — Fight!", NamedTextColor.GREEN));
        plugin.getBoardManager().updateMatch(this);
    }

    public void handleDeath(Player died) {
        if (state != MatchState.IN_PROGRESS) return;

        MatchPlayer winner = getOpponent(died.getUniqueId());
        MatchPlayer loser = getMatchPlayer(died.getUniqueId());
        if (winner == null || loser == null) return;

        loser.setAlive(false);
        winner.incrementRoundsWon();

        Bukkit.getPluginManager().callEvent(new MatchDeathEvent(this, died));

        broadcastMatch(Component.text(winner.getName(), NamedTextColor.AQUA)
                .append(Component.text(" won round " + currentRound + "! (" + winner.getRoundsWon() + "/" + format.getWinsRequired() + ")", NamedTextColor.YELLOW)));

        SoundUtil.playRoundWin(getPlayerByUuid(winner.getUuid()));

        Bukkit.getPluginManager().callEvent(new MatchRoundEndEvent(this, winner, loser));

        if (winner.getRoundsWon() >= format.getWinsRequired()) {
            endMatch(winner, loser);
        } else {
            currentRound++;
            resetRound();
        }
    }

    private void resetRound() {
        state = MatchState.STARTING;
        teleportPlayers();
        preparePlayersForMatch();
        playerA.setAlive(true);
        playerB.setAlive(true);
        startCountdown();
    }

    private void endMatch(MatchPlayer winner, MatchPlayer loser) {
        state = MatchState.ENDING;

        Player winnerPlayer = getPlayerByUuid(winner.getUuid());
        Player loserPlayer = getPlayerByUuid(loser.getUuid());

        if (type == MatchType.RANKED && tierlistId != null) {
            Profile winnerProfile = plugin.getProfileManager().getProfile(winner.getUuid());
            Profile loserProfile = plugin.getProfileManager().getProfile(loser.getUuid());

            if (winnerProfile != null && loserProfile != null) {
                winnerProfile.incrementMatches(tierlistId);
                loserProfile.incrementMatches(tierlistId);
                winnerProfile.recordWin();
                loserProfile.recordLoss();

                plugin.getProfileManager().saveProfile(winnerProfile);
                plugin.getProfileManager().saveProfile(loserProfile);

                plugin.getMatchManager().logMatch(this, winner.getUuid(), loser.getUuid());
            }
        }

        SoundUtil.playMatchWin(winnerPlayer);
        SoundUtil.playMatchLoss(loserPlayer);

        Bukkit.getPluginManager().callEvent(new MatchEndEvent(this, winner, loser));

        plugin.getMatchManager().showMatchEndGui(this, winner, loser);

        Bukkit.getScheduler().runTaskLater(plugin, () -> finish(winner, loser), 100L);
    }

    public void handleLeave(Player left) {
        if (state == MatchState.FINISHED) return;
        MatchPlayer leaver = getMatchPlayer(left.getUniqueId());
        MatchPlayer remaining = getOpponent(left.getUniqueId());
        if (leaver == null || remaining == null) return;

        leaver.setDisconnected(true);
        broadcastMatch(Component.text(left.getName(), NamedTextColor.RED)
                .append(Component.text(" left the match. " + remaining.getName() + " wins!", NamedTextColor.GRAY)));

        Bukkit.getPluginManager().callEvent(new MatchLeaveEvent(this, left));
        endMatch(remaining, leaver);
    }

    private void finish(MatchPlayer winner, MatchPlayer loser) {
        state = MatchState.FINISHED;

        // Restore players
        Player winnerPlayer = getPlayerByUuid(winner.getUuid());
        Player loserPlayer = getPlayerByUuid(loser.getUuid());
        if (winnerPlayer != null) winner.restore(winnerPlayer);
        if (loserPlayer != null) loser.restore(loserPlayer);

        // Give lobby items back
        if (winnerPlayer != null) dev.shura.core.lobby.LobbyItems.give(plugin, winnerPlayer);
        if (loserPlayer != null) dev.shura.core.lobby.LobbyItems.give(plugin, loserPlayer);

        // Update tablist — players back in lobby
        if (winnerPlayer != null) plugin.getTabManager().update(winnerPlayer);
        if (loserPlayer != null) plugin.getTabManager().update(loserPlayer);

        // Remove spectators
        spectators.forEach(uuid -> {
            Player spec = Bukkit.getPlayer(uuid);
            if (spec != null) plugin.getSpectatorManager().removeSpectator(spec);
        });

        // Reset arena async
        plugin.getArenaManager().resetArena(arenaCopy);
        plugin.getMatchManager().removeMatch(matchId);
        plugin.getBoardManager().updateLobby(winnerPlayer);
        plugin.getBoardManager().updateLobby(loserPlayer);
    }

    public void forceEnd() {
        if (countdownTask != null) countdownTask.cancel();
        state = MatchState.FINISHED;
        Player a = getPlayerA();
        Player b = getPlayerB();
        if (a != null) playerA.restore(a);
        if (b != null) playerB.restore(b);
        plugin.getArenaManager().resetArena(arenaCopy);
    }

    private void broadcastMatch(Component message) {
        Player a = getPlayerA();
        Player b = getPlayerB();
        if (a != null) a.sendMessage(message);
        if (b != null) b.sendMessage(message);
        spectators.forEach(uuid -> {
            Player spec = Bukkit.getPlayer(uuid);
            if (spec != null) spec.sendMessage(message);
        });
    }

    public MatchPlayer getMatchPlayer(UUID uuid) {
        if (playerA.getUuid().equals(uuid)) return playerA;
        if (playerB.getUuid().equals(uuid)) return playerB;
        return null;
    }

    public MatchPlayer getOpponent(UUID uuid) {
        if (playerA.getUuid().equals(uuid)) return playerB;
        if (playerB.getUuid().equals(uuid)) return playerA;
        return null;
    }

    public boolean hasPlayer(UUID uuid) {
        return playerA.getUuid().equals(uuid) || playerB.getUuid().equals(uuid);
    }

    public boolean hasSpectator(UUID uuid) { return spectators.contains(uuid); }
    public void addSpectator(UUID uuid) { spectators.add(uuid); }
    public void removeSpectator(UUID uuid) { spectators.remove(uuid); }

    private Player getPlayerA() { return Bukkit.getPlayer(playerA.getUuid()); }
    private Player getPlayerB() { return Bukkit.getPlayer(playerB.getUuid()); }
    private Player getPlayerByUuid(UUID uuid) { return Bukkit.getPlayer(uuid); }

    public UUID getMatchId() { return matchId; }
    public MatchPlayer getMatchPlayerA() { return playerA; }
    public MatchPlayer getMatchPlayerB() { return playerB; }
    public Kit getKit() { return kit; }
    public Arena getArena() { return arena; }
    public ArenaCopy getArenaCopy() { return arenaCopy; }
    public MatchType getType() { return type; }
    public MatchFormat getFormat() { return format; }
    public String getTierlistId() { return tierlistId; }
    public MatchState getState() { return state; }
    public int getCurrentRound() { return currentRound; }
    public long getStartTime() { return startTime; }
    public long getDuration() { return System.currentTimeMillis() - startTime; }
    public Set<UUID> getSpectators() { return Collections.unmodifiableSet(spectators); }
}
