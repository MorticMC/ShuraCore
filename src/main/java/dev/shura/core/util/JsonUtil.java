package dev.shura.core.util;

import com.google.gson.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

public class JsonUtil {

    private static final Gson GSON = new GsonBuilder().create();

    // ItemStack[] <-> Base64 string
    public static String itemsToBase64(ItemStack[] items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize items.", e);
        }
    }

    public static ItemStack[] itemsFromBase64(String data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) items[i] = (ItemStack) dataInput.readObject();
            return items;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize items.", e);
        }
    }

    // PotionEffect list <-> JSON
    public static String effectsToJson(List<PotionEffect> effects) {
        JsonArray array = new JsonArray();
        for (PotionEffect effect : effects) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", effect.getType().key().asString());
            obj.addProperty("duration", effect.getDuration());
            obj.addProperty("amplifier", effect.getAmplifier());
            obj.addProperty("ambient", effect.isAmbient());
            obj.addProperty("particles", effect.hasParticles());
            obj.addProperty("icon", effect.hasIcon());
            array.add(obj);
        }
        return GSON.toJson(array);
    }

    public static List<PotionEffect> effectsFromJson(String json) {
        List<PotionEffect> effects = new ArrayList<>();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            PotionEffectType type = PotionEffectType.getByKey(
                    org.bukkit.NamespacedKey.fromString(obj.get("type").getAsString()));
            if (type == null) continue;
            effects.add(new PotionEffect(
                    type,
                    obj.get("duration").getAsInt(),
                    obj.get("amplifier").getAsInt(),
                    obj.get("ambient").getAsBoolean(),
                    obj.get("particles").getAsBoolean(),
                    obj.get("icon").getAsBoolean()
            ));
        }
        return effects;
    }

    public static String toJson(Object obj) { return GSON.toJson(obj); }
    public static <T> T fromJson(String json, Class<T> clazz) { return GSON.fromJson(json, clazz); }
}
