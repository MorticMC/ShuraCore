package dev.shura.core.match;

public enum MatchFormat {
    FT1(1, "First to 1"),
    FT2(2, "First to 2"),
    FT3(3, "First to 3"),
    FT5(5, "First to 5");

    private final int winsRequired;
    private final String display;

    MatchFormat(int winsRequired, String display) {
        this.winsRequired = winsRequired;
        this.display = display;
    }

    public int getWinsRequired() { return winsRequired; }
    public String getDisplay() { return display; }
}
