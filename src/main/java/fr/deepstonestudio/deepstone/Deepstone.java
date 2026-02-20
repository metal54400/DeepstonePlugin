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
    saveDefaultConfig();

    // ===== Detect deps FIRST =====
    economy = setupEconomy();

    Plugin gp = Bukkit.getPluginManager().getPlugin("GriefPrevention");
    gpEnabled = (gp != null && gp.isEnabled());
    getLogger().info(gpEnabled ? "GriefPrevention détecté ✔" : "GriefPrevention non détecté.");

    // Essentials hook
    Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
    if (essentials != null && essentials.isEnabled()) {
        this.essentialsHook = EssentialsHook.tryCreate(essentials);
        if (this.essentialsHook != null) {
            getLogger().info("Hook Essentials: OK (Deepstone n'active pas son AFK interne).");
        }
    }

    // ===== Core services (SYNC only) =====
    this.protectionManager = new ProtectionManager(this);
    protectionManager.startCleanupTask();

    this.blessingManager = new BlessingManager(this);

    this.clearService = new ClearService(this);
    this.clearLoop = new ClearLoop(this, clearService);

    // IMPORTANT: no local variables => use fields ONLY
    this.warService = new WarService();
    this.gloryService = new GloryService(this);

    // Storage + clans
    var store = new YamlStore(this);
    this.clans = new ClanService(store, gpEnabled);
    MercenaryService mercService = new MercenaryService(clans, store);

    // ===== Register commands/listeners that DON'T need loaded data =====
    registerCommands(mercService);
    registerListeners(mercService);

    // AFK + updater can start now (they don't need clans loaded)
    setupAfk();
    setupUpdater();

    // ===== Load data ASYNC =====
    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
        try {
            mercService.loadAll();
            this.clans.loadAll();
        } catch (Exception e) {
            getLogger().severe("Load error: " + e.getMessage());
            e.printStackTrace();
        }

        // Back to main thread for Bukkit stuff
        Bukkit.getScheduler().runTask(this, () -> {
            DeepstoneAPI.init(this);

            DiscordWebhook discord = new DiscordWebhook(this);
            SeasonService season = new SeasonService(this, this.clans, this.gloryService, discord);
            season.startScheduler();

            var capital = new CapitalManager(this, clans);
            getServer().getPluginManager().registerEvents(capital, this);
            capital.start();

            // Blessings cleanup
            getServer().getScheduler().runTaskTimer(this,
                    () -> blessingManager.cleanupExpired(true),
                    20L * 60L, 20L * 60L * 5L);

            getLogger().info("Deepstone activé (data chargées) !");
        });
    });
}

private void registerCommands(MercenaryService mercService) {
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
        getCommand("war").setExecutor(new WarCommand(this.clans, this.warService));
    }

    if (getCommand("priere") != null) {
        getCommand("priere").setExecutor(
                new PriereCommand(economy, sacrificeMap, priereDeathCauseMap, blessingManager)
        );
    }

    if (getCommand("mercenary") != null) {
        MercenaryCommand mercCmd = new MercenaryCommand(mercService);
        getCommand("mercenary").setExecutor(mercCmd);
        getCommand("mercenary").setTabCompleter(mercCmd);
    }
}

private void registerListeners(MercenaryService mercService) {
    RuneProtectionManager runeProtection = new RuneProtectionManager();

    getServer().getPluginManager().registerEvents(new RuneProtectionListener(runeProtection), this);
    getServer().getPluginManager().registerEvents(new CommandBlockListener(), this);
    getServer().getPluginManager().registerEvents(new MobHeadDropListener(this), this);
    getServer().getPluginManager().registerEvents(new VillagerTradeLimiter(this), this);
    getServer().getPluginManager().registerEvents(new RaidListener(this), this);
    getServer().getPluginManager().registerEvents(new TradeHours(this), this);
    getServer().getPluginManager().registerEvents(new CreativeItemLoreListener(this), this);
    getServer().getPluginManager().registerEvents(new TeleportListener(protectionManager, 2.5), this);
    getServer().getPluginManager().registerEvents(new PvpListener(protectionManager), this);
    getServer().getPluginManager().registerEvents(new DeathListener(this, runeProtection), this);
    getServer().getPluginManager().registerEvents(new ClanFriendlyFireListener(clans, mercService), this);
    getServer().getPluginManager().registerEvents(new ClanChatListener(this, clans), this);
    getServer().getPluginManager().registerEvents(new SacrificeListener(sacrificeMap), this);
    getServer().getPluginManager().registerEvents(new ShopPriceListener(), this);
    getServer().getPluginManager().registerEvents(new DecapitationListener(this), this);

    // Use FIELDS war/glory (no locals)
    getServer().getPluginManager().registerEvents(
            new WarListener(this.clans, this.warService, this.gloryService),
            this
    );

    getServer().getPluginManager().registerEvents(new PriereDeathListener(priereDeathCauseMap), this);
    getServer().getPluginManager().registerEvents(new BlessingListener(blessingManager), this);
    getServer().getPluginManager().registerEvents(new ClaimActionbarListener(this), this);
    getServer().getPluginManager().registerEvents(new ZombieVillagerBoostListener(this), this);
}

private void setupUpdater() {
    GitHubUpdater updater = new GitHubUpdater(this);

    if (getConfig().getBoolean("updater.check-on-start", true)) {
        Bukkit.getScheduler().runTaskAsynchronously(this,
                () -> updater.checkAndDownloadIfNeeded(Bukkit.getConsoleSender()));
    }

    long minutes = getConfig().getLong("updater.check-interval-minutes", 60);
    if (minutes > 0) {
        long ticks = minutes * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> updater.checkAndDownloadIfNeeded(null),
                ticks, ticks);
    }
}

private void setupAfk() {
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

    boolean useInternalAfk = (this.essentialsHook == null);

    this.afkService = new AfkService(
            this,
            useInternalAfk ? autoAfk : -1,
            useInternalAfk ? autoAfkKick : -1,
            kickMessage,
            true,
            broadcast,
            msgNowAfk,
            msgNoLonger
    );

    if (useInternalAfk) {
        Bukkit.getPluginManager().registerEvents(
                new PlayerActivityListener(this.afkService, cancelOnMove, cancelOnChat, cancelOnInteract),
                this
        );
        this.afkService.start();
    }

    Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
    if (papi != null && papi.isEnabled()) {
        new DeepstoneAfkExpansion(this, this.afkService, this.essentialsHook).register();
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
