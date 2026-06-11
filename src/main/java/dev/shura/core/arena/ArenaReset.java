package dev.shura.core.arena;

import dev.shura.core.ShuraCore;

/**
 * Arena regeneration backed by {@link SchematicService}.
 * <p>
 * Instead of storing a full {@code BlockData} snapshot for every live copy
 * (which grew memory linearly with the number of clones), we keep a single
 * cached clipboard per arena and simply re-paste it at the copy's origin to
 * regen. The paste runs on FAWE's async pipeline.
 */
public class ArenaReset {

    private final ShuraCore plugin;
    private final SchematicService schematics;

    public ArenaReset(ShuraCore plugin, SchematicService schematics) {
        this.plugin = plugin;
        this.schematics = schematics;
    }

    /** Pre-warms the clipboard cache for an arena so the first match doesn't pay the capture cost. */
    public void warm(Arena arena) {
        schematics.getOrCapture(arena);
    }

    public void reset(ArenaCopy copy, Runnable onComplete) {
        Arena arena = plugin.getArenaManager().getArena(copy.getArenaId());
        if (arena == null) {
            copy.release();
            if (onComplete != null) onComplete.run();
            return;
        }
        schematics.pasteAsync(arena, copy.getOrigin(), () -> {
            copy.release();
            if (onComplete != null) onComplete.run();
        });
    }

    public void invalidate(String arenaId) {
        schematics.invalidate(arenaId);
    }
}
