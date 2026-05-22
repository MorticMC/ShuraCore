package dev.shura.core.command.util;

import dev.shura.core.ShuraCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class MaintenanceCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public MaintenanceCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player && !player.hasPermission("shura.command.maintenance")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /maintenance <on|off>", NamedTextColor.RED));
            return true;
        }

        Player executor = sender instanceof Player p ? p : null;

        switch (args[0].toLowerCase()) {
            case "on", "enable", "true" -> plugin.getMaintenanceManager().setEnabled(true, executor);
            case "off", "disable", "false" -> plugin.getMaintenanceManager().setEnabled(false, executor);
            default -> sender.sendMessage(Component.text("Usage: /maintenance <on|off>", NamedTextColor.RED));
        }
        return true;
    }
}
