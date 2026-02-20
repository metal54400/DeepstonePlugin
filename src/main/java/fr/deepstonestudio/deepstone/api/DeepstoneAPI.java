package fr.deepstonestudio.deepstone.api;

import fr.deepstonestudio.deepstone.Deepstone;
import fr.deepstonestudio.deepstone.util.ClanService;
import fr.deepstonestudio.deepstone.util.WarService;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

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
        return plugin.getClans(); // on ajoute un getter dans Deepstone
    }

    public static WarService getWars() {
        ensureReady();
        return plugin.getWarService(); // getter à ajouter
    }

    /** Petit helper : vérifier que Deepstone est présent */
    public static boolean isPresent(Plugin p) {
        return Objects.equals(p.getServer().getPluginManager().getPlugin("Deepstone"), plugin);
    }
}