package fr.deepstonestudio.deepstone.util;

import fr.deepstonestudio.deepstone.model.Clan;
import fr.deepstonestudio.deepstone.storage.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;

public final class MercenaryService {

    private final ClanService clans;
    private final YamlStore store;

    // Contrat actif: joueur -> (clanId, expiresAt)
    private final Map<UUID, Contract> contracts = new HashMap<>();
    private record Contract(String clanId, long expiresAt) {}

    // Bourse gloire: joueur -> montant
    private final Map<UUID, Integer> wallet = new HashMap<>();

    // Réglages simples
    private final long contractDurationMs;
    private final int payPerKill;

    public MercenaryService(ClanService clans, YamlStore store) {
        this(clans, store, 60 * 60_000L, 5); // 60 min, 5 gloires par kill
    }

    public MercenaryService(ClanService clans, YamlStore store, long contractDurationMs, int payPerKill) {
        this.clans = clans;
        this.store = store;
        this.contractDurationMs = contractDurationMs;
        this.payPerKill = payPerKill;
    }

    public void loadAll() throws IOException {
        wallet.clear();
        wallet.putAll(store.loadMercenaryGlory());
    }

    public void saveAll() throws IOException {
        store.saveMercenaryGlory(wallet);
    }

    public boolean isMercenary(UUID player) {
        cleanupIfExpired(player);
        return contracts.containsKey(player);
    }

    public Clan getEmployerClan(UUID player) {
        cleanupIfExpired(player);
        Contract c = contracts.get(player);
        if (c == null) return null;
        return clans.getClanByName(c.clanId());
    }

    public long getRemainingMs(UUID player) {
        cleanupIfExpired(player);
        Contract c = contracts.get(player);
        if (c == null) return 0L;
        return Math.max(0L, c.expiresAt() - System.currentTimeMillis());
    }

    public int getWallet(UUID player) {
        return Math.max(0, wallet.getOrDefault(player, 0));
    }

    public void addWallet(UUID player, int amount) {
        if (amount <= 0) return;
        wallet.put(player, getWallet(player) + amount);
    }

    public void join(Player p, String clanName) {
        if (clans.hasClan(p.getUniqueId()))
            throw new IllegalStateException("§7[§e?§7] Tu as déjà un clan. Les mercenaires doivent être sans clan.");

        cleanupIfExpired(p.getUniqueId());
        if (contracts.containsKey(p.getUniqueId()))
            throw new IllegalStateException("§7[§c!§7] Tu es déjà mercenaire. Fais /mercenary leave.");

        Clan employer = clans.getClanByName(clanName);
        if (employer == null) throw new IllegalStateException("Clan introuvable: " + clanName);

        long expires = System.currentTimeMillis() + contractDurationMs;
        contracts.put(p.getUniqueId(), new Contract(employer.getName(), expires));
    }

    public void leave(Player p) {
        contracts.remove(p.getUniqueId());
    }

    /** Paiement lors d'un kill: le clan paye payPerKill en gloire au mercenaire */
    public void payOnKill(Player mercenary) {
        UUID u = mercenary.getUniqueId();
        Clan employer = getEmployerClan(u);
        if (employer == null) return;

        int clanGlory = employer.getGlory();
        if (clanGlory <= 0) {
            mercenary.sendMessage(Msg.err("Ton clan employeur n’a plus de gloire. Contrat terminé."));
            contracts.remove(u);
            return;
        }

        int pay = Math.min(payPerKill, clanGlory);
        employer.removeGlory(pay);
        addWallet(u, pay);

        mercenary.sendMessage(Msg.ok("Paiement: +" + pay + " gloire (bourse mercenaire = " + getWallet(u) + ")."));

        // info au roi (optionnel)
        UUID king = employer.getKing();
        if (king != null) {
            Player k = Bukkit.getPlayer(king);
            if (k != null) k.sendMessage(Msg.info("Mercenaire " + mercenary.getName() + " payé: -" + pay + " gloire."));
        }
    }

    /** Mercenaire considéré allié avec le clan employeur (et ses alliés = logique politique) */
    public boolean isFriendly(UUID a, UUID b) {
        cleanupIfExpired(a);
        cleanupIfExpired(b);

        Clan ca = clans.getClanOf(a);
        Clan cb = clans.getClanOf(b);

        Clan ma = getEmployerClan(a);
        Clan mb = getEmployerClan(b);

        // même clan classique
        if (ca != null && cb != null && ca.getName().equalsIgnoreCase(cb.getName())) return true;

        // mercenaire vs employeur
        if (ma != null && cb != null) {
            if (ma.getName().equalsIgnoreCase(cb.getName())) return true;
            // et aussi alliés de l'employeur
            if (ma.isAlliedWith(cb.getName()) && cb.isAlliedWith(ma.getName())) return true;
        }
        if (mb != null && ca != null) {
            if (mb.getName().equalsIgnoreCase(ca.getName())) return true;
            if (mb.isAlliedWith(ca.getName()) && ca.isAlliedWith(mb.getName())) return true;
        }

        // mercenaire vs mercenaire (même employeur)
        if (ma != null && mb != null && ma.getName().equalsIgnoreCase(mb.getName())) return true;

        return false;
    }

    private void cleanupIfExpired(UUID player) {
        Contract c = contracts.get(player);
        if (c == null) return;
        if (System.currentTimeMillis() <= c.expiresAt()) return;

        contracts.remove(player);
        Player p = Bukkit.getPlayer(player);
        if (p != null) p.sendMessage(Msg.info("Ton contrat de mercenaire a expiré."));
    }
}