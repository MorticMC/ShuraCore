package dev.shura.core.kit;

/**
 * Per-kit gameplay rules. Enforced by {@link dev.shura.core.listener.KitRulesListener}
 * for both 1v1 and team matches.
 * <p>
 * Existing fields are kept (and gson-compatible) so previously saved kits still
 * deserialize. New flags default to {@code false}.
 */
public class KitRules {

    private boolean noShield;          // shields cannot be raised/used
    private boolean noBuilding;        // blocks cannot be placed
    private boolean noBlockBreak;      // blocks cannot be broken
    private boolean noRegen;           // no natural health regeneration
    private boolean restrictedWeapons; // only kit weapons allowed (informational)
    private boolean noOffhand;         // offhand swap disabled
    private boolean noFallDamage;      // fall damage cancelled
    private boolean noHunger;          // food level never drops
    private boolean noEnderpearl;      // ender pearls cannot be thrown

    public KitRules() {}

    public boolean isNoShield() { return noShield; }
    public void setNoShield(boolean noShield) { this.noShield = noShield; }

    public boolean isNoBuilding() { return noBuilding; }
    public void setNoBuilding(boolean noBuilding) { this.noBuilding = noBuilding; }

    public boolean isNoBlockBreak() { return noBlockBreak; }
    public void setNoBlockBreak(boolean noBlockBreak) { this.noBlockBreak = noBlockBreak; }

    public boolean isNoRegen() { return noRegen; }
    public void setNoRegen(boolean noRegen) { this.noRegen = noRegen; }

    public boolean isRestrictedWeapons() { return restrictedWeapons; }
    public void setRestrictedWeapons(boolean restrictedWeapons) { this.restrictedWeapons = restrictedWeapons; }

    public boolean isNoOffhand() { return noOffhand; }
    public void setNoOffhand(boolean noOffhand) { this.noOffhand = noOffhand; }

    public boolean isNoFallDamage() { return noFallDamage; }
    public void setNoFallDamage(boolean noFallDamage) { this.noFallDamage = noFallDamage; }

    public boolean isNoHunger() { return noHunger; }
    public void setNoHunger(boolean noHunger) { this.noHunger = noHunger; }

    public boolean isNoEnderpearl() { return noEnderpearl; }
    public void setNoEnderpearl(boolean noEnderpearl) { this.noEnderpearl = noEnderpearl; }

    /** Toggles a rule by its key (used by the rules GUI). Returns the new value. */
    public boolean toggle(String key) {
        switch (key) {
            case "noShield" -> noShield = !noShield;
            case "noBuilding" -> noBuilding = !noBuilding;
            case "noBlockBreak" -> noBlockBreak = !noBlockBreak;
            case "noRegen" -> noRegen = !noRegen;
            case "restrictedWeapons" -> restrictedWeapons = !restrictedWeapons;
            case "noOffhand" -> noOffhand = !noOffhand;
            case "noFallDamage" -> noFallDamage = !noFallDamage;
            case "noHunger" -> noHunger = !noHunger;
            case "noEnderpearl" -> noEnderpearl = !noEnderpearl;
            default -> { return false; }
        }
        return get(key);
    }

    public boolean get(String key) {
        return switch (key) {
            case "noShield" -> noShield;
            case "noBuilding" -> noBuilding;
            case "noBlockBreak" -> noBlockBreak;
            case "noRegen" -> noRegen;
            case "restrictedWeapons" -> restrictedWeapons;
            case "noOffhand" -> noOffhand;
            case "noFallDamage" -> noFallDamage;
            case "noHunger" -> noHunger;
            case "noEnderpearl" -> noEnderpearl;
            default -> false;
        };
    }
}
