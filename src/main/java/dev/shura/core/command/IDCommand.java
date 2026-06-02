package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.MessageService;
import dev.shura.core.id.IDManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class IDCommand implements CommandExecutor, TabCompleter {

    private final ShuraCore plugin;
    private final IDManager idManager;

    public IDCommand(ShuraCore plugin) {
        this.plugin = plugin;
        this.idManager = plugin.getIDManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            idManager.openIDMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("order")) {
            idManager.startIDCreation(player);
            return true;
        }

        player.sendMessage(MessageService.colorize("&#FF0000Usage: /id [order]"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("order");
        }
        return completions;
    }
}
