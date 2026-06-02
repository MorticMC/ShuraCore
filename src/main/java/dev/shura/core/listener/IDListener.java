package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.id.IDManager;
import dev.shura.core.extra.MessageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;

public class IDListener implements Listener {

    private final ShuraCore plugin;
    private final IDManager idManager;

    public IDListener(ShuraCore plugin) {
        this.plugin = plugin;
        this.idManager = plugin.getIDManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        if (idManager.hasActiveSession(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage();
            
            // Send the message only to the player (private)
            player.sendMessage(MessageService.colorize("&#808080You: &#FFFFFF" + message));
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                idManager.handleChatInput(player, message);
            });
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().title().toString();
        
        if (title.contains("ID Creation")) {
            if (event.getCurrentItem() != null && event.getCurrentItem().getType().name().equals("LECTERN")) {
                event.setCancelled(true);
                player.closeInventory();
                idManager.startIDCreation(player);
            }
        } else if (title.contains("Activate ID")) {
            if (event.getCurrentItem() != null && event.getCurrentItem().getType().name().equals("NETHERITE_UPGRADE_SMITHING_TEMPLATE")) {
                event.setCancelled(true);
                player.closeInventory();
                idManager.startActivation(player);
            }
        }
    }
}
