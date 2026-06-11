package dev.shura.core.match;

import net.kyori.adventure.text.format.NamedTextColor;

/** The two sides of a team match (party-vs-party duel and party split). */
public enum Team {
    RED("Red", NamedTextColor.RED),
    BLUE("Blue", NamedTextColor.BLUE);

    private final String display;
    private final NamedTextColor color;

    Team(String display, NamedTextColor color) {
        this.display = display;
        this.color = color;
    }

    public String getDisplay() { return display; }
    public NamedTextColor getColor() { return color; }

    public Team other() { return this == RED ? BLUE : RED; }
}
