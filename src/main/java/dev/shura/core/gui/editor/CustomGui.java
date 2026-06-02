package dev.shura.core.gui.editor;

import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CustomGui {

    private final String name;
    private GuiType guiType;
    private String title;
    private String command;
    private final Map<Integer, ItemStack> items = new HashMap<>();

    public CustomGui(String name, GuiType guiType, String title) {
        this.name = name;
        this.guiType = guiType;
        this.title = title;
        this.command = null;
    }

    public String getName() { return name; }
    public GuiType getGuiType() { return guiType; }
    public int getRows() { return guiType.getRows(); }
    public InventoryType getInventoryType() { return guiType.getType(); }
    public String getTitle() { return title; }
    public String getCommand() { return command; }
    public Map<Integer, ItemStack> getItems() { return items; }

    public void setGuiType(GuiType guiType) { this.guiType = guiType; }
    public void setTitle(String title) { this.title = title; }
    public void setCommand(String command) { this.command = command; }
    
    public void setItem(int slot, ItemStack item) {
        if (item == null) items.remove(slot);
        else items.put(slot, item.clone());
    }

    public ItemStack getItem(int slot) {
        return items.get(slot);
    }
}
