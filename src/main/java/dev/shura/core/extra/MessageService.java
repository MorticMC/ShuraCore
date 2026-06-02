package dev.shura.core.extra;

import dev.shura.core.ShuraCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;

public class MessageService {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final ShuraCore plugin;
    private FileConfiguration messages;
    private String prefix;
    private boolean prefixEnabled;

    public MessageService(ShuraCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(file);
        prefixEnabled = messages.getBoolean("prefix-enabled", true);
        prefix = colorize(messages.getString("prefix", "&8[&bShuraPvP&8] &r"));
    }

    // Get a raw colored string from messages.yml
    public String getRaw(String path, Map<String, String> placeholders) {
        String message = messages.getString(path, "&cMissing message: " + path);
        message = message.replace("{prefix}", prefixEnabled ? prefix : "");
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return colorize(message);
    }

    public String getRaw(String path) {
        return getRaw(path, null);
    }

    // Get as Adventure Component
    public Component get(String path, Map<String, String> placeholders) {
        return LEGACY.deserialize(getRaw(path, placeholders));
    }

    public Component get(String path) {
        return get(path, null);
    }

    // Send directly to player
    public void send(Player player, String path, Map<String, String> placeholders) {
        player.sendMessage(get(path, placeholders));
    }

    public void send(Player player, String path) {
        send(player, path, null);
    }

    /**
     * Colorize a string ? supports &#RRGGBB, &#RGB, and legacy &amp; codes (e.g. &amp;6).
     * Legacy codes are not applied to the '#' in incomplete &# sequences.
     */
    public static String colorize(String input) {
        if (input == null) return "";
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (input.charAt(i) != '&' || i + 1 >= input.length()) {
                out.append(input.charAt(i));
                i++;
                continue;
            }

            char code = input.charAt(i + 1);
            if (code == '#') {
                int start = i + 2;
                int end = start;
                while (end < input.length() && isHexDigit(input.charAt(end)) && end - start < 6) {
                    end++;
                }
                int len = end - start;
                if (len == 6 || len == 3) {
                    String hex = input.substring(start, end);
                    if (len == 3) {
                        hex = expandShortHex(hex);
                    }
                    out.append("§x");
                    for (char c : hex.toCharArray()) {
                        out.append('§').append(c);
                    }
                    i = end;
                    continue;
                }
                // Not a valid hex color ? treat & as legacy (e.g. &6) and keep '#'
                out.append('§').append(code);
                i += 2;
                continue;
            }

            out.append('§').append(code);
            i += 2;
        }
        return out.toString();
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    private static String expandShortHex(String hex) {
        return "" + hex.charAt(0) + hex.charAt(0)
                + hex.charAt(1) + hex.charAt(1)
                + hex.charAt(2) + hex.charAt(2);
    }

    // Colorize to Component directly
    public static Component colorizeComponent(String input) {
        return LEGACY.deserialize(colorize(input));
    }

    public String getPrefix() { return prefix; }
}
