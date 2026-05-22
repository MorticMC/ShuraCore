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

public class EnderChestCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public EnderChestCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("shura.command.enderchest")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }

        if (args.length == 0) {
            player.openInventory(player.getEnderChest());
            SoundUtil.playGuiOpen(player);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("target", args[0]));
            return true;
        }

        player.openInventory(target.getEnderChest());
        player.sendMessage(Component.text("Viewing ", NamedTextColor.GRAY)
                .append(Component.text(target.getName(), NamedTextColor.AQUA))
                .append(Component.text("'s enderchest.", NamedTextColor.GRAY)));
        SoundUtil.playGuiOpen(player);
        return true;
    }
}
