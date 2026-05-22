package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.match.Match;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.message.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.SoundUtil;
import dev.shura.core.util.GuiUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;

public class SpectatorGui {

    private final ShuraCore plugin;

    public SpectatorGui(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        Collection<Match> matches = plugin.getMatchManager().getActiveMatches();

        PaginatedGui gui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(gc.spectatorTitle())
                .rows(gc.spectatorRows())
                .pageSize(21)
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.spectatorBorder())
                .name(Component.empty()).asGuiItem());

        if (matches.isEmpty()) {
            gui.setItem(2, 5, GuiUtil.cleanItem(gc.spectatorNoMatchMat())
                    .name(gc.spectatorNoMatchName())
                    .asGuiItem());
        } else {
            for (Match match : matches) {
                String nameA = match.getMatchPlayerA().getName();
                String nameB = match.getMatchPlayerB().getName();
                int winsA = match.getMatchPlayerA().getRoundsWon();
                int winsB = match.getMatchPlayerB().getRoundsWon();
                Player targetA = Bukkit.getPlayer(match.getMatchPlayerA().getUuid());

                gui.addItem(GuiUtil.cleanItem(gc.spectatorMatchMat())
                        .name(MessageService.colorizeComponent("&e" + nameA + " &7vs &e" + nameB))
                        .lore(
                                Component.empty(),
                                Component.text("Kit: ", NamedTextColor.GRAY).append(Component.text(match.getKit().getName(), NamedTextColor.WHITE)),
                                Component.text("Format: ", NamedTextColor.GRAY).append(Component.text(match.getFormat().getDisplay(), NamedTextColor.WHITE)),
                                Component.text("Score: ", NamedTextColor.GRAY).append(Component.text(winsA + " - " + winsB, NamedTextColor.WHITE)),
                                Component.text("Round: ", NamedTextColor.GRAY).append(Component.text(match.getCurrentRound(), NamedTextColor.WHITE)),
                                Component.empty(),
                                Component.text("Click to spectate!", NamedTextColor.YELLOW)
                        )
                        .asGuiItem(e -> {
                            SoundUtil.playGuiClick(player);
                            player.closeInventory();
                            if (targetA != null) plugin.getSpectatorManager().addSpectator(player, targetA);
                        }));
            }
        }

        gui.setItem(4, 3, GuiUtil.cleanItem(gc.spectatorPrevMat())
                .name(gc.spectatorPrevName())
                .asGuiItem(e -> { gui.previous(); SoundUtil.playGuiClick(player); }));

        gui.setItem(4, 7, GuiUtil.cleanItem(gc.spectatorNextMat())
                .name(gc.spectatorNextName())
                .asGuiItem(e -> { gui.next(); SoundUtil.playGuiClick(player); }));

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }
}
