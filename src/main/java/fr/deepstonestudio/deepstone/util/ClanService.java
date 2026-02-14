package fr.deepstonestudio.deepstone.util;

import fr.deepstonestudio.deepstone.model.Clan;
import fr.deepstonestudio.deepstone.model.Role;
import fr.deepstonestudio.deepstone.storage.YamlStore;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;

public final class ClanService {
    private final YamlStore store;
    private final boolean gpEnabled;

    // In-memory
    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<UUID, String> playerClan = new HashMap<>();

    // Invitations: target -> (clanName, expiresAt)
    private final Map<UUID, Invite> invites = new HashMap<>();
    private record Invite(String clan, long expiresAt) {}

    // ✅ clan chat toggle
    private final Set<UUID> clanChat = new HashSet<>();

    public ClanService(YamlStore store, boolean gpEnabled) {
        this.store = store;
        this.gpEnabled = gpEnabled;
    }

    public void loadAll() throws IOException {
        clans.clear();
        clans.putAll(store.loadClans());
        playerClan.clear();
        playerClan.putAll(store.loadPlayerClanIndex());
    }

    public void saveAll() throws IOException {
        store.saveClans(clans);
        store.savePlayerClanIndex(playerClan);
    }

    public Collection<Clan> listClans() { return clans.values(); }
    public Clan getClanByName(String name) { return clans.get(name.toLowerCase(Locale.ROOT)); }

    public Clan getClanOf(UUID player) {
        String c = playerClan.get(player);
        if (c == null) return null;
        return getClanByName(c);
    }

    public boolean hasClan(UUID player) { return playerClan.containsKey(player); }

    public Clan createClan(Player creator, String name) {
        String id = name.toLowerCase(Locale.ROOT);
        if (clans.containsKey(id)) throw new IllegalStateException("Clan déjà existant.");
        if (hasClan(creator.getUniqueId())) throw new IllegalStateException("Tu es déjà dans un clan.");

        Clan c = new Clan(name);
        c.initFounder(creator.getUniqueId());

        clans.put(c.getName(), c);
        playerClan.put(creator.getUniqueId(), c.getName());
        return c;
    }

