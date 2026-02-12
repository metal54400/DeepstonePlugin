package fr.deepstonestudio.deepstone.api;

import fr.deepstonestudio.deepstone.model.Clan;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class DiscordWebhook {

    private final JavaPlugin plugin;

    public DiscordWebhook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendTop3(List<Clan> top) {
        if (!plugin.getConfig().getBoolean("discord.enabled", true)) return;

        String webhook = plugin.getConfig().getString("discord.webhook-url", "");
        if (webhook == null || webhook.isBlank()) return;

        String mentionRole = plugin.getConfig().getString("discord.mention-role-id", "");
        String mention = (mentionRole != null && !mentionRole.isBlank()) ? "<@&" + mentionRole + "> " : "";

        StringBuilder sb = new StringBuilder();
        sb.append(mention).append("🏆 **Fin de saison Deepstone**\n");
        for (int i = 0; i < top.size(); i++) {
            Clan c = top.get(i);
            sb.append("**#").append(i + 1).append("** ")
                    .append(c.getDisplayName())
                    .append(" — ").append(c.getGlory()).append(" gloire\n");
        }

        String json = "{\"content\":\"" + escape(sb.toString()) + "\"}";

        // ASYNC (important)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> post(webhook, json));
    }

    private void post(String webhookUrl, String json) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(webhookUrl).openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = con.getOutputStream()) {
                os.write(bytes);
            }

            int code = con.getResponseCode();
            if (code < 200 || code >= 300) {
                plugin.getLogger().warning("Discord webhook failed: HTTP " + code);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Discord webhook error: " + e.getMessage());
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}