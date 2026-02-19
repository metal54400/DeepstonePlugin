package fr.deepstonestudio.deepstone.Listener;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class ZombieVillagerBoostListener implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public ZombieVillagerBoostListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {

        if (!plugin.getConfig().getBoolean("zombie-villager-boost.enabled", true)) return;

        // Seulement zombies normaux
        if (event.getEntityType() != EntityType.ZOMBIE) return;
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        // Seulement spawn naturel
        if (plugin.getConfig().getBoolean("zombie-villager-boost.only-natural-spawns", true)) {
            if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        }

        double percent = plugin.getConfig().getDouble("zombie-villager-boost.extra-chance-percent", 5.0);
        double chance = Math.max(0.0, Math.min(100.0, percent)) / 100.0;

        if (random.nextDouble() >= chance) return;

        Location loc = zombie.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        event.setCancelled(true);

        ZombieVillager zv = (ZombieVillager) world.spawnEntity(loc, EntityType.ZOMBIE_VILLAGER);

        // Copie propriétés utiles
        zv.setBaby(zombie.isBaby());
        zv.setCanPickupItems(zombie.getCanPickupItems());

        if (zombie.getEquipment() != null && zv.getEquipment() != null) {
            zv.getEquipment().setArmorContents(zombie.getEquipment().getArmorContents());
            zv.getEquipment().setItemInMainHand(zombie.getEquipment().getItemInMainHand());
            zv.getEquipment().setItemInOffHand(zombie.getEquipment().getItemInOffHand());
        }
    }
}