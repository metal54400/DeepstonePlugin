package fr.deepstonestudio.deepstone.api.events;

import fr.deepstonestudio.deepstone.api.events.RagnarDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RagnarDeathAutoStart {

    private final JavaPlugin plugin;
    private final RagnarDeathEvent event;

    private int taskId = -1;

    // cooldown en jours MC
    private long cooldownDaysLeft = 0;

    public RagnarDeathAutoStart(JavaPlugin plugin, RagnarDeathEvent event) {
        this.plugin = plugin;
        this.event = event;
    }

    public void start() {
        stop();

        boolean enabled = plugin.getConfig().getBoolean("ragnardeath.random-enabled", true);
        if (!enabled) return;

        int chance = plugin.getConfig().getInt("ragnardeath.random-chance-percent", 15);
        chance = Math.max(0, Math.min(chance, 100));

        int cooldownDays = Math.max(0, plugin.getConfig().getInt("ragnardeath.cooldown-days", 0));

        List<String> worlds = plugin.getConfig().getStringList("ragnardeath.worlds");

        // 24000 ticks = 1 jour minecraft
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {

            // si event déjà en cours -> rien
            if (event.isRunning()) return;

            // cooldown
            if (cooldownDaysLeft > 0) {
                cooldownDaysLeft--;
                return;
            }

            // tirage
            if (!roll(chance)) return;

            // monde cible
            World w = null;
            if (worlds != null && !worlds.isEmpty()) {
                for (String name : worlds) {
                    World candidate = Bukkit.getWorld(name);
                    if (candidate != null) { w = candidate; break; }
                }
            }

            // fallback: premier monde chargé
            if (w == null) {
                if (!Bukkit.getWorlds().isEmpty()) w = Bukkit.getWorlds().get(0);
            }

            if (w == null) return;

            event.start(w);

            // applique cooldown après un start
            cooldownDaysLeft = cooldownDays;

        }, 24000L, 24000L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private boolean roll(int percent) {
        if (percent <= 0) return false;
        if (percent >= 100) return true;
        return ThreadLocalRandom.current().nextInt(100) < percent;
    }
}