package fr.deepstonestudio.deepstone.model.inv;

import org.bukkit.GameMode;

public enum InventoryGroup {
    SURVIVAL,  // SURVIVAL + ADVENTURE
    CREATIVE;  // CREATIVE + SPECTATOR

    public static InventoryGroup from(GameMode gm) {
        return switch (gm) {
            case SURVIVAL, ADVENTURE -> SURVIVAL;
            case CREATIVE, SPECTATOR -> CREATIVE;
        };
    }
}