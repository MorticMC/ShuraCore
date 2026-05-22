package dev.shura.core.whitelist;

import dev.shura.core.ShuraCore;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class WhitelistManager {

    private final ShuraCore plugin;
    // group -> set of allowed commands
    private final Map<String, Set<String>> allowedCommands = new ConcurrentHashMap<>();

    public WhitelistManager(ShuraCore plugin) {
        this.plugin = plugin;
        loadFromDatabase();
    }

    private void loadFromDatabase() {
        plugin.getDatabaseService().query("SELECT * FROM command_whitelist WHERE allowed = 1", null, rs -> {
            try {
                while (rs.next()) {
                    String group = rs.getString("lp_group");
                    String command = rs.getString("command").toLowerCase();
                    allowedCommands.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet()).add(command);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load command whitelist.", e);
            }
            return null;
        }).thenRun(() -> {});
    }

    public boolean isAllowed(Player player, String command) {
        // Operators bypass whitelist
        if (player.isOp()) return true;

        String primaryGroup = getPrimaryGroup(player);
        Set<String> allowed = allowedCommands.get(primaryGroup);

        // Also check "default" group as fallback
        Set<String> defaultAllowed = allowedCommands.get("default");

        String cmd = command.toLowerCase().replace("/", "");
        return (allowed != null && allowed.contains(cmd))
                || (defaultAllowed != null && defaultAllowed.contains(cmd));
    }

    public void setAllowed(String group, String command, boolean allowed) {
        String cmd = command.toLowerCase();
        if (allowed) {
            allowedCommands.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet()).add(cmd);
        } else {
            Set<String> cmds = allowedCommands.get(group);
            if (cmds != null) cmds.remove(cmd);
        }

        plugin.getDatabaseService().updateAsync(
                "INSERT INTO command_whitelist (command, lp_group, allowed) VALUES (?, ?, ?) " +
                "ON CONFLICT(command, lp_group) DO UPDATE SET allowed=excluded.allowed",
                stmt -> {
                    stmt.setString(1, cmd);
                    stmt.setString(2, group);
                    stmt.setBoolean(3, allowed);
                });
    }

    public Set<String> getAllowedCommandsForPlayer(Player player) {
        String primaryGroup = getPrimaryGroup(player);
        Set<String> result = ConcurrentHashMap.newKeySet();
        Set<String> defaultAllowed = allowedCommands.get("default");
        if (defaultAllowed != null) result.addAll(defaultAllowed);
        Set<String> groupAllowed = allowedCommands.get(primaryGroup);
        if (groupAllowed != null) result.addAll(groupAllowed);
        return result;
    }

    public Set<String> getAllowedCommands(String group) {
        return allowedCommands.getOrDefault(group, Set.of());
    }

    public Map<String, Set<String>> getAllEntries() { return allowedCommands; }

    private String getPrimaryGroup(Player player) {
        User user = plugin.getLuckPerms().getUserManager().getUser(player.getUniqueId());
        if (user == null) return "default";
        return user.getPrimaryGroup();
    }

    public void reload() {
        allowedCommands.clear();
        loadFromDatabase();
    }
}
