package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.arena.Arena;
import dev.shura.core.arena.ArenaValidator;
import dev.shura.core.arena.ArenaWand;
import dev.shura.core.kit.Kit;
import dev.shura.core.message.MessageService;
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
            case "wand" -> handleWand(sender);
            case "practice" -> handlePractice(sender, args);
            case "inventoryitems" -> handleInventoryItems(sender, args);
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
        plugin.getGuiEditorManager().reloadGuis();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ((ShuraCore) plugin).registerCustomGuiCommands();
        }, 1L);
        sender.sendMessage(MessageService.colorizeComponent("&#00B4FF[ShuraCore] &aConfiguration reloaded."));
        sender.sendMessage(MessageService.colorizeComponent("  &7• config.yml"));
        sender.sendMessage(MessageService.colorizeComponent("  &7• messages.yml"));
        sender.sendMessage(MessageService.colorizeComponent("  &7• gui.yml"));
        sender.sendMessage(MessageService.colorizeComponent("  &7• scoreboards-sounds.yml"));
        sender.sendMessage(MessageService.colorizeComponent("  &7• kits-arenas.yml"));
        sender.sendMessage(MessageService.colorizeComponent("  &7• custom-guis/"));
        if (sender instanceof Player p) SoundUtil.playSuccess(p);
    }

    // ── ARENA ───────────────────────────────────────────────────────────────
    private void handleArena(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Arena commands require a player.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /shura arena <create|setspawn|save|delete|list|enable|disable>", NamedTextColor.RED));
            return;
        }

        ArenaWand wand = plugin.getArenaManager().getArenaWand();

        switch (args[1].toLowerCase()) {
            case "create" -> {
                if (args.length < 3) { player.sendMessage(Component.text("Usage: /shura arena create <name>", NamedTextColor.RED)); return; }
                if (!wand.hasBothPositions(player)) {
                    player.sendMessage(Component.text("Use the arena wand to set pos1 and pos2 first.", NamedTextColor.RED));
                    return;
                }
                String name = args[2];
                Arena arena = new Arena(UUID.randomUUID().toString(), name, player.getWorld().getName());
                arena.setPos1(wand.getPos1(player));
                arena.setPos2(wand.getPos2(player));
                plugin.getArenaManager().saveArena(arena);
                wand.clear(player);
                player.sendMessage(MessageService.colorizeComponent("&aArena &e" + name + " &acreated! Now set spawns with &e/shura arena setspawn A/B"));
                SoundUtil.playSuccess(player);
            }
            case "setspawn" -> {
                if (args.length < 3) { player.sendMessage(Component.text("Usage: /shura arena setspawn <A|B> [arena]", NamedTextColor.RED)); return; }
                String side = args[1].equalsIgnoreCase("setspawn") && args.length > 2 ? args[2] : "A";
                String arenaName = args.length > 3 ? args[3] : null;

                // Find arena being edited — use last created or specified
                Arena arena = arenaName != null
                        ? plugin.getArenaManager().getArenaByName(arenaName)
                        : plugin.getArenaManager().getAllArenas().stream().reduce((a, b) -> b).orElse(null);

                if (arena == null) { player.sendMessage(Component.text("Arena not found.", NamedTextColor.RED)); return; }

                if (args[2].equalsIgnoreCase("A")) {
                    arena.setSpawnA(player.getLocation());
                    player.sendMessage(MessageService.colorizeComponent("&aSpawn A set for arena &e" + arena.getName()));
                } else {
                    arena.setSpawnB(player.getLocation());
                    player.sendMessage(MessageService.colorizeComponent("&aSpawn B set for arena &e" + arena.getName()));
                }
                plugin.getArenaManager().saveArena(arena);
                SoundUtil.playSuccess(player);
            }
            case "save" -> {
                if (args.length < 3) { player.sendMessage(Component.text("Usage: /shura arena save <name>", NamedTextColor.RED)); return; }
                Arena arena = plugin.getArenaManager().getArenaByName(args[2]);
                if (arena == null) { player.sendMessage(Component.text("Arena not found.", NamedTextColor.RED)); return; }
                ArenaValidator.ValidationResult result = ArenaValidator.validate(arena);
                if (!result.valid()) {
                    player.sendMessage(Component.text("Arena validation failed:", NamedTextColor.RED));
                    result.errors().forEach(e -> player.sendMessage(Component.text("  - " + e, NamedTextColor.YELLOW)));
                    return;
                }
                plugin.getArenaManager().saveArena(arena);
                player.sendMessage(MessageService.colorizeComponent("&aArena &e" + arena.getName() + " &asaved."));
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
            case "enable" -> {
                if (args.length < 3) return;
                Arena arena = plugin.getArenaManager().getArenaByName(args[2]);
                if (arena == null) { player.sendMessage(Component.text("Arena not found.", NamedTextColor.RED)); return; }
                arena.setEnabled(true);
                plugin.getArenaManager().saveArena(arena);
                player.sendMessage(MessageService.colorizeComponent("&aArena &e" + arena.getName() + " &aenabled."));
            }
            case "disable" -> {
                if (args.length < 3) return;
                Arena arena = plugin.getArenaManager().getArenaByName(args[2]);
                if (arena == null) { player.sendMessage(Component.text("Arena not found.", NamedTextColor.RED)); return; }
                arena.setEnabled(false);
                plugin.getArenaManager().saveArena(arena);
                player.sendMessage(MessageService.colorizeComponent("&cArena &e" + arena.getName() + " &cdisabled."));
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
            player.sendMessage(Component.text("Usage: /shura kit <create|delete|list|settierlist>", NamedTextColor.RED));
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
                Kit kit = plugin.getKitManager().getAllKits().stream()
                        .filter(k -> k.getName().equalsIgnoreCase(args[2]))
                        .findFirst().orElse(null);
                if (kit == null) { player.sendMessage(Component.text("Kit not found.", NamedTextColor.RED)); return; }
                plugin.getKitManager().deleteKit(kit.getId());
                player.sendMessage(MessageService.colorizeComponent("&cKit &e" + args[2] + " &cdeleted."));
            }
            case "list" -> {
                player.sendMessage(MessageService.colorizeComponent("&#00B4FF&lKits:"));
                plugin.getKitManager().getAllKits().forEach(k ->
                        player.sendMessage(MessageService.colorizeComponent(
                                "  &e" + k.getName() + " &8| &7Tierlist: &f" +
                                (k.getTierlistId() != null ? k.getTierlistId() : "none"))));
            }
            case "settierlist" -> {
                if (args.length < 4) { player.sendMessage(Component.text("Usage: /shura kit settierlist <kit> <tierlist>", NamedTextColor.RED)); return; }
                Kit kit = plugin.getKitManager().getAllKits().stream()
                        .filter(k -> k.getName().equalsIgnoreCase(args[2]))
                        .findFirst().orElse(null);
                if (kit == null) { player.sendMessage(Component.text("Kit not found.", NamedTextColor.RED)); return; }
                kit.setTierlistId(args[3]);
                plugin.getKitManager().saveKit(kit);
                player.sendMessage(MessageService.colorizeComponent("&aKit &e" + kit.getName() + " &alinked to tierlist &e" + args[3]));
            }
            default -> player.sendMessage(Component.text("Unknown kit subcommand.", NamedTextColor.RED));
        }
    }

    // ── WHITELIST ────────────────────────────────────────────────────────────
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

    // ── WAND ─────────────────────────────────────────────────────────────────
    private void handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Wand command requires a player.");
            return;
        }
        player.getInventory().addItem(ArenaWand.WAND_ITEM);
        player.sendMessage(MessageService.colorizeComponent("&aArena wand given. &eLeft-click &afor pos1, &eRight-click &afor pos2."));
        SoundUtil.playSuccess(player);
    }

    // ── PRACTICE ─────────────────────────────────────────────────────────────
    private void handlePractice(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Practice commands require a player.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /shura practice setspawn <gamemode>", NamedTextColor.RED));
            return;
        }
        if (args[1].equalsIgnoreCase("setspawn")) {
            if (args.length < 3) {
                player.sendMessage(Component.text("Usage: /shura practice setspawn <gamemode>", NamedTextColor.RED));
                return;
            }
            dev.shura.core.practice.PracticeGamemode gm = dev.shura.core.practice.PracticeGamemode.fromId(args[2]);
            if (gm == null) {
                player.sendMessage(Component.text("Unknown gamemode: " + args[2], NamedTextColor.RED));
                return;
            }
            plugin.getPracticeManager().setSpawn(gm, player.getLocation());
            player.sendMessage(MessageService.colorizeComponent("&aSpawn set for practice gamemode &e" + gm.getDisplayName()));
            SoundUtil.playSuccess(player);
        } else {
            player.sendMessage(Component.text("Unknown practice subcommand.", NamedTextColor.RED));
        }
    }

    // ── INVENTORY ITEMS ───────────────────────────────────────────────────────
    private static final List<String> INVENTORY_ITEM_TYPES = List.of("lobby", "party-leader", "party-member");

    private void handleInventoryItems(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("inventoryitems commands require a player.");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /shura inventoryitems set <party-leader|party-member|lobby>", NamedTextColor.RED));
            return;
        }
        
        if (args[1].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                player.sendMessage(Component.text("Usage: /shura inventoryitems set <party-leader|party-member|lobby>", NamedTextColor.RED));
                return;
            }
            
            String type = args[2].toLowerCase();
            if (!INVENTORY_ITEM_TYPES.contains(type)) {
                player.sendMessage(Component.text("Unknown type. Use: lobby, party-leader, or party-member", NamedTextColor.RED));
                return;
            }
            
            int savedCount = 0;
            // Read all items from hotbar (slots 0-8)
            for (int slot = 0; slot <= 8; slot++) {
                org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);
                if (item == null || item.getType().isAir()) continue;
                
                String itemKey = type + "-item" + slot;
                String path = "lobby-items." + itemKey;
                
                // Save complete item data as Base64
                String itemData = dev.shura.core.util.JsonUtil.itemsToBase64(new org.bukkit.inventory.ItemStack[]{item});
                plugin.getConfig().set(path + ".data", itemData);
                plugin.getConfig().set(path + ".slot", slot);
                savedCount++;
            }
            
            plugin.saveConfig();
            player.sendMessage(MessageService.colorizeComponent("&aSaved " + savedCount + " items for &e" + type));
            SoundUtil.playSuccess(player);
            return;
        }
        
        player.sendMessage(Component.text("Usage: /shura inventoryitems set <party-leader|party-member|lobby>", NamedTextColor.RED));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageService.colorizeComponent("&#00B4FF&lShuraCore Admin Commands"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura reload &8— &7Reload config & messages"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura wand &8— &7Get arena wand"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura arena <create|setspawn|save|delete|list|enable|disable>"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura kit <create|delete|list|settierlist>"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura whitelist <allow|deny> <group> <command>"));
        sender.sendMessage(MessageService.colorizeComponent("  &e/shura inventoryitems set <party-leader|party-member|lobby>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("shura.admin")) return List.of();

        if (args.length == 1)
            return filter(List.of("reload", "wand", "arena", "kit", "whitelist", "practice", "inventoryitems"), args[0]);

        switch (args[0].toLowerCase()) {
            case "arena" -> {
                if (args.length == 2)
                    return filter(List.of("create", "setspawn", "save", "delete", "list", "enable", "disable"), args[1]);
                if (args.length == 3 && List.of("setspawn", "save", "delete", "enable", "disable").contains(args[1].toLowerCase()))
                    return plugin.getArenaManager().getAllArenas().stream()
                            .map(a -> a.getName())
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                if (args.length == 3 && args[1].equalsIgnoreCase("setspawn"))
                    return filter(List.of("A", "B"), args[2]);
            }
            case "kit" -> {
                if (args.length == 2)
                    return filter(List.of("create", "delete", "list", "settierlist"), args[1]);
                if (args.length == 3 && List.of("delete", "settierlist").contains(args[1].toLowerCase()))
                    return plugin.getKitManager().getAllKits().stream()
                            .map(k -> k.getName())
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                if (args.length == 4 && args[1].equalsIgnoreCase("settierlist"))
                    return plugin.getTierlistManager().getAllTierlists().stream()
                            .map(t -> t.getId())
                            .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
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
            case "practice" -> {
                if (args.length == 2) return filter(List.of("setspawn"), args[1]);
                if (args.length == 3 && args[1].equalsIgnoreCase("setspawn"))
                    return java.util.Arrays.stream(dev.shura.core.practice.PracticeGamemode.values())
                            .map(gm -> gm.getId())
                            .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
            }
            case "inventoryitems" -> {
                if (args.length == 2) return filter(List.of("set"), args[1]);
                if (args.length == 3 && args[1].equalsIgnoreCase("set"))
                    return filter(INVENTORY_ITEM_TYPES, args[2]);
            }
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String input) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
