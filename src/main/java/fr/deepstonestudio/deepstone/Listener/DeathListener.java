package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.Deepstone;
import fr.deepstonestudio.deepstone.util.RunePlacer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {
    private final RunePlacer runePlacer;
    private final Deepstone plugin;
    public DeathListener(Deepstone plugin) {
        this.runePlacer = new RunePlacer(plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        runePlacer.placeNordicDeath(event.getEntity().getLocation());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return; // évite doublon
        runePlacer.placeNordicDeath(event.getEntity().getLocation());
    }
}
