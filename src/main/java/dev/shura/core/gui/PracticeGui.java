package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.message.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.practice.PracticeGamemode;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.SoundUtil;
import dev.shura.core.util.GuiUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PracticeGui {

    private final ShuraCore plugin;

    public PracticeGui(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        Gui gui = Gui.gui()
                .title(gc.practiceTitle())
                .rows(gc.practiceRows())
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.practiceBorder())
                .name(Component.empty()).asGuiItem());

        PracticeGamemode[] gamemodes = PracticeGamemode.values();
        int[] defaultSlots = {10, 11, 12, 13, 14, 15, 16, 17};

        for (int i = 0; i < gamemodes.length; i++) {
            PracticeGamemode gm = gamemodes[i];
            int count = plugin.getPracticeManager().getPlayerCount(gm);
            int slot = gc.practicGamodeSlot(gm.getId(), defaultSlots[i]);

            List<Component> lore = new java.util.ArrayList<>();
            for (String line : gc.practiceGamemodeLore(gm.getId())) {
                String resolved = line.replace("{players}", String.valueOf(count));
                lore.add(MessageService.colorizeComponent(resolved)
                        .decoration(TextDecoration.ITALIC, false));
            }

            gui.setItem(slot, GuiUtil.cleanItem(gc.practiceGamemodeMaterial(gm.getId()))
                    .name(gc.practicGamemodeName(gm.getId()))
                    .lore(lore)
                    .asGuiItem(e -> {
                        SoundUtil.playGuiClick(player);
                        plugin.getPracticeManager().joinPractice(player, gm);
                        player.closeInventory();
                    }));
        }

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }
}
