package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.match.Match;
import dev.shura.core.match.MatchPlayer;
import dev.shura.core.match.MatchType;
import dev.shura.core.util.SoundUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MatchEndGui {

    private final ShuraCore plugin;
    private final Match match;
    private final MatchPlayer winner;
    private final MatchPlayer loser;
    private final boolean isWinner;

    public MatchEndGui(ShuraCore plugin, Match match, MatchPlayer winner, MatchPlayer loser, boolean isWinner) {
        this.plugin = plugin;
        this.match = match;
        this.winner = winner;
        this.loser = loser;
        this.isWinner = isWinner;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        Gui gui = Gui.gui()
                .title(gc.matchEndTitle())
                .rows(gc.matchEndRows())
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.matchEndBorder())
                .name(Component.empty()).asGuiItem());

        gui.setItem(2, 2, GuiUtil.cleanItem(isWinner ? gc.matchEndVictoryMat() : gc.matchEndDefeatMat())
                .name(isWinner ? gc.matchEndVictoryName() : gc.matchEndDefeatName())
                .lore(
                        Component.empty(),
                        Component.text("Kit: ", NamedTextColor.GRAY).append(Component.text(match.getKit().getName(), NamedTextColor.WHITE)),
                        Component.text("Format: ", NamedTextColor.GRAY).append(Component.text(match.getFormat().getDisplay(), NamedTextColor.WHITE)),
                        Component.text("Rounds: ", NamedTextColor.GRAY).append(Component.text(winner.getRoundsWon() + " - " + loser.getRoundsWon(), NamedTextColor.WHITE)),
                        Component.empty()
                )
                .asGuiItem());

        String opponentName = isWinner ? loser.getName() : winner.getName();
        gui.setItem(2, 8, ItemBuilder.skull()
                .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                .name(Component.text("Opponent: ", NamedTextColor.GRAY).append(Component.text(opponentName, NamedTextColor.AQUA)))
                .asGuiItem());

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }
}
