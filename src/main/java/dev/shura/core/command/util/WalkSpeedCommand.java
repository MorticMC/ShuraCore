package dev.shura.core.command.util;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class WalkSpeedCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public WalkSpeedCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("shura.command.walkspeed")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.getMessageService().send(player, "errors.invalid-args", Map.of("usage", "/walkspeed <0-5> [player]"));
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            plugin.getMessageService().send(player, "errors.invalid-args", Map.of("usage", "/walkspeed <0-5> [player]"));
            return true;
        }

        if (level < 0 || level > 5) {
            player.sendMessage(Component.text("Speed must be between 0 and 5.", NamedTextColor.RED));
            return true;
        }

        Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : player;
        if (target == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[1]));
            return true;
        }

        float speed = level == 0 ? 0.0f : level / 5.0f;
        target.setWalkSpeed(speed);

        target.sendMessage(Component.text("Walk speed set to ", NamedTextColor.GRAY)
                .append(Component.text(level, NamedTextColor.AQUA)));
        if (!target.equals(player))
            player.sendMessage(Component.text("Set walk speed of ", NamedTextColor.GRAY)
                    .append(Component.text(target.getName(), NamedTextColor.AQUA))
                    .append(Component.text(" to " + level, NamedTextColor.GRAY)));

        SoundUtil.playSuccess(target);
        return true;
    }
}
