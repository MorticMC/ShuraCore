package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.message.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.profile.Profile;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.tierlist.Tierlist;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.SoundUtil;
import dev.shura.core.util.GuiUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RankingsGui {

    private final ShuraCore plugin;

    public RankingsGui(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        Collection<Tierlist> tierlists = plugin.getTierlistManager().getAllTierlists();

        Gui gui = Gui.gui()
                .title(gc.rankingsTitle())
                .rows(gc.rankingsRows())
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.rankingsBorder())
                .name(Component.empty()).asGuiItem());

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22};
        int i = 0;

        for (Tierlist tierlist : tierlists) {
            if (i >= slots.length) break;
            int slot = slots[i++];
            gui.setItem(slot, GuiUtil.cleanItem(gc.rankingsTierMat())
                    .name(MessageService.colorizeComponent("&#FFD700&l" + tierlist.getName()).decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.empty(),
                            Component.text("Status: ", NamedTextColor.GRAY).append(tierlist.isEnabled() ? Component.text("Active", NamedTextColor.GREEN) : Component.text("Inactive", NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false),
                            Component.text("Queue: ", NamedTextColor.GRAY).append(tierlist.isQueueEnabled() ? Component.text("Enabled", NamedTextColor.GREEN) : Component.text("Disabled", NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false),
                            Component.empty(),
                            Component.text("Click to view Top 10!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                    )
                    .asGuiItem(e -> {
                        SoundUtil.playGuiClick(player);
                        openTop10(player, tierlist);
                    }));
        }

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }

    private void openTop10(Player player, Tierlist tierlist) {
        GuiConfig gc = plugin.getGuiConfig();
        plugin.getProfileManager().getTopByTierlist(tierlist.getId(), 10)
                .thenAccept(profiles -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Gui top10 = Gui.gui()
                            .title(MessageService.colorizeComponent("&#FFD700&l" + tierlist.getName() + " &8— &7Top 10"))
                            .rows(gc.rankingsTop10Rows())
                            .disableAllInteractions()
                            .create();

                    top10.getFiller().fillBorder(GuiUtil.cleanItem(gc.rankingsTop10Border())
                            .name(Component.empty()).asGuiItem());

                    int[] slots = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22};
                    String[] medals = {"&#FFD700①", "&#C0C0C0②", "&#CD7F32③", "&e④", "&e⑤", "&e⑥", "&e⑦", "&e⑧", "&e⑨", "&e⑩"};

                    for (int i = 0; i < Math.min(profiles.size(), 10); i++) {
                        Profile profile = profiles.get(i);
                        int elo = profile.getElo(tierlist.getId());
                        int pos = i;

                        List<Component> lore = new ArrayList<>();
                        lore.add(Component.empty());
                        lore.add(Component.text("Matches: ", NamedTextColor.GRAY).append(Component.text(profile.getMatches(tierlist.getId()), NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.text("W/L: ", NamedTextColor.GRAY).append(Component.text(profile.getTotalWins() + "/" + profile.getTotalLosses(), NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.empty());

                        top10.setItem(slots[i], ItemBuilder.skull()
                                .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                                .name(MessageService.colorizeComponent(medals[pos] + " &f" + profile.getName()).decoration(TextDecoration.ITALIC, false))
                                .lore(lore)
                                .asGuiItem());
                    }

                    top10.setItem(4, 5, GuiUtil.cleanItem(gc.rankingsBackMat())
                            .name(gc.rankingsBackName().decoration(TextDecoration.ITALIC, false))
                            .asGuiItem(e -> {
                                SoundUtil.playGuiClick(player);
                                open(player);
                            }));

                    top10.open(player);
                    SoundUtil.playGuiOpen(player);
                }));
    }
}
