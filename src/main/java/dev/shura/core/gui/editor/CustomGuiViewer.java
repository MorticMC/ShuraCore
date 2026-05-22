package dev.shura.core.gui.editor;

import dev.shura.core.ShuraCore;
import dev.shura.core.message.MessageService;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CustomGuiViewer {

    private final ShuraCore plugin;
    private final CustomGui customGui;

    public CustomGuiViewer(ShuraCore plugin, CustomGui customGui) {
        this.plugin = plugin;
        this.customGui = customGui;
    }

    public void open(Player player) {
        if (customGui.getGuiType().isChest()) {
            Gui gui = Gui.gui()
                    .title(MessageService.colorizeComponent(customGui.getTitle()))
                    .rows(customGui.getRows())
                    .disableAllInteractions()
                    .create();

            for (int slot = 0; slot < customGui.getRows() * 9; slot++) {
                ItemStack item = customGui.getItem(slot);
                if (item != null) {
                    int finalSlot = slot;
                    gui.setItem(slot, ItemBuilder.from(item).asGuiItem(e -> handleClick(player, finalSlot)));
                }
            }

            gui.open(player);
        } else {
            Inventory inv = Bukkit.createInventory(null, customGui.getInventoryType(), MessageService.colorizeComponent(customGui.getTitle()));
            
            for (int slot = 0; slot < inv.getSize(); slot++) {
                ItemStack item = customGui.getItem(slot);
                if (item != null) inv.setItem(slot, item);
            }
            
            player.openInventory(inv);
        }
    }

    private void handleClick(Player player, int slot) {
        if (customGui.getName().equalsIgnoreCase("party") && slot == 2) {
            player.closeInventory();
            plugin.getPartyManager().createParty(player);
        }
    }
}
