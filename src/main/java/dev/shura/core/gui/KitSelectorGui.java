package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.kit.Kit;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.SoundUtil;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Set;

public class KitSelectorGui {

    private final ShuraCore plugin;
    private final Player target;
    private final String tierlistId;

    public KitSelectorGui(ShuraCore plugin, Player target, String tierlistId) {
        this.plugin = plugin;
        this.target = target;
        this.tierlistId = tierlistId;
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

        Set<String> gamemodeKeys = plugin.getKitsArenasConfig().getGamemodeKeys(tierlistId);

        if (gamemodeKeys.isEmpty()) {
            player.sendMessage(Component.text("No gamemodes available for this tierlist.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        for (String gamemodeKey : gamemodeKeys) {
            String gamemodeName = plugin.getKitsArenasConfig().getGamemodeName(tierlistId, gamemodeKey);
            Material gamemodeMaterial = plugin.getKitsArenasConfig().getGamemodeMaterial(tierlistId, gamemodeKey);
            String fullId = plugin.getKitsArenasConfig().getGamemodeFullId(tierlistId, gamemodeKey);
            
            Kit kit = plugin.getKitManager().getAllKits().stream()
                    .filter(k -> k.getId().equalsIgnoreCase(fullId) || 
                                 k.getName().equalsIgnoreCase(gamemodeName) ||
                                 k.getName().equalsIgnoreCase(gamemodeKey))
                    .findFirst().orElse(null);
            
            if (kit == null) {
                gui.addItem(GuiUtil.cleanItem(gamemodeMaterial)
                        .name(GuiUtil.noItalic(MessageService.colorizeComponent("&e" + gamemodeName)))
                        .lore(
                                GuiUtil.noItalic(Component.empty()),
                                GuiUtil.noItalic(Component.text("Kit not configured", NamedTextColor.RED)),
                                GuiUtil.noItalic(Component.text("ID: " + fullId, NamedTextColor.GRAY))
                        )
                        .asGuiItem(e -> {
                            player.sendMessage(Component.text("This kit hasn't been configured yet.", NamedTextColor.RED));
                            SoundUtil.playError(player);
                        }));
                continue;
            }

            gui.addItem(GuiUtil.cleanItem(gamemodeMaterial)
                    .name(GuiUtil.noItalic(MessageService.colorizeComponent("&e" + gamemodeName)))
                    .lore(
                            GuiUtil.noItalic(Component.empty()),
                            GuiUtil.noItalic(Component.text("Click to select this kit", NamedTextColor.YELLOW))
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

        gui.setItem(4, 5, GuiUtil.cleanItem(gc.roundSelectorBackMat())
                .name(gc.roundSelectorBackName())
                .asGuiItem(e -> {
                    SoundUtil.playGuiClick(player);
                    new TierlistSelectorGui(plugin, target).open(player);
                }));

        gui.setItem(4, 7, GuiUtil.cleanItem(gc.kitSelectorNextMaterial())
                .name(gc.kitSelectorNextName())
                .asGuiItem(e -> { gui.next(); SoundUtil.playGuiClick(player); }));

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }
}
