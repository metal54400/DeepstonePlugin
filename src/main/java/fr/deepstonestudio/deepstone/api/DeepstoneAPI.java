package fr.deepstonestudio.deepstone.api;

import fr.deepstonestudio.deepstone.Deepstone;
import fr.deepstonestudio.deepstone.util.ClanService;
import fr.deepstonestudio.deepstone.util.GloryService;
import fr.deepstonestudio.deepstone.util.WarService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class DeepstoneAPI {

    private static Deepstone plugin;

    private DeepstoneAPI() {}

    /** Appelée par Deepstone au onEnable() */
    public static void init(Deepstone instance) {
        plugin = instance;
    }

    /** Vérifie que l’API est prête */
    private static void ensureReady() {
        if (plugin == null) throw new IllegalStateException("DeepstoneAPI not initialized.");
    }

    /** Récupère le plugin Deepstone */
    public static Deepstone getPlugin() {
        ensureReady();
        return plugin;
    }

    /** Services exposés */
    public static ClanService getClans() {
        ensureReady();
        return plugin.getClans();
    }

    public static WarService getWars() {
        ensureReady();
        return plugin.getWarService();
    }

    public static GloryService getGlory() {
        ensureReady();
        return plugin.getGloryService();
    }

    /** Vérifie si Deepstone est installé + initialisé */
    public static boolean isReady() {
        return plugin != null && plugin.isEnabled();
    }

    /** Vérifie si le plugin Deepstone est présent sur le serveur */
    public static boolean isPresent() {
        Plugin p = Bukkit.getPluginManager().getPlugin("Deepstone");
        return p != null && p.isEnabled();
    }
}