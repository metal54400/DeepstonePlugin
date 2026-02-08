package fr.deepstonestudio.deepstone;

import fr.deepstonestudio.deepstone.Commands.ClearLagCommand;
import fr.deepstonestudio.deepstone.Listener.CommandBlockListener;
import fr.deepstonestudio.deepstone.Listener.PvpListener;
import fr.deepstonestudio.deepstone.Listener.TeleportListener;
import fr.deepstonestudio.deepstone.Manager.ProtectionManager;
import fr.deepstonestudio.deepstone.util.ClearLoop;
import fr.deepstonestudio.deepstone.util.ClearService;
import org.bukkit.plugin.java.JavaPlugin;

public final class Deepstone extends JavaPlugin {

    private ClearService clearService;
    private ClearLoop clearLoop;
    private ProtectionManager protectionManager;

    @Override
    public void onEnable() {
        this.protectionManager = new ProtectionManager(this, 15 * 60); // 15 minutes en secondes



        protectionManager.startCleanupTask();

        getServer().getPluginManager().registerEvents(
                new CommandBlockListener(), this
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

        getLogger().info("DeepstoneClearLagg activé !");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (clearLoop != null) clearLoop.stop();
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
}
