package dev.shura.core.gui.editor;

import org.bukkit.event.inventory.InventoryType;

public enum GuiType {
    CHEST_1("chest-1", 1, InventoryType.CHEST),
    CHEST_2("chest-2", 2, InventoryType.CHEST),
    CHEST_3("chest-3", 3, InventoryType.CHEST),
    CHEST_4("chest-4", 4, InventoryType.CHEST),
    CHEST_5("chest-5", 5, InventoryType.CHEST),
    CHEST_6("chest-6", 6, InventoryType.CHEST),
    HOPPER("hopper", 1, InventoryType.HOPPER),
    DROPPER("dropper", 3, InventoryType.DROPPER),
    DISPENSER("dispenser", 3, InventoryType.DISPENSER);

    private final String name;
    private final int rows;
    private final InventoryType type;

    GuiType(String name, int rows, InventoryType type) {
        this.name = name;
        this.rows = rows;
        this.type = type;
    }

    public String getName() { return name; }
    public int getRows() { return rows; }
    public InventoryType getType() { return type; }
    public boolean isChest() { return type == InventoryType.CHEST; }

    public static GuiType fromString(String name) {
        for (GuiType type : values()) {
            if (type.name.equalsIgnoreCase(name)) return type;
        }
        return null;
    }

    public static String[] getNames() {
        GuiType[] types = values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].name;
        }
        return names;
    }
}
