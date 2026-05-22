package dev.shura.core.match;

public enum MatchType {
    RANKED,     // competitive queue, ELO affected
    UNRANKED,   // duel request or party match, no ELO
    PRACTICE    // FFA practice, no ELO
}
