package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.kit.Kit;
import dev.shura.core.match.MatchFormat;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.SoundUtil;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class RoundSelectorGui {

    private final ShuraCore plugin;
    private final Player target;
    private final Kit kit;

    public RoundSelectorGui(ShuraCore plugin, Player target, Kit kit) {
        this.plugin = plugin;
        this.target = target;
        this.kit = kit;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        Gui gui = Gui.gui()
                .title(gc.roundSelectorTitle())
                .rows(gc.roundSelectorRows())
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.roundSelectorBorder())
                .name(Component.empty()).asGuiItem());

        int[] slots = {11, 12, 14, 15};
        MatchFormat[] formats = MatchFormat.values();

        for (int i = 0; i < formats.length; i++) {
            MatchFormat format = formats[i];
            gui.setItem(slots[i], GuiUtil.cleanItem(gc.roundSelectorFormatMat())
                    .name(GuiUtil.noItalic(MessageService.colorizeComponent("&#FFD700&l" + format.getDisplay())))
                    .lore(
                            GuiUtil.noItalic(Component.empty()),
                            GuiUtil.noItalic(Component.text("Kit: ", NamedTextColor.GRAY)
                                    .append(Component.text(kit.getName(), NamedTextColor.WHITE))),
                            GuiUtil.noItalic(Component.empty()),
                            GuiUtil.noItalic(Component.text("Click to send request!", NamedTextColor.YELLOW))
                    )
                    .asGuiItem(e -> {
                        SoundUtil.playGuiClick(player);
                        player.closeInventory();
                        plugin.getDuelCommand().clearDuelTarget(player);
                        plugin.getDuelManager().sendRequest(player, target, kit, format);
                    }));
        }

        gui.setItem(3, 5, GuiUtil.cleanItem(gc.roundSelectorBackMat())
                .name(gc.roundSelectorBackName())
                .asGuiItem(e -> {
                    SoundUtil.playGuiClick(player);
                    new DuelMCTiersGui(plugin, target).open(player);
                }));

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }
}
