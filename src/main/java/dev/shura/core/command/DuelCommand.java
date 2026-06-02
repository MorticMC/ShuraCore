package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.duel.DuelSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DuelCommand implements CommandExecutor, TabCompleter {

    private final ShuraCore plugin;
    private final Map<UUID, UUID> duelTargets = new ConcurrentHashMap<>();
    private final Map<UUID, DuelSession> duelSessions = new ConcurrentHashMap<>();

    public DuelCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public Player getDuelTarget(Player player) {
        UUID targetUuid = duelTargets.get(player.getUniqueId());
        return targetUuid != null ? Bukkit.getPlayer(targetUuid) : null;
    }

    public void clearDuelTarget(Player player) {
        duelTargets.remove(player.getUniqueId());
    }
    
    public DuelSession getDuelSession(Player player) {
        return duelSessions.get(player.getUniqueId());
    }
    
    public void setDuelSession(Player player, DuelSession session) {
        duelSessions.put(player.getUniqueId(), session);
    }
    
    public void clearDuelSession(Player player) {
        duelSessions.remove(player.getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send((Player) sender, "errors.players-only");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /duel <player>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }
        
        if (target.equals(player)) {
            player.sendMessage(Component.text("You cannot duel yourself.", NamedTextColor.RED));
            return true;
        }
        
        duelTargets.put(player.getUniqueId(), target.getUniqueId());
        plugin.getGuiEditorManager().openGui(player, "main-menu-duels");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
