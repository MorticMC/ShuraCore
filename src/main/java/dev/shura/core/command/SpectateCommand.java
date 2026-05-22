package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.SpectatorGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SpectateCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public SpectateCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }

        // /spectate with no args — open GUI list of matches
        if (args.length == 0) {
            if (plugin.getMatchManager().getActiveMatches().isEmpty()) {
                plugin.getMessageService().send(player, "spectate.no-matches");
                return true;
            }
            new SpectatorGui(plugin).open(player);
            return true;
        }

        // /spectate leave
        if (args[0].equalsIgnoreCase("leave")) {
            if (!plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
                player.sendMessage(Component.text("You are not spectating.", NamedTextColor.RED));
                return true;
            }
            plugin.getSpectatorManager().removeSpectator(player);
            plugin.getMessageService().send(player, "spectate.stopped");
            return true;
        }

        // /spectate <player>
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[0]));
            return true;
        }

        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
            plugin.getMessageService().send(player, "spectate.already-spectating");
            return true;
        }

        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) {
            plugin.getMessageService().send(player, "spectate.in-match");
            return true;
        }

        plugin.getSpectatorManager().addSpectator(player, target);
        return true;
    }
}
