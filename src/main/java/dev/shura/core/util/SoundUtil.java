package dev.shura.core.util;

import dev.shura.core.ShuraCore;
import org.bukkit.entity.Player;

public class SoundUtil {

    private static ShuraCore plugin;

    public static void init(ShuraCore instance) {
        plugin = instance;
    }

    private static void play(Player player, String key) {
        if (plugin == null || player == null) return;
        plugin.getScoreboardSoundsConfig().playSound(player, key);
    }

    public static void playCountdownTick(Player player, int secondsRemaining) {
        if (plugin == null || player == null) return;
        plugin.getScoreboardSoundsConfig().playCountdownTick(player, secondsRemaining);
    }

    public static void playMatchStart(Player player)   { play(player, "match-start"); }
    public static void playMatchWin(Player player)     { play(player, "match-win"); }
    public static void playMatchLoss(Player player)    { play(player, "match-loss"); }
    public static void playRoundWin(Player player)     { play(player, "round-win"); }
    public static void playPotionApply(Player player)  { play(player, "potion-apply"); }
    public static void playMatchFound(Player player)   { play(player, "match-found"); }
    public static void playGuiOpen(Player player)      { play(player, "gui-open"); }
    public static void playGuiClick(Player player)     { play(player, "gui-click"); }
    public static void playGuiClose(Player player)     { play(player, "gui-close"); }
    public static void playDuelRequest(Player player)  { play(player, "duel-request"); }
    public static void playDuelAccept(Player player)   { play(player, "duel-accept"); }
    public static void playRankUp(Player player)       { play(player, "rank-up"); }
    public static void playRankDown(Player player)     { play(player, "rank-down"); }
    public static void playPartyInvite(Player player)  { play(player, "party-invite"); }
    public static void playPartyCreate(Player player)  { play(player, "party-create"); }
    public static void playError(Player player)        { play(player, "error"); }
    public static void playSuccess(Player player)      { play(player, "success"); }
    public static void playTeleport(Player player)     { play(player, "teleport"); }
    public static void playMaintenanceOn(Player player) { play(player, "maintenance-on"); }
    public static void playMaintenanceOff(Player player){ play(player, "maintenance-off"); }
    public static void playCommandBlocked(Player player){ play(player, "command-blocked"); }
}
