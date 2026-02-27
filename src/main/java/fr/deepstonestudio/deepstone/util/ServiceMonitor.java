package fr.deepstonestudio.deepstone.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.deepstonestudio.deepstone.model.monitoring.Stats;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.YearMonth;

public class ServiceMonitor {

    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File statsFile;
    private Stats stats;
    private long startTime;

    public ServiceMonitor(JavaPlugin plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        startTime = System.currentTimeMillis();

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        statsFile = new File(plugin.getDataFolder(), "stats.json");
        load();

        startScheduler();
    }

    private void startScheduler() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            stats.tps = Bukkit.getServer().getTPS()[0];
            stats.onlinePlayers = Bukkit.getOnlinePlayers().size();
            stats.uptime = (System.currentTimeMillis() - startTime) / 1000;
            stats.lastUpdate = System.currentTimeMillis();
            save();
        }, 0L, 200L); // toutes les 10 secondes
    }

    public void registerJoin() {
        String month = YearMonth.now().toString();
        stats.monthlyJoins.put(month,
                stats.monthlyJoins.getOrDefault(month, 0) + 1);
        stats.totalJoins++;
        save();
    }

    private void load() {
        try {
            if (!statsFile.exists()) {
                stats = new Stats();
                save();
                return;
            }

            FileReader reader = new FileReader(statsFile);
            stats = gson.fromJson(reader, Stats.class);
            reader.close();

        } catch (Exception e) {
            stats = new Stats();
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            FileWriter writer = new FileWriter(statsFile);
            gson.toJson(stats, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Stats getStats() {
        return stats;
    }
}