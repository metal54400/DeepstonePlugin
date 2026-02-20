package fr.deepstonestudio.deepstone.api.updater;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GitHubUpdater {

    private final JavaPlugin plugin;

    public GitHubUpdater(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkAndDownloadIfNeeded(CommandSender feedbackTo) {
        FileConfiguration c = plugin.getConfig();

        if (!c.getBoolean("updater.enabled", true)) return;

        String owner = c.getString("updater.github.owner", "");
        String repo = c.getString("updater.github.repo", "");
        String forcedAssetName = c.getString("updater.github.asset-name", "");

        if (owner.isBlank() || repo.isBlank()) {
            if (feedbackTo != null) feedbackTo.sendMessage("§7[§c!§7] Updater: owner/repo manquant dans config.yml");
            return;
        }

        String currentVersion = plugin.getDescription().getVersion();

        try {
            LatestRelease rel = fetchLatestRelease(owner, repo);

            if (rel == null) {
                if (feedbackTo != null) feedbackTo.sendMessage("§7[§c!§7] Updater: impossible de lire la release GitHub.");
                return;
            }

            if (!isNewer(rel.tagName, currentVersion)) {
                if (feedbackTo != null) feedbackTo.sendMessage("§7[§e?§7] Deepstone est déjà à jour. (v" + currentVersion + ")");
                return;
            }

            String jarUrl = pickJarAssetDownloadUrl(rel.assetsJson, forcedAssetName);
            String jarName = pickJarAssetName(rel.assetsJson, forcedAssetName);

            if (jarUrl == null || jarName == null) {
                if (feedbackTo != null) feedbackTo.sendMessage("§7[§c!§7] Updater: aucun asset .jar trouvé sur la dernière release.");
                return;
            }

            File updateFolder = Bukkit.getUpdateFolderFile();
            if (!updateFolder.exists()) updateFolder.mkdirs();

            // IMPORTANT: pour que l’update folder remplace bien, il faut souvent le même nom que le jar actuel
            // => on utilise le nom actuel du plugin si possible
            String targetName = plugin.getName() + ".jar";
            File outFile = new File(updateFolder, targetName);

            downloadFile(jarUrl, outFile);

            String msg = "§7[§a✓§7] Nouvelle version trouvée: §e" + rel.tagName +
                    "§7 (actuelle: §e" + currentVersion + "§7). Téléchargée dans §e" + updateFolder.getName() +
                    "§7. §cRedémarre le serveur§7 pour appliquer la mise à jour.";
            if (feedbackTo != null) feedbackTo.sendMessage(msg);
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission(c.getString("updater.notify-permission", "deepstone.admin")))
                    .forEach(p -> p.sendMessage(msg));

        } catch (Exception ex) {
            plugin.getLogger().warning("Updater error: " + ex.getMessage());
            if (feedbackTo != null) feedbackTo.sendMessage("§7[§c!§7] Updater: erreur: " + ex.getMessage());
        }
    }

    /* =========================
       GitHub API
       ========================= */

    private LatestRelease fetchLatestRelease(String owner, String repo) throws IOException {
        // GitHub API: GET /repos/{owner}/{repo}/releases/latest :contentReference[oaicite:2]{index=2}
        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";

        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/vnd.github+json");
        con.setRequestProperty("User-Agent", plugin.getName() + "-Updater"); // important pour GitHub
        con.setConnectTimeout(8000);
        con.setReadTimeout(15000);

        int code = con.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
        if (is == null) return null;

        String body = new String(is.readAllBytes());
        // On parse minimalement (sans lib JSON):
        String tag = extractJsonString(body, "tag_name");
        String assets = extractJsonArray(body, "assets"); // on garde le json assets brut

        if (tag == null) return null;
        return new LatestRelease(tag, assets);
    }

    private static String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        return m.find() ? unescape(m.group(1)) : null;
    }

    private static String extractJsonArray(String json, String key) {
        // extraction simple: "assets": [ ... ]
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\n", "\n");
    }

    /* =========================
       Assets
       ========================= */

    private String pickJarAssetDownloadUrl(String assetsJson, String forcedAssetName) {
        if (assetsJson == null) return null;

        // Chaque asset a "name": "...", "browser_download_url": "..."
        Pattern assetPattern = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
        Matcher m = assetPattern.matcher(assetsJson);

        while (m.find()) {
            String asset = m.group(1);
            String name = extractJsonString("{" + asset + "}", "name");
            String url = extractJsonString("{" + asset + "}", "browser_download_url");
            if (name == null || url == null) continue;

            boolean nameOk = forcedAssetName != null && !forcedAssetName.isBlank()
                    ? name.equalsIgnoreCase(forcedAssetName)
                    : name.toLowerCase(Locale.ROOT).endsWith(".jar");

            if (nameOk) return url;
        }
        return null;
    }

    private String pickJarAssetName(String assetsJson, String forcedAssetName) {
        if (assetsJson == null) return null;

        Pattern assetPattern = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
        Matcher m = assetPattern.matcher(assetsJson);

        while (m.find()) {
            String asset = m.group(1);
            String name = extractJsonString("{" + asset + "}", "name");
            if (name == null) continue;

            boolean nameOk = forcedAssetName != null && !forcedAssetName.isBlank()
                    ? name.equalsIgnoreCase(forcedAssetName)
                    : name.toLowerCase(Locale.ROOT).endsWith(".jar");

            if (nameOk) return name;
        }
        return null;
    }

    /* =========================
       Download
       ========================= */

    private void downloadFile(String url, File outFile) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", plugin.getName() + "-Updater");
        con.setConnectTimeout(8000);
        con.setReadTimeout(30000);

        int code = con.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " lors du téléchargement.");
        }

        // écrit dans un fichier temporaire puis rename (safe)
        File tmp = new File(outFile.getParentFile(), outFile.getName() + ".tmp");
        try (InputStream in = con.getInputStream();
             OutputStream out = Files.newOutputStream(tmp.toPath())) {
            in.transferTo(out);
        }

        // remplace
        if (outFile.exists() && !outFile.delete()) {
            throw new IOException("Impossible de remplacer " + outFile.getName());
        }
        if (!tmp.renameTo(outFile)) {
            throw new IOException("Impossible de finaliser le téléchargement (rename).");
        }
    }

    /* =========================
       Version compare (simple)
       ========================= */

    private boolean isNewer(String latestTag, String currentVersion) {
        // Accepte "v1.2.3" ou "1.2.3"
        String a = normalizeVersion(latestTag);
        String b = normalizeVersion(currentVersion);

        int[] va = parseSemver(a);
        int[] vb = parseSemver(b);

        for (int i = 0; i < 3; i++) {
            if (va[i] > vb[i]) return true;
            if (va[i] < vb[i]) return false;
        }
        return false;
    }

    private String normalizeVersion(String v) {
        v = v.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        return v;
    }

    private int[] parseSemver(String v) {
        // fallback: tout ce qui n'est pas chiffre/point => coupé
        v = v.replaceAll("[^0-9.]", "");
        String[] parts = v.split("\\.");
        int[] out = new int[]{0,0,0};
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            try { out[i] = Integer.parseInt(parts[i]); } catch (Exception ignored) {}
        }
        return out;
    }

    private record LatestRelease(String tagName, String assetsJson) {}
}