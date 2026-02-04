package fr.deepstonestudio.deepstone;

import fr.deepstonestudio.deepstone.Commands.ClearLagCommand;
import fr.deepstonestudio.deepstone.Listener.CommandBlockListener;
import fr.deepstonestudio.deepstone.util.ClearLoop;
import fr.deepstonestudio.deepstone.util.ClearService;
import org.bukkit.plugin.java.JavaPlugin;

public final class Deepstone extends JavaPlugin {

    private ClearService clearService;
    private ClearLoop clearLoop;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(
                new CommandBlockListener(), this
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
}
