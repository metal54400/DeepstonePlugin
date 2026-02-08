package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.Manager.ProtectionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener {

    private final ProtectionManager protectionManager;
    private final double nearRadius;

    public TeleportListener(ProtectionManager protectionManager, double nearRadius) {
        this.protectionManager = protectionManager;
        this.nearRadius = nearRadius;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player teleported = event.getPlayer();
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;

        double maxDistSq = nearRadius * nearRadius;

        for (Player other : to.getWorld().getPlayers()) {
            if (other.equals(teleported)) continue;

            if (other.getLocation().distanceSquared(to) <= maxDistSq) {
                // "tp sur un joueur" détecté
                protectionManager.grant(teleported);
                break;
            }
        }
    }
}
