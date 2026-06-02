package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.MessageService;
import dev.shura.core.id.PlayerID;
import dev.shura.core.util.GuiUtil;
import dev.shura.core.util.ItemBuilder;
import dev.shura.core.util.SoundUtil;
import dev.triumphteam.gui.guis.Gui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class IDPlayerGui {

    private final ShuraCore plugin;
    private final Player player;
    private final PlayerID playerID;

    public IDPlayerGui(ShuraCore plugin, Player player, PlayerID playerID) {
        this.plugin = plugin;
        this.player = player;
        this.playerID = playerID;
    }

    public void open() {
        Gui gui = Gui.gui()
                .title(MessageService.colorizeComponent("Player ID"))
                .rows(6)
                .disableAllInteractions()
                .create();

        // Fill borders with black glass
        fillBorders(gui);
        
        // Fill center with glass panes
        for (int i : new int[]{10, 11, 12, 13, 14, 15, 16, 20, 28, 29, 30, 32, 33}) {
            gui.setItem(i, GuiUtil.cleanItem(Material.GLASS_PANE).name(Component.empty()).asGuiItem());
        }

        // Player Head (slot 34)
        gui.setItem(34, dev.triumphteam.gui.builder.item.ItemBuilder.skull()
                .owner(player)
                .name(MessageService.colorizeComponent("&#fff000☻ &#FFFF00Friends Count &#404040| &#FFFFFF0").decoration(TextDecoration.ITALIC, false))
                .flags(org.bukkit.inventory.ItemFlag.values())
                .asGuiItem());

        // Gender (slot 19)
        gui.setItem(19, GuiUtil.cleanItem(Material.ARMOR_STAND)
                .name(MessageService.colorizeComponent("&#fff000♂/♀ &#FFFF00Gender &#404040| &#FFFFFF" + playerID.getGender()).decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        // Alts (slot 21)
        List<Component> altsLore = new ArrayList<>();
        altsLore.add(MessageService.colorizeComponent("&#ff0000Suspended &#404040| §7Offline &#404040| §aOnline").decoration(TextDecoration.ITALIC, false));
        altsLore.add(MessageService.colorizeComponent("&#5c5c5c=&#6b6b6b=&#7a7a7a=&#888888=&#979797=&#a6a6a6=&#b5b5b5=&#c4c4c4=&#d3d3d3=&#e1e1e1=&#f0f0f0=§f=&#f1f1f1=&#e4e4e4=&#d6d6d6=&#c8c8c8=&#bababa=&#adadad=&#9f9f9f=&#919191=&#838383=&#767676=&#686868=").decoration(TextDecoration.ITALIC, false));
        altsLore.add(Component.text("§7No alts detected").decoration(TextDecoration.ITALIC, false));
        
        gui.setItem(21, GuiUtil.cleanItem(Material.DARK_OAK_HANGING_SIGN)
                .name(MessageService.colorizeComponent("&#fff000👥 &#FFFF00Alts").decoration(TextDecoration.ITALIC, false))
                .lore(altsLore)
                .asGuiItem());

        // Warnings (slot 22)
        gui.setItem(22, GuiUtil.cleanItem(Material.MANGROVE_HANGING_SIGN)
                .name(MessageService.colorizeComponent("&#fff000§l❕ &#FFFF00Warnings Count &#404040| &#FFFFFF0").decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        // Mutes (slot 23)
        gui.setItem(23, GuiUtil.cleanItem(Material.SPRUCE_HANGING_SIGN)
                .name(MessageService.colorizeComponent("&#fff000§l🔇 &#FFFF00Mute Count &#404040| &#FFFF000").decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        // Bans (slot 24)
        gui.setItem(24, GuiUtil.cleanItem(Material.JUNGLE_HANGING_SIGN)
                .name(MessageService.colorizeComponent("&#fff000⚠ &#FFFF00Ban Count &#404040| &#FFFFFF0").decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        // Joined Date (slot 25)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String joinDate = playerID.getRegistrationDate().format(formatter);
        
        gui.setItem(25, GuiUtil.cleanItem(Material.OAK_HANGING_SIGN)
                .name(MessageService.colorizeComponent("&#fff000◎ &#FFFF00Joined The Server At &#404040| &#FFFFFF" + joinDate).decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        // Expiry Date (slot 31)
        String expiryDate = playerID.getExpiryDate().format(formatter);
        
        gui.setItem(31, GuiUtil.cleanItem(Material.DAMAGED_ANVIL)
                .name(MessageService.colorizeComponent("&#fff000⌚ &#FFFF00Expiry Date&#404040 | &#FFFFFF" + expiryDate).decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        // Discord Username (slot 32)
        gui.setItem(32, GuiUtil.cleanItem(Material.DRAGON_BREATH)
                .name(MessageService.colorizeComponent("&#fff000👤 &#FFFF00Discord Username &#404040| &#FFFFFF" + playerID.getDiscordUsername()).decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        // Combat Log Count (slot 33)
        gui.setItem(33, GuiUtil.cleanItem(Material.IRON_HORSE_ARMOR)
                .name(MessageService.colorizeComponent("&#fff000🪓 &#FFFF00Combat Log Count &#404040| &#FFFFFF0").decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        // IRL Name (slot 46)
        gui.setItem(46, dev.triumphteam.gui.builder.item.ItemBuilder.skull()
                .owner(player)
                .name(MessageService.colorizeComponent("&#FFD700🪪 &#fff000IRL Name &#404040| &#FFFF00" + playerID.getIrlName()).decoration(TextDecoration.ITALIC, false))
                .flags(org.bukkit.inventory.ItemFlag.values())
                .asGuiItem());

        // Age (slot 47)
        gui.setItem(47, GuiUtil.cleanItem(Material.CLOCK)
                .name(MessageService.colorizeComponent("&#FFD700⏳ &#fff000Age &#404040| &#FFFF00" + playerID.getAge()).decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        // ID Registration Date (slot 48)
        gui.setItem(48, GuiUtil.cleanItem(Material.MAP)
                .name(MessageService.colorizeComponent("&#FFD700🪪 &#fff000Date Of ID Registration &#404040| &#FFFF00" + joinDate).decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        // Country (slot 49)
        gui.setItem(49, GuiUtil.cleanItem(Material.FILLED_MAP)
                .name(MessageService.colorizeComponent("&#FFD700⚐ &#fff000Country &#404040| &#FFFF00" + playerID.getCountry()).decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        // ID Number (slot 50) - Clickable to reveal
        gui.setItem(50, GuiUtil.cleanItem(Material.KNOWLEDGE_BOOK)
                .name(MessageService.colorizeComponent("&#FFD700🪪 &#fff000ID Number &#404040| &#FFFF00xxxxxxxxxxxxx").decoration(TextDecoration.ITALIC, false))
                .lore(Component.empty(), MessageService.colorizeComponent("&#FFD700▟§f▙ LMB To View ID Number").decoration(TextDecoration.ITALIC, false))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    player.sendMessage(MessageService.colorize("&#FFD700Your ID Number: &#FFFF00" + playerID.getIdNumber()));
                    SoundUtil.playGuiClick(player);
                }));

        // Account Status (slot 52)
        gui.setItem(52, GuiUtil.cleanItem(Material.COMMAND_BLOCK)
                .name(MessageService.colorizeComponent("&#FFD700👤 &#fff000Account Status &#404040| &#FFFF00All Good ☑").decoration(TextDecoration.ITALIC, false))
                .asGuiItem());

        gui.open(player);
        SoundUtil.playGuiOpen(player);
    }

    private void fillBorders(Gui gui) {
        Material border = Material.BLACK_STAINED_GLASS_PANE;
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
            gui.setItem(45 + i, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
        }
        
        // Side columns
        for (int i : new int[]{9, 17, 18, 26, 27, 35, 36, 44}) {
            gui.setItem(i, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
        }
        
        // Specific slots
        gui.setItem(37, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
        gui.setItem(38, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
        gui.setItem(39, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
        gui.setItem(40, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
        gui.setItem(41, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
        gui.setItem(42, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
        gui.setItem(43, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
        gui.setItem(51, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
        gui.setItem(53, GuiUtil.cleanItem(border).name(Component.empty()).asGuiItem());
    }
}
