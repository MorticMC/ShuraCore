package dev.shura.core.command.util;

import dev.shura.core.ShuraCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class GamemodeCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public GamemodeCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send((org.bukkit.entity.Player) null, "errors.players-only");
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("shura.command.gamemode")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }

        GameMode mode = switch (label.toLowerCase()) {
            case "gmc" -> GameMode.CREATIVE;
            case "gms" -> GameMode.SURVIVAL;
            case "gma" -> GameMode.ADVENTURE;
            case "gmsp" -> GameMode.SPECTATOR;
            default -> null;
        };

        if (mode == null) return true;

        // /gm* <player> — change another player's gamemode
        if (args.length > 0) {
            if (!player.hasPermission("shura.command.gamemode.others")) {
                plugin.getMessageService().send(player, "errors.no-permission");
                return true;
            }
            Player target = org.bukkit.Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[0]));
                return true;
            }
            target.setGameMode(mode);
            target.sendMessage(Component.text("Your gamemode was set to ", NamedTextColor.GRAY)
                    .append(Component.text(mode.name(), NamedTextColor.AQUA))
                    .append(Component.text(" by ", NamedTextColor.GRAY))
                    .append(Component.text(player.getName(), NamedTextColor.WHITE)));
            player.sendMessage(Component.text("Set ", NamedTextColor.GRAY)
                    .append(Component.text(target.getName(), NamedTextColor.AQUA))
                    .append(Component.text("'s gamemode to ", NamedTextColor.GRAY))
                    .append(Component.text(mode.name(), NamedTextColor.WHITE)));
            return true;
        }

        player.setGameMode(mode);
        player.sendMessage(Component.text("Gamemode set to ", NamedTextColor.GRAY)
                .append(Component.text(mode.name(), NamedTextColor.AQUA)));
        return true;
    }
}
