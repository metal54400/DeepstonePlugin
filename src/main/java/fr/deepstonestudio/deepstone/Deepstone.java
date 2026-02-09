package fr.deepstonestudio.deepstone;

import fr.deepstonestudio.deepstone.Commands.ClearLagCommand;
import fr.deepstonestudio.deepstone.Listener.*;
import fr.deepstonestudio.deepstone.Manager.ProtectionManager;
import fr.deepstonestudio.deepstone.api.AFK.AfkService;
import fr.deepstonestudio.deepstone.api.AFK.Listener.PlayerActivityListener;
import fr.deepstonestudio.deepstone.api.DeepstoneAfkExpansion;
import fr.deepstonestudio.deepstone.api.EssentialsHook;
import fr.deepstonestudio.deepstone.util.ClearLoop;
import fr.deepstonestudio.deepstone.util.ClearService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Deepstone extends JavaPlugin {

    private ClearService clearService;
    private ClearLoop clearLoop;
    private ProtectionManager protectionManager;
    private EssentialsHook essentialsHook;
    private AfkService afkService;
    public static Deepstone instance;

    @Override
    public void onEnable() {

        this.protectionManager = new ProtectionManager(this, 15 * 60); // 15 minutes en secondes

        protectionManager.startCleanupTask();

        getServer().getPluginManager().registerEvents(
                new CommandBlockListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new VillagerTradeLimiter(), this
        );
        getServer().getPluginManager().registerEvents(
                new RaidListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new TradeHours(), this
        );
        getServer().getPluginManager().registerEvents(
                new CreativeItemLoreListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new TeleportListener(protectionManager, 2.5), this
        );
        getServer().getPluginManager().registerEvents(
                new PvpListener(protectionManager), this
        );


        // Plugin startup logic
        saveDefaultConfig();

        this.clearService = new ClearService(this);
        this.clearLoop = new ClearLoop(this, clearService);

        getCommand("clearlag").setExecutor(new ClearLagCommand(clearService));

        // Démarre la boucle
        clearLoop.start();

        EssentialsHook essentialsHook = null;
        Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials != null && essentials.isEnabled()) {
            essentialsHook = EssentialsHook.tryCreate(essentials);
            if (essentialsHook != null) {
                getLogger().info("Hook Essentials: OK (statut AFK géré par Essentials)");
            } else {
                getLogger().warning("Essentials détecté mais hook impossible (version incompatible ?).");
            }
        } else {
            getLogger().info("Essentials non présent: AFK + kick gérés par DeepstoneAFK.");
        }

        // Si Essentials absent -> on active le système AFK interne
        if (essentialsHook == null) {
            long afkTimeout = getConfig().getLong("afk.timeout-seconds", 300L);
            long kickSeconds = getConfig().getLong("afk.kick-seconds", 1200L);
            String kickMessage = getConfig().getString("afk.kick-message", "&cKick: AFK trop longtemps.");

            this.afkService = new AfkService(this, afkTimeout, kickSeconds, kickMessage);

            getServer().getPluginManager().registerEvents(new PlayerActivityListener(afkService), this);
            afkService.start();
        } else {
            // On garde un service "vide" (utile pour placeholder fallback éventuel)
            this.afkService = new AfkService(this, -1, -1, "§6AFK");
        }

        // Hook PlaceholderAPI (optionnel)
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi != null && papi.isEnabled()) {
            new DeepstoneAfkExpansion(this, afkService, essentialsHook).register();
            getLogger().info("PlaceholderAPI détecté: %deepstone_afk% enregistré.");
        } else {
            getLogger().info("PlaceholderAPI non présent: aucun placeholder enregistré.");
        }
        instance = this;

        getLogger().info("Deepstone activé !");
        getLogger().info("Deepstone ClearLagg activé !");
        getLogger().info("Deepstone AFK activé !");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (clearLoop != null) clearLoop.stop();
        if (afkService != null) afkService.stop();
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
    public static Deepstone getInstance() {
        return instance;
    }
}
