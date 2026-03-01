package fr.deepstonestudio.deepstone.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.deepstonestudio.deepstone.model.monitoring.Stats;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.YearMonth;

public class ServiceMonitor {

    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File statsFile;
    private Stats stats;
    private long startTime;
    private final String DASHBOARD_URL = "https://deep.deepstone.fr/launchers/monitor/index.php";

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
        if (stats != null) {
            stats.restarts.add(System.currentTimeMillis());
        }
        startScheduler();
    }

    private void startScheduler() {
        // --- Toutes les 10 sec : mise à jour stats locales
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                if (stats == null) return;
                double[] tps = Bukkit.getServer().getTPS();
                stats.tps = (tps != null && tps.length > 0) ? tps[0] : 0.0;
                stats.onlinePlayers = (Bukkit.getOnlinePlayers() != null) ? Bukkit.getOnlinePlayers().size() : 0;
                stats.uptime = (System.currentTimeMillis() - startTime) / 1000;
                stats.lastUpdate = System.currentTimeMillis();
                save();
            } catch (Exception e) {
                plugin.getLogger().warning("⚠️ Erreur mise à jour stats locales: " + e.getMessage());
            }
        }, 0L, 200L);

        // --- Toutes les 15 min : envoyer stats au dashboard PHP
        long interval15Min = 15 * 60 * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sendStatsToDashboard, 0L, interval15Min);
    }

    public void registerJoin() {
        try {
            if (stats == null) return;
            String month = YearMonth.now().toString();
            stats.monthlyJoins.put(month, stats.monthlyJoins.getOrDefault(month, 0) + 1);
            stats.totalJoins++;
            save();
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Erreur registerJoin: " + e.getMessage());
        }
    }

    private void sendStatsToDashboard() {
        try {
            if (stats == null) return;
            URL url = new URL(DASHBOARD_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            String json = gson.toJson(stats);

            try (OutputStream os = con.getOutputStream()) {
                os.write(json.getBytes("UTF-8"));
            }

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                plugin.getLogger().info("✅ Stats envoyées au dashboard");
            } else {
                plugin.getLogger().warning("⚠️ Erreur en envoyant stats: " + responseCode);
            }

            con.disconnect();
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Impossible d'envoyer stats: " + e.getMessage());
        }
    }

    private void load() {
        try {
            if (!statsFile.exists()) {
                stats = new Stats();
                save();
                return;
            }

            try (FileReader reader = new FileReader(statsFile)) {
                stats = gson.fromJson(reader, Stats.class);
            }
            if (stats == null) stats = new Stats();

        } catch (Exception e) {
            stats = new Stats();
            plugin.getLogger().warning("⚠️ Erreur chargement stats.json: " + e.getMessage());
        }
    }

    private void save() {
        try {
            if (stats == null) return;
            try (FileWriter writer = new FileWriter(statsFile)) {
                gson.toJson(stats, writer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Erreur sauvegarde stats.json: " + e.getMessage());
        }
    }

    public Stats getStats() {
        return stats;
    }
}