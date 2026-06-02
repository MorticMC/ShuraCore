package dev.shura.core.gui.editor;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.MessageService;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiEditorScreen {

    private final ShuraCore plugin;
    private final GuiEditorManager manager;
    private final CustomGui customGui;

    public GuiEditorScreen(ShuraCore plugin, GuiEditorManager manager, CustomGui customGui) {
        this.plugin = plugin;
        this.manager = manager;
        this.customGui = customGui;
    }

    public void open(Player player) {
        if (customGui.getGuiType().isChest()) {
            Gui gui = Gui.gui()
                    .title(MessageService.colorizeComponent("&6Editing: &e" + customGui.getName()))
                    .rows(customGui.getRows())
                    .create();

            gui.getFiller().fill(ItemBuilder.from(org.bukkit.Material.AIR).asGuiItem());

            for (int slot = 0; slot < customGui.getRows() * 9; slot++) {
                ItemStack item = customGui.getItem(slot);
                if (item != null) {
                    gui.setItem(slot, ItemBuilder.from(item).asGuiItem());
                }
            }

            gui.setDefaultClickAction(e -> {
                if (e.getClick() == ClickType.SHIFT_RIGHT && e.getCurrentItem() != null) {
                    customGui.setItem(e.getSlot(), null);
                    manager.saveGui(customGui);
                    e.setCurrentItem(null);
                }
            });

            gui.open(player);
        } else {
            Inventory inv = Bukkit.createInventory(null, customGui.getInventoryType(), MessageService.colorizeComponent("&6Editing: &e" + customGui.getName()));
            
            for (int slot = 0; slot < inv.getSize(); slot++) {
                ItemStack item = customGui.getItem(slot);
                if (item != null) inv.setItem(slot, item);
            }
            
            player.openInventory(inv);
        }
    }
}
