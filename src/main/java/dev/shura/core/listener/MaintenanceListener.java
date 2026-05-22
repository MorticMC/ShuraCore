package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class MaintenanceListener implements Listener {

    private final ShuraCore plugin;

    public MaintenanceListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!plugin.getMaintenanceManager().isEnabled()) return;
        if (event.getPlayer().hasPermission("shuracore.maintenance.allow")) return;

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                plugin.getMaintenanceManager().getKickMessage());
    }
}
