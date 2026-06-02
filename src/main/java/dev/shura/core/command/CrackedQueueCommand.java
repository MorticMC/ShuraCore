package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.CrackedQueueGui;
import dev.shura.core.gui.QueueModeSelectGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CrackedQueueCommand implements CommandExecutor, TabCompleter {

    private final ShuraCore plugin;

    public CrackedQueueCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }

        // Check if any ranked gamemodes exist across all tierlists
        boolean hasRankedGamemodes = false;
        for (String tierlistKey : plugin.getKitsArenasConfig().getTierlistIds()) {
            if (!plugin.getKitsArenasConfig().getRankedGamemodes(tierlistKey).isEmpty()) {
                hasRankedGamemodes = true;
                break;
            }
        }

        if (!hasRankedGamemodes) {
            player.sendMessage(Component.text("No ranked queues are currently available.", NamedTextColor.RED));
            return true;
        }

        // If gamemode specified, open hopper GUI for ranked/unranked selection
        if (args.length > 0) {
            String gamemodeFullId = args[0];
            
            // Validate gamemode exists
            boolean found = false;
            for (String tierlistKey : plugin.getKitsArenasConfig().getTierlistIds()) {
                for (String gamemodeKey : plugin.getKitsArenasConfig().getRankedGamemodes(tierlistKey)) {
                    if (plugin.getKitsArenasConfig().getGamemodeFullId(tierlistKey, gamemodeKey).equalsIgnoreCase(gamemodeFullId)) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            
            if (!found) {
                player.sendMessage(Component.text("Invalid gamemode: " + gamemodeFullId, NamedTextColor.RED));
                return true;
            }
            
            new QueueModeSelectGui(plugin, gamemodeFullId, false).open(player);
            return true;
        }

        // No args - open main queue GUI
        new CrackedQueueGui(plugin).open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> fullIds = new ArrayList<>();
            for (String tierlistKey : plugin.getKitsArenasConfig().getTierlistIds()) {
                for (String gamemodeKey : plugin.getKitsArenasConfig().getRankedGamemodes(tierlistKey)) {
                    fullIds.add(plugin.getKitsArenasConfig().getGamemodeFullId(tierlistKey, gamemodeKey));
                }
            }
            return fullIds.stream()
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
