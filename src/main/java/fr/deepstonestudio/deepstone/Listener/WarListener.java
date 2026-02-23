package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.model.clan.Clan;
import fr.deepstonestudio.deepstone.model.clan.War;
import fr.deepstonestudio.deepstone.util.ClanService;
import fr.deepstonestudio.deepstone.util.GloryService;
import fr.deepstonestudio.deepstone.util.WarService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class WarListener implements Listener {

    private final ClanService clans;
    private final WarService wars;
    private final GloryService glory;

    public WarListener(ClanService clans, WarService wars, GloryService glory) {
        this.clans = clans;
        this.wars = wars;
        this.glory = glory;
    }

    /**
     * ✅ Important:
     * - HIGHEST + ignoreCancelled = false => on peut "débloquer" après GriefPrevention
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = getAttacker(event);
        if (attacker == null) return;

        Clan victimClan = clans.getClanOf(victim.getUniqueId());
        Clan attackerClan = clans.getClanOf(attacker.getUniqueId());
        if (victimClan == null || attackerClan == null) return;

        // ✅ Friendly fire OFF (même clan)
        if (victimClan.getName().equalsIgnoreCase(attackerClan.getName())) {
            event.setCancelled(true);
            return;
        }

        // ✅ PvP autorisé uniquement si guerre entre ces 2 clans
        if (areAtWarTogether(victimClan.getName(), attackerClan.getName())) {
            // Si GP (ou autre) a annulé dans un claim => on ré-autorise
            event.setCancelled(false);
        } else {
            // pas en guerre => pas de PvP clan
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;

        Clan deadClan = clans.getClanOf(dead.getUniqueId());
        Clan killerClan = clans.getClanOf(killer.getUniqueId());
        if (deadClan == null || killerClan == null) return;

        // On ne compte que si ils sont vraiment en guerre
        if (!areAtWarTogether(deadClan.getName(), killerClan.getName())) return;

        glory.add(killer.getUniqueId(), 10);
        glory.remove(dead.getUniqueId(), 5);

        if (dead.getUniqueId().equals(deadClan.getKing())) {

            Bukkit.broadcastMessage("§7[§c!§7] Le ROI de "
                    + deadClan.getDisplayName()
                    + " est tombé !");

            Bukkit.broadcastMessage("§7[§e?§7]"
                    + killerClan.getDisplayName()
                    + " remporte la guerre !");

            deadClan.addGlory(-50);
            killerClan.addGlory(100);

            War war = wars.getWar(deadClan.getName());
            if (war != null) {
                wars.endWar(war);
            }
        }
    }

    /* ========================= */
    /*           UTILS           */
    /* ========================= */

    private Player getAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;

        if (event.getDamager() instanceof Projectile proj) {
            if (proj.getShooter() instanceof Player p) return p;
        }

        return null;
    }

    /**
     * True si clanA et clanB sont dans la même guerre.
     * On utilise ton wars.getWar(clanName) et on vérifie que l'autre clan est dans cette war.
     *
     * ⚠️ Comme je n’ai pas ta classe War, je teste avec des getters courants :
     * - getAttacker()/getDefender()
     * - clan1/clan2
     * Adapte si besoin.
     */
    private boolean areAtWarTogether(String clanA, String clanB) {
        War war = wars.getWar(clanA);
        if (war == null) return false;

        // 👉 ADAPTE ces 2 lignes selon ton modèle War
        String w1 = getWarClan1(war);
        String w2 = getWarClan2(war);

        if (w1 == null || w2 == null) return false;

        boolean aIn = w1.equalsIgnoreCase(clanA) || w2.equalsIgnoreCase(clanA);
        boolean bIn = w1.equalsIgnoreCase(clanB) || w2.equalsIgnoreCase(clanB);
        return aIn && bIn;
    }

    // ✅ Helpers pour éviter de casser si ton War n’a pas les mêmes noms
    private String getWarClan1(War war) {
        try { return (String) war.getClass().getMethod("getClan1").invoke(war); } catch (Exception ignored) {}
        try { return (String) war.getClass().getMethod("getAttacker").invoke(war); } catch (Exception ignored) {}
        try { return (String) war.getClass().getMethod("getA").invoke(war); } catch (Exception ignored) {}
        return null;
    }

    private String getWarClan2(War war) {
        try { return (String) war.getClass().getMethod("getClan2").invoke(war); } catch (Exception ignored) {}
        try { return (String) war.getClass().getMethod("getDefender").invoke(war); } catch (Exception ignored) {}
        try { return (String) war.getClass().getMethod("getB").invoke(war); } catch (Exception ignored) {}
        return null;
    }
}