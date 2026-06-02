package dev.shura.core.command;

import dev.shura.core.ShuraCore;
import dev.shura.core.gui.editor.GuiEditorManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CustomGuiCommand implements CommandExecutor {

    private final ShuraCore plugin;
    private final GuiEditorManager manager;
    private final String guiName;

    public CustomGuiCommand(ShuraCore plugin, String guiName) {
        this.plugin = plugin;
        this.manager = plugin.getGuiEditorManager();
        this.guiName = guiName;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        manager.openGui(player, guiName);
        return true;
    }
}
