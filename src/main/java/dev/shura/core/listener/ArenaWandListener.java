package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.ArenaWand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ArenaWandListener implements Listener {

    private final ShuraCore plugin;

    public ArenaWandListener(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("shura.admin")) return;

        ArenaWand wand = plugin.getArenaManager().getArenaWand();
        if (!wand.isWand(player.getInventory().getItemInMainHand())) return;

        if (event.getClickedBlock() == null) return;
        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            wand.setPos1(player, event.getClickedBlock().getLocation());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            wand.setPos2(player, event.getClickedBlock().getLocation());
        }
    }
}
