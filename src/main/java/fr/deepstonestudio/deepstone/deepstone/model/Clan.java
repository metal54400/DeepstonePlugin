package fr.deepstonestudio.deepstone.model;
import java.util.*;

public final class Clan {
    private final String name;          // ID canonical
    private String displayName;         // joli nom
    private UUID king;
    private UUID jarl;
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

    public Map<UUID, Role> getMembers() { return members; }

    public boolean isMember(UUID u) { return members.containsKey(u); }
    public Role getRole(UUID u) { return members.get(u); }

    public void addMember(UUID u, Role role) { members.put(u, role); }
    public void removeMember(UUID u) { members.remove(u); }

    public void setKing(UUID u) {
        this.king = u;
        members.put(u, Role.KING);
    }

    public void setJarl(UUID u) {
        this.jarl = u;
        // Si c'est le roi, on ne downgrade pas
        if (Objects.equals(king, u)) return;
        members.put(u, Role.JARL);
    }

    public int size() { return members.size(); }
}
