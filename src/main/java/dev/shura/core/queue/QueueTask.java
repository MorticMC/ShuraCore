package dev.shura.core.queue;

import dev.shura.core.ShuraCore;
import org.bukkit.scheduler.BukkitRunnable;

public class QueueTask extends BukkitRunnable {

    private final ShuraCore plugin;

    public QueueTask(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getQueueManager().tick();
    }
}
