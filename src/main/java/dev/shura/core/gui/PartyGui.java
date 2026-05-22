package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.message.MessageService;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.party.Party;
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
import java.util.List;
import java.util.UUID;

public class PartyGui {

    private final ShuraCore plugin;

    public PartyGui(ShuraCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) openCreateScreen(player);
        else openManageScreen(player, party);
    }

    // ── No party — show create screen ────────────────────────────────────────
    private void openCreateScreen(Player player) {
        GuiConfig gc = plugin.getGuiConfig();
        Gui gui = Gui.gui()
                .title(gc.partyTitle())
                .rows(gc.partyCreateRows())
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.partyBorder())
                .name(Component.empty()).asGuiItem());

        gui.setItem(2, 5, GuiUtil.cleanItem(gc.partyCreateMat())
                .name(gc.partyCreateName())
                .lore(Component.empty(), Component.text("Click to create a new party!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false), Component.empty())
                .asGuiItem(e -> {
                    SoundUtil.playSuccess(player);
                    player.closeInventory();
                    plugin.getPartyManager().createParty(player);
                }));

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }

    // ── In party — show management screen ────────────────────────────────────
    private void openManageScreen(Player player, Party party) {
        GuiConfig gc = plugin.getGuiConfig();
        boolean isLeader = party.isLeader(player.getUniqueId());

        Gui gui = Gui.gui()
                .title(gc.partyTitle())
                .rows(gc.partyManageRows())
                .disableAllInteractions()
                .create();

        gui.getFiller().fillBorder(GuiUtil.cleanItem(gc.partyBorder())
                .name(Component.empty()).asGuiItem());

        // Member heads — row 2
        List<UUID> members = new ArrayList<>(party.getMembers());
        int[] memberSlots = {10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < Math.min(members.size(), memberSlots.length); i++) {
            UUID memberUuid = members.get(i);
            Player member = Bukkit.getPlayer(memberUuid);
            String memberName = member != null ? member.getName() : Bukkit.getOfflinePlayer(memberUuid).getName();
            if (memberName == null) memberName = memberUuid.toString().substring(0, 8);
            boolean memberIsLeader = party.isLeader(memberUuid);
            boolean isSelf = memberUuid.equals(player.getUniqueId());

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            if (memberIsLeader)
                lore.add(Component.text("★ Party Leader", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(member != null ? "Online" : "Offline",
                    member != null ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            if (isLeader && !isSelf) {
                lore.add(Component.text("Left-click to transfer leadership", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Right-click to kick", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            }

            String finalMemberName = memberName;
            gui.setItem(memberSlots[i], ItemBuilder.skull()
                    .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                    .name(Component.text((memberIsLeader ? "★ " : "") + finalMemberName,
                            memberIsLeader ? NamedTextColor.GOLD : NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false))
                    .lore(lore)
                    .asGuiItem(e -> {
                        if (!isLeader || isSelf) return;
                        if (member == null) return;
                        if (e.isRightClick()) {
                            SoundUtil.playGuiClick(player);
                            plugin.getPartyManager().kickPlayer(player, member);
                            player.closeInventory();
                        } else if (e.isLeftClick()) {
                            SoundUtil.playGuiClick(player);
                            plugin.getPartyManager().transferLeader(player, member);
                            open(player); // refresh
                        }
                    }));
        }

        if (isLeader) {
            gui.setItem(3, 2, GuiUtil.cleanItem(gc.partyInviteMat())
                    .name(gc.partyInviteName())
                    .lore(Component.empty(), Component.text("Use /party invite <player>", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false), Component.empty())
                    .asGuiItem());
        }

        gui.setItem(3, 5, GuiUtil.cleanItem(gc.partyInfoMat())
                .name(gc.partyInfoName())
                .lore(
                        Component.empty(),
                        Component.text("Members: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(party.size() + "/8", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)),
                        Component.text("Leader: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(
                                Bukkit.getOfflinePlayer(party.getLeader()).getName() != null ? Bukkit.getOfflinePlayer(party.getLeader()).getName() : "Unknown",
                                NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)),
                        Component.empty()
                )
                .asGuiItem());

        if (isLeader) {
            gui.setItem(3, 8, GuiUtil.cleanItem(gc.partyDisbandMat())
                    .name(gc.partyDisbandName())
                    .lore(Component.empty(), Component.text("Click to disband!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(e -> {
                        SoundUtil.playGuiClick(player);
                        player.closeInventory();
                        plugin.getPartyManager().disbandParty(player);
                    }));
        } else {
            gui.setItem(3, 8, GuiUtil.cleanItem(gc.partyLeaveMat())
                    .name(gc.partyLeaveName())
                    .lore(Component.empty(), Component.text("Click to leave!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(e -> {
                        SoundUtil.playGuiClick(player);
                        player.closeInventory();
                        plugin.getPartyManager().leaveParty(player);
                    }));
        }

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }
}
