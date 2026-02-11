package fr.deepstonestudio.deepstone.model;

import java.util.*;

public final class Clan {

    private final String name;          // ID canonical
    private String displayName;         // joli nom

    private UUID king;
    private UUID jarl;

    private int glory = 0;              // 🔥 Gloire du clan

    private final Map<UUID, Role> members = new HashMap<>();

    public Clan(String name) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.displayName = name;
    }

    // =============================
    // Basic getters
    // =============================

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public UUID getKing() { return king; }
    public UUID getJarl() { return jarl; }

    public int getGlory() { return glory; }

    public Map<UUID, Role> getMembers() { return members; }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public Role getRole(UUID uuid) {
        return members.get(uuid);
    }

    public int size() {
        return members.size();
    }

    // =============================
    // GLOIRE
    // =============================

    public void addGlory(int amount) {
        this.glory += amount;
    }

    public void removeGlory(int amount) {
        this.glory = Math.max(0, this.glory - amount);
    }

    public void setGlory(int glory) {
        this.glory = Math.max(0, glory);
    }

    // =============================
    // MEMBERS
    // =============================

    public void addMember(UUID uuid, Role role) {
        members.put(uuid, role);
    }

    public void removeMember(UUID uuid) {

        members.remove(uuid);

        if (Objects.equals(uuid, king)) {
            king = null;
        }

        if (Objects.equals(uuid, jarl)) {
            jarl = null;
        }
    }

    // =============================
    // ROLES
    // =============================

    public void setKing(UUID uuid) {

        if (!isMember(uuid)) {
            throw new IllegalArgumentException("Le joueur doit être membre du clan.");
        }

        // Ancien roi devient Jarl si possible
        if (king != null && !king.equals(uuid)) {
            members.put(king, Role.JARL);
        }

        king = uuid;
        members.put(uuid, Role.KING);

        // Si pas de Jarl ou si le Jarl était l'ancien roi
        if (jarl == null || Objects.equals(jarl, king)) {
            jarl = uuid;
        }
    }

    public void setJarl(UUID uuid) {

        if (!isMember(uuid)) {
            throw new IllegalArgumentException("Le joueur doit être membre du clan.");
        }

        if (Objects.equals(uuid, king)) {
            jarl = uuid;
            return;
        }

        jarl = uuid;
        members.put(uuid, Role.JARL);
    }

    public void promote(UUID uuid, Role role) {

        if (!isMember(uuid)) {
            throw new IllegalArgumentException("Le joueur doit être membre.");
        }

        if (role == Role.KING) {
            throw new IllegalArgumentException("Utilise setKing().");
        }

        if (role == Role.JARL) {
            setJarl(uuid);
            return;
        }

        members.put(uuid, role);
    }
}
