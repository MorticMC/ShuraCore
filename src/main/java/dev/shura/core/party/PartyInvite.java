package dev.shura.core.party;

import java.util.UUID;

public record PartyInvite(UUID from, UUID to, long expiresAt) {

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
