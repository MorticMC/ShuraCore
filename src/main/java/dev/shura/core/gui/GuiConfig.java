package dev.shura.core.gui;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class GuiConfig {

    private final ShuraCore plugin;
    private FileConfiguration config;

    public GuiConfig(ShuraCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("gui.yml", false);
            } catch (IllegalArgumentException e) {
                // gui.yml doesn't exist in resources, create empty config
                config = new YamlConfiguration();
                return;
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void setInventoryItems(String type, org.bukkit.inventory.ItemStack[] items) {
        config.set("inventory-items." + type, items);
        try {
            config.save(new File(plugin.getDataFolder(), "gui.yml"));
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Failed to save inventory items: " + e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public Component title(String path) {
        return MessageService.colorizeComponent(config.getString(path, "&cMissing: " + path));
    }

    public Material material(String path, Material fallback) {
        String val = config.getString(path);
        if (val == null) return fallback;
        try { return Material.valueOf(val.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    public Component name(String path) {
        return MessageService.colorizeComponent(config.getString(path, "&cMissing: " + path))
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    public String str(String path, String fallback) {
        return config.getString(path, fallback);
    }

    public int size(String path, int fallback) {
        return Math.min(6, Math.max(1, config.getInt(path, fallback)));
    }

    // ── Per-GUI convenience methods ───────────────────────────────────────────

    // Kit Selector
    public Component kitSelectorTitle()        { return title("kit-selector.title"); }
    public int kitSelectorRows()               { return size("kit-selector.rows", 4); }
    public Material kitSelectorBorder()        { return material("kit-selector.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public Material kitSelectorKitMaterial()   { return material("kit-selector.kit-item-material", Material.BOOK); }
    public Material kitSelectorPrevMaterial()  { return material("kit-selector.prev-page.material", Material.ARROW); }
    public Component kitSelectorPrevName()     { return name("kit-selector.prev-page.name"); }
    public Material kitSelectorNextMaterial()  { return material("kit-selector.next-page.material", Material.ARROW); }
    public Component kitSelectorNextName()     { return name("kit-selector.next-page.name"); }

    // Round Selector
    public Component roundSelectorTitle()      { return title("round-selector.title"); }
    public int roundSelectorRows()             { return size("round-selector.rows", 3); }
    public Material roundSelectorBorder()      { return material("round-selector.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public Material roundSelectorFormatMat()   { return material("round-selector.format-item-material", Material.PAPER); }
    public Material roundSelectorBackMat()     { return material("round-selector.back.material", Material.ARROW); }
    public Component roundSelectorBackName()   { return name("round-selector.back.name"); }

    // Duel Request
    public Component duelRequestTitle()        { return title("duel-request.title"); }
    public int duelRequestRows()               { return size("duel-request.rows", 1); }
    public Material duelRequestAcceptMat()     { return material("duel-request.accept.material", Material.LIME_STAINED_GLASS_PANE); }
    public Component duelRequestAcceptName()   { return name("duel-request.accept.name"); }
    public Material duelRequestDenyMat()       { return material("duel-request.deny.material", Material.RED_STAINED_GLASS_PANE); }
    public Component duelRequestDenyName()     { return name("duel-request.deny.name"); }

    // Queue Selector (legacy)
    public Component queueSelectorTitle()      { return title("queue-selector.title"); }
    public int queueSelectorRows()             { return size("queue-selector.rows", 3); }
    public Material queueSelectorBorder()      { return material("queue-selector.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public Material queueSelectorTierMat()     { return material("queue-selector.tierlist-item-material", Material.PAPER); }

    // Cracked Queue
    public Component crackedQueueTitle()       { return title("cracked-queue-selector.title"); }
    public Component crackedQueueTitleRanked() { return title("cracked-queue-selector.title-ranked"); }
    public Component crackedQueueTitleUnranked() { return title("cracked-queue-selector.title-unranked"); }
    public int crackedQueueRows()              { return size("cracked-queue-selector.rows", 3); }
    public Material crackedQueueBorder()       { return material("cracked-queue-selector.border-material", Material.BLUE_STAINED_GLASS_PANE); }
    public Material crackedQueueTierMat()      { return material("cracked-queue-selector.tierlist-item-material", Material.PAPER); }
    public int crackedQueueStartSlot()         { return config.getInt("cracked-queue-selector.start-slot", 10); }

    // Premium Queue
    public Component premiumQueueTitle()       { return title("premium-queue-selector.title"); }
    public Component premiumQueueTitleRanked() { return title("premium-queue-selector.title-ranked"); }
    public Component premiumQueueTitleUnranked() { return title("premium-queue-selector.title-unranked"); }
    public int premiumQueueRows()              { return size("premium-queue-selector.rows", 3); }
    public Material premiumQueueBorder()       { return material("premium-queue-selector.border-material", Material.YELLOW_STAINED_GLASS_PANE); }
    public Material premiumQueueTierMat()      { return material("premium-queue-selector.tierlist-item-material", Material.PAPER); }
    public int premiumQueueStartSlot()         { return config.getInt("premium-queue-selector.start-slot", 10); }

    // Practice
    public Component practiceTitle()           { return title("practice.title"); }
    public int practiceRows()                  { return size("practice.rows", 3); }
    public Material practiceBorder()           { return material("practice.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public Material practiceGamemodeMaterial(String id) { return material("practice.gamemodes." + id + ".material", Material.PAPER); }
    public Component practicGamemodeName(String id)     { return name("practice.gamemodes." + id + ".name"); }
    public int practicGamodeSlot(String id, int fallback) {
        org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection("practice.gamemodes." + id);
        return sec != null ? sec.getInt("slot", fallback) : fallback;
    }
    public List<String> practiceGamemodeLore(String id) {
        return config.getStringList("practice.gamemodes." + id + ".lore");
    }

    // Spectator
    public Component spectatorTitle()          { return title("spectator.title"); }
    public int spectatorRows()                 { return size("spectator.rows", 4); }
    public Material spectatorBorder()          { return material("spectator.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public Material spectatorMatchMat()        { return material("spectator.match-item-material", Material.PLAYER_HEAD); }
    public List<String> spectatorMatchLore()   { return config.getStringList("spectator.match-lore"); }
    public Material spectatorNoMatchMat()      { return material("spectator.no-matches.material", Material.BARRIER); }
    public Component spectatorNoMatchName()    { return name("spectator.no-matches.name"); }
    public int spectatorNoMatchSlot()          { return config.getInt("spectator.no-matches.slot", 13); }
    public Material spectatorPrevMat()         { return material("spectator.prev-page.material", Material.ARROW); }
    public Component spectatorPrevName()       { return name("spectator.prev-page.name"); }
    public int spectatorPrevSlot()             { return config.getInt("spectator.prev-page.slot", 30); }
    public Material spectatorNextMat()         { return material("spectator.next-page.material", Material.ARROW); }
    public Component spectatorNextName()       { return name("spectator.next-page.name"); }
    public int spectatorNextSlot()             { return config.getInt("spectator.next-page.slot", 32); }

    // Kit Selector
    public List<String> kitSelectorKitLore()   { return config.getStringList("kit-selector.kit-item-lore"); }
    public int kitSelectorPrevSlot()           { return config.getInt("kit-selector.prev-page.slot", 30); }
    public int kitSelectorNextSlot()           { return config.getInt("kit-selector.next-page.slot", 32); }

    // Round Selector
    public List<Integer> roundSelectorFormatSlots() {
        return config.getIntegerList("round-selector.format-slots");
    }
    public List<String> roundSelectorFormatLore() { return config.getStringList("round-selector.format-lore"); }
    public int roundSelectorBackSlot()         { return config.getInt("round-selector.back.slot", 22); }

    // Duel Request
    public int duelRequestAcceptSlot()         { return config.getInt("duel-request.accept.slot", 1); }
    public List<String> duelRequestAcceptLore(){ return config.getStringList("duel-request.accept.lore"); }
    public int duelRequestSkullSlot()          { return config.getInt("duel-request.skull.slot", 2); }
    public int duelRequestDenySlot()           { return config.getInt("duel-request.deny.slot", 3); }
    public List<String> duelRequestDenyLore()  { return config.getStringList("duel-request.deny.lore"); }

    // Queue Selector
    public int queueSelectorStartSlot()        { return config.getInt("queue-selector.start-slot", 10); }
    public List<String> queueSelectorLore()    { return config.getStringList("queue-selector.tierlist-lore"); }

    // Rankings
    public Component rankingsTitle()           { return title("rankings.title"); }
    public int rankingsRows()                  { return size("rankings.rows", 4); }
    public int rankingsTop10Rows()             { return size("rankings.top10-rows", 4); }
    public Material rankingsBorder()           { return material("rankings.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public Material rankingsTierMat()          { return material("rankings.tierlist-item-material", Material.PLAYER_HEAD); }
    public List<Integer> rankingsTierSlots()   { return config.getIntegerList("rankings.tierlist-slots"); }
    public List<String> rankingsTierLore()     { return config.getStringList("rankings.tierlist-lore"); }
    public Material rankingsTop10Border()      { return material("rankings.top10-border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public List<Integer> rankingsTop10Slots()  { return config.getIntegerList("rankings.top10-slots"); }
    public List<String> rankingsTop10Lore()    { return config.getStringList("rankings.top10-lore"); }
    public Material rankingsBackMat()          { return material("rankings.back.material", Material.ARROW); }
    public Component rankingsBackName()        { return name("rankings.back.name"); }
    public int rankingsBackSlot()              { return config.getInt("rankings.back.slot", 31); }

    // Party
    public Component partyTitle()             { return title("party.title"); }
    public int partyCreateRows()              { return size("party.create-rows", 3); }
    public int partyManageRows()              { return size("party.manage-rows", 4); }
    public Material partyBorder()             { return material("party.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public Material partyCreateMat()          { return material("party.create.material", Material.LIME_DYE); }
    public Component partyCreateName()        { return name("party.create.name"); }
    public int partyCreateSlot()              { return config.getInt("party.create.slot", 13); }
    public List<String> partyCreateLore()     { return config.getStringList("party.create.lore"); }
    public Material partyInfoMat()            { return material("party.info.material", Material.PAPER); }
    public Component partyInfoName()          { return name("party.info.name"); }
    public int partyInfoSlot()                { return config.getInt("party.info.slot", 22); }
    public List<String> partyInfoLore()       { return config.getStringList("party.info.lore"); }
    public Material partyInviteMat()          { return material("party.invite.material", Material.PLAYER_HEAD); }
    public Component partyInviteName()        { return name("party.invite.name"); }
    public int partyInviteSlot()              { return config.getInt("party.invite.slot", 19); }
    public List<String> partyInviteLore()     { return config.getStringList("party.invite.lore"); }
    public Material partyDisbandMat()         { return material("party.disband.material", Material.TNT); }
    public Component partyDisbandName()       { return name("party.disband.name"); }
    public int partyDisbandSlot()             { return config.getInt("party.disband.slot", 25); }
    public List<String> partyDisbandLore()    { return config.getStringList("party.disband.lore"); }
    public Material partyLeaveMat()           { return material("party.leave.material", Material.OAK_DOOR); }
    public Component partyLeaveName()         { return name("party.leave.name"); }
    public int partyLeaveSlot()               { return config.getInt("party.leave.slot", 25); }
    public List<String> partyLeaveLore()      { return config.getStringList("party.leave.lore"); }

    // Match End
    public Component matchEndTitle()          { return title("match-end.title"); }
    public int matchEndRows()                 { return size("match-end.rows", 3); }
    public Material matchEndBorder()          { return material("match-end.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public Material matchEndVictoryMat()      { return material("match-end.victory.material", Material.GOLDEN_SWORD); }
    public Component matchEndVictoryName()    { return name("match-end.victory.name"); }
    public int matchEndVictorySlot()          { return config.getInt("match-end.victory.slot", 11); }
    public List<String> matchEndVictoryLore() { return config.getStringList("match-end.victory.lore"); }
    public Material matchEndDefeatMat()       { return material("match-end.defeat.material", Material.BONE); }
    public Component matchEndDefeatName()     { return name("match-end.defeat.name"); }
    public int matchEndDefeatSlot()           { return config.getInt("match-end.defeat.slot", 11); }
    public List<String> matchEndDefeatLore()  { return config.getStringList("match-end.defeat.lore"); }
    public int matchEndEloSlot()              { return config.getInt("match-end.elo-change.slot", 13); }
    public List<String> matchEndEloLore()     { return config.getStringList("match-end.elo-change.lore"); }
    public int matchEndOpponentSlot()         { return config.getInt("match-end.opponent.slot", 15); }
    public List<String> matchEndOpponentLore(){ return config.getStringList("match-end.opponent.lore"); }
    public Material matchEndEloGainMat()      { return material("match-end.elo-gain-material", Material.EMERALD); }
    public Material matchEndEloLossMat()      { return material("match-end.elo-loss-material", Material.REDSTONE); }

    // Kit Editor
    public int kitEditorRows()                { return size("kit-editor.rows", 5); }
    public Material kitEditorSaveMat()        { return material("kit-editor.save.material", Material.LIME_DYE); }
    public Component kitEditorSaveName()      { return name("kit-editor.save.name"); }
    public int kitEditorSaveSlot()            { return config.getInt("kit-editor.save.slot", 31); }
    public List<String> kitEditorSaveLore()   { return config.getStringList("kit-editor.save.lore"); }
    public Material kitEditorResetMat()       { return material("kit-editor.reset.material", Material.RED_DYE); }
    public Component kitEditorResetName()     { return name("kit-editor.reset.name"); }
    public int kitEditorResetSlot()           { return config.getInt("kit-editor.reset.slot", 33); }
    public List<String> kitEditorResetLore()  { return config.getStringList("kit-editor.reset.lore"); }
    public Material kitEditorBackMat()        { return material("kit-editor.back.material", Material.ARROW); }
    public Component kitEditorBackName()      { return name("kit-editor.back.name"); }
    public int kitEditorBackSlot()            { return config.getInt("kit-editor.back.slot", 35); }

    // Arena Editor
    public int arenaEditorRows()              { return size("arena-editor.rows", 3); }
    public Material arenaEditorBorder()       { return material("arena-editor.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public Material arenaEditorEnabledMat()   { return material("arena-editor.enabled-material", Material.LIME_DYE); }
    public Material arenaEditorDisabledMat()  { return material("arena-editor.disabled-material", Material.RED_DYE); }
    public int arenaEditorToggleSlot()        { return config.getInt("arena-editor.toggle-slot", 17); }
    public List<String> arenaEditorToggleLore(){ return config.getStringList("arena-editor.toggle-lore"); }
    public Material arenaEditorSaveMat()      { return material("arena-editor.save.material", Material.LIME_DYE); }
    public Component arenaEditorSaveName()    { return name("arena-editor.save.name"); }
    public int arenaEditorSaveSlot()          { return config.getInt("arena-editor.save.slot", 22); }
    public Material arenaEditorDeleteMat()    { return material("arena-editor.delete.material", Material.TNT); }
    public Component arenaEditorDeleteName()  { return name("arena-editor.delete.name"); }
    public int arenaEditorDeleteSlot()        { return config.getInt("arena-editor.delete.slot", 24); }
    public List<String> arenaEditorDeleteLore(){ return config.getStringList("arena-editor.delete.lore"); }

    // Confirmation
    public int confirmationRows()             { return size("confirmation.rows", 3); }
    public Material confirmationBorder()      { return material("confirmation.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public Material confirmationInfoMat()     { return material("confirmation.info-material", Material.PAPER); }
    public int confirmationInfoSlot()         { return config.getInt("confirmation.info-slot", 13); }
    public Material confirmationConfirmMat()  { return material("confirmation.confirm.material", Material.LIME_STAINED_GLASS_PANE); }
    public Component confirmationConfirmName(){ return name("confirmation.confirm.name"); }
    public int confirmationConfirmSlot()      { return config.getInt("confirmation.confirm.slot", 11); }
    public Material confirmationCancelMat()   { return material("confirmation.cancel.material", Material.RED_STAINED_GLASS_PANE); }
    public Component confirmationCancelName() { return name("confirmation.cancel.name"); }
    public int confirmationCancelSlot()       { return config.getInt("confirmation.cancel.slot", 15); }

    // Whitelist
    public int whitelistRows()                { return size("whitelist.rows", 5); }
    public Material whitelistBorder()         { return material("whitelist.border-material", Material.GRAY_STAINED_GLASS_PANE); }

    // Profile
    public Component profileTitle()           { return title("profile.title"); }
    public int profileRows()                  { return size("profile.rows", 4); }
    public Material profileBorder()           { return material("profile.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public int profileSkullSlot()             { return config.getInt("profile.skull-slot", 10); }
    public List<Integer> profileTierSlots()   { return config.getIntegerList("profile.tier-slots"); }
    public int profileSettingsShortcutSlot()  { return config.getInt("profile.settings-shortcut-slot", 28); }

    // Settings
    public Component settingsTitle()          { return title("settings.title"); }
    public int settingsRows()                 { return size("settings.rows", 3); }
    public Material settingsBorder()          { return material("settings.border-material", Material.GRAY_STAINED_GLASS_PANE); }
    public Material settingsEnabledMat()      { return material("settings.enabled-material", Material.LIME_DYE); }
    public Material settingsDisabledMat()     { return material("settings.disabled-material", Material.RED_DYE); }
    public int settingsScoreboardSlot()       { return config.getInt("settings.scoreboard-slot", 11); }
    public int settingsTabSlot()              { return config.getInt("settings.tab-slot", 12); }
    public int settingsPartyInvitesSlot()     { return config.getInt("settings.party-invites-slot", 14); }
    public int settingsDuelRequestsSlot()     { return config.getInt("settings.duel-requests-slot", 15); }

    // Queue Mode Selector
    public Component queueModeSelectorTitle() { return title("queue-mode-selector.title"); }
    public Material queueModeSelectorRankedMat() { return material("queue-mode-selector.ranked.material", Material.GOLD_INGOT); }
    public Component queueModeSelectorRankedName() { return name("queue-mode-selector.ranked.name"); }
    public int queueModeSelectorRankedSlot()  { return config.getInt("queue-mode-selector.ranked.slot", 1); }
    public List<String> queueModeSelectorRankedLore() { return config.getStringList("queue-mode-selector.ranked.lore"); }
    public Material queueModeSelectorUnrankedMat() { return material("queue-mode-selector.unranked.material", Material.EMERALD); }
    public Component queueModeSelectorUnrankedName() { return name("queue-mode-selector.unranked.name"); }
    public int queueModeSelectorUnrankedSlot() { return config.getInt("queue-mode-selector.unranked.slot", 3); }
    public List<String> queueModeSelectorUnrankedLore() { return config.getStringList("queue-mode-selector.unranked.lore"); }

    // Whitelist
    public Material whitelistAllowedMat()     { return material("whitelist.allowed-material", Material.LIME_DYE); }
    public Material whitelistDeniedMat()      { return material("whitelist.denied-material", Material.RED_DYE); }
    public List<String> whitelistItemLore()   { return config.getStringList("whitelist.item-lore"); }
    public Material whitelistPrevMat()        { return material("whitelist.prev-page.material", Material.ARROW); }
    public Component whitelistPrevName()      { return name("whitelist.prev-page.name"); }
    public int whitelistPrevSlot()            { return config.getInt("whitelist.prev-page.slot", 38); }
    public Material whitelistNextMat()        { return material("whitelist.next-page.material", Material.ARROW); }
    public Component whitelistNextName()      { return name("whitelist.next-page.name"); }
    public int whitelistNextSlot()            { return config.getInt("whitelist.next-page.slot", 42); }
}
