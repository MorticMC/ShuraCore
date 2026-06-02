package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.event.MainEvents.PartyCreateEvent;
import dev.shura.core.event.MainEvents.PartyDisbandEvent;
import dev.shura.core.event.MainEvents.PartyJoinEvent;
import dev.shura.core.event.MainEvents.QueueJoinEvent;
import dev.shura.core.event.MainEvents.QueueLeaveEvent;
import dev.shura.core.extra.LobbyItems;
import dev.shura.core.queue.QueueEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class GameListener implements Listener {

    private final ShuraCore plugin;

    public GameListener(ShuraCore plugin) {
        this.plugin = plugin;
        startQueueScoreboardUpdater();
    }

    private void startQueueScoreboardUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (!plugin.getQueueManager().isInQueue(player.getUniqueId())) continue;
                    QueueEntry entry = firstQueueEntry(player);
                    if (entry != null) {
                        plugin.getBoardManager().updateQueue(player, entry);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ==================== PARTY EVENTS ====================

    @EventHandler
    public void onPartyCreate(PartyCreateEvent event) {
        Player leader = event.getLeader();
        if (leader == null || !leader.isOnline()) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> LobbyItems.give(plugin, leader), 1L);
    }

    @EventHandler
    public void onPartyJoin(PartyJoinEvent event) {
        event.getParty().getMembers().forEach(uuid -> {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline())
                Bukkit.getScheduler().runTaskLater(plugin, () -> LobbyItems.give(plugin, member), 1L);
        });
    }

    @EventHandler
    public void onPartyDisband(PartyDisbandEvent event) {
        event.getParty().getMembers().forEach(uuid -> {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline())
                LobbyItems.give(plugin, member);
        });
    }

    // ==================== QUEUE EVENTS ====================

    @EventHandler
    public void onQueueJoin(QueueJoinEvent event) {
        Player player = event.getPlayer();
        QueueEntry entry = findQueueEntry(player, event.getTierlistId());
        if (entry != null) {
            plugin.getBoardManager().updateQueue(player, entry);
        }
    }

    @EventHandler
    public void onQueueLeave(QueueLeaveEvent event) {
        plugin.getBoardManager().refreshForPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuitWhileInQueue(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            plugin.getQueueManager().leaveAllQueues(player);
        }
    }

    // ==================== SPECTATOR EVENTS ====================

    @EventHandler
    public void onSpectatorDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager
                && plugin.getSpectatorManager().isSpectating(damager.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpectatorBlockBreak(BlockBreakEvent event) {
        if (plugin.getSpectatorManager().isSpectating(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onSpectatorBlockPlace(BlockPlaceEvent event) {
        if (plugin.getSpectatorManager().isSpectating(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onSpectatorItemDrop(PlayerDropItemEvent event) {
        if (plugin.getSpectatorManager().isSpectating(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onSpectatorInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // ==================== HELPER METHODS ====================

    private QueueEntry firstQueueEntry(Player player) {
        Set<QueueEntry> entries = plugin.getQueueManager().getEntries(player.getUniqueId());
        if (entries.isEmpty()) return null;
        return entries.iterator().next();
    }

    private QueueEntry findQueueEntry(Player player, String tierlistId) {
        for (QueueEntry entry : plugin.getQueueManager().getEntries(player.getUniqueId())) {
            if (entry.getTierlistId().equals(tierlistId)) {
                return entry;
            }
        }
        return firstQueueEntry(player);
    }
}
