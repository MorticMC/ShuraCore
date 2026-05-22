package dev.shura.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<String, Long> cooldowns = new HashMap<>();

    private String key(UUID uuid, String action) {
        return uuid + ":" + action;
    }

    public void set(UUID uuid, String action, long durationMillis) {
        cooldowns.put(key(uuid, action), System.currentTimeMillis() + durationMillis);
    }

    public boolean isOnCooldown(UUID uuid, String action) {
        Long expiry = cooldowns.get(key(uuid, action));
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(key(uuid, action));
            return false;
        }
        return true;
    }

    public long getRemaining(UUID uuid, String action) {
        Long expiry = cooldowns.get(key(uuid, action));
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public long getRemainingSeconds(UUID uuid, String action) {
        return (long) Math.ceil(getRemaining(uuid, action) / 1000.0);
    }

    public void clear(UUID uuid, String action) {
        cooldowns.remove(key(uuid, action));
    }

    public void clearAll(UUID uuid) {
        cooldowns.keySet().removeIf(k -> k.startsWith(uuid.toString()));
    }
}
