package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.util.ClanService;
import fr.deepstonestudio.deepstone.util.MercenaryService;
import fr.deepstonestudio.deepstone.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class ClanFriendlyFireListener implements Listener {
    private final ClanService clans;
    private final MercenaryService mercs;

    public ClanFriendlyFireListener(ClanService clans, MercenaryService mercs) {
        this.clans = clans;
        this.mercs = mercs;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player a)) return;
        if (!(e.getEntity() instanceof Player b)) return;

        if (clans.areAllies(a.getUniqueId(), b.getUniqueId())) {
            e.setCancelled(true);
            a.sendMessage(Msg.error("Tu ne peux pas frapper un allié."));
        }

        if (mercs.isFriendly(a.getUniqueId(), b.getUniqueId())) {
            e.setCancelled(true);
            a.sendMessage(Msg.error("Tu ne peux pas frapper un allié (mercenariat)."));
        }
    }
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;

        if (mercs.isMercenary(killer.getUniqueId())) {
            mercs.payOnKill(killer);
            try { mercs.saveAll(); } catch (Exception ignored) {}
        }
    }

}
