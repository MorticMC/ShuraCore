package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.editor.CustomGui;
import dev.shura.core.gui.editor.GuiEditorManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CustomGuiListener implements Listener {

    private final ShuraCore plugin;
    private final GuiEditorManager manager;

    public CustomGuiListener(ShuraCore plugin) {
        this.plugin = plugin;
        this.manager = plugin.getGuiEditorManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        
        for (String guiName : manager.getGuiNames()) {
            CustomGui gui = manager.getGui(guiName);
            if (gui == null) continue;
            
            String guiTitle = dev.shura.core.message.MessageService.colorizeComponent(gui.getTitle()).toString();
            if (title.contains(guiTitle) || title.contains(gui.getTitle())) {
                event.setCancelled(true);
                
                if (gui.getName().equalsIgnoreCase("party") && event.getSlot() == 2) {
                    player.closeInventory();
                    plugin.getPartyManager().createParty(player);
                }
                return;
            }
        }
    }
}
