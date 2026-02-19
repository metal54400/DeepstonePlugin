package fr.deepstonestudio.deepstone.Listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;
import java.util.UUID;

public class PriereDeathListener implements Listener {

    private final Map<UUID, String> priereDeathCauseMap;

    public PriereDeathListener(Map<UUID, String> priereDeathCauseMap) {
        this.priereDeathCauseMap = priereDeathCauseMap;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        String data = priereDeathCauseMap.remove(uuid);
        if (data == null) return;

        String[] parts = data.split("\\|\\|", 2);
        String cause = parts.length > 0 ? parts[0] : "Cause inconnue";
        String message = parts.length > 1 ? parts[1] : ("§c☠ §f" + event.getEntity().getName() + " §7est mort.");

        event.setDeathMessage(message);
        event.getEntity().sendMessage("§8Cause: §7" + cause);
    }
}