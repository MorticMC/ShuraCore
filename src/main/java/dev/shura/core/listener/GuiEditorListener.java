package dev.shura.core.listener;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.editor.CustomGui;
import dev.shura.core.gui.editor.GuiEditorManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiEditorListener implements Listener {

    private final ShuraCore plugin;
    private final GuiEditorManager manager;

    public GuiEditorListener(ShuraCore plugin) {
        this.plugin = plugin;
        this.manager = plugin.getGuiEditorManager();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.contains("Editing: ")) return;

        String guiName = title.replace("Editing: ", "").replace("§6", "").replace("§e", "").trim();
        CustomGui gui = manager.getGui(guiName);
        if (gui == null) return;

        Inventory inv = event.getInventory();
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) {
                gui.setItem(slot, null);
            } else {
                gui.setItem(slot, item);
            }
        }
        manager.saveGui(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (!title.contains("Editing: ")) return;

        String guiName = title.replace("Editing: ", "").replace("§6", "").replace("§e", "").trim();
        CustomGui gui = manager.getGui(guiName);
        if (gui == null || gui.getGuiType().isChest()) return;

        event.setCancelled(false);
    }
}
