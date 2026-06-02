package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.arena.ArenaValidator;
import dev.shura.core.kit.Kit;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ShuraCommand implements CommandExecutor, TabCompleter {

    private static final List<String> INVENTORY_ITEM_TYPES = List.of("party-leader", "party-member", "lobby");
    private final ShuraCore plugin;

    public ShuraCommand(ShuraCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("shura.admin")) {
            if (sender instanceof Player p) plugin.getMessageService().send(p, "errors.no-permission");
            else sender.sendMessage("No permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "arena" -> handleArena(sender, args);
            case "kit" -> handleKit(sender, args);
            case "whitelist" -> handleWhitelist(sender, args);
            case "inventoryitems" -> handleInventoryItems(sender, args);
            case "interaction" -> handleInteraction(sender, args);
            case "autocloning" -> handleAutocloning(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    // ── RELOAD ──────────────────────────────────────────────────────────────
    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getMessageService().reload();
        plugin.getGuiConfig().reload();
        plugin.getScoreboardSoundsConfig().reload();
        plugin.getKitsArenasConfig().reload();
        plugin.getTierlistManager().reload();
        plugin.getGuiEditorManager().reloadGuis();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.registerCustomGuiCommands();
        }, 1L);
        sender.sendMessage(MessageService.colorizeComponent("&#00B4FF[ShuraCore] &aConfiguration reloaded."));
        if (sender instanceof Player p) SoundUtil.playSuccess(p);
    }

    // ── ARENA ───────────────────────────────────────────────────────────────
    private void handleArena(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Arena commands require a player.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /shura arena <create|delete|list|menu|settings>", NamedTextColor.RED));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> {
                if (args.length < 3) { player.sendMessage(Component.text("Usage: /shura arena create <name>", NamedTextColor.RED)); return; }
                String name = args[2];
                if (plugin.getArenaManager().getArenaByName(name) != null) {
                    player.sendMessage(Component.text("Arena with that name already exists.", NamedTextColor.RED));
                    return;
                }
                Arena arena = new Arena(UUID.randomUUID().toString(), name, player.getWorld().getName());
                plugin.getArenaManager().saveArena(arena);
                player.sendMessage(MessageService.colorizeComponent("&aArena &e" + name + " &acreated! Use &e/shura arena menu " + name + " &ato configure."));
                SoundUtil.playSuccess(player);
            }
            case "delete" -> {
                if (args.length < 3) { player.sendMessage(Component.text("Usage: /shura arena delete <name>", NamedTextColor.RED)); return; }
                Arena arena = plugin.getArenaManager().getArenaByName(args[2]);
                if (arena == null) { player.sendMessage(Component.text("Arena not found.", NamedTextColor.RED)); return; }
                plugin.getArenaManager().deleteArena(arena.getId());
                player.sendMessage(MessageService.colorizeComponent("&cArena &e" + args[2] + " &cdeleted."));
            }
            case "list" -> {
                player.sendMessage(MessageService.colorizeComponent("&#00B4FF&lArenas:"));
                plugin.getArenaManager().getAllArenas().forEach(a ->
                        player.sendMessage(MessageService.colorizeComponent(
                                "  &e" + a.getName() + " &8| &7Configured: " +
                                (a.isFullyConfigured() ? "&aYes" : "&cNo") +
                                " &8| &7Enabled: " + (a.isEnabled() ? "&aYes" : "&cNo") +
                                " &8| &7Copies: &f" + a.getCopies().size())));
            }
            case "settings" -> {
                if (args.length < 3) { player.sendMessage(Component.text("Usage: /shura arena settings <name>", NamedTextColor.RED)); return; }
                Arena arena = plugin.getArenaManager().getArenaByName(args[2]);
                if (arena == null) { player.sendMessage(Component.text("Arena not found.", NamedTextColor.RED)); return; }
                new dev.shura.core.gui.ArenaSettingsGui(plugin, arena).open(player);
            }
            case "menu" -> {
                if (args.length < 3) { player.sendMessage(Component.text("Usage: /shura arena menu <name>", NamedTextColor.RED)); return; }
                Arena arena = plugin.getArenaManager().getArenaByName(args[2]);
                if (arena == null) { player.sendMessage(Component.text("Arena not found.", NamedTextColor.RED)); return; }
                new dev.shura.core.gui.ArenaMenuGui(plugin, arena).open(player);
            }
            default -> player.sendMessage(Component.text("Unknown arena subcommand.", NamedTextColor.RED));
        }
    }

    // ── KIT ─────────────────────────────────────────────────────────────────
    private void handleKit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Kit commands require a player.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /shura kit <create|delete|list|update>", NamedTextColor.RED));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> {
                if (args.length < 3) { player.sendMessage(Component.text("Usage: /shura kit create <name> [tierlist]", NamedTextColor.RED)); return; }
                String tierlistId = args.length > 3 ? args[3] : null;
                Kit kit = plugin.getKitManager().createKit(args[2], tierlistId);
                player.sendMessage(MessageService.colorizeComponent("&aKit &e" + kit.getName() + " &acreated with ID: &7" + kit.getId()));
                SoundUtil.playSuccess(player);
            }
            case "delete" -> {
                if (args.length < 3) { player.sendMessage(Component.text("Usage: /shura kit delete <name>", NamedTextColor.RED)); return; }
                String searchName = args[2];
                Kit kit = findKitByNameOrFullId(searchName);
                if (kit == null) { player.sendMessage(Component.text("Kit not found.", NamedTextColor.RED)); return; }
                plugin.getKitManager().deleteKit(kit.getId());
                player.sendMessage(MessageService.colorizeComponent("&cKit &e" + kit.getName() + " &cdeleted."));
            }
            case "list" -> {
                if (args.length < 3) {
                    player.sendMessage(MessageService.colorizeComponent("&#00B4FF&lAll Kits:"));
                    var kac = plugin.getKitsArenasConfig();
                    for (String tierlistKey : kac.getTierlistIds()) {
                        for (String gamemodeKey : kac.getGamemodeKeys(tierlistKey)) {
                            String name = kac.getGamemodeName(tierlistKey, gamemodeKey);
                            String fullId = kac.getGamemodeFullId(tierlistKey, gamemodeKey);
                            Kit kit = findKitByName(name);
                            player.sendMessage(MessageService.colorizeComponent(
                                    "  &e" + name + " &8| &7ID: &f" + fullId +
                                    " &8| &7Status: " + (kit != null ? "&aConfigured" : "&cNot configured")));
                        }
                    }
                } else {
                    String tierlistId = args[2];
                    player.sendMessage(MessageService.colorizeComponent("&#00B4FF&lKits in " + tierlistId + ":"));
                    var kac = plugin.getKitsArenasConfig();
                    for (String gamemodeKey : kac.getGamemodeKeys(tierlistId)) {
                        String name = kac.getGamemodeName(tierlistId, gamemodeKey);
                        String fullId = kac.getGamemodeFullId(tierlistId, gamemodeKey);
                        Kit kit = findKitByName(name);
                        player.sendMessage(MessageService.colorizeComponent(
                                "  &e" + name + " &8| &7ID: &f" + fullId +
                                " &8| &7Status: " + (kit != null ? "&aConfigured" : "&cNot configured")));
                    }
                }
            }
            case "update" -> {
                if (args.length < 3) { player.sendMessage(Component.text("Usage: /shura kit update <full-id>", NamedTextColor.RED)); return; }
                String searchId = args[2];
                
                // Find kit by matching full-id from kits-arenas.yml
                Kit kit = plugin.getKitManager().getAllKits().stream()
                        .filter(k -> k.getId().equalsIgnoreCase(searchId))
                        .findFirst().orElse(null);
                
                if (kit == null) {
                    // Kit doesn't exist, find in kits-arenas.yml and create
                    var kac = plugin.getKitsArenasConfig();
                    for (String tKey : kac.getTierlistIds()) {
                        for (String gamemodeKey : kac.getGamemodeKeys(tKey)) {
                            String fullId = kac.getGamemodeFullId(tKey, gamemodeKey);
                            if (fullId.equalsIgnoreCase(searchId)) {
                                String name = kac.getGamemodeName(tKey, gamemodeKey);
                                kit = new Kit(fullId, name);
                                kit.setTierlistId(tKey);
                                player.sendMessage(MessageService.colorizeComponent("&eCreating new kit &a" + name + " &7(" + fullId + ")"));
                                break;
                            }
                        }
                        if (kit != null) break;
                    }
                }
                
                if (kit == null) { player.sendMessage(Component.text("Kit not found in kits-arenas.yml", NamedTextColor.RED)); return; }
                
                kit.setInventory(player.getInventory().getContents().clone());
                kit.setArmor(player.getInventory().getArmorContents().clone());
                kit.setOffhand(player.getInventory().getItemInOffHand().clone());
                plugin.getKitManager().saveKit(kit);
                player.sendMessage(MessageService.colorizeComponent("&aKit &e" + kit.getName() + " &7(" + kit.getId() + ") &aupdated."));
                SoundUtil.playSuccess(player);
            }
            default -> player.sendMessage(Component.text("Unknown kit subcommand.", NamedTextColor.RED));
        }
    }

    private Kit findKitByName(String name) {
        return plugin.getKitManager().getAllKits().stream()
                .filter(k -> k.getName().equalsIgnoreCase(name) || k.getId().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    private Kit findKitByNameOrFullId(String search) {
        var kac = plugin.getKitsArenasConfig();
        for (String tierlistKey : kac.getTierlistIds()) {
            for (String gamemodeKey : kac.getGamemodeKeys(tierlistKey)) {
                String name = kac.getGamemodeName(tierlistKey, gamemodeKey);
                String fullId = kac.getGamemodeFullId(tierlistKey, gamemodeKey);
                if (name.equalsIgnoreCase(search) || fullId.equalsIgnoreCase(search)) {
                    return plugin.getKitManager().getAllKits().stream()
                            .filter(k -> k.getId().equalsIgnoreCase(fullId) || 
                                       k.getName().equalsIgnoreCase(name) ||
                                       k.getName().equalsIgnoreCase(gamemodeKey))
                            .findFirst().orElse(null);
                }
            }
        }
        return findKitByName(search);
    }

    private void handleInventoryItems(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command requires a player.");
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("set")) {
            player.sendMessage(Component.text("Usage: /shura inventoryitems set <party-leader|party-member|lobby>", NamedTextColor.RED));
            return;
        }
        String type = args[2].toLowerCase();
        if (!INVENTORY_ITEM_TYPES.contains(type)) {
            player.sendMessage(Component.text("Invalid type. Use: party-leader, party-member, or lobby", NamedTextColor.RED));
            return;
        }
        plugin.getGuiConfig().setInventoryItems(type, player.getInventory().getContents());
        player.sendMessage(MessageService.colorizeComponent("&aInventory items for &e" + type + " &asaved."));
        SoundUtil.playSuccess(player);
    }

    private void handleInteraction(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /shura interaction <enable|disable>", NamedTextColor.RED));
            return;
        }
        boolean enable = args[1].equalsIgnoreCase("enable");
        plugin.getConfig().set("lobby-restrictions.enabled", enable);
        plugin.saveConfig();
        sender.sendMessage(MessageService.colorizeComponent(
                (enable ? "&aEnabled" : "&cDisabled") + " &7lobby interaction restrictions."));
        if (sender instanceof Player p) SoundUtil.playSuccess(p);
    }

    private void handleAutocloning(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /shura autocloning <arena> <clones>", NamedTextColor.RED));
            return;
        }
        Arena arena = plugin.getArenaManager().getArenaByName(args[1]);
        if (arena == null) {
            sender.sendMessage(Component.text("Arena not found.", NamedTextColor.RED));
            return;
        }
        try {
            int clones = Integer.parseInt(args[2]);
            arena.setAutoCloneCount(clones);
            plugin.getArenaManager().saveArena(arena);
            sender.sendMessage(MessageService.colorizeComponent(
                    "&aSet auto-clone count for &e" + arena.getName() + " &ato &e" + clones));
            if (sender instanceof Player p) SoundUtil.playSuccess(p);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number.", NamedTextColor.RED));
        }
    }

    private void handleWhitelist(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /shura whitelist <allow|deny> <group> <command>", NamedTextColor.RED));
            return;
        }
        boolean allow = args[1].equalsIgnoreCase("allow");
        String group = args[2];
        String cmd = args[3].toLowerCase();
        plugin.getWhitelistManager().setAllowed(group, cmd, allow);
        sender.sendMessage(MessageService.colorizeComponent(
                (allow ? "&a" : "&c") + "Command &e/" + cmd +
                (allow ? " &aallowed" : " &cdenied") + " for group &e" + group));
        if (sender instanceof Player p) SoundUtil.playSuccess(p);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageService.colorizeComponent("&#00B4FF&lShuraCore Admin Commands"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura reload &8— &7Reload configurations"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura arena <create|delete|list|menu|settings>"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura kit <create|delete|list|update>"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura whitelist <allow|deny> <group> <command>"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura inventoryitems set <party-leader|party-member|lobby>"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura interaction <enable|disable> &8— &7Toggle lobby restrictions"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura autocloning <arena> <clones> &8— &7Set auto-clone count"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("shura.admin")) return List.of();

        if (args.length == 1)
            return filter(List.of("reload", "arena", "kit", "whitelist", "inventoryitems", "interaction", "autocloning"), args[0]);

        switch (args[0].toLowerCase()) {
            case "arena" -> {
                if (args.length == 2)
                    return filter(List.of("create", "delete", "list", "menu", "settings"), args[1]);
                if (args.length == 3 && List.of("delete", "menu", "settings").contains(args[1].toLowerCase()))
                    return plugin.getArenaManager().getAllArenas().stream()
                            .map(a -> a.getName())
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
            }
            case "kit" -> {
                if (args.length == 2)
                    return filter(List.of("create", "delete", "list", "update"), args[1]);
                if (args.length == 3 && List.of("delete", "update").contains(args[1].toLowerCase())) {
                    List<String> names = new java.util.ArrayList<>();
                    var kac = plugin.getKitsArenasConfig();
                    for (String tierlistKey : kac.getTierlistIds()) {
                        for (String gamemodeKey : kac.getGamemodeKeys(tierlistKey)) {
                            names.add(kac.getGamemodeFullId(tierlistKey, gamemodeKey));
                        }
                    }
                    return names.stream()
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("list"))
                    return plugin.getKitsArenasConfig().getTierlistIds().stream()
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
            }
            case "whitelist" -> {
                if (args.length == 2) return filter(List.of("allow", "deny"), args[1]);
                if (args.length == 3) {
                    return plugin.getLuckPerms().getGroupManager().getLoadedGroups().stream()
                            .map(g -> g.getName())
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 4) {
                    return plugin.getServer().getCommandMap().getKnownCommands().keySet().stream()
                            .filter(c -> c.toLowerCase().startsWith(args[3].toLowerCase()))
                            .sorted()
                            .collect(Collectors.toList());
                }
            }
            case "inventoryitems" -> {
                if (args.length == 2) return filter(List.of("set"), args[1]);
                if (args.length == 3 && args[1].equalsIgnoreCase("set"))
                    return filter(INVENTORY_ITEM_TYPES, args[2]);
            }
            case "interaction" -> {
                if (args.length == 2) return filter(List.of("enable", "disable"), args[1]);
            }
            case "autocloning" -> {
                if (args.length == 2)
                    return plugin.getArenaManager().getAllArenas().stream()
                            .map(Arena::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                if (args.length == 3)
                    return filter(List.of("0", "1", "2", "3", "4", "5"), args[2]);
            }
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String input) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
