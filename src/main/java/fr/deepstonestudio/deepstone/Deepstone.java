package fr.deepstonestudio.deepstone;

import fr.deepstonestudio.deepstone.Commands.*;
import fr.deepstonestudio.deepstone.Listener.*;
import fr.deepstonestudio.deepstone.Manager.CapitalManager;
import fr.deepstonestudio.deepstone.Manager.ProtectionManager;
import fr.deepstonestudio.deepstone.api.AFK.AfkService;
import fr.deepstonestudio.deepstone.api.AFK.Listener.PlayerActivityListener;
import fr.deepstonestudio.deepstone.api.DeepstoneAfkExpansion;
import fr.deepstonestudio.deepstone.api.DiscordWebhook;
import fr.deepstonestudio.deepstone.api.EssentialsHook;
import fr.deepstonestudio.deepstone.api.ShopPriceListener;
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

    @Override
    public void onEnable() {
        instance = this;
        economy = setupEconomy();
        this.protectionManager = new ProtectionManager(this, 15 * 60);
        protectionManager.startCleanupTask();
        saveDefaultConfig();

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
        getCommand("priere").setExecutor(new PriereCommand(economy, sacrificeMap));
        MercenaryCommand mercCmd = new MercenaryCommand(mercService);
        getCommand("mercenary").setExecutor(mercCmd);
        getCommand("mercenary").setTabCompleter(mercCmd);



        if (economy == null) {
            getLogger().warning("Vault/economy non trouvé -> fallback activé: la récompense € sera remplacée par 10 lingots de fer.");
        } else {
            getLogger().info("Vault economy détectée -> récompense € active.");
        }


        // ===== Events =====
        getServer().getPluginManager().registerEvents(new CommandBlockListener(), this);
        getServer().getPluginManager().registerEvents(new VillagerTradeLimiter(), this);
        getServer().getPluginManager().registerEvents(new RaidListener(), this);
        getServer().getPluginManager().registerEvents(new TradeHours(), this);
        getServer().getPluginManager().registerEvents(new CreativeItemLoreListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(protectionManager, 2.5), this);
        getServer().getPluginManager().registerEvents(new PvpListener(protectionManager), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanFriendlyFireListener(clans,mercService), this);
        getServer().getPluginManager().registerEvents(new ClanChatListener(this, clans), this);
        getServer().getPluginManager().registerEvents(new SacrificeListener(sacrificeMap), this);
        getServer().getPluginManager().registerEvents(new ShopPriceListener(), this);
        // IMPORTANT: on passe la même instance de ClanService + war + glory
        getServer().getPluginManager().registerEvents(
                new WarListener(this.clans, warService, gloryService),
                this
        );
        var capital = new CapitalManager(this, clans);
        getServer().getPluginManager().registerEvents(capital, this);
        capital.start();

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



    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public static Deepstone getInstance() {
        return instance;
    }
}
