package dev.shura.core.maintenance;

import dev.shura.core.ShuraCore;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MaintenanceManager {

    private static final String BYPASS_PERMISSION = "shuracore.maintenance.allow";
    private static final String SETTING_KEY = "maintenance";

    private final ShuraCore plugin;
    private boolean enabled;

    public MaintenanceManager(ShuraCore plugin) {
        this.plugin = plugin;
        // Load persisted state from DB
        plugin.getDatabaseService().getSetting(SETTING_KEY, "false")
                .thenAccept(val -> this.enabled = Boolean.parseBoolean(val));
    }

    public void setEnabled(boolean enabled, Player executor) {
        this.enabled = enabled;
        plugin.getDatabaseService().setSetting(SETTING_KEY, String.valueOf(enabled));

        if (enabled) {
            kickNonPermitted();
            Bukkit.broadcast(Component.text("[ShuraPvP] ", NamedTextColor.AQUA)
                    .append(Component.text("Server is now under maintenance.", NamedTextColor.RED)));
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission(BYPASS_PERMISSION))
                    .forEach(SoundUtil::playMaintenanceOn);
        } else {
            Bukkit.broadcast(Component.text("[ShuraPvP] ", NamedTextColor.AQUA)
                    .append(Component.text("Maintenance mode disabled. Welcome back!", NamedTextColor.GREEN)));
            Bukkit.getOnlinePlayers().forEach(SoundUtil::playMaintenanceOff);
        }

        if (executor != null) {
            executor.sendMessage(Component.text("Maintenance mode " + (enabled ? "enabled." : "disabled."),
                    enabled ? NamedTextColor.RED : NamedTextColor.GREEN));
        }
    }

    private void kickNonPermitted() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(BYPASS_PERMISSION)) {
                player.kick(Component.text("Server is under maintenance. Please try again later.", NamedTextColor.RED));
            }
        }
    }

    public boolean isEnabled() { return enabled; }

    public boolean canJoin(Player player) {
        return !enabled || player.hasPermission(BYPASS_PERMISSION);
    }

    public Component getKickMessage() {
        return Component.text("Server is currently under maintenance.", NamedTextColor.RED)
                .appendNewline()
                .append(Component.text("Please try again later.", NamedTextColor.GRAY));
    }
}
