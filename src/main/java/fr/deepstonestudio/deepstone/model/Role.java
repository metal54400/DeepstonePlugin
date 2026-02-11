package fr.deepstonestudio.deepstone.model;

public enum Role {
    KING,   // Roi
    JARL,   // Chef militaire
    WARRIOR,
    PEASANT;

    public static Role from(String s) {
        return Role.valueOf(s.toUpperCase());
    }
}