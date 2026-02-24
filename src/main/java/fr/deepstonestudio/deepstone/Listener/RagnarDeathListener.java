package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.api.events.RagnarDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class RagnarDeathListener implements Listener {

    private final RagnarDeathEvent event;

    public RagnarDeathListener(RagnarDeathEvent event) {
        this.event = event;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        event.onMobSpawn(e);
    }
}