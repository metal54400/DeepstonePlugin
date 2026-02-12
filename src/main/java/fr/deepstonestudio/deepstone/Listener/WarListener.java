package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.model.Clan;
import fr.deepstonestudio.deepstone.model.War;
import fr.deepstonestudio.deepstone.util.ClanService;
import fr.deepstonestudio.deepstone.util.GloryService;
import fr.deepstonestudio.deepstone.util.WarService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
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

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        Clan c1 = clans.getClanOf(victim.getUniqueId());
        Clan c2 = clans.getClanOf(attacker.getUniqueId());

        if (c1 == null || c2 == null) return;

        if (c1 == c2) {
            event.setCancelled(true); // Friendly fire off
            return;
        }

        War war = wars.getWar(c1.getName());
        if (war == null) {
            event.setCancelled(true); // pas de guerre = pas de pvp clan
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        Player dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;

        Clan deadClan = clans.getClanOf(dead.getUniqueId());
        Clan killerClan = clans.getClanOf(killer.getUniqueId());
        if (deadClan == null || killerClan == null) return;

        War war = wars.getWar(deadClan.getName());
        if (war == null) return;

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

            wars.endWar(war);
        }
    }
}
