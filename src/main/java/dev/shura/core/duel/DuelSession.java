package dev.shura.core.duel;

import dev.shura.core.kit.Kit;
import dev.shura.core.match.MatchFormat;
import org.bukkit.entity.Player;

public class DuelSession {
    
    private final Player target;
    private final Kit kit;
    private MatchFormat format;
    private int customRounds;
    
    public DuelSession(Player target, Kit kit) {
        this.target = target;
        this.kit = kit;
        this.format = MatchFormat.FT1;
        this.customRounds = 1;
    }
    
    public Player getTarget() {
        return target;
    }
    
    public Kit getKit() {
        return kit;
    }
    
    public MatchFormat getFormat() {
        return format;
    }
    
    public void setFormat(MatchFormat format) {
        this.format = format;
    }
    
    public int getCustomRounds() {
        return customRounds;
    }
    
    public void incrementRounds() {
        if (customRounds < 10) {
            customRounds++;
            updateFormat();
        }
    }
    
    public void decrementRounds() {
        if (customRounds > 1) {
            customRounds--;
            updateFormat();
        }
    }
    
    private void updateFormat() {
        switch (customRounds) {
            case 1 -> format = MatchFormat.FT1;
            case 2 -> format = MatchFormat.FT2;
            case 3 -> format = MatchFormat.FT3;
            case 5 -> format = MatchFormat.FT5;
            default -> format = MatchFormat.FT1;
        }
    }
}
