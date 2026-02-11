package fr.deepstonestudio.deepstone.Listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RaidListener implements Listener {
    private final Map<UUID, Long> raidCooldown = new HashMap<>();
    private final long ONE_HOUR = 3600000L; // 1 heure en millisecondes

    @EventHandler
    public void onRaidFinish(RaidFinishEvent event) {
        // Récupère les joueurs qui ont remporté le raid
        List<Player> winners = event.getWinners();
        long now = System.currentTimeMillis();

        for (Player player : winners) {
            UUID uuid = player.getUniqueId();
            if (raidCooldown.containsKey(uuid)) {
                long lastTime = raidCooldown.get(uuid);
                if (now - lastTime < ONE_HOUR) {
                    long remainingMinutes = (ONE_HOUR - (now - lastTime)) / 60000;
                    player.sendMessage("Vous devez attendre encore " + remainingMinutes + " minutes avant de participer à un nouveau raid.");
                    // Ici, tu peux également annuler une récompense si besoin
                    continue;
                }
            }
            // Met à jour le cooldown pour ce joueur
            raidCooldown.put(uuid, now);

            // Par exemple, donne un totem de vie
            player.getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        }
    }
}

