package dev.shura.core.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.TextColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .hexColors()
            .character('&')
            .build();

    public static Component color(String message) {
        return LEGACY.deserialize(message);
    }

    public static String strip(String message) {
        return LEGACY.serialize(color(message)).replaceAll("§.", "");
    }

    public static Component mini(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    public static String colorize(String message) {
        return message.replace('&', '§');
    }
}
