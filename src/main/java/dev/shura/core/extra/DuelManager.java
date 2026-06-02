package dev.shura.core.extra;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.arena.ArenaCopy;
import dev.shura.core.gui.DuelRequestGui;
import dev.shura.core.kit.Kit;
import dev.shura.core.match.MatchFormat;
import dev.shura.core.match.MatchType;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {

    private final ShuraCore plugin;
    // receiverUUID -> request
    private final Map<UUID, DuelRequest> pendingRequests = new ConcurrentHashMap<>();
    // requestId -> expiry task
    private final Map<UUID, BukkitTask> expiryTasks = new ConcurrentHashMap<>();

    public DuelManager(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player sender, Player receiver, Kit kit, MatchFormat format) {
        MessageService msg = plugin.getMessageService();

        if (sender.getUniqueId().equals(receiver.getUniqueId())) {
            msg.send(sender, "duel.self-duel");
            return;
        }
        if (plugin.getMatchManager().isInMatch(sender.getUniqueId())) {
            msg.send(sender, "duel.you-in-match");
            return;
        }
        if (plugin.getQueueManager().isInQueue(sender.getUniqueId())) {
            msg.send(sender, "duel.you-in-queue");
            return;
        }
        if (plugin.getMatchManager().isInMatch(receiver.getUniqueId())) {
            msg.send(sender, "duel.target-in-match", Map.of("target", receiver.getName()));
            return;
        }
        if (plugin.getQueueManager().isInQueue(receiver.getUniqueId())) {
            msg.send(sender, "duel.target-in-queue", Map.of("target", receiver.getName()));
            return;
        }
        if (pendingRequests.containsKey(receiver.getUniqueId())) {
            msg.send(sender, "duel.already-sent", Map.of("target", receiver.getName()));
            return;
        }

        DuelRequest request = new DuelRequest(sender.getUniqueId(), sender.getName(),
                receiver.getUniqueId(), kit, format);
        pendingRequests.put(receiver.getUniqueId(), request);

        // Notify sender
        msg.send(sender, "duel.request-sent", Map.of("target", receiver.getName()));

        // Notify receiver with clickable [Click Here]
        String rawMsg = msg.getRaw("duel.request-received", Map.of("player", sender.getName()));
        Component clickable = MessageService.colorizeComponent(rawMsg)
                .clickEvent(ClickEvent.runCommand("/duelaccept"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text("Click to open duel request!", NamedTextColor.YELLOW)));
        receiver.sendMessage(clickable);
        SoundUtil.playDuelRequest(receiver);

        // Schedule expiry
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.remove(receiver.getUniqueId()) != null) {
                expiryTasks.remove(receiver.getUniqueId());
                Player s = Bukkit.getPlayer(request.getSenderUuid());
                Player r = Bukkit.getPlayer(request.getReceiverUuid());
                if (s != null) msg.send(s, "duel.request-expired-sender", Map.of("target", receiver.getName()));
                if (r != null) msg.send(r, "duel.request-expired-receiver", Map.of("player", sender.getName()));
            }
        }, 600L); // 30 seconds

        expiryTasks.put(receiver.getUniqueId(), task);
    }

    public void openRequestGui(Player receiver) {
        DuelRequest request = pendingRequests.get(receiver.getUniqueId());
        if (request == null || request.isExpired()) {
            plugin.getMessageService().send(receiver, "duel.no-request");
            return;
        }
        new DuelRequestGui(plugin, request).open(receiver);
    }

    public void acceptRequest(Player receiver) {
        DuelRequest request = pendingRequests.remove(receiver.getUniqueId());
        cancelExpiryTask(receiver.getUniqueId());
        
        if (request == null || request.isExpired()) {
            plugin.getMessageService().send(receiver, "duel.no-request");
            return;
        }

        Player sender = Bukkit.getPlayer(request.getSenderUuid());
        if (sender == null) {
            plugin.getMessageService().send(receiver, "errors.player-not-found",
                    Map.of("target", request.getSenderName()));
            return;
        }

        plugin.getMessageService().send(receiver, "duel.accepted");
        SoundUtil.playDuelAccept(sender);

        Arena arena = plugin.getArenaManager().getAvailableArenaForKit(request.getKit().getId());
        if (arena == null) {
            sender.sendMessage(Component.text("No arenas available. Please try again.", NamedTextColor.RED));
            receiver.sendMessage(Component.text("No arenas available. Please try again.", NamedTextColor.RED));
            return;
        }

        ArenaCopy copy = plugin.getArenaManager().getOrCreateCopy(arena);
        if (copy == null) return;

        plugin.getMatchManager().createMatch(
                sender, receiver,
                request.getKit(), arena, copy,
                MatchType.UNRANKED, request.getFormat(), null
        );

        // Update tablist for both players
        plugin.getTabManager().update(sender);
        plugin.getTabManager().update(receiver);
    }

    public void denyRequest(Player receiver) {
        DuelRequest request = pendingRequests.remove(receiver.getUniqueId());
        cancelExpiryTask(receiver.getUniqueId());

        if (request == null) {
            plugin.getMessageService().send(receiver, "duel.no-request");
            return;
        }

        plugin.getMessageService().send(receiver, "duel.denied");
        Player sender = Bukkit.getPlayer(request.getSenderUuid());
        if (sender != null) {
            plugin.getMessageService().send(sender, "duel.denied-sender",
                    Map.of("target", receiver.getName()));
        }
    }

    public boolean hasPendingRequest(UUID receiverUuid) {
        DuelRequest req = pendingRequests.get(receiverUuid);
        return req != null && !req.isExpired();
    }

    private void cancelExpiryTask(UUID uuid) {
        BukkitTask task = expiryTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void cleanup(UUID uuid) {
        pendingRequests.remove(uuid);
        cancelExpiryTask(uuid);
    }

    public boolean hasRequest(UUID uuid) {
        return pendingRequests.containsKey(uuid);
    }
}
