package dev.shura.core.command;

import dev.shura.core.ShuraCore;
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
import java.util.stream.Collectors;

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final ShuraCore plugin;

    public PartyCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Party Commands:", NamedTextColor.GOLD));
            player.sendMessage(Component.text("/party create - Create a party", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/party invite <player> - Invite a player", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/party disband - Disband your party", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/party leave - Leave the party", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/party match - Start a party match", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/party duel - Challenge another party", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (plugin.getPartyManager().isInParty(player.getUniqueId())) {
                    player.sendMessage(Component.text("You are already in a party.", NamedTextColor.RED));
                    return true;
                }
                plugin.getGuiEditorManager().openGui(player, "party");
            }

            case "disband" -> {
                var party = plugin.getPartyManager().getParty(player.getUniqueId());
                if (party == null) {
                    player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
                    return true;
                }
                if (!party.isLeader(player.getUniqueId())) {
                    player.sendMessage(Component.text("Only the party leader can disband.", NamedTextColor.RED));
                    return true;
                }
                plugin.getGuiEditorManager().openGui(player, "party-close");
            }

            case "leave" -> plugin.getPartyManager().leaveParty(player);

            case "accept" -> plugin.getPartyManager().acceptInvite(player);

            case "invite" -> {
                if (args.length < 2) {
                    plugin.getMessageService().send(player, "errors.invalid-args",
                            Map.of("usage", "/party invite <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getMessageService().send(player, "errors.player-not-found",
                            Map.of("target", args[1]));
                    return true;
                }
                plugin.getPartyManager().invitePlayer(player, target);
            }

            case "kick" -> {
                if (args.length < 2) {
                    plugin.getMessageService().send(player, "errors.invalid-args",
                            Map.of("usage", "/party kick <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getMessageService().send(player, "errors.player-not-found",
                            Map.of("target", args[1]));
                    return true;
                }
                plugin.getPartyManager().kickPlayer(player, target);
            }

            case "transfer" -> {
                if (args.length < 2) {
                    plugin.getMessageService().send(player, "errors.invalid-args",
                            Map.of("usage", "/party transfer <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getMessageService().send(player, "errors.player-not-found",
                            Map.of("target", args[1]));
                    return true;
                }
                plugin.getPartyManager().transferLeader(player, target);
            }

            case "match" -> {
                var party = plugin.getPartyManager().getParty(player.getUniqueId());
                if (party == null) {
                    player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
                    return true;
                }
                if (!party.isLeader(player.getUniqueId())) {
                    player.sendMessage(Component.text("Only the party leader can start a match.", NamedTextColor.RED));
                    return true;
                }
                plugin.getGuiEditorManager().openGui(player, "party-match");
            }

            case "duel" -> {
                var party = plugin.getPartyManager().getParty(player.getUniqueId());
                if (party == null) {
                    player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
                    return true;
                }
                if (!party.isLeader(player.getUniqueId())) {
                    player.sendMessage(Component.text("Only the party leader can duel.", NamedTextColor.RED));
                    return true;
                }
                plugin.getGuiEditorManager().openGui(player, "party-vs-party");
            }

            case "spectate" -> {
                var party = plugin.getPartyManager().getParty(player.getUniqueId());
                if (party == null) {
                    player.sendMessage(Component.text("You are not in a party.", NamedTextColor.RED));
                    return true;
                }
                if (!party.isInMatch()) {
                    player.sendMessage(Component.text("Your party is not in a match.", NamedTextColor.RED));
                    return true;
                }
                plugin.getSpectatorManager().spectateMatch(player, party.getMatchId());
            }

            default -> player.sendMessage(Component.text(
                    "Usage: /party <create|invite|kick|disband|leave|accept|transfer|match|spectate>",
                    NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1)
            return filter(List.of("create", "invite", "kick", "disband", "leave", "accept", "transfer", "match", "duel", "spectate"), args[0]);
        if (args.length == 2 && List.of("invite", "kick", "transfer").contains(args[0].toLowerCase()))
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        return List.of();
    }

    private List<String> filter(List<String> list, String input) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
