package dev.shura.core.practice;

public enum PracticeGamemode {

    NETHPOT("NethPot",  "nethpot"),
    SWORD("Sword",      "sword"),
    MACE("Mace",        "mace"),
    DIASMP("DiaSMP",    "diasmp"),
    POTION("Potion",    "potion"),
    SMP("SMP",          "smp"),
    UHC("UHC",          "uhc"),
    AXE("Axe",          "axe");

    private final String displayName;
    private final String id;

    PracticeGamemode(String displayName, String id) {
        this.displayName = displayName;
        this.id = id;
    }

    public String getDisplayName() { return displayName; }
    public String getId() { return id; }

    public static PracticeGamemode fromId(String id) {
        for (PracticeGamemode gm : values())
            if (gm.id.equalsIgnoreCase(id)) return gm;
        return null;
    }
}
