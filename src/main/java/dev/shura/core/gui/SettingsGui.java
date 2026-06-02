package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.profile.Profile;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.SoundUtil;
import dev.shura.core.util.GuiUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import dev.triumphteam.gui.components.GuiAction;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.List;

public class SettingsGui {

    private final ShuraCore plugin;

    public SettingsGui(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Profile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        GuiConfig gc = plugin.getGuiConfig();

        Gui gui = Gui.gui()
                .title(gc.settingsTitle())
                .rows(gc.settingsRows())
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.settingsBorder())
                .name(Component.empty()).asGuiItem());

        // Slot 11: Scoreboard
        gui.setItem(gc.settingsScoreboardSlot(), toggleItem(gc, profile != null && profile.isScoreboardEnabled(),
                "Scoreboard",
                "Shows your live stats on the sidebar.",
                e -> {
                    if (profile == null) return;
                    profile.setScoreboardEnabled(!profile.isScoreboardEnabled());
                    SoundUtil.playGuiClick(player);
                    open(player);
                }));

        // Slot 12: Tab list
        gui.setItem(gc.settingsTabSlot(), toggleItem(gc, profile != null && profile.isTabEnabled(),
                "Tab List",
                "Shows player info in the tab list.",
                e -> {
                    if (profile == null) return;
                    profile.setTabEnabled(!profile.isTabEnabled());
                    SoundUtil.playGuiClick(player);
                    open(player);
                }));

        // Slot 14: Party invites
        gui.setItem(gc.settingsPartyInvitesSlot(), toggleItem(gc, profile != null && profile.isPartyInvitesEnabled(),
                "Party Invites",
                "Allow others to invite you to parties.",
                e -> {
                    if (profile == null) return;
                    profile.setPartyInvitesEnabled(!profile.isPartyInvitesEnabled());
                    SoundUtil.playGuiClick(player);
                    open(player);
                }));

        // Slot 15: Duel requests
        gui.setItem(gc.settingsDuelRequestsSlot(), toggleItem(gc, profile != null && profile.isDuelRequestsEnabled(),
                "Duel Requests",
                "Allow others to send you duel requests.",
                e -> {
                    if (profile == null) return;
                    profile.setDuelRequestsEnabled(!profile.isDuelRequestsEnabled());
                    SoundUtil.playGuiClick(player);
                    open(player);
                }));

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }

    private dev.triumphteam.gui.guis.GuiItem toggleItem(GuiConfig gc, boolean enabled, String label, String description,
                                                         GuiAction<org.bukkit.event.inventory.InventoryClickEvent> action) {
        String statusColor = enabled ? "&#00FF7F" : "&#FF4444";
        String statusText  = enabled ? "✔ Enabled" : "✘ Disabled";

        return GuiUtil.cleanItem(enabled ? gc.settingsEnabledMat() : gc.settingsDisabledMat())
                .name(MessageService.colorizeComponent("&#00B4FF&l" + label).decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.empty(),
                        Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        MessageService.colorizeComponent("Status: " + statusColor + statusText).decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("Click to toggle.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(action);
    }
}
