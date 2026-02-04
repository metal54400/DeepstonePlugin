package fr.deepstonestudio.deepstone.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;

public class ClearService {

    private final JavaPlugin plugin;

    public ClearService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int clearGroundItems() {
        int count = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    entity.remove();
                    count++;
                }
            }
        }

        plugin.getLogger().info("ClearLagg: " + count + " items supprimés.");
        return count;
    }
}
