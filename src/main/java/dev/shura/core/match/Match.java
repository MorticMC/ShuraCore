package dev.shura.core.match;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.arena.ArenaCopy;
import dev.shura.core.event.MainEvents.*;
import dev.shura.core.kit.Kit;
import dev.shura.core.extra.PotionApplicator;
import dev.shura.core.extra.MessageService;
import dev.shura.core.profile.Profile;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
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
        dragPlayersToSpawns(() -> {
            preparePlayersForMatch();
            startCountdown();
            Bukkit.getPluginManager().callEvent(new MatchStartEvent(this));
        });
    }

    private void teleportPlayersToSpawns() {
        Player a = getPlayerA();
        Player b = getPlayerB();
        if (a != null) a.teleport(arenaCopy.getSpawnA());
        if (b != null) b.teleport(arenaCopy.getSpawnB());
    }

    private void dragPlayersToSpawns(Runnable onComplete) {
        Player a = getPlayerA();
        Player b = getPlayerB();
        
        if (a == null && b == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        
        final boolean[] aComplete = {a == null};
        final boolean[] bComplete = {b == null};
        
        Runnable checkBothComplete = () -> {
            if (aComplete[0] && bComplete[0] && onComplete != null) {
                onComplete.run();
            }
        };
        
        if (a != null) {
            dev.shura.core.util.PlayerDragUtil.dragPlayer(a, arenaCopy.getSpawnA(), plugin, () -> {
                aComplete[0] = true;
                checkBothComplete.run();
            });
        }
        
        if (b != null) {
            dev.shura.core.util.PlayerDragUtil.dragPlayer(b, arenaCopy.getSpawnB(), plugin, () -> {
                bComplete[0] = true;
                checkBothComplete.run();
            });
        }
    }

    private void preparePlayersForMatch() {
        Player a = getPlayerA();
        Player b = getPlayerB();
        if (a != null) preparePlayer(a);
        if (b != null) preparePlayer(b);
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
        kit.applyTo(p);
    }

    private void startCountdown() {
        state = MatchState.COUNTDOWN;
        plugin.getBoardManager().updateMatch(this);
        
        final int[] remaining = {5};

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (remaining[0] == 0) {
                countdownTask.cancel();
                SoundUtil.playCountdownFinal(getPlayerA());
                SoundUtil.playCountdownFinal(getPlayerB());
                beginRound();
                return;
            }
            
            if (remaining[0] > 0) {
                String countdownKey = "match.countdown-" + remaining[0];
                broadcastMatch(MessageService.colorizeComponent(
                        plugin.getMessageService().getRaw(countdownKey)));
                SoundUtil.playCountdownTick(getPlayerA());
                SoundUtil.playCountdownTick(getPlayerB());
            }
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

        broadcastMatch(MessageService.colorizeComponent(
                plugin.getMessageService().getRaw("match.fight")));
        plugin.getBoardManager().updateMatch(this);
    }

    public void handleDeath(Player died) {
        if (state != MatchState.IN_PROGRESS) return;

        MatchPlayer winner = getOpponent(died.getUniqueId());
        MatchPlayer loser = getMatchPlayer(died.getUniqueId());
        if (winner == null || loser == null) return;

        state = MatchState.ENDING;
        loser.setAlive(false);
        winner.incrementRoundsWon();

        Bukkit.getPluginManager().callEvent(new MatchDeathEvent(this, died));

        // Fake death - teleport and reset without respawn screen
        Bukkit.getScheduler().runTask(plugin, () -> {
            died.setHealth(died.getMaxHealth());
            died.setFoodLevel(20);
            died.setFireTicks(0);
        });

        broadcastMatch(Component.text(winner.getName(), NamedTextColor.AQUA)
                .append(Component.text(" won round " + currentRound + "! (" + winner.getRoundsWon() + "/" + format.getWinsRequired() + ")", NamedTextColor.YELLOW)));

        showRoundWinTitle(winner, loser);
        plugin.getScoreboardSoundsConfig().playSound(getPlayerByUuid(winner.getUuid()), "round-win");
        plugin.getScoreboardSoundsConfig().playSound(getPlayerByUuid(loser.getUuid()), "round-loss");
        Bukkit.getPluginManager().callEvent(new MatchRoundEndEvent(this, winner, loser));

        if (winner.getRoundsWon() >= format.getWinsRequired()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> endMatch(winner, loser), 40L);
        } else {
            currentRound++;
            Bukkit.getScheduler().runTaskLater(plugin, this::resetRound, 40L);
        }
    }

    private void resetRound() {
        state = MatchState.STARTING;
        playerA.setAlive(true);
        playerB.setAlive(true);
        
        dragPlayersToSpawns(() -> {
            Player a = getPlayerA();
            Player b = getPlayerB();
            
            if (a != null) {
                preparePlayer(a);
                a.teleport(arenaCopy.getSpawnA());
            }
            if (b != null) {
                preparePlayer(b);
                b.teleport(arenaCopy.getSpawnB());
            }
            
            plugin.getBoardManager().updateMatch(this);
            beginRound();
        });
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

        // Teleport to spawn
        if (winnerPlayer != null) plugin.getSpawnManager().teleportToSpawn(winnerPlayer);
        if (loserPlayer != null) plugin.getSpawnManager().teleportToSpawn(loserPlayer);

        // Give lobby items back
        if (winnerPlayer != null) dev.shura.core.extra.LobbyItems.give(plugin, winnerPlayer);
        if (loserPlayer != null) dev.shura.core.extra.LobbyItems.give(plugin, loserPlayer);

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
        if (winnerPlayer != null) plugin.getBoardManager().updateLobby(winnerPlayer);
        if (loserPlayer != null) plugin.getBoardManager().updateLobby(loserPlayer);
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

    private void showRoundWinTitle(MatchPlayer winner, MatchPlayer loser) {
        Player winnerPlayer = getPlayerByUuid(winner.getUuid());
        Player loserPlayer = getPlayerByUuid(loser.getUuid());
        
        String winTitle = plugin.getScoreboardSoundsConfig().getRoundWinTitle();
        String lossTitle = plugin.getScoreboardSoundsConfig().getRoundLossTitle();
        
        String winnerName = winner.getName();
        String loserName = loser.getName();
        int winnerScore = winner.getRoundsWon();
        int loserScore = loser.getRoundsWon();
        
        // Extract individual colored emojis from config strings
        String[] winFrames = winTitle.split("(?<=\uD83C\uDFC6)");
        String[] lossFrames = lossTitle.split("(?<=\u2620)");
        
        int maxFrames = Math.max(winFrames.length, lossFrames.length);
        
        // Animate both titles with subtitle animation
        for (int i = 0; i < maxFrames; i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String subtitle = buildAnimatedSubtitle(winnerName, loserName, winnerScore, loserScore, index);
                Component subtitleComp = MessageService.colorizeComponent(subtitle);
                
                if (winnerPlayer != null && index < winFrames.length) {
                    Component titleComp = MessageService.colorizeComponent(winFrames[index]);
                    winnerPlayer.showTitle(Title.title(titleComp, subtitleComp,
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)));
                }
                
                if (loserPlayer != null && index < lossFrames.length) {
                    Component titleComp = MessageService.colorizeComponent(lossFrames[index]);
                    loserPlayer.showTitle(Title.title(titleComp, subtitleComp,
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)));
                }
            }, i * 2L);
        }
        
        // Continue with bold animation after emoji animation
        int boldStart = maxFrames * 2;
        for (int i = 0; i < winnerName.length(); i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String subtitle = buildBoldSubtitle(winnerName, loserName, winnerScore, loserScore, index, true);
                Component subtitleComp = MessageService.colorizeComponent(subtitle);
                
                if (winnerPlayer != null) {
                    winnerPlayer.showTitle(Title.title(MessageService.colorizeComponent(winFrames[winFrames.length - 1]), subtitleComp,
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)));
                }
                if (loserPlayer != null) {
                    loserPlayer.showTitle(Title.title(MessageService.colorizeComponent(lossFrames[lossFrames.length - 1]), subtitleComp,
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)));
                }
            }, boldStart + i);
        }
        
        // Animate loser name bold
        int loserBoldStart = boldStart + winnerName.length();
        for (int i = 0; i < loserName.length(); i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String subtitle = buildBoldSubtitle(winnerName, loserName, winnerScore, loserScore, index, false);
                Component subtitleComp = MessageService.colorizeComponent(subtitle);
                
                if (winnerPlayer != null) {
                    winnerPlayer.showTitle(Title.title(MessageService.colorizeComponent(winFrames[winFrames.length - 1]), subtitleComp,
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)));
                }
                if (loserPlayer != null) {
                    loserPlayer.showTitle(Title.title(MessageService.colorizeComponent(lossFrames[lossFrames.length - 1]), subtitleComp,
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)));
                }
            }, loserBoldStart + i);
        }
        
        // Final normalized subtitle
        int finalFrame = loserBoldStart + loserName.length();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String finalSubtitle = "&6" + winnerName + " " + winnerScore + " &8- &6" + loserScore + " " + loserName;
            Component subtitleComp = MessageService.colorizeComponent(finalSubtitle);
            
            if (winnerPlayer != null) {
                winnerPlayer.showTitle(Title.title(MessageService.colorizeComponent(winFrames[winFrames.length - 1]), subtitleComp,
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)));
            }
            if (loserPlayer != null) {
                loserPlayer.showTitle(Title.title(MessageService.colorizeComponent(lossFrames[lossFrames.length - 1]), subtitleComp,
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)));
            }
        }, finalFrame);
    }
    
    private String buildAnimatedSubtitle(String winnerName, String loserName, int winnerScore, int loserScore, int step) {
        if (step < winnerName.length()) {
            String colored = "&6" + winnerName.substring(0, step + 1) + "&e" + winnerName.substring(step + 1);
            return colored + " " + winnerScore + " &8- &f" + loserScore + " " + loserName;
        } else {
            int loserStep = step - winnerName.length();
            if (loserStep < loserName.length()) {
                String colored = "&6" + loserName.substring(0, loserStep + 1) + "&e" + loserName.substring(loserStep + 1);
                return "&6" + winnerName + " " + winnerScore + " &8- &f" + loserScore + " " + colored;
            }
        }
        return "&6" + winnerName + " " + winnerScore + " &8- &f" + loserScore + " &6" + loserName;
    }
    
    private String buildBoldSubtitle(String winnerName, String loserName, int winnerScore, int loserScore, int step, boolean isWinner) {
        if (isWinner) {
            String bold = "&e&l" + winnerName.charAt(step) + "&6" + winnerName.substring(step + 1);
            String prefix = step > 0 ? "&6" + winnerName.substring(0, step) : "";
            return prefix + bold + " " + winnerScore + " &8- &f" + loserScore + " &6" + loserName;
        } else {
            String bold = "&e&l" + loserName.charAt(step) + "&6" + loserName.substring(step + 1);
            String prefix = step > 0 ? "&6" + loserName.substring(0, step) : "";
            return "&6" + winnerName + " " + winnerScore + " &8- &f" + loserScore + " " + prefix + bold;
        }
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
