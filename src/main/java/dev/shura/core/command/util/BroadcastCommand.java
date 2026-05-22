package dev.shura.core.command.util;

import dev.shura.core.ShuraCore;
import dev.shura.core.message.MessageService;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class BroadcastCommand implements CommandExecutor {

    private final ShuraCore plugin;

    public BroadcastCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player && !player.hasPermission("shura.command.broadcast")) {
            plugin.getMessageService().send(player, "errors.no-permission");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /broadcast <message>", NamedTextColor.RED));
            return true;
        }

        String raw = String.join(" ", args);
        Component message = Component.text("[Broadcast] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(MessageService.colorizeComponent(raw));

        Bukkit.broadcast(message);
        Bukkit.getOnlinePlayers().forEach(p -> SoundUtil.playSuccess(p));
        return true;
    }
}
