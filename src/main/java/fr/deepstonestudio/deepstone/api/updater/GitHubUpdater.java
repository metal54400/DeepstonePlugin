package fr.deepstonestudio.deepstone.api.updater;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        String notifyPerm = c.getString("updater.notify-permission", "deepstone.admin");

        if (owner.isBlank() || repo.isBlank()) {
            if (feedbackTo != null) feedbackTo.sendMessage("§7[§c!§7] Updater: owner/repo manquant dans config.yml");
            return;
        }

        String currentVersion = plugin.getDescription().getVersion();

        try {
            plugin.getLogger().info("Updater: checking latest release for " + owner + "/" + repo + " (current=" + currentVersion + ")");

            LatestRelease rel = fetchLatestRelease(owner, repo);

            if (rel == null) {
                if (feedbackTo != null) feedbackTo.sendMessage("§7[§c!§7] Updater: impossible de lire la release GitHub.");
                return;
            }

            if (!isNewer(rel.tagName, currentVersion)) {
                if (feedbackTo != null) feedbackTo.sendMessage("§7[§e?§7] Deepstone est déjà à jour. (v" + currentVersion + ")");
                return;
            }

            Asset asset = pickJarAsset(rel.fullJson, forcedAssetName);
            if (asset == null) {
                if (feedbackTo != null) feedbackTo.sendMessage("§7[§c!§7] Updater: aucun asset .jar trouvé sur la dernière release.");
                return;
            }

            File updateFolder = Bukkit.getUpdateFolderFile();
            if (!updateFolder.exists() && !updateFolder.mkdirs()) {
                if (feedbackTo != null) feedbackTo.sendMessage("§7[§c!§7] Updater: impossible de créer le dossier update/");
                return;
            }

            // ✅ Nom réel du jar actuel (Paper remplace mieux si même nom)
            String targetName = getCurrentJarFileName();
            File outFile = new File(updateFolder, targetName);

            plugin.getLogger().info("Updater: downloading " + asset.name + " -> " + outFile.getPath());
            downloadFile(asset.url, outFile);

            String msg =
                    "§7[§a✓§7] Nouvelle version trouvée: §e" + rel.tagName +
                            "§7 (actuelle: §e" + currentVersion + "§7). Téléchargée dans §e" + updateFolder.getName() +
                            "§7 sous §e" + targetName + "§7. §cRedémarre le serveur§7 pour appliquer la mise à jour.";

            if (feedbackTo != null) feedbackTo.sendMessage(msg);

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission(notifyPerm))
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

        if (code < 200 || code >= 300) {
            // Très utile: 403 rate-limit / 404 / etc.
            throw new IOException("GitHub API HTTP " + code + " : " + truncate(body, 300));
        }

        String tag = extractJsonString(body, "tag_name");
        if (tag == null) return null;

        return new LatestRelease(tag, body);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        s = s.replace("\n", " ").replace("\r", " ");
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        return m.find() ? unescape(m.group(1)) : null;
    }

    private static String unescape(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\/", "/");
    }

    /* =========================
       Assets (robuste sans JSON lib)
       ========================= */

    private static final class Asset {
        final String name;
        final String url;

        Asset(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    /**
     * Cherche un asset qui match:
     * - si forcedAssetName non vide => égalité name (case-insensitive)
     * - sinon => premier .jar
     *
     * On ne découpe PAS par { } car la réponse GitHub contient des objets imbriqués.
     */
    private Asset pickJarAsset(String fullReleaseJson, String forcedAssetName) {
        if (fullReleaseJson == null) return null;

        Pattern p = Pattern.compile(
                "\"name\"\\s*:\\s*\"(.*?)\".*?\"browser_download_url\"\\s*:\\s*\"(.*?)\"",
                Pattern.DOTALL
        );
        Matcher m = p.matcher(fullReleaseJson);

        while (m.find()) {
            String name = unescape(m.group(1));
            String url = unescape(m.group(2));
            if (name == null || url == null) continue;

            boolean nameOk = forcedAssetName != null && !forcedAssetName.isBlank()
                    ? name.equalsIgnoreCase(forcedAssetName)
                    : name.toLowerCase(Locale.ROOT).endsWith(".jar");

            if (nameOk) return new Asset(name, url);
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
            InputStream es = con.getErrorStream();
            String err = (es != null) ? new String(es.readAllBytes()) : "";
            throw new IOException("HTTP " + code + " lors du téléchargement: " + truncate(err, 200));
        }

        // écrit dans un fichier temporaire puis rename (safe)
        File tmp = new File(outFile.getParentFile(), outFile.getName() + ".tmp");

        try (InputStream in = con.getInputStream();
             OutputStream out = Files.newOutputStream(tmp.toPath())) {
            in.transferTo(out);
        }

        // remplace atomiquement "à l'ancienne"
        if (outFile.exists() && !outFile.delete()) {
            // cleanup tmp
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            throw new IOException("Impossible de remplacer " + outFile.getName());
        }
        if (!tmp.renameTo(outFile)) {
            // cleanup tmp
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
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
        int[] out = new int[]{0, 0, 0};
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    /* =========================
       Jar name (important)
       ========================= */

    private String getCurrentJarFileName() {
        try {
            URL url = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            File file = new File(url.toURI());
            String name = file.getName();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".jar")) return plugin.getName() + ".jar";
            return name;
        } catch (Exception e) {
            return plugin.getName() + ".jar";
        }
    }

    private record LatestRelease(String tagName, String fullJson) {}
}