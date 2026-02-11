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

    private final boolean messagesEnabled;
    private final boolean broadcastMessages;
    private final String msgNowAfk;
    private final String msgNoLongerAfk;

    private final Map<UUID, Long> lastActivityMs = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> afkState = new ConcurrentHashMap<>();
    private final Map<UUID, Long> afkSinceMs = new ConcurrentHashMap<>();

    private int taskId = -1;

    public AfkService(JavaPlugin plugin,
                      long afkTimeoutSeconds,
                      long kickSeconds,
                      String kickMessage,
                      boolean messagesEnabled,
                      boolean broadcastMessages,
                      String msgNowAfk,
                      String msgNoLongerAfk) {

        this.plugin = plugin;
        this.afkTimeoutSeconds = afkTimeoutSeconds;
        this.kickSeconds = kickSeconds;
        this.kickMessageColored = ChatColor.translateAlternateColorCodes('&', kickMessage);

        this.messagesEnabled = messagesEnabled;
        this.broadcastMessages = broadcastMessages;

        // ✅ Traduction des couleurs ici
        this.msgNowAfk = ChatColor.translateAlternateColorCodes('&', msgNowAfk);
        this.msgNoLongerAfk = ChatColor.translateAlternateColorCodes('&', msgNoLongerAfk);
    }

    private void sendAfkMessage(Player player, String template) {
        if (!messagesEnabled) return;
        if (template == null || template.isBlank()) return;

        String msg = template
                .replace("{PLAYER}", player.getName())
                .replace("{DISPLAYNAME}", ChatColor.stripColor(player.getDisplayName()));

        if (broadcastMessages) {
            Bukkit.broadcastMessage(msg);
        } else {
            player.sendMessage(msg);
        }
    }

    public void markActive(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        lastActivityMs.put(id, now);

        // ✅ Si le joueur était AFK, on le remet actif + message
        if (Boolean.TRUE.equals(afkState.get(id))) {
            afkState.put(id, false);
            afkSinceMs.remove(id);
            sendAfkMessage(player, msgNoLongerAfk);
        }
    }

    public boolean isAfk(Player player) {
        return Boolean.TRUE.equals(afkState.get(player.getUniqueId()));
    }

    public void start() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            lastActivityMs.putIfAbsent(p.getUniqueId(), now);
            afkState.putIfAbsent(p.getUniqueId(), false);
        }

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (afkTimeoutSeconds < 0) return;

            long now2 = System.currentTimeMillis();

            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();

                // Bypass: jamais AFK + jamais kick
                if (p.hasPermission(PERM_BYPASS)) {
                    boolean wasAfk = Boolean.TRUE.equals(afkState.get(id));
                    afkState.put(id, false);
                    afkSinceMs.remove(id);
                    lastActivityMs.put(id, now2);

                    if (wasAfk) {
                        sendAfkMessage(p, msgNoLongerAfk);
                    }
                    continue;
                }

                long last = lastActivityMs.getOrDefault(id, now2);
                boolean shouldBeAfk = (now2 - last) >= (afkTimeoutSeconds * 1000L);
                boolean currentlyAfk = Boolean.TRUE.equals(afkState.get(id));

                // ✅ Transition AFK on/off + messages
                if (shouldBeAfk && !currentlyAfk) {
                    afkState.put(id, true);
                    afkSinceMs.put(id, now2);
                    sendAfkMessage(p, msgNowAfk);

                } else if (!shouldBeAfk && currentlyAfk) {
                    afkState.put(id, false);
                    afkSinceMs.remove(id);
                    sendAfkMessage(p, msgNoLongerAfk);
                }

                // Auto-kick si AFK
                if (kickSeconds >= 0
                        && Boolean.TRUE.equals(afkState.get(id))
                        && !p.hasPermission(PERM_KICK_EXEMPT)) {

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
