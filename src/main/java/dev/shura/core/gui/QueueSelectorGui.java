package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.extra.MessageService;
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
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueueSelectorGui {

    private final ShuraCore plugin;

    public QueueSelectorGui(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        List<Tierlist> tierlists = plugin.getTierlistManager().getQueueEnabledTierlists();
        Profile profile = plugin.getProfileManager().getProfile(player);

        int rows = Math.max(gc.queueSelectorRows(), (int) Math.ceil((tierlists.size() + 2) / 9.0) + 1);
        int actualRows = Math.min(rows, 6);
        Gui gui = Gui.gui()
                .title(gc.queueSelectorTitle())
                .rows(actualRows)
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.queueSelectorBorder())
                .name(Component.empty()).asGuiItem());

        List<Integer> innerSlots = new ArrayList<>();
        for (int row = 1; row < actualRows - 1; row++)
            for (int col = 1; col <= 7; col++)
                innerSlots.add(row * 9 + col);

        for (int i = 0; i < Math.min(tierlists.size(), innerSlots.size()); i++) {
            Tierlist tierlist = tierlists.get(i);
            int matches = profile != null ? profile.getMatches(tierlist.getId()) : 0;
            int queueSize = plugin.getQueueManager().getQueueSize(tierlist.getId());

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("In Queue: ", NamedTextColor.GRAY).append(Component.text(queueSize, NamedTextColor.WHITE)));
            lore.add(Component.empty());
            lore.add(Component.text("Click to queue!", NamedTextColor.YELLOW));

            gui.setItem(innerSlots.get(i), GuiUtil.cleanItem(gc.queueSelectorTierMat())
                    .name(MessageService.colorizeComponent("&#FFD700&l" + tierlist.getName()))
                    .lore(lore)
                    .asGuiItem(e -> {
                        SoundUtil.playGuiClick(player);
                        player.closeInventory();
                        plugin.getQueueManager().joinQueue(player, tierlist.getId());
                    }));
        }

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }
}
