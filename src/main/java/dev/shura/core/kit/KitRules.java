package dev.shura.core.kit;

public class KitRules {

    private boolean noShield;
    private boolean noBuilding;
    private boolean noRegen;
    private boolean restrictedWeapons; // only kit weapons allowed
    private boolean noOffhand;

    public KitRules() {}

    public boolean isNoShield() { return noShield; }
    public void setNoShield(boolean noShield) { this.noShield = noShield; }
    public boolean isNoBuilding() { return noBuilding; }
    public void setNoBuilding(boolean noBuilding) { this.noBuilding = noBuilding; }
    public boolean isNoRegen() { return noRegen; }
    public void setNoRegen(boolean noRegen) { this.noRegen = noRegen; }
    public boolean isRestrictedWeapons() { return restrictedWeapons; }
    public void setRestrictedWeapons(boolean restrictedWeapons) { this.restrictedWeapons = restrictedWeapons; }
    public boolean isNoOffhand() { return noOffhand; }
    public void setNoOffhand(boolean noOffhand) { this.noOffhand = noOffhand; }
}
