package fr.deepstonestudio.deepstone.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public final class Clan {

    private final String name;
    private String displayName;

    private UUID king;
    private UUID jarl;

    private int glory = 0;

    private final Map<UUID, Role> members = new HashMap<>();

    // ✅ Diplomatie (ids en lower-case)
    private final Set<String> allies = new HashSet<>();
    private final Set<String> truces = new HashSet<>();

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

    // -------------------------
    // ✅ Diplomatie
    // -------------------------
    public Set<String> getAllies() { return allies; }
    public Set<String> getTruces() { return truces; }

    public boolean isAlliedWith(String otherClanId) {
        if (otherClanId == null) return false;
        return allies.contains(otherClanId.toLowerCase(Locale.ROOT));
    }

    public boolean isTruceWith(String otherClanId) {
        if (otherClanId == null) return false;
        return truces.contains(otherClanId.toLowerCase(Locale.ROOT));
    }

    public void removeRelation(String otherClanId) {
        String id = otherClanId.toLowerCase(Locale.ROOT);
        allies.remove(id);
        truces.remove(id);
    }

    public void addAlliance(String otherClanId) {
        String id = otherClanId.toLowerCase(Locale.ROOT);
        truces.remove(id);
        allies.add(id);
    }

    public void addTruce(String otherClanId) {
        String id = otherClanId.toLowerCase(Locale.ROOT);
        if (!allies.contains(id)) truces.add(id);
    }

    // -------------------------
    // ✅ Création / gestion
    // -------------------------
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

    public static final class Capital {
        private final String world;
        private final int x, y, z;

        public Capital(String world, int x, int y, int z) {
            this.world = world;
            this.x = x; this.y = y; this.z = z;
        }

        public String getWorld() { return world; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }

        public Location toLocation() {
            World w = Bukkit.getWorld(world);
            if (w == null) return null;
            return new Location(w, x + 0.5, y, z + 0.5);
        }
    }

    private Capital capital;

    public Capital getCapital() { return capital; }

    public void setCapital(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            this.capital = null;
            return;
        }
        this.capital = new Capital(
                loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()
        );
    }

    public void setCapitalRaw(Capital cap) { this.capital = cap; }
    public boolean hasCapital() { return capital != null; }
}
