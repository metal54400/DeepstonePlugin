package fr.deepstonestudio.deepstone;

import fr.deepstonestudio.deepstone.Commands.*;
import fr.deepstonestudio.deepstone.Listener.*;
import fr.deepstonestudio.deepstone.Manager.BlessingManager;
import fr.deepstonestudio.deepstone.Manager.CapitalManager;
import fr.deepstonestudio.deepstone.Manager.ProtectionManager;
import fr.deepstonestudio.deepstone.Manager.RuneProtectionManager;
import fr.deepstonestudio.deepstone.api.*;
import fr.deepstonestudio.deepstone.api.AFK.AfkService;
import fr.deepstonestudio.deepstone.api.AFK.Listener.PlayerActivityListener;
import fr.deepstonestudio.deepstone.api.updater.GitHubUpdater;
import fr.deepstonestudio.deepstone.storage.YamlStore;
import fr.deepstonestudio.deepstone.util.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Deepstone extends JavaPlugin {

    private ClearService clearService;
    private ClearLoop clearLoop;
    private ProtectionManager protectionManager;

    private EssentialsHook essentialsHook; // champ (pas variable locale)
    private AfkService afkService;
    private ClanService clans;
    private WarService warService;
    private GloryService gloryService;
    private boolean gpEnabled = false;
    public static Deepstone instance;
    private Economy economy; // peut rester null
    private final Map<UUID, Long> sacrificeMap = new HashMap<>();
    private final Map<UUID, String> priereDeathCauseMap = new HashMap<>();

    private BlessingManager blessingManager;


    @Override
    public void onEnable() {
        instance = this;
        economy = setupEconomy();
        this.protectionManager = new ProtectionManager(this);
        protectionManager.startCleanupTask();
        saveDefaultConfig();

        blessingManager = new BlessingManager(this);

        WarService warService = new WarService();
        GloryService gloryService = new GloryService(this);

        this.clearService = new ClearService(this);
        this.clearLoop = new ClearLoop(this, clearService);
        this.warService = new WarService();
        this.gloryService = new GloryService(this);

        DiscordWebhook discord = new DiscordWebhook(this);
        SeasonService season = new SeasonService(this, this.clans, this.gloryService, discord);
        season.startScheduler();

        var store = new YamlStore(this);
        this.clans = new ClanService(store, gpEnabled);
        MercenaryService mercService = new MercenaryService(clans, store);
        try {
            mercService.loadAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DeepstoneAPI.init(this);

        try {
            this.clans.loadAll();
        } catch (Exception e) {
            getLogger().severe("Failed to load clans: " + e.getMessage());
            e.printStackTrace();
        }

        // ===== Commands =====
        if (getCommand("clearlag") != null) {
            getCommand("clearlag").setExecutor(new ClearLagCommand(clearService));
        }

        if (getCommand("clan") != null) {
            ClanCommand cmd = new ClanCommand(this.clans);
            getCommand("clan").setExecutor(cmd);
            getCommand("clan").setTabCompleter(cmd);
        } else {
            getLogger().severe("Commande /clan manquante dans plugin.yml");
        }

        if (getCommand("war") != null) {
            getCommand("war").setExecutor(new WarCommand(this.clans, warService));
        } else {
            getLogger().severe("Commande /war manquante dans plugin.yml");
        }
        getCommand("priere").setExecutor(
                new PriereCommand(economy, sacrificeMap, priereDeathCauseMap, blessingManager)
        );
        MercenaryCommand mercCmd = new MercenaryCommand(mercService);
        getCommand("mercenary").setExecutor(mercCmd);
        getCommand("mercenary").setTabCompleter(mercCmd);



        if (economy == null) {
            getLogger().warning("Vault/economy non trouvé -> fallback activé: la récompense € sera remplacée par 10 lingots de fer.");
        } else {
            getLogger().info("Vault economy détectée -> récompense € active.");
        }


        // ===== Events =====
        RuneProtectionManager runeProtection = new RuneProtectionManager();
        getServer().getPluginManager().registerEvents(new RuneProtectionListener(runeProtection), this);
        getServer().getPluginManager().registerEvents(new CommandBlockListener(), this);
        getServer().getPluginManager().registerEvents(new MobHeadDropListener(this),this);
        getServer().getPluginManager().registerEvents(new VillagerTradeLimiter(this), this);
        getServer().getPluginManager().registerEvents(new RaidListener(this), this);
        getServer().getPluginManager().registerEvents(new TradeHours(this), this);
        getServer().getPluginManager().registerEvents(new CreativeItemLoreListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(protectionManager, 2.5), this);
        getServer().getPluginManager().registerEvents(new PvpListener(protectionManager), this);
        getServer().getPluginManager().registerEvents(new DeathListener(runeProtection, this), this);
        getServer().getPluginManager().registerEvents(new ClanFriendlyFireListener(clans,mercService), this);
        getServer().getPluginManager().registerEvents(new ClanChatListener(this, clans), this);
        getServer().getPluginManager().registerEvents(new SacrificeListener(sacrificeMap), this);
        getServer().getPluginManager().registerEvents(new ShopPriceListener(), this);
        getServer().getPluginManager().registerEvents(new DecapitationListener(this), this);
        // IMPORTANT: on passe la même instance de ClanService + war + glory
        getServer().getPluginManager().registerEvents(
                new WarListener(this.clans, warService, gloryService),
                this
        );
        getServer().getPluginManager().registerEvents(new PriereDeathListener(priereDeathCauseMap), this);
        getServer().getPluginManager().registerEvents(new BlessingListener(blessingManager), this);
        getServer().getPluginManager().registerEvents(new ClaimActionbarListener(this), this);
        getServer().getPluginManager().registerEvents(new ZombieVillagerBoostListener(this), this);

        // Nettoyage périodique des bénédictions expirées (toutes les 5 minutes)
        getServer().getScheduler().runTaskTimer(this, () -> blessingManager.cleanupExpired(true), 20L * 60L, 20L * 60L * 5L);
        var capital = new CapitalManager(this, clans);
        getServer().getPluginManager().registerEvents(capital, this);
        capital.start();
        RunePlacer runePlacer = new RunePlacer(this, runeProtection);
        //clearLoop.start();

        // ===============================
        //           AFK SETUP
        // ===============================

        // Config "classique"
        long autoAfk = getConfig().getLong("auto-afk", 300L);
        long autoAfkKick = getConfig().getLong("auto-afk-kick", 1200L);

        boolean cancelOnMove = getConfig().getBoolean("cancel-afk-on-move", true);
        boolean cancelOnChat = getConfig().getBoolean("cancel-afk-on-chat", true);
        boolean cancelOnInteract = getConfig().getBoolean("cancel-afk-on-interact", true);

        boolean broadcast = getConfig().getBoolean("broadcast-afk-message", true);

        String afkSuffix = getConfig().getString("afk-suffix", "&7[AFK]");
        String msgNowAfk = getConfig().getString("message-now-afk", "&e{PLAYER} &7est maintenant " + afkSuffix);
        String msgNoLonger = getConfig().getString("message-no-longer-afk", "&e{PLAYER} &7n'est plus AFK");
        String kickMessage = getConfig().getString("kick-message", "&cKick: AFK trop longtemps.");

        // Hook Essentials (optionnel)
        Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials != null && essentials.isEnabled()) {
            this.essentialsHook = EssentialsHook.tryCreate(essentials);
            if (this.essentialsHook != null) {
                getLogger().info("Hook Essentials: OK (Deepstone n'active pas son AFK interne).");
            } else {
                getLogger().warning("Essentials détecté mais hook impossible (version incompatible ?).");
            }
        } else {
            getLogger().info("Essentials non présent: AFK + kick gérés par Deepstone.");
        }

        if (getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
            gpEnabled = true;
            getLogger().info("GriefPrevention détecté ✔");
        } else {
            getLogger().info("GriefPrevention non détecté. Fonctionnalités de trust désactivées.");
        }

        boolean useInternalAfk = (this.essentialsHook == null);

        // Création du service AFK
        // Si Essentials présent -> on désactive le timer interne (-1 / -1)
        this.afkService = new AfkService(
                this,
                useInternalAfk ? autoAfk : -1,
                useInternalAfk ? autoAfkKick : -1,
                kickMessage,
                true,          // messagesEnabled
                broadcast,      // broadcastMessages
                msgNowAfk,
                msgNoLonger
        );

        // Listener activité + timer seulement si Essentials absent
        if (useInternalAfk) {
            Bukkit.getPluginManager().registerEvents(
                    new PlayerActivityListener(this.afkService, cancelOnMove, cancelOnChat, cancelOnInteract),
                    this
            );
            this.afkService.start();
        }

        // Hook PlaceholderAPI (optionnel)
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi != null && papi.isEnabled()) {
            new DeepstoneAfkExpansion(this, this.afkService, this.essentialsHook).register();
            getLogger().info("PlaceholderAPI détecté: %deepstone_afk% enregistré.");
        } else {
            getLogger().info("PlaceholderAPI non présent: aucun placeholder enregistré.");
        }

        getLogger().info("Deepstone activé !");
        getLogger().info("DeepstoneClans activé.");
        getLogger().info("Deepstone ClearLagg désactivé !");
        getLogger().info("Deepstone AFK activé !");
        getLogger().info("Deepstone githubupdateer !");


        GitHubUpdater updater = new GitHubUpdater(this);

        if (getConfig().getBoolean("updater.check-on-start", true)) {
            // check async pour ne pas freeze le serveur
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> updater.checkAndDownloadIfNeeded(Bukkit.getConsoleSender()));
        }

// check régulier
        long minutes = getConfig().getLong("updater.check-interval-minutes", 60);
        if (minutes > 0) {
            long ticks = minutes * 60L * 20L;
            Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                    () -> updater.checkAndDownloadIfNeeded(null),
                    ticks, ticks);
        }
    }

    private Economy setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return null;

        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) return null;
        return rsp.getProvider();
    }

    @Override
    public void onDisable() {
       // if (clearLoop != null) clearLoop.stop();
        if (afkService != null) afkService.stop();
        try {
            if (clans != null) clans.saveAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean isGpEnabled() {
        return gpEnabled;
    }



    public ClanService getClans() { return clans; }
    public WarService getWarService() { return warService; }
    public GloryService getGloryService() { return gloryService; }
    public BlessingManager getBlessingManager() { return blessingManager; }
    public ProtectionManager getProtectionManager() { return protectionManager; }

    public static Deepstone getInstance() {
        return instance;
    }
}
