package dev.shura.core.extra;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InteractionManager {

    private final Set<UUID> unrestrictedPlayers = new HashSet<>();

    public void setInteractionEnabled(UUID uuid, boolean enabled) {
        if (enabled) unrestrictedPlayers.add(uuid);
        else unrestrictedPlayers.remove(uuid);
    }

    public boolean isUnrestricted(UUID uuid) {
        return unrestrictedPlayers.contains(uuid);
    }

    public void remove(UUID uuid) {
        unrestrictedPlayers.remove(uuid);
    }
}
