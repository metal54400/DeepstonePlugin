package fr.deepstonestudio.deepstone.util;

import fr.deepstonestudio.deepstone.storage.TipsStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TipsService {

    private final JavaPlugin plugin;
    private final TipsStore store;

    private int taskId = -1;

    public TipsService(JavaPlugin plugin, TipsStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void start() {
        stop();

        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("tips.enabled", true)) return;

        int intervalSeconds = Math.max(30, cfg.getInt("tips.interval-seconds", 300));
        long intervalTicks = intervalSeconds * 20L;

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            FileConfiguration c = plugin.getConfig(); // reload-safe
            if (!c.getBoolean("tips.enabled", true)) return;

            String prefix = color(c.getString("tips.prefix", "&f[Server] &7[&e?&7] "));
            List<String> msgs = c.getStringList("tips.messages");
            if (msgs == null || msgs.isEmpty()) return;

            String msg;
            if (c.getBoolean("tips.random", true)) {
                msg = msgs.get(ThreadLocalRandom.current().nextInt(msgs.size()));
            } else {
                // rotation simple
                int i = c.getInt("tips._rotation", 0);
                if (i >= msgs.size()) i = 0;
                msg = msgs.get(i);
                c.set("tips._rotation", i + 1);
                plugin.saveConfig();
            }

            String finalMsg = prefix + color(msg);

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (store.isTipsEnabled(p.getUniqueId())) {
                    p.sendMessage(finalMsg);
                }
            }
        }, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void restart() {
        start();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    private static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}