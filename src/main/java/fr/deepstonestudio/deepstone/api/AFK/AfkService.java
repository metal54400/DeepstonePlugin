package fr.deepstonestudio.deepstone.api.AFK;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AfkService {

    private static final String PERM_BYPASS = "deepstone.afk.bypass";
    private static final String PERM_KICK_EXEMPT = "deepstone.afk.kickexempt";

    private final JavaPlugin plugin;
    private final long afkTimeoutSeconds;   // -1 disable
    private final long kickSeconds;         // -1 disable
    private final String kickMessageColored;

    private final Map<UUID, Long> lastActivityMs = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> afkState = new ConcurrentHashMap<>();
    private final Map<UUID, Long> afkSinceMs = new ConcurrentHashMap<>();

    private int taskId = -1;

    public AfkService(JavaPlugin plugin, long afkTimeoutSeconds, long kickSeconds, String kickMessage) {
        this.plugin = plugin;
        this.afkTimeoutSeconds = afkTimeoutSeconds;
        this.kickSeconds = kickSeconds;
        this.kickMessageColored = ChatColor.translateAlternateColorCodes('&', kickMessage);
    }

    public void markActive(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        lastActivityMs.put(id, now);

        // Sortie AFK immédiate si besoin
        if (Boolean.TRUE.equals(afkState.get(id))) {
            afkState.put(id, false);
            afkSinceMs.remove(id);
        }
    }

    public boolean isAfk(Player player) {
        return Boolean.TRUE.equals(afkState.get(player.getUniqueId()));
    }

    public void start() {
        // init joueurs déjà connectés
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            lastActivityMs.putIfAbsent(p.getUniqueId(), now);
            afkState.putIfAbsent(p.getUniqueId(), false);
        }

        // Check toutes les 5 secondes
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (afkTimeoutSeconds < 0) return;

            long now2 = System.currentTimeMillis();

            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();

                // Bypass: jamais AFK + jamais kick
                if (p.hasPermission(PERM_BYPASS)) {
                    afkState.put(id, false);
                    afkSinceMs.remove(id);
                    lastActivityMs.put(id, now2);
                    continue;
                }

                long last = lastActivityMs.getOrDefault(id, now2);
                boolean shouldBeAfk = (now2 - last) >= (afkTimeoutSeconds * 1000L);
                boolean currentlyAfk = Boolean.TRUE.equals(afkState.get(id));

                // Transition AFK on/off
                if (shouldBeAfk && !currentlyAfk) {
                    afkState.put(id, true);
                    afkSinceMs.put(id, now2);
                } else if (!shouldBeAfk && currentlyAfk) {
                    afkState.put(id, false);
                    afkSinceMs.remove(id);
                }

                // Auto-kick si AFK (mode Deepstone)
                if (kickSeconds >= 0 && Boolean.TRUE.equals(afkState.get(id)) && !p.hasPermission(PERM_KICK_EXEMPT)) {
                    long since = afkSinceMs.getOrDefault(id, now2);
                    long afkDurationSec = (now2 - since) / 1000L;

                    if (afkDurationSec >= kickSeconds) {
                        p.kickPlayer(kickMessageColored);
                    }
                }
            }
        }, 100L, 100L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        lastActivityMs.clear();
        afkState.clear();
        afkSinceMs.clear();
    }
}