package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.duel.DuelRequest;
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

import java.util.Map;

public class DuelRequestGui {

    private final ShuraCore plugin;
    private final DuelRequest request;

    public DuelRequestGui(ShuraCore plugin, DuelRequest request) {
        this.plugin = plugin;
        this.request = request;
    }

    public void open(Player receiver) {
        GuiConfig gc = plugin.getGuiConfig();
        Gui gui = Gui.gui()
                .title(gc.duelRequestTitle())
                .rows(gc.duelRequestRows())
                .disableAllInteractions()
                .create();

        Player sender = org.bukkit.Bukkit.getPlayer(request.getSenderUuid());
        String senderName = sender != null ? sender.getName() : request.getSenderName();

        gui.setItem(1, GuiUtil.cleanItem(gc.duelRequestAcceptMat())
                .name(gc.duelRequestAcceptName())
                .lore(
                        Component.empty(),
                        Component.text("From: ", NamedTextColor.GRAY).append(Component.text(senderName, NamedTextColor.AQUA)),
                        Component.text("Kit: ", NamedTextColor.GRAY).append(Component.text(request.getKit().getName(), NamedTextColor.WHITE)),
                        Component.text("Format: ", NamedTextColor.GRAY).append(Component.text(request.getFormat().getDisplay(), NamedTextColor.WHITE)),
                        Component.empty()
                )
                .asGuiItem(e -> {
                    SoundUtil.playDuelAccept(receiver);
                    receiver.closeInventory();
                    plugin.getDuelManager().acceptRequest(receiver);
                }));

        gui.setItem(2, ItemBuilder.skull()
                .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                .name(MessageService.colorizeComponent("&e" + senderName))
                .lore(
                        Component.empty(),
                        Component.text("Kit: ", NamedTextColor.GRAY).append(Component.text(request.getKit().getName(), NamedTextColor.WHITE)),
                        Component.text("Format: ", NamedTextColor.GRAY).append(Component.text(request.getFormat().getDisplay(), NamedTextColor.WHITE)),
                        Component.empty()
                )
                .asGuiItem());

        gui.setItem(3, GuiUtil.cleanItem(gc.duelRequestDenyMat())
                .name(gc.duelRequestDenyName())
                .lore(Component.empty())
                .asGuiItem(e -> {
                    SoundUtil.playError(receiver);
                    receiver.closeInventory();
                    plugin.getDuelManager().denyRequest(receiver);
                }));

        gui.open(receiver);
        SoundUtil.playDuelRequest(receiver);
    }
}
