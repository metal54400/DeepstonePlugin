package fr.deepstonestudio.deepstone.model;

import java.util.*;

public final class Clan {

    private final String name;
    private String displayName;

    private UUID king;
    private UUID jarl;

    private int glory = 0;

    private final Map<UUID, Role> members = new HashMap<>();

    public Clan(String name) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.displayName = name;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public UUID getKing() { return king; }
    public UUID getJarl() { return jarl; }

    public int getGlory() { return glory; }
    public void setGlory(int glory) { this.glory = Math.max(0, glory); }
    public void addGlory(int amount) { this.glory += amount; }
    public void removeGlory(int amount) { this.glory = Math.max(0, this.glory - amount); }

    public Map<UUID, Role> getMembers() { return members; }
    public boolean isMember(UUID uuid) { return members.containsKey(uuid); }
    public Role getRole(UUID uuid) { return members.get(uuid); }
    public int size() { return members.size(); }

    /** ✅ Méthode propre pour créer un clan */
    public void initFounder(UUID uuid) {
        members.put(uuid, Role.KING);
        this.king = uuid;
        this.jarl = uuid;
    }

    public void addMember(UUID uuid, Role role) {
        members.put(uuid, role);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        if (Objects.equals(uuid, king)) king = null;
        if (Objects.equals(uuid, jarl)) jarl = null;
    }

    public void setKing(UUID uuid) {
        if (!isMember(uuid)) throw new IllegalArgumentException("Le joueur doit être membre du clan.");

        if (king != null && !king.equals(uuid)) {
            members.put(king, Role.JARL);
        }

        king = uuid;
        members.put(uuid, Role.KING);

        if (jarl == null) jarl = uuid;
    }

    public void setJarl(UUID uuid) {
        if (!isMember(uuid)) throw new IllegalArgumentException("Le joueur doit être membre du clan.");
        if (Objects.equals(uuid, king)) {
            jarl = uuid;
            return;
        }
        jarl = uuid;
        members.put(uuid, Role.JARL);
    }
}
