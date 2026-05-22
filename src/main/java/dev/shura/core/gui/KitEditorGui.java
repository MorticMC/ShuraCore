package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.kit.Kit;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.message.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.SoundUtil;
import dev.shura.core.util.GuiUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;

public class KitEditorGui {

    // Layout: rows 1-3 = inventory slots (27), row 4 = armor + controls
    // Slot map: 0-26 = kit inventory slots 0-26, 27-30 = armor (helmet/chest/legs/boots)
    private static final int HELMET_SLOT    = 36;
    private static final int CHEST_SLOT     = 37;
    private static final int LEGS_SLOT      = 38;
    private static final int BOOTS_SLOT     = 39;
    private static final int SAVE_SLOT      = 31;
    private static final int RESET_SLOT     = 33;
    private static final int BACK_SLOT      = 35;

    private final ShuraCore plugin;
    private final Player player;
    private final Kit kit;

    public KitEditorGui(ShuraCore plugin, Player player, Kit kit) {
        this.plugin = plugin;
        this.player = player;
        this.kit = kit;
    }

    public void open(Player player) {
        Gui gui = Gui.gui()
                .title(MessageService.colorizeComponent("&#00B4FF&lKit Editor &8— &e" + kit.getName()))
                .rows(plugin.getGuiConfig().kitEditorRows())
                .create(); // NOT disableAllInteractions — players need to place items

        // Allow item placement in slots 0-26 (inventory) and 36-39 (armor)
        gui.setDefaultClickAction(e -> {
            int slot = e.getSlot();
            // Block interaction on control slots
            if (slot == SAVE_SLOT || slot == RESET_SLOT || slot == BACK_SLOT) {
                e.setCancelled(true);
            }
        });

        // Pre-fill with existing custom kit or default kit
        ItemStack[] customInv = plugin.getKitEditor().getCustomInventory(player.getUniqueId(), kit.getId());
        ItemStack[] customArmor = plugin.getKitEditor().getCustomArmor(player.getUniqueId(), kit.getId());

        ItemStack[] inv = customInv != null ? customInv : kit.getInventory();
        ItemStack[] armor = customArmor != null ? customArmor : kit.getArmor();

        for (int i = 0; i < Math.min(inv.length, 27); i++) {
            if (inv[i] != null) gui.getInventory().setItem(i, inv[i].clone());
        }

        // Armor slots in row 4
        if (armor.length > 0 && armor[0] != null) gui.getInventory().setItem(HELMET_SLOT, armor[0].clone());
        if (armor.length > 1 && armor[1] != null) gui.getInventory().setItem(CHEST_SLOT, armor[1].clone());
        if (armor.length > 2 && armor[2] != null) gui.getInventory().setItem(LEGS_SLOT, armor[2].clone());
        if (armor.length > 3 && armor[3] != null) gui.getInventory().setItem(BOOTS_SLOT, armor[3].clone());

        // Armor labels
        gui.setItem(27, GuiUtil.cleanItem(Material.IRON_HELMET).name(MessageService.colorizeComponent("&7Helmet Slot")).asGuiItem());
        gui.setItem(28, GuiUtil.cleanItem(Material.IRON_CHESTPLATE).name(MessageService.colorizeComponent("&7Chestplate Slot")).asGuiItem());
        gui.setItem(29, GuiUtil.cleanItem(Material.IRON_LEGGINGS).name(MessageService.colorizeComponent("&7Leggings Slot")).asGuiItem());
        gui.setItem(30, GuiUtil.cleanItem(Material.IRON_BOOTS).name(MessageService.colorizeComponent("&7Boots Slot")).asGuiItem());

        GuiConfig gc = plugin.getGuiConfig();
        gui.setItem(SAVE_SLOT, GuiUtil.cleanItem(gc.kitEditorSaveMat())
                .name(gc.kitEditorSaveName())
                .lore(Component.empty(), Component.text("Click to save your loadout!", NamedTextColor.YELLOW))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    saveKit(player, gui);
                    SoundUtil.playSuccess(player);
                    player.closeInventory();
                }));

        gui.setItem(RESET_SLOT, GuiUtil.cleanItem(gc.kitEditorResetMat())
                .name(gc.kitEditorResetName())
                .lore(Component.empty(), Component.text("Resets to the default kit loadout.", NamedTextColor.GRAY))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    SoundUtil.playGuiClick(player);
                    plugin.getKitEditor().saveCustomKit(player, kit, kit.getInventory(), kit.getArmor());
                    open(player);
                }));

        gui.setItem(BACK_SLOT, GuiUtil.cleanItem(gc.kitEditorBackMat())
                .name(gc.kitEditorBackName())
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    SoundUtil.playGuiClick(player);
                    new KitSelectorGui(plugin, null).open(player);
                }));

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }

    private void saveKit(Player player, Gui gui) {
        ItemStack[] newInv = new ItemStack[36];
        for (int i = 0; i < 27; i++) {
            newInv[i] = gui.getInventory().getItem(i);
        }

        ItemStack[] newArmor = new ItemStack[4];
        newArmor[0] = gui.getInventory().getItem(HELMET_SLOT);
        newArmor[1] = gui.getInventory().getItem(CHEST_SLOT);
        newArmor[2] = gui.getInventory().getItem(LEGS_SLOT);
        newArmor[3] = gui.getInventory().getItem(BOOTS_SLOT);

        plugin.getKitEditor().saveCustomKit(player, kit, newInv, newArmor);
    }
}
