package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.MessageService;
import dev.shura.core.kit.Kit;
import dev.shura.core.kit.KitRules;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.SoundUtil;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Admin GUI to toggle a kit's {@link KitRules}. Changes are saved live.
 */
public class KitRulesGui {

    private record RuleDef(String key, String name, Material icon, String description) {}

    // Ordered list of rules shown in the GUI
    private static final List<RuleDef> RULES = List.of(
            new RuleDef("noShield",          "No Shield",          Material.SHIELD,          "Removes shields from the kit."),
            new RuleDef("noOffhand",         "No Offhand",         Material.BLAZE_ROD,       "Disables off-hand item & swapping."),
            new RuleDef("noBuilding",        "No Building",        Material.BRICKS,          "Players cannot place blocks."),
            new RuleDef("noBlockBreak",      "No Block Break",     Material.IRON_PICKAXE,    "Players cannot break blocks."),
            new RuleDef("noRegen",           "No Natural Regen",   Material.GOLDEN_APPLE,    "Disables natural health regeneration."),
            new RuleDef("noFallDamage",      "No Fall Damage",     Material.FEATHER,         "Fall damage is cancelled."),
            new RuleDef("noHunger",          "No Hunger",          Material.COOKED_BEEF,     "Food level never drops."),
            new RuleDef("noEnderpearl",      "No Ender Pearls",    Material.ENDER_PEARL,     "Ender pearls cannot be thrown."),
            new RuleDef("restrictedWeapons", "Restricted Weapons", Material.DIAMOND_SWORD,   "Marks the kit as kit-weapons-only.")
    );

    private final ShuraCore plugin;
    private final Kit kit;

    public KitRulesGui(ShuraCore plugin, Kit kit) {
        this.plugin = plugin;
        this.kit = kit;
    }

    public void open(Player player) {
        Gui gui = Gui.gui()
                .title(MessageService.colorizeComponent("&#00B4FF&lKit Rules &8— &e" + kit.getName()))
                .rows(5)
                .disableAllInteractions()
                .create();

        // Border
        for (int slot = 0; slot < 45; slot++) {
            int row = slot / 9, col = slot % 9;
            if (row == 0 || row == 4 || col == 0 || col == 8) {
                gui.setItem(slot, GuiUtil.cleanItem(Material.GRAY_STAINED_GLASS_PANE)
                        .name(Component.empty()).asGuiItem());
            }
        }

        KitRules rules = kit.getRules();
        int[] slots = {10, 12, 14, 16, 19, 21, 23, 25, 31};
        for (int i = 0; i < RULES.size() && i < slots.length; i++) {
            RuleDef def = RULES.get(i);
            boolean enabled = rules.get(def.key());
            gui.setItem(slots[i], GuiUtil.cleanItem(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                    .name(MessageService.colorizeComponent((enabled ? "&a" : "&7") + def.name()))
                    .lore(
                            Component.text(def.description(), NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("Status: ", NamedTextColor.GRAY)
                                    .append(enabled
                                            ? Component.text("ENABLED", NamedTextColor.GREEN)
                                            : Component.text("DISABLED", NamedTextColor.RED)),
                            Component.text("Click to toggle", NamedTextColor.YELLOW))
                    .asGuiItem(e -> {
                        e.setCancelled(true);
                        rules.toggle(def.key());
                        plugin.getKitManager().saveKit(kit);
                        SoundUtil.playGuiClick(player);
                        open(player); // re-render
                    }));
        }

        gui.setItem(40, GuiUtil.cleanItem(Material.BARRIER)
                .name(Component.text("Close", NamedTextColor.RED))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    SoundUtil.playGuiClick(player);
                    player.closeInventory();
                }));

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }
}
