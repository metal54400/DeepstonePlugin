package fr.deepstonestudio.deepstone.util;

import fr.deepstonestudio.deepstone.model.Clan;
import fr.deepstonestudio.deepstone.model.Role;
import fr.deepstonestudio.deepstone.storage.YamlStore;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;

public final class ClanService {
    private final YamlStore store;

    // In-memory
    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<UUID, String> playerClan = new HashMap<>();

    // Invitations: target -> (clanName, expiresAt)
    private final Map<UUID, Invite> invites = new HashMap<>();

    private record Invite(String clan, long expiresAt) {}

    public ClanService(YamlStore store) {
        this.store = store;
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
        c.setKing(creator.getUniqueId());
        c.setJarl(creator.getUniqueId()); // par défaut, roi = jarl jusqu'à nomination
        c.addMember(creator.getUniqueId(), Role.KING);

        clans.put(c.getName(), c);
        playerClan.put(creator.getUniqueId(), c.getName());
        return c;
    }

    public void disband(Player actor) {
        Clan c = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), c, Role.KING);

        // remove all members index
        for (UUID u : new ArrayList<>(c.getMembers().keySet())) {
            playerClan.remove(u);
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
        // si c'était jarl, reset
        if (Objects.equals(c.getJarl(), p.getUniqueId())) {
            c.setJarl(c.getKing()); // fallback roi
        }
    }

    public void kick(Player actor, Player target) {
        Clan c = requireClan(actor);
        if (getClanOf(target.getUniqueId()) == null || !Objects.equals(getClanOf(target.getUniqueId()).getName(), c.getName()))
            throw new IllegalStateException("Ce joueur n’est pas dans ton clan.");

        Role ar = c.getRole(actor.getUniqueId());
        if (ar != Role.KING && ar != Role.JARL) throw new IllegalStateException("Seul le Roi/Jarl peut kick.");
        Role tr = c.getRole(target.getUniqueId());
        if (tr == Role.KING) throw new IllegalStateException("Impossible de kick le Roi.");
        if (tr == Role.JARL && ar != Role.KING) throw new IllegalStateException("Seul le Roi peut kick le Jarl.");

        c.removeMember(target.getUniqueId());
        playerClan.remove(target.getUniqueId());
        if (Objects.equals(c.getJarl(), target.getUniqueId())) c.setJarl(c.getKing());
    }

    public void setRole(Player actor, Player target, Role newRole) {
        Clan c = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), c, Role.JARL);

        if (!c.isMember(target.getUniqueId())) throw new IllegalStateException("Ce joueur n’est pas dans ton clan.");
        if (newRole == Role.KING) throw new IllegalStateException("Utilise /clan setking.");
        if (newRole == Role.JARL) throw new IllegalStateException("Utilise /clan setjarl.");
        if (c.getRole(target.getUniqueId()) == Role.KING) throw new IllegalStateException("Impossible de changer le rôle du Roi.");

        // Jarl ne peut pas promouvoir en Jarl/King déjà géré.
        c.addMember(target.getUniqueId(), newRole);
    }

    public void setKing(Player actor, Player target) {
        Clan c = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), c, Role.KING);
        if (!c.isMember(target.getUniqueId())) throw new IllegalStateException("Ce joueur n’est pas dans ton clan.");

        UUID oldKing = c.getKing();
        c.setKing(target.getUniqueId());

        // l'ancien roi devient jarl ou warrior (on choisit Jarl si personne d'autre)
        if (oldKing != null && !oldKing.equals(target.getUniqueId())) {
            if (Objects.equals(c.getJarl(), oldKing)) {
                c.addMember(oldKing, Role.JARL);
            } else {
                c.addMember(oldKing, Role.JARL);
            }
        }
        // si le jarl = null, le roi prend le rôle
        if (c.getJarl() == null) c.setJarl(target.getUniqueId());
    }

    public void setJarl(Player actor, Player target) {
        Clan c = requireClan(actor);
        requireRoleAtLeast(actor.getUniqueId(), c, Role.KING); // seul roi
        if (!c.isMember(target.getUniqueId())) throw new IllegalStateException("Ce joueur n’est pas dans ton clan.");
        c.setJarl(target.getUniqueId());
    }

    private Clan requireClan(Player p) {
        Clan c = getClanOf(p.getUniqueId());
        if (c == null) throw new IllegalStateException("Tu n’es dans aucun clan.");
        return c;
    }

    private void requireRoleAtLeast(UUID actor, Clan clan, Role needed) {
        Role r = clan.getRole(actor);
        if (r == null) throw new IllegalStateException("Pas membre.");
        // ordre hiérarchique
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
}