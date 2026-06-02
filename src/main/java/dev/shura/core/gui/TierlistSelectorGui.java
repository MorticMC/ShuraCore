package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.tierlist.Tierlist;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.SoundUtil;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class TierlistSelectorGui {

    private final ShuraCore plugin;
    private final Player target;

    public TierlistSelectorGui(ShuraCore plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        Gui gui = Gui.gui()
                .title(MessageService.colorizeComponent("&#00B4FF&lSelect Tierlist"))
                .rows(4)
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.kitSelectorBorder())
                .name(Component.empty()).asGuiItem());

        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int index = 0;

        for (Tierlist tierlist : plugin.getTierlistManager().getAllTierlists()) {
            if (!tierlist.isEnabled() || index >= slots.length) continue;

            Material mat = getTierlistMaterial(tierlist.getId());
            gui.setItem(slots[index], GuiUtil.cleanItem(mat)
                    .name(GuiUtil.noItalic(MessageService.colorizeComponent("&#FFD700&l" + tierlist.getName())))
                    .lore(
                            GuiUtil.noItalic(Component.empty()),
                            GuiUtil.noItalic(Component.text("Click to select gamemode", NamedTextColor.YELLOW))
                    )
                    .asGuiItem(e -> {
                        SoundUtil.playGuiClick(player);
                        new KitSelectorGui(plugin, target, tierlist.getId()).open(player);
                    }));
            index++;
        }

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }

    private Material getTierlistMaterial(String tierlistId) {
        return switch (tierlistId) {
            case "mctiers" -> Material.DIAMOND_SWORD;
            case "pvptiers" -> Material.IRON_SWORD;
            case "novatiers" -> Material.GOLDEN_SWORD;
            case "extiers" -> Material.NETHERITE_SWORD;
            case "cactustiers" -> Material.CACTUS;
            case "extragm" -> Material.ENDER_PEARL;
            case "subtiers" -> Material.STONE_SWORD;
            default -> Material.PAPER;
        };
    }
}
