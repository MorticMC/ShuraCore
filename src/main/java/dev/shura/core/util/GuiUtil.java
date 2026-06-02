package dev.shura.core.util;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;

public class GuiUtil {

    /**
     * Creates an ItemBuilder with all attributes and tooltips hidden
     */
    public static ItemBuilder cleanItem(Material material) {
        List<ItemFlag> flags = new ArrayList<>();
        flags.add(ItemFlag.HIDE_ATTRIBUTES);
        flags.add(ItemFlag.HIDE_ENCHANTS);
        flags.add(ItemFlag.HIDE_UNBREAKABLE);
        flags.add(ItemFlag.HIDE_DESTROYS);
        flags.add(ItemFlag.HIDE_PLACED_ON);
        
        // Add newer flags if they exist in this version
        try {
            flags.add(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
        } catch (IllegalArgumentException ignored) {}
        
        try {
            flags.add(ItemFlag.valueOf("HIDE_POTION_EFFECTS"));
        } catch (IllegalArgumentException ignored) {}
        
        try {
            flags.add(ItemFlag.valueOf("HIDE_DYE"));
        } catch (IllegalArgumentException ignored) {}
        
        try {
            flags.add(ItemFlag.valueOf("HIDE_ARMOR_TRIM"));
        } catch (IllegalArgumentException ignored) {}
        
        return ItemBuilder.from(material).flags(flags.toArray(new ItemFlag[0]));
    }
    
    /**
     * Removes italic decoration from a component
     */
    public static Component noItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
