package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.event.QueueJoinEvent;
import dev.shura.core.event.QueueLeaveEvent;
import dev.shura.core.queue.QueueEntry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class QueueListener implements Listener {

    private final ShuraCore plugin;

    public QueueListener(ShuraCore plugin) {
        this.plugin = plugin;
        // Update queue scoreboards every second while in queue
        new BukkitRunnable() {
            @Override public void run() {
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
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            plugin.getQueueManager().leaveAllQueues(player);
        }
    }

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
