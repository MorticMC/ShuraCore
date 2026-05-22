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
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ProfileGui {

    private final ShuraCore plugin;
    private final Player target;

    // View own profile
    public ProfileGui(ShuraCore plugin) {
        this.plugin = plugin;
        this.target = null;
    }

    // View another player's profile
    public ProfileGui(ShuraCore plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
    }

    public void open(Player viewer) {
        Player subject = target != null ? target : viewer;
        Profile profile = plugin.getProfileManager().getProfile(subject.getUniqueId());
        GuiConfig gc = plugin.getGuiConfig();

        Gui gui = Gui.gui()
                .title(gc.profileTitle())
                .rows(gc.profileRows())
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.profileBorder())
                .name(Component.empty()).asGuiItem());

        // ── Slot 10: Player skull + global stats ─────────────────────────────
        int wins    = profile != null ? profile.getTotalWins() : 0;
        int losses  = profile != null ? profile.getTotalLosses() : 0;
        int streak  = profile != null ? profile.getCurrentStreak() : 0;
        int best    = profile != null ? profile.getBestStreak() : 0;
        double wr   = profile != null ? profile.getWinRate() : 0.0;

        Component streakLine = streak >= 0
                ? Component.text("Win Streak: ", NamedTextColor.GRAY).append(Component.text("+" + streak, NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false)
                : Component.text("Loss Streak: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(streak), NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false);

        gui.setItem(gc.profileSkullSlot(), ItemBuilder.skull()
                .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                .name(MessageService.colorizeComponent("&#00B4FF&l" + subject.getName()).decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.empty(),
                        Component.text("Wins: ", NamedTextColor.GRAY).append(Component.text(wins, NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false),
                        Component.text("Losses: ", NamedTextColor.GRAY).append(Component.text(losses, NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false),
                        Component.text("Win Rate: ", NamedTextColor.GRAY).append(Component.text(String.format("%.1f%%", wr), NamedTextColor.YELLOW)).decoration(TextDecoration.ITALIC, false),
                        streakLine,
                        Component.text("Best Streak: ", NamedTextColor.GRAY).append(Component.text(best, NamedTextColor.GOLD)).decoration(TextDecoration.ITALIC, false),
                        Component.empty()
                )
                .asGuiItem());

        // ── Slots 12–16 + 21–25: Tierlist ELO cards (up to 10) ───────────────
        List<Integer> tierSlots = gc.profileTierSlots();
        if (tierSlots.isEmpty()) tierSlots = java.util.Arrays.asList(12, 13, 14, 15, 16, 21, 22, 23, 24, 25);
        Collection<Tierlist> tierlists = plugin.getTierlistManager().getAllTierlists();
        int i = 0;
        for (Tierlist tierlist : tierlists) {
            if (i >= tierSlots.size()) break;
            int elo     = profile != null ? profile.getElo(tierlist.getId()) : 1000;
            int matches = profile != null ? profile.getMatches(tierlist.getId()) : 0;

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Matches: ", NamedTextColor.GRAY).append(Component.text(matches, NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());

            gui.setItem(tierSlots.get(i++), GuiUtil.cleanItem(Material.PAPER)
                    .name(MessageService.colorizeComponent("&#FFD700&l" + tierlist.getName()).decoration(TextDecoration.ITALIC, false))
                    .lore(lore)
                    .asGuiItem());
        }

        // ── Slot 28: Settings shortcut (own profile only) ─────────────────────
        if (target == null) {
            gui.setItem(gc.profileSettingsShortcutSlot(), GuiUtil.cleanItem(Material.COMPARATOR)
                    .name(MessageService.colorizeComponent("&#00B4FF&lSettings").decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.empty(),
                            Component.text("Click to open settings.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                            Component.empty()
                    )
                    .asGuiItem(e -> {
                        SoundUtil.playGuiClick(viewer);
                        new SettingsGui(plugin).open(viewer);
                    }));
        }

        gui.open(viewer);
        SoundUtil.playGuiOpen(viewer);
    }
}
