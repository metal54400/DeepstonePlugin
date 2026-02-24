package fr.deepstonestudio.deepstone.Listener;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ZombieInfectListener implements Listener {

    private final JavaPlugin plugin;

    public ZombieInfectListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onZombieAttack(EntityDamageByEntityEvent event) {
        FileConfiguration config = plugin.getConfig();
        boolean enabled = config.getBoolean("zombieInfect.enabled", true);

        if (!enabled) return; // Si désactivé, on ne fait rien

        if (event.getDamager() instanceof Zombie && event.getEntity() instanceof Villager) {
            Villager villager = (Villager) event.getEntity();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!villager.isDead()) {
                    villager.getWorld().spawnEntity(villager.getLocation(), EntityType.ZOMBIE_VILLAGER);
                    villager.remove();
                }
            }, 1L);
        }
    }
}
