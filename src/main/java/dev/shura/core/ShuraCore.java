package dev.shura.core;

import dev.shura.core.arena.ArenaManager;
import dev.shura.core.database.DatabaseService;
import dev.shura.core.kit.KitManager;
import dev.shura.core.maintenance.MaintenanceManager;
import dev.shura.core.match.MatchManager;
import dev.shura.core.party.PartyManager;
import dev.shura.core.practice.PracticeManager;
import dev.shura.core.profile.ProfileManager;
import dev.shura.core.queue.QueueManager;
import dev.shura.core.scoreboard.BoardManager;
import dev.shura.core.spectator.SpectatorManager;
import dev.shura.core.tierlist.TierlistManager;
import dev.shura.core.whitelist.WhitelistManager;
import dev.shura.core.config.ScoreboardSoundsConfig;
import dev.shura.core.config.KitsArenasConfig;
import dev.shura.core.gui.GuiConfig;
import dev.shura.core.gui.editor.GuiEditorManager;
import dev.shura.core.message.MessageService;
import dev.shura.core.duel.DuelManager;
import dev.shura.core.tab.TabManager;
import dev.shura.core.chat.ChatManager;
import dev.shura.core.kit.KitEditor;
import dev.shura.core.command.*;
import dev.shura.core.command.util.*;
import dev.shura.core.listener.*;
import dev.shura.core.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShuraCore extends JavaPlugin {

    private static ShuraCore instance;
    private LuckPerms luckPerms;

    private DatabaseService databaseService;
    private ProfileManager profileManager;
    private KitManager kitManager;
    private ArenaManager arenaManager;
    private MatchManager matchManager;
    private QueueManager queueManager;
    private PartyManager partyManager;
    private SpectatorManager spectatorManager;
    private PracticeManager practiceManager;
    private TierlistManager tierlistManager;
    private BoardManager boardManager;
    private WhitelistManager whitelistManager;
    private MaintenanceManager maintenanceManager;
    private MessageService messageService;
    private GuiConfig guiConfig;
    private ScoreboardSoundsConfig scoreboardSoundsConfig;
    private KitsArenasConfig kitsArenasConfig;
    private DuelManager duelManager;
    private PlayerListener playerListener;
    private TabManager tabManager;
    private ChatManager chatManager;
    private KitEditor kitEditor;
    private GuiEditorManager guiEditorManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms not found! Disabling ShuraCore.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        messageService = new MessageService(this);
        guiConfig = new GuiConfig(this);
        scoreboardSoundsConfig = new ScoreboardSoundsConfig(this);
        kitsArenasConfig = new KitsArenasConfig(this);
        dev.shura.core.util.SoundUtil.init(this);
        dev.shura.core.lobby.LobbyItems.init(this);
        databaseService = new DatabaseService(this);
        if (!databaseService.connect()) {
            getLogger().severe("Failed to connect to database! Disabling ShuraCore.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        profileManager = new ProfileManager(this);
        kitManager = new KitManager(this);
        arenaManager = new ArenaManager(this);
        tierlistManager = new TierlistManager(this);
        matchManager = new MatchManager(this);
        queueManager = new QueueManager(this);
        partyManager = new PartyManager(this);
        spectatorManager = new SpectatorManager(this);
        practiceManager = new PracticeManager(this);
        boardManager = new BoardManager(this);
        whitelistManager = new WhitelistManager(this);
        maintenanceManager = new MaintenanceManager(this);
        duelManager = new DuelManager(this);
        tabManager = new TabManager(this);
        chatManager = new ChatManager(this);
        kitEditor = new KitEditor(this);
        guiEditorManager = new GuiEditorManager(this);

        registerCommands();
        registerListeners();
        registerCustomGuiCommands();
    }

    @Override
    public void onDisable() {
        if (matchManager != null) matchManager.endAllMatches();
        if (queueManager != null) queueManager.clearAll();
        if (databaseService != null) databaseService.disconnect();
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = getServer()
                .getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) return false;
        luckPerms = provider.getProvider();
        return true;
    }

    private void registerCommands() {
        DuelCommand duelCommand = new DuelCommand(this);
        getCommand("duel").setExecutor(duelCommand);
        getCommand("duel").setTabCompleter(duelCommand);
        getCommand("duelaccept").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player p) getDuelManager().openRequestGui(p);
            return true;
        });
        getCommand("dueldeny").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player p) getDuelManager().denyRequest(p);
            return true;
        });

        CrackedQueueCommand crackedQueueCommand = new CrackedQueueCommand(this);
        getCommand("crackedqueue").setExecutor(crackedQueueCommand);
        getCommand("crackedqueue").setTabCompleter(crackedQueueCommand);
        
        PremiumQueueCommand premiumQueueCommand = new PremiumQueueCommand(this);
        getCommand("premiumqueue").setExecutor(premiumQueueCommand);
        getCommand("premiumqueue").setTabCompleter(premiumQueueCommand);

        PartyCommand partyCommand = new PartyCommand(this);
        getCommand("party").setExecutor(partyCommand);
        getCommand("party").setTabCompleter(partyCommand);

        PracticeCommand practiceCommand = new PracticeCommand(this);
        getCommand("practice").setExecutor(practiceCommand);
        getCommand("practice").setTabCompleter(practiceCommand);

        getCommand("rankings").setExecutor(new RankingsCommand(this));

        ShuraCommand shuraCommand = new ShuraCommand(this);
        getCommand("shura").setExecutor(shuraCommand);
        getCommand("shura").setTabCompleter(shuraCommand);

        GuiEditorCommand guiEditorCommand = new GuiEditorCommand(this);
        getCommand("shuragui").setExecutor(guiEditorCommand);
        getCommand("shuragui").setTabCompleter(guiEditorCommand);

        getCommand("spectate").setExecutor(new SpectateCommand(this));

        GamemodeCommand gm = new GamemodeCommand(this);
        getCommand("gmc").setExecutor(gm);
        getCommand("gms").setExecutor(gm);
        getCommand("gma").setExecutor(gm);
        getCommand("gmsp").setExecutor(gm);

        getCommand("fly").setExecutor(new FlyCommand(this));
        getCommand("flyspeed").setExecutor(new FlySpeedCommand(this));
        getCommand("walkspeed").setExecutor(new WalkSpeedCommand(this));
        getCommand("heal").setExecutor(new HealCommand(this));
        getCommand("feed").setExecutor(new FeedCommand(this));
        getCommand("god").setExecutor(new GodCommand(this));
        getCommand("vanish").setExecutor(new VanishCommand(this));
        getCommand("invsee").setExecutor(new InvSeeCommand(this));
        getCommand("enderchest").setExecutor(new EnderChestCommand(this));
        getCommand("tp").setExecutor(new TeleportCommand(this));
        TpaCommand tpaCommand = new TpaCommand(this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpahere").setExecutor(tpaCommand);
        getCommand("tpaccept").setExecutor(tpaCommand);
        getCommand("tpdeny").setExecutor(tpaCommand);
        getCommand("spawn").setExecutor(new SpawnCommand(this, false));
        getCommand("setspawn").setExecutor(new SpawnCommand(this, true));
        getCommand("back").setExecutor(new BackCommand(this));
        getCommand("msg").setExecutor(new MessageCommand(this));
        getCommand("r").setExecutor(new MessageCommand(this));
        getCommand("broadcast").setExecutor(new BroadcastCommand(this));
        getCommand("staffchat").setExecutor(new StaffChatCommand(this));
        getCommand("maintenance").setExecutor(new MaintenanceCommand(this));
        getCommand("settings").setExecutor(new SettingsCommand(this));
        getCommand("profile").setExecutor(new ProfileCommand(this));
        getCommand("leavequeue").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player p) getQueueManager().leaveAllQueues(p);
            return true;
        });
        getCommand("leave").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) return true;
            if (getQueueManager().isInQueue(p.getUniqueId())) {
                getQueueManager().leaveAllQueues(p);
            } else if (getSpectatorManager().isSpectating(p.getUniqueId())) {
                getSpectatorManager().removeSpectator(p);
            } else if (getDuelManager().hasRequest(p.getUniqueId())) {
                getDuelManager().cleanup(p.getUniqueId());
                p.sendMessage(Component.text("Duel requests cleared.", NamedTextColor.YELLOW));
            } else {
                p.sendMessage(Component.text("Nothing to leave.", NamedTextColor.RED));
            }
            return true;
        });
    }

    private void registerListeners() {
        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(new MatchListener(this), this);
        getServer().getPluginManager().registerEvents(new QueueListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaWandListener(this), this);
        getServer().getPluginManager().registerEvents(new PracticeListener(this), this);
        getServer().getPluginManager().registerEvents(new PartyListener(this), this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandWhitelistListener(this), this);
        getServer().getPluginManager().registerEvents(new MaintenanceListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiEditorListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomGuiListener(this), this);
        dev.shura.core.gui.QueueModeSelectGui.registerListener(this);
    }

    private void registerCustomGuiCommands() {
        for (String guiName : guiEditorManager.getGuiNames()) {
            dev.shura.core.gui.editor.CustomGui gui = guiEditorManager.getGui(guiName);
            if (gui.getCommand() != null) {
                try {
                    getCommand(gui.getCommand()).setExecutor(new CustomGuiCommand(this, guiName));
                } catch (Exception e) {
                    getLogger().warning("Failed to register command for GUI: " + guiName);
                }
            }
        }
    }

    public static ShuraCore getInstance() { return instance; }
    public LuckPerms getLuckPerms() { return luckPerms; }
    public DatabaseService getDatabaseService() { return databaseService; }
    public ProfileManager getProfileManager() { return profileManager; }
    public KitManager getKitManager() { return kitManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public MatchManager getMatchManager() { return matchManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public SpectatorManager getSpectatorManager() { return spectatorManager; }
    public PracticeManager getPracticeManager() { return practiceManager; }
    public TierlistManager getTierlistManager() { return tierlistManager; }
    public BoardManager getBoardManager() { return boardManager; }
    public WhitelistManager getWhitelistManager() { return whitelistManager; }
    public MaintenanceManager getMaintenanceManager() { return maintenanceManager; }
    public ScoreboardSoundsConfig getScoreboardSoundsConfig() { return scoreboardSoundsConfig; }
    public GuiConfig getGuiConfig() { return guiConfig; }
    public MessageService getMessageService() { return messageService; }
    public KitsArenasConfig getKitsArenasConfig() { return kitsArenasConfig; }
    public DuelManager getDuelManager() { return duelManager; }
    public PlayerListener getPlayerListener() { return playerListener; }
    public TabManager getTabManager() { return tabManager; }
    public ChatManager getChatManager() { return chatManager; }
    public KitEditor getKitEditor() { return kitEditor; }
    public GuiEditorManager getGuiEditorManager() { return guiEditorManager; }
    public dev.shura.core.lobby.LobbyItems getLobbyItems() { return new dev.shura.core.lobby.LobbyItems(); }
}
