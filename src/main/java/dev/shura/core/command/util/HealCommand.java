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

public class HealCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public HealCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("shura.command.heal")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }

        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : player;
        if (target == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[0]));
            return true;
        }

        target.setHealth(target.getMaxHealth());
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.getActivePotionEffects().forEach(e -> target.removePotionEffect(e.getType()));

        target.sendMessage(Component.text("You have been healed.", NamedTextColor.GREEN));
        if (!target.equals(player))
            player.sendMessage(Component.text("Healed ", NamedTextColor.GREEN)
                    .append(Component.text(target.getName(), NamedTextColor.AQUA)));

        SoundUtil.playSuccess(target);
        return true;
    }
}