    public void disband(Player actor) {
        Clan c = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), c, Role.KING);

        // ✅ retirer relation chez les autres
        for (Clan other : clans.values()) {
            if (!other.getName().equalsIgnoreCase(c.getName())) {
                other.removeRelation(c.getName());
            }
        }

        // remove all members index
        for (UUID u : new ArrayList<>(c.getMembers().keySet())) {
            playerClan.remove(u);
            clanChat.remove(u);
        }
        clans.remove(c.getName());
    }

    public void invite(Player actor, Player target) {
        Clan c = requireClan(actor);
        Role r = c.getRole(actor.getUniqueId());
        if (r != Role.KING && r != Role.JARL) throw new IllegalStateException("Seul le Roi/Jarl peut inviter.");
        if (hasClan(target.getUniqueId())) throw new IllegalStateException("Ce joueur a déjà un clan.");

        long expires = System.currentTimeMillis() + 120_000; // 2 min
        invites.put(target.getUniqueId(), new Invite(c.getName(), expires));
    }

    public Clan acceptInvite(Player target) {
        if (hasClan(target.getUniqueId())) throw new IllegalStateException("Tu es déjà dans un clan.");
        Invite inv = invites.get(target.getUniqueId());
        if (inv == null) throw new IllegalStateException("Aucune invitation.");
        if (System.currentTimeMillis() > inv.expiresAt) {
            invites.remove(target.getUniqueId());
            throw new IllegalStateException("Invitation expirée.");
        }
        Clan c = getClanByName(inv.clan());
        if (c == null) throw new IllegalStateException("Clan introuvable.");
        c.addMember(target.getUniqueId(), Role.PEASANT);
        playerClan.put(target.getUniqueId(), c.getName());
        invites.remove(target.getUniqueId());
        return c;
    }

    public void leave(Player p) {
        Clan c = requireClan(p);
        Role r = c.getRole(p.getUniqueId());
        if (r == Role.KING) throw new IllegalStateException("Le Roi ne peut pas quitter. /clan disband ou /clan setking");

        c.removeMember(p.getUniqueId());
        playerClan.remove(p.getUniqueId());
        clanChat.remove(p.getUniqueId());

        if (Objects.equals(c.getJarl(), p.getUniqueId())) {
            c.setJarl(c.getKing());
        }
    }

    public void kick(Player actor, Player target) {
        Clan c = requireClan(actor);
        Clan tc = getClanOf(target.getUniqueId());
        if (tc == null || !Objects.equals(tc.getName(), c.getName()))
            throw new IllegalStateException("Ce joueur n’est pas dans ton clan.");

        Role ar = c.getRole(actor.getUniqueId());
        if (ar != Role.KING && ar != Role.JARL) throw new IllegalStateException("Seul le Roi/Jarl peut kick.");
        Role tr = c.getRole(target.getUniqueId());
        if (tr == Role.KING) throw new IllegalStateException("Impossible de kick le Roi.");
        if (tr == Role.JARL && ar != Role.KING) throw new IllegalStateException("Seul le Roi peut kick le Jarl.");

        c.removeMember(target.getUniqueId());
        playerClan.remove(target.getUniqueId());
        clanChat.remove(target.getUniqueId());

        if (Objects.equals(c.getJarl(), target.getUniqueId())) c.setJarl(c.getKing());
    }

    public void setRole(Player actor, Player target, Role newRole) {
        Clan c = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), c, Role.JARL);

        if (!c.isMember(target.getUniqueId())) throw new IllegalStateException("Ce joueur n’est pas dans ton clan.");
        if (newRole == Role.KING) throw new IllegalStateException("Utilise /clan setking.");
        if (newRole == Role.JARL) throw new IllegalStateException("Utilise /clan setjarl.");
        if (c.getRole(target.getUniqueId()) == Role.KING) throw new IllegalStateException("Impossible de changer le rôle du Roi.");

        c.addMember(target.getUniqueId(), newRole);
    }

    public void setKing(Player actor, Player target) {
        Clan c = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), c, Role.KING);
        if (!c.isMember(target.getUniqueId())) throw new IllegalStateException("Ce joueur n’est pas dans ton clan.");

        UUID oldKing = c.getKing();
        c.setKing(target.getUniqueId());

        if (oldKing != null && !oldKing.equals(target.getUniqueId())) {
            c.addMember(oldKing, Role.JARL);
        }
        if (c.getJarl() == null) c.setJarl(target.getUniqueId());
    }

    public void setJarl(Player actor, Player target) {
        Clan c = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), c, Role.KING);
        if (!c.isMember(target.getUniqueId())) throw new IllegalStateException("Ce joueur n’est pas dans ton clan.");
        c.setJarl(target.getUniqueId());
    }

    // -------------------------
    // ✅ Diplomatie: ally/truce/break
    // -------------------------
    public void ally(Player actor, String otherClanName) {
        Clan a = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), a, Role.KING);

        Clan b = requireClanByName(otherClanName);
        if (a.getName().equalsIgnoreCase(b.getName()))
            throw new IllegalStateException("Tu ne peux pas t’allier avec ton propre clan.");

        a.addAlliance(b.getName());
        b.addAlliance(a.getName());
    }

    public void truce(Player actor, String otherClanName) {
        Clan a = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), a, Role.KING);

        Clan b = requireClanByName(otherClanName);
        if (a.getName().equalsIgnoreCase(b.getName()))
            throw new IllegalStateException("Tu ne peux pas faire une trêve avec ton propre clan.");

        a.addTruce(b.getName());
        b.addTruce(a.getName());
    }

    public void breakAlliance(Player actor, String otherClanName) {
        Clan a = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), a, Role.KING);

        Clan b = requireClanByName(otherClanName);
        if (a.getName().equalsIgnoreCase(b.getName()))
            throw new IllegalStateException("Inutile.");

        a.removeRelation(b.getName());
        b.removeRelation(a.getName());
    }

    private Clan requireClanByName(String name) {
        if (name == null || name.isBlank()) throw new IllegalStateException("Nom de clan invalide.");
        Clan c = getClanByName(name);
        if (c == null) throw new IllegalStateException("Clan introuvable: " + name);
        return c;
    }

    /** True si même clan OU alliance mutuelle */
    public boolean areAllies(UUID p1, UUID p2) {
        Clan c1 = getClanOf(p1);
        Clan c2 = getClanOf(p2);
        if (c1 == null || c2 == null) return false;
        if (c1.getName().equalsIgnoreCase(c2.getName())) return true;
        return c1.isAlliedWith(c2.getName()) && c2.isAlliedWith(c1.getName());
    }

    public Set<Clan> alliedClansOf(Clan clan) {
        if (clan == null) return Set.of();
        Set<Clan> out = new HashSet<>();
        for (String allyId : clan.getAllies()) {
            Clan c = getClanByName(allyId);
            if (c != null) out.add(c);
        }
        return out;
    }

    // -------------------------
    // ✅ Clan Chat (toggle + recipients)
    // -------------------------
    public boolean toggleClanChat(UUID u) {
        if (clanChat.contains(u)) { clanChat.remove(u); return false; }
        clanChat.add(u);
        return true;
    }

    public boolean isInClanChat(UUID u) { return clanChat.contains(u); }

    public Set<UUID> sharedChatRecipients(UUID sender) {
        Clan me = getClanOf(sender);
        if (me == null) return Set.of();

        Set<UUID> recipients = new HashSet<>(me.getMembers().keySet());
        for (Clan ally : alliedClansOf(me)) {
            recipients.addAll(ally.getMembers().keySet());
        }
        return recipients;
    }

    // -------------------------
    // Internals
    // -------------------------
    private Clan requireClan(Player p) {
        Clan c = getClanOf(p.getUniqueId());
        if (c == null) throw new IllegalStateException("Tu n’es dans aucun clan.");
        return c;
    }

    private void requireRoleAtLeast(UUID actor, Clan clan, Role needed) {
        Role r = clan.getRole(actor);
        if (r == null) throw new IllegalStateException("Pas membre.");

        int rank = switch (r) {
            case KING -> 3;
            case JARL -> 2;
            case WARRIOR -> 1;
            case PEASANT -> 0;
        };
        int need = switch (needed) {
            case KING -> 3;
            case JARL -> 2;
            case WARRIOR -> 1;
            case PEASANT -> 0;
        };
        if (rank < need) throw new IllegalStateException("Permission insuffisante.");
    }

    public List<String> clanNames() {
        return clans.keySet().stream().sorted().toList();
    }

    public List<String> roleNames() {
        return Arrays.stream(Role.values()).map(Enum::name).toList();
    }

    public void setCapital(Player actor) {
        Clan c = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), c, Role.KING);

        // Le bloc sous les pieds du roi devient le "coeur"
        var core = actor.getLocation().getBlock().getLocation();
        c.setCapital(core);
    }

    public boolean isEnemy(UUID a, UUID b) {
        // “ennemi” = pas même clan, et pas alliés (ton système)
        Clan ca = getClanOf(a);
        Clan cb = getClanOf(b);
        if (ca == null || cb == null) return false;
        if (ca.getName().equalsIgnoreCase(cb.getName())) return false;
        // si alliés mutuels => pas ennemis
        if (ca.isAlliedWith(cb.getName()) && cb.isAlliedWith(ca.getName())) return false;
        return true;
    }

}
