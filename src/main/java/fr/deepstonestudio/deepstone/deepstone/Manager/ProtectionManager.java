package fr.deepstonestudio.deepstone.Manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectionManager {

    private final JavaPlugin plugin;
    private final long protectionMs;
    private final Map<UUID, Long> protectedUntil = new ConcurrentHashMap<>();

    public ProtectionManager(JavaPlugin plugin, int durationSeconds) {
        this.plugin = plugin;
        this.protectionMs = durationSeconds * 1000L;
    }

    public boolean isProtected(Player p) {
        Long until = protectedUntil.get(p.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public void grant(Player p) {
        long until = System.currentTimeMillis() + protectionMs;
        protectedUntil.put(p.getUniqueId(), until);
        p.sendMessage(ChatColor.GREEN + "§7[§e?§7] Protection anti-TPKill activée pendant 15 minutes.");
    }

    public void remove(Player p, String reason) {
        if (protectedUntil.remove(p.getUniqueId()) != null) {
            if (reason == null || reason.isEmpty()) {
                p.sendMessage(ChatColor.YELLOW + "§7[§c!§7] Protection anti-TPKill retirée.");
            } else {
                p.sendMessage(ChatColor.YELLOW + "§7[§c!§7] Protection anti-TPKill retirée (" + reason + ").");
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
        }, 20L * 60L, 20L * 60L); // toutes les 60 secondes
    }
}
