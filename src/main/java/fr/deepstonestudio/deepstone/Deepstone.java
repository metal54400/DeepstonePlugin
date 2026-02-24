package fr.deepstonestudio.deepstone;

import fr.deepstonestudio.deepstone.Commands.*;
import fr.deepstonestudio.deepstone.Listener.*;
import fr.deepstonestudio.deepstone.Manager.*;
import fr.deepstonestudio.deepstone.api.*;
import fr.deepstonestudio.deepstone.api.AFK.AfkService;
import fr.deepstonestudio.deepstone.api.AFK.Listener.PlayerActivityListener;
import fr.deepstonestudio.deepstone.api.events.*;
import fr.deepstonestudio.deepstone.api.updater.GitHubUpdater;
import fr.deepstonestudio.deepstone.storage.*;
import fr.deepstonestudio.deepstone.util.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class Deepstone extends JavaPlugin {

    public static Deepstone instance;

    // Core
    private Economy economy;
    private boolean gpEnabled = false;

    // Services
    private ProtectionManager protectionManager;
    private BlessingManager blessingManager;
    private ClearService clearService;
    private ClearLoop clearLoop;

    private ClanService clans;
    private WarService warService;
    private GloryService gloryService;

    private EssentialsHook essentialsHook;
    private AfkService afkService;

    private InvSyncManager invSync;

    // Ragnar Event
    private RagnarDeathEvent ragnarEvent;
    private RagnarDeathAutoStart ragnarAuto;

    // Tips
    private TipsStore tipsStore;
    private TipsService tipsService;

    // Misc
    private final Map<UUID, Long> sacrificeMap = new HashMap<>();
    private final Map<UUID, String> priereDeathCauseMap = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        detectDependencies();
        setupCore();
        setupCommandsAndListeners();
        setupAfk();
        setupUpdater();
        setupRagnarDeath();
        loadAsyncData();
    }

    // =====================================================
    // DEPENDENCIES
    // =====================================================

    private void detectDependencies() {
        economy = setupEconomy();

        Plugin gp = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        gpEnabled = (gp != null && gp.isEnabled());

        Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials != null && essentials.isEnabled()) {
            essentialsHook = EssentialsHook.tryCreate(essentials);
        }
    }

    // =====================================================
    // CORE
    // =====================================================

    private void setupCore() {
        protectionManager = new ProtectionManager(this);
        protectionManager.startCleanupTask();

        blessingManager = new BlessingManager(this);

        clearService = new ClearService(this);
        clearLoop = new ClearLoop(this, clearService);

        warService = new WarService();
        gloryService = new GloryService(this);

        var store = new YamlStore(this);
        clans = new ClanService(store, gpEnabled);
    }

    // =====================================================
    // COMMANDS + LISTENERS
    // =====================================================

    private void setupCommandsAndListeners() {
        var store = new YamlStore(this);
        MercenaryService mercService = new MercenaryService(clans, store);

        registerCommand("clearlag", c -> c.setExecutor(new ClearLagCommand(clearService)));

        registerCommand("clan", c -> {
            ClanCommand cmd = new ClanCommand(clans);
            c.setExecutor(cmd);
            c.setTabCompleter(cmd);
        });

        registerCommand("war", c -> c.setExecutor(new WarCommand(clans, warService)));

        SagaStore sagaStore = new SagaStore(this);
        registerCommand("saga", c -> c.setExecutor(new SagaCommand(sagaStore)));

        registerCommand("priere", c ->
                c.setExecutor(new PriereCommand(economy, sacrificeMap, priereDeathCauseMap, blessingManager))
        );

        registerCommand("mercenary", c -> {
            MercenaryCommand cmd = new MercenaryCommand(mercService);
            c.setExecutor(cmd);
            c.setTabCompleter(cmd);
        });

        // Listeners principaux
        Bukkit.getPluginManager().registerEvents(new BlessingListener(blessingManager), this);
        Bukkit.getPluginManager().registerEvents(new SacrificeListener(sacrificeMap), this);
        Bukkit.getPluginManager().registerEvents(new PriereDeathListener(priereDeathCauseMap), this);

        invSync = new InvSyncManager(this);
        if (invSync.isEnabled()) {
            Bukkit.getPluginManager().registerEvents(new InvSyncListener(invSync), this);
        }
    }

    private void registerCommand(String name, Consumer<PluginCommand> consumer) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Commande /" + name + " manquante dans plugin.yml");
            return;
        }
        consumer.accept(cmd);
    }

    // =====================================================
    // AFK (FIXED CONFIG PATH)
    // =====================================================

    private void setupAfk() {
        long autoAfk = getConfig().getLong("afk.auto-afk", 300L);
        long autoAfkKick = getConfig().getLong("afk.auto-afk-kick", 1200L);

        boolean cancelMove = getConfig().getBoolean("afk.cancel-on-move", true);
        boolean cancelChat = getConfig().getBoolean("afk.cancel-on-chat", true);
        boolean cancelInteract = getConfig().getBoolean("afk.cancel-on-interact", true);

        boolean broadcast = getConfig().getBoolean("afk.messages.broadcast", true);

        String suffix = getConfig().getString("afk.suffix", "&7[AFK]");
        String nowMsg = getConfig().getString("afk.messages.now-afk", "&e{PLAYER} &7est AFK");
        String noLonger = getConfig().getString("afk.messages.no-longer-afk", "&e{PLAYER} &7n'est plus AFK");
        String kickMsg = getConfig().getString("afk.kick-message", "&cKick AFK");

        boolean internal = essentialsHook == null;

        afkService = new AfkService(
                this,
                internal ? autoAfk : -1,
                internal ? autoAfkKick : -1,
                kickMsg,
                true,
                broadcast,
                nowMsg,
                noLonger
        );

        if (internal) {
            Bukkit.getPluginManager().registerEvents(
                    new PlayerActivityListener(afkService, cancelMove, cancelChat, cancelInteract),
                    this
            );
            afkService.start();
        }
    }

    // =====================================================
    // RAGNAR EVENT
    // =====================================================

    private void setupRagnarDeath() {
        ragnarEvent = new RagnarDeathEvent(this);

        Bukkit.getPluginManager().registerEvents(
                new RagnarDeathListener(ragnarEvent),
                this
        );

        registerCommand("ragnardeath",
                cmd -> cmd.setExecutor(new RagnarDeathCommand(ragnarEvent))
        );

        ragnarAuto = new RagnarDeathAutoStart(this, ragnarEvent);
        ragnarAuto.start();
    }

    // =====================================================
    // UPDATER
    // =====================================================

    private void setupUpdater() {
        GitHubUpdater updater = new GitHubUpdater(this);

        if (getConfig().getBoolean("updater.check-on-start", true)) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> updater.checkAndDownloadIfNeeded(null));
        }
    }

    // =====================================================
    // ASYNC DATA
    // =====================================================

    private void loadAsyncData() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                clans.loadAll();
            } catch (Exception e) {
                getLogger().severe("Load error: " + e.getMessage());
            }
        });
    }

    // =====================================================
    // ECONOMY
    // =====================================================

    private Economy setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return null;
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    // =====================================================
    // SHUTDOWN
    // =====================================================

    @Override
    public void onDisable() {
        if (tipsService != null) tipsService.stop();
        if (afkService != null) afkService.stop();
        if (ragnarAuto != null) ragnarAuto.stop();
        if (ragnarEvent != null && ragnarEvent.isRunning()) ragnarEvent.stop();
        if (invSync != null) invSync.flush();
        if (clans != null) {
            try {
                clans.saveAll();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // =====================================================
    // GETTERS
    // =====================================================

    public static Deepstone getInstance() {
        return instance;
    }

    public ClanService getClans() { return clans; }
    public WarService getWarService() { return warService; }
    public GloryService getGloryService() { return gloryService; }
}