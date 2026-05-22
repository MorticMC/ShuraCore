package dev.shura.core.arena;

import java.util.ArrayList;
import java.util.List;

public class ArenaValidator {

    public record ValidationResult(boolean valid, List<String> errors) {}

    public static ValidationResult validate(Arena arena) {
        List<String> errors = new ArrayList<>();

        if (arena.getPos1() == null) errors.add("Position 1 is not set.");
        if (arena.getPos2() == null) errors.add("Position 2 is not set.");
        if (arena.getSpawnA() == null) errors.add("Spawn A is not set.");
        if (arena.getSpawnB() == null) errors.add("Spawn B is not set.");

        if (arena.getPos1() != null && arena.getPos2() != null) {
            if (arena.getWidth() < 5 || arena.getLength() < 5)
                errors.add("Arena is too small. Minimum 5x5 blocks.");
            if (arena.getHeight() < 3)
                errors.add("Arena height is too small. Minimum 3 blocks.");
        }

        if (arena.getSpawnA() != null && arena.getSpawnB() != null) {
            if (arena.getSpawnA().distance(arena.getSpawnB()) < 3)
                errors.add("Spawn points are too close together. Minimum 3 blocks apart.");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
