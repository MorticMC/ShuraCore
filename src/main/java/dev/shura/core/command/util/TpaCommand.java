package dev.shura.core.command.util;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.CooldownManager;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaCommand implements CommandExecutor {

    private final ShuraCore plugin;
    // receiverUUID -> senderUUID
    private final Map<UUID, UUID> pendingTpa = new ConcurrentHashMap<>();
    // receiverUUID -> true means tpahere (sender wants receiver to come to them)
    private final Map<UUID, Boolean> tpaHere = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> expiryTasks = new ConcurrentHashMap<>();
    private final CooldownManager cooldowns = new CooldownManager();

    public TpaCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }

        switch (label.toLowerCase()) {
            case "tpa" -> handleTpa(player, args, false);
            case "tpahere" -> handleTpa(player, args, true);
            case "tpaccept" -> handleAccept(player);
            case "tpdeny" -> handleDeny(player);
        }
        return true;
    }

    private void handleTpa(Player sender, String[] args, boolean here) {
        if (!sender.hasPermission("shura.command.tpa")) {
            plugin.getMessageService().send(sender, "errors.no-permission");
            return;
        }
        if (args.length == 0) {
            plugin.getMessageService().send(sender, "errors.invalid-args",
                    Map.of("usage", here ? "/tpahere <player>" : "/tpa <player>"));
            return;
        }
        if (cooldowns.isOnCooldown(sender.getUniqueId(), "tpa")) {
            plugin.getMessageService().send(sender, "errors.cooldown",
                    Map.of("seconds", String.valueOf(cooldowns.getRemainingSeconds(sender.getUniqueId(), "tpa"))));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageService().send(sender, "errors.player-not-found", Map.of("target", args[0]));
            return;
        }
        if (target.equals(sender)) {
            sender.sendMessage(Component.text("You cannot send a TPA request to yourself.", NamedTextColor.RED));
            return;
        }

        pendingTpa.put(target.getUniqueId(), sender.getUniqueId());
        tpaHere.put(target.getUniqueId(), here);
        cooldowns.set(sender.getUniqueId(), "tpa", 5000L);

        sender.sendMessage(Component.text("Teleport request sent to ", NamedTextColor.GREEN)
                .append(Component.text(target.getName(), NamedTextColor.AQUA)));
        target.sendMessage(Component.text(sender.getName(), NamedTextColor.AQUA)
                .append(Component.text(here ? " wants you to teleport to them." : " wants to teleport to you.", NamedTextColor.GREEN))
                .append(Component.text(" /tpaccept | /tpdeny", NamedTextColor.YELLOW)));
        SoundUtil.playDuelRequest(target);

        // Expire after 30s
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingTpa.remove(target.getUniqueId()) != null) {
                tpaHere.remove(target.getUniqueId());
                expiryTasks.remove(target.getUniqueId());
                sender.sendMessage(Component.text("Teleport request to ", NamedTextColor.RED)
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" expired.", NamedTextColor.RED)));
                target.sendMessage(Component.text("Teleport request from ", NamedTextColor.RED)
                        .append(Component.text(sender.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" expired.", NamedTextColor.RED)));
            }
        }, 600L);

        BukkitTask old = expiryTasks.put(target.getUniqueId(), task);
        if (old != null) old.cancel();
    }

    private void handleAccept(Player receiver) {
        UUID senderUuid = pendingTpa.remove(receiver.getUniqueId());
        boolean here = tpaHere.getOrDefault(receiver.getUniqueId(), false);
        tpaHere.remove(receiver.getUniqueId());
        BukkitTask task = expiryTasks.remove(receiver.getUniqueId());
        if (task != null) task.cancel();

        if (senderUuid == null) {
            receiver.sendMessage(Component.text("You have no pending teleport request.", NamedTextColor.RED));
            return;
        }

        Player sender = Bukkit.getPlayer(senderUuid);
        if (sender == null) {
            receiver.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
            return;
        }

        if (here) {
            receiver.teleport(sender.getLocation());
            receiver.sendMessage(Component.text("Teleported to ", NamedTextColor.GREEN)
                    .append(Component.text(sender.getName(), NamedTextColor.AQUA)));
        } else {
            sender.teleport(receiver.getLocation());
            sender.sendMessage(Component.text("Teleported to ", NamedTextColor.GREEN)
                    .append(Component.text(receiver.getName(), NamedTextColor.AQUA)));
        }
        receiver.sendMessage(Component.text("Teleport request accepted.", NamedTextColor.GREEN));
        SoundUtil.playTeleport(here ? receiver : sender);
    }

    private void handleDeny(Player receiver) {
        UUID senderUuid = pendingTpa.remove(receiver.getUniqueId());
        tpaHere.remove(receiver.getUniqueId());
        BukkitTask task = expiryTasks.remove(receiver.getUniqueId());
        if (task != null) task.cancel();

        if (senderUuid == null) {
            receiver.sendMessage(Component.text("You have no pending teleport request.", NamedTextColor.RED));
            return;
        }

        receiver.sendMessage(Component.text("Teleport request denied.", NamedTextColor.RED));
        Player sender = Bukkit.getPlayer(senderUuid);
        if (sender != null)
            sender.sendMessage(Component.text(receiver.getName(), NamedTextColor.AQUA)
                    .append(Component.text(" denied your teleport request.", NamedTextColor.RED)));
    }
}
