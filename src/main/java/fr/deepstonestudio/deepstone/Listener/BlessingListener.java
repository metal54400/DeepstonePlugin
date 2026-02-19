package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.Manager.BlessingManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class BlessingListener implements Listener {

    private final BlessingManager blessingManager;

    public BlessingListener(BlessingManager blessingManager) {
        this.blessingManager = blessingManager;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        blessingManager.applyBlessingSoon(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        blessingManager.applyBlessingSoon(event.getPlayer());
    }
}
