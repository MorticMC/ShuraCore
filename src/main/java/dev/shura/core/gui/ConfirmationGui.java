package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.SoundUtil;
import dev.shura.core.util.GuiUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class ConfirmationGui {

    private final ShuraCore plugin;
    private final String title;
    private final Component description;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmationGui(ShuraCore plugin, String title, Component description, Runnable onConfirm, Runnable onCancel) {
        this.plugin = plugin;
        this.title = title;
        this.description = description;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();

        Gui gui = Gui.gui()
                .title(MessageService.colorizeComponent(title))
                .rows(gc.confirmationRows())
                .disableAllInteractions()
                .create();

        gui.getFiller().fill(GuiUtil.cleanItem(gc.confirmationBorder())
                .name(Component.empty()).asGuiItem());

        gui.setItem(2, 5, GuiUtil.cleanItem(gc.confirmationInfoMat())
                .name(MessageService.colorizeComponent("&eAre you sure?"))
                .lore(Component.empty(), description, Component.empty())
                .asGuiItem());

        gui.setItem(2, 3, GuiUtil.cleanItem(gc.confirmationConfirmMat())
                .name(gc.confirmationConfirmName())
                .asGuiItem(e -> {
                    SoundUtil.playSuccess(player);
                    player.closeInventory();
                    if (onConfirm != null) onConfirm.run();
                }));

        gui.setItem(2, 7, GuiUtil.cleanItem(gc.confirmationCancelMat())
                .name(gc.confirmationCancelName())
                .asGuiItem(e -> {
                    SoundUtil.playError(player);
                    player.closeInventory();
                    if (onCancel != null) onCancel.run();
                }));

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }
}
