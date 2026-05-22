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
import dev.triumphteam.gui.guis.PaginatedGui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;

public class KitSelectorGui {

    private final ShuraCore plugin;
    private final Player target;

    public KitSelectorGui(ShuraCore plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        PaginatedGui gui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(gc.kitSelectorTitle())
                .rows(gc.kitSelectorRows())
                .pageSize(21)
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.kitSelectorBorder())
                .name(Component.empty()).asGuiItem());

        Collection<Kit> kits = plugin.getKitManager().getAllKits();
        for (Kit kit : kits) {
            gui.addItem(GuiUtil.cleanItem(gc.kitSelectorKitMaterial())
                    .name(MessageService.colorizeComponent("&e" + kit.getName()))
                    .lore(
                            Component.empty(),
                            Component.text("Click to select this kit", NamedTextColor.YELLOW)
                    )
                    .asGuiItem(e -> {
                        SoundUtil.playGuiClick(player);
                        player.closeInventory();
                        new RoundSelectorGui(plugin, target, kit).open(player);
                    }));
        }

        gui.setItem(4, 3, GuiUtil.cleanItem(gc.kitSelectorPrevMaterial())
                .name(gc.kitSelectorPrevName())
                .asGuiItem(e -> { gui.previous(); SoundUtil.playGuiClick(player); }));

        gui.setItem(4, 7, GuiUtil.cleanItem(gc.kitSelectorNextMaterial())
                .name(gc.kitSelectorNextName())
                .asGuiItem(e -> { gui.next(); SoundUtil.playGuiClick(player); }));

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }
}
