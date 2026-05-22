package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.QueueSelectorGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class QueueCommand implements CommandExecutor, TabCompleter {

    private final ShuraCore plugin;

    public QueueCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("leave")) {
            if (!plugin.getQueueManager().isInQueue(player.getUniqueId())) {
                plugin.getMessageService().send(player, "errors.unknown-command");
                return true;
            }
            plugin.getQueueManager().leaveAllQueues(player);
            return true;
        }

        // Check if no queue-enabled tierlists exist
        if (plugin.getTierlistManager().getQueueEnabledTierlists().isEmpty()) {
            plugin.getMessageService().send(player, "queue.no-queues-enabled");
            return true;
        }

        new QueueSelectorGui(plugin).open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && "leave".startsWith(args[0].toLowerCase()))
            return List.of("leave");
        return List.of();
    }
}
