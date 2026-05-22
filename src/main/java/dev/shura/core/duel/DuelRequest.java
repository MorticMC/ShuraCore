package dev.shura.core.duel;

import dev.shura.core.kit.Kit;
import dev.shura.core.match.MatchFormat;

import java.util.UUID;

public class DuelRequest {

    private final UUID senderUuid;
    private final String senderName;
    private final UUID receiverUuid;
    private final Kit kit;
    private final MatchFormat format;
    private final long expiresAt;

    private static final long EXPIRY_MS = 30_000L;

    public DuelRequest(UUID senderUuid, String senderName, UUID receiverUuid, Kit kit, MatchFormat format) {
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.receiverUuid = receiverUuid;
        this.kit = kit;
        this.format = format;
        this.expiresAt = System.currentTimeMillis() + EXPIRY_MS;
    }

    public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }

    public UUID getSenderUuid() { return senderUuid; }
    public String getSenderName() { return senderName; }
    public UUID getReceiverUuid() { return receiverUuid; }
    public Kit getKit() { return kit; }
    public MatchFormat getFormat() { return format; }
    public long getExpiresAt() { return expiresAt; }
}
