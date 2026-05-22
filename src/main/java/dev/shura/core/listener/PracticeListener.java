package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.practice.PracticeGamemode;
import dev.shura.core.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PracticeListener implements Listener {

    private final ShuraCore plugin;

    public PracticeListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getPracticeManager().isInPractice(player.getUniqueId())) return;

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PracticeGamemode gm = plugin.getPracticeManager().getGamemode(player.getUniqueId());
        if (gm == null) return;

        // Rejoin practice on respawn
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getPracticeManager().joinPractice(player, gm);
            SoundUtil.playTeleport(player);
        }, 1L);
    }
}
