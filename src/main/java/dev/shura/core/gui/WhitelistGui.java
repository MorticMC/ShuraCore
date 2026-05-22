package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.message.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.SoundUtil;
import dev.shura.core.util.GuiUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WhitelistGui {

    private final ShuraCore plugin;
    private final String group;

    public WhitelistGui(ShuraCore plugin, String group) {
        this.plugin = plugin;
        this.group = group;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        Set<String> allowed = plugin.getWhitelistManager().getAllowedCommands(group);

        PaginatedGui gui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(MessageService.colorizeComponent("&#00B4FF&lWhitelist &8— &e" + group))
                .rows(gc.whitelistRows())
                .pageSize(28)
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.whitelistBorder())
                .name(Component.empty()).asGuiItem());

        plugin.getServer().getCommandMap().getKnownCommands().keySet().stream()
                .sorted()
                .forEach(cmd -> {
                    boolean isAllowed = allowed.contains(cmd.toLowerCase());
                    Material mat = isAllowed ? gc.whitelistAllowedMat() : gc.whitelistDeniedMat();
                    String color = isAllowed ? "&#00FF7F" : "&#FF4444";
                    String status = isAllowed ? "✔ Allowed" : "✘ Denied";

                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.empty());
                    lore.add(Component.text("Group: ", NamedTextColor.GRAY).append(Component.text(group, NamedTextColor.AQUA)));
                    lore.add(Component.text("Status: ", NamedTextColor.GRAY).append(MessageService.colorizeComponent(color + status)));
                    lore.add(Component.empty());
                    lore.add(Component.text("Click to toggle!", NamedTextColor.YELLOW));

                    gui.addItem(GuiUtil.cleanItem(mat)
                            .name(MessageService.colorizeComponent(color + "&l/" + cmd))
                            .lore(lore)
                            .asGuiItem(e -> {
                                boolean nowAllowed = !allowed.contains(cmd.toLowerCase());
                                plugin.getWhitelistManager().setAllowed(group, cmd, nowAllowed);
                                SoundUtil.playGuiClick(player);
                                open(player);
                            }));
                });

        gui.setItem(5, 3, GuiUtil.cleanItem(gc.whitelistPrevMat())
                .name(gc.whitelistPrevName())
                .asGuiItem(e -> { gui.previous(); SoundUtil.playGuiClick(player); }));

        gui.setItem(5, 7, GuiUtil.cleanItem(gc.whitelistNextMat())
                .name(gc.whitelistNextName())
                .asGuiItem(e -> { gui.next(); SoundUtil.playGuiClick(player); }));

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }
}
