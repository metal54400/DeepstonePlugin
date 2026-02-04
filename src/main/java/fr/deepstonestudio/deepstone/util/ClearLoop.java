package fr.deepstonestudio.deepstone.util;

import fr.deepstonestudio.deepstone.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;

public class ClearLoop {

    private final JavaPlugin plugin;
    private final ClearService clearService;
    private BukkitTask task;

    public ClearLoop(JavaPlugin plugin, ClearService clearService) {
        this.plugin = plugin;
        this.clearService = clearService;
    }

    public void start() {
        stop(); // sécurité

        int interval = plugin.getConfig().getInt("interval-seconds", 300);

        // On déclenche un cycle toutes les "interval"
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> runCycle(interval), 20L, interval * 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void runCycle(int intervalSeconds) {
        // warnings dans config.yml
        List<Map<?, ?>> warnings = plugin.getConfig().getMapList("warnings");

        for (Map<?, ?> w : warnings) {
            int time = (int) w.get("time");
            String msg = String.valueOf(w.get("message"));

            // exemple: interval=300, warning time=60 => envoi à 240s après le début
            if (time > 0 && time < intervalSeconds) {
                long delayTicks = (intervalSeconds - time) * 20L;
                Bukkit.getScheduler().runTaskLater(plugin, () -> Msg.broadcast(Msg.color(msg)), delayTicks);
            }
        }

        // Clear à la fin
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int removed = clearService.clearGroundItems();
            String done = plugin.getConfig().getString("done-message", "&a{count} items ont été supprimés.");
            Msg.broadcast(Msg.color(done.replace("{count}", String.valueOf(removed))));
        }, intervalSeconds * 20L);
    }
}
