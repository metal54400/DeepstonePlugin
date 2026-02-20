package fr.deepstonestudio.deepstone.Manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectionManager {

    private final JavaPlugin plugin;

    private long protectionMs;
    private long cleanupIntervalTicks;

    private boolean messageEnabled;
    private String message;

    private final Map<UUID, Long> protectedUntil = new ConcurrentHashMap<>();

    public ProtectionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    /** Recharge les valeurs depuis config.yml */
    public void reloadFromConfig() {
        // Valeurs par défaut si absent du config
        int durationSeconds = plugin.getConfig().getInt("protection.duration-seconds", 300);
        int cleanupIntervalSeconds = plugin.getConfig().getInt("protection.cleanup-interval-seconds", 60);

        this.protectionMs = Math.max(1, durationSeconds) * 1000L;
        this.cleanupIntervalTicks = Math.max(1, cleanupIntervalSeconds) * 20L;

        this.messageEnabled = plugin.getConfig().getBoolean("protection.message-enabled", true);
        this.message = plugin.getConfig().getString(
                "protection.message",
                "§7[§e?§7] §aProtection anti-TPKill activée pendant {minutes} minutes ({seconds}s)."
        );
    }

    public boolean isProtected(Player p) {
        Long until = protectedUntil.get(p.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public void grant(Player p) {
        long until = System.currentTimeMillis() + protectionMs;
        protectedUntil.put(p.getUniqueId(), until);

        if (messageEnabled) {
            long secs = protectionMs / 1000L;
            long mins = secs / 60L;

            String msg = message
                    .replace("{seconds}", String.valueOf(secs))
                    .replace("{minutes}", String.valueOf(mins));

            p.sendMessage(msg);
        }
    }

    public void remove(Player p, String reason) {
        if (protectedUntil.remove(p.getUniqueId()) != null) {
            if (reason == null || reason.isEmpty()) {
                p.sendMessage("§7[§c!§7] Protection anti-TPKill retirée.");
            } else {
                p.sendMessage("§7[§c!§7] Protection anti-TPKill retirée (§e" + reason + "§7).");
            }
        }
    }

    public long getRemainingSeconds(Player p) {
        Long until = protectedUntil.get(p.getUniqueId());
        if (until == null) return 0;
        long remainingMs = until - System.currentTimeMillis();
        return Math.max(0, remainingMs / 1000L);
    }

    public void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            protectedUntil.entrySet().removeIf(e -> e.getValue() <= now);
        }, cleanupIntervalTicks, cleanupIntervalTicks);
    }
}