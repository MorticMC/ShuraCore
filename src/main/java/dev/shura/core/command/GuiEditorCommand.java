package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.editor.CustomGui;
import dev.shura.core.gui.editor.GuiEditorManager;
import dev.shura.core.gui.editor.GuiType;
import dev.shura.core.extra.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiEditorCommand implements CommandExecutor, TabCompleter {

    private final ShuraCore plugin;
    private final GuiEditorManager manager;

    public GuiEditorCommand(ShuraCore plugin) {
        this.plugin = plugin;
        this.manager = plugin.getGuiEditorManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("shura.gui.editor")) {
            player.sendMessage(MessageService.colorizeComponent("&cYou don't have permission."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 4) {
                    player.sendMessage(MessageService.colorizeComponent("&cUsage: /shuragui create <name> <type> <title>"));
                    player.sendMessage(MessageService.colorizeComponent("&7Types: &e" + String.join(", ", GuiType.getNames())));
                    return true;
                }
                String name = args[1];
                GuiType guiType = GuiType.fromString(args[2]);
                if (guiType == null) {
                    player.sendMessage(MessageService.colorizeComponent("&cInvalid type. Available: &e" + String.join(", ", GuiType.getNames())));
                    return true;
                }
                String title = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                if (manager.createGui(name, guiType, title)) {
                    player.sendMessage(MessageService.colorizeComponent("&aCreated GUI: &e" + name));
                } else {
                    player.sendMessage(MessageService.colorizeComponent("&cGUI already exists: &e" + name));
                }
            }
            case "list" -> {
                var guis = manager.getGuiNames();
                if (guis.isEmpty()) {
                    player.sendMessage(MessageService.colorizeComponent("&cNo custom GUIs found."));
                } else {
                    player.sendMessage(MessageService.colorizeComponent("&6Custom GUIs: &e" + String.join(", ", guis)));
                }
            }
            case "edit" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageService.colorizeComponent("&cUsage: /shuragui edit <name>"));
                    return true;
                }
                String name = args[1];
                if (manager.getGui(name) == null) {
                    player.sendMessage(MessageService.colorizeComponent("&cGUI not found: &e" + name));
                    return true;
                }
                manager.openEditor(player, name);
            }
            case "open" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageService.colorizeComponent("&cUsage: /shuragui open <name>"));
                    return true;
                }
                String name = args[1];
                if (manager.getGui(name) == null) {
                    player.sendMessage(MessageService.colorizeComponent("&cGUI not found: &e" + name));
                    return true;
                }
                manager.openGui(player, name);
            }
            case "remove" -> {
                if (args.length < 2) {
                    player.sendMessage(MessageService.colorizeComponent("&cUsage: /shuragui remove <name>"));
                    return true;
                }
                String name = args[1];
                if (manager.removeGui(name)) {
                    player.sendMessage(MessageService.colorizeComponent("&aRemoved GUI: &e" + name));
                } else {
                    player.sendMessage(MessageService.colorizeComponent("&cGUI not found: &e" + name));
                }
            }
            case "command" -> {
                if (args.length < 3) {
                    player.sendMessage(MessageService.colorizeComponent("&cUsage: /shuragui command <name> <command>"));
                    return true;
                }
                String name = args[1];
                CustomGui gui = manager.getGui(name);
                if (gui == null) {
                    player.sendMessage(MessageService.colorizeComponent("&cGUI not found: &e" + name));
                    return true;
                }
                String cmd = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                gui.setCommand(cmd);
                manager.saveGui(gui);
                player.sendMessage(MessageService.colorizeComponent("&aSet command for &e" + name + "&a: &7/" + cmd));
                player.sendMessage(MessageService.colorizeComponent("&7Restart server to register command."));
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageService.colorizeComponent("&6&lGUI Editor Commands:"));
        player.sendMessage(MessageService.colorizeComponent("&e/shuragui create <name> <type> <title> &7- Create a new GUI"));
        player.sendMessage(MessageService.colorizeComponent("&e/shuragui list &7- List all custom GUIs"));
        player.sendMessage(MessageService.colorizeComponent("&e/shuragui edit <name> &7- Edit a GUI"));
        player.sendMessage(MessageService.colorizeComponent("&e/shuragui open <name> &7- Open a GUI"));
        player.sendMessage(MessageService.colorizeComponent("&e/shuragui remove <name> &7- Remove a GUI"));
        player.sendMessage(MessageService.colorizeComponent("&e/shuragui command <name> <command> &7- Set command for GUI"));
        player.sendMessage(MessageService.colorizeComponent("&7Types: &e" + String.join(", ", GuiType.getNames())));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "list", "edit", "open", "remove", "command");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("command"))) {
            return new ArrayList<>(manager.getGuiNames());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            return List.of("<name>");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return Arrays.asList(GuiType.getNames());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("command")) {
            return List.of("<command>");
        }
        return List.of();
    }
}
