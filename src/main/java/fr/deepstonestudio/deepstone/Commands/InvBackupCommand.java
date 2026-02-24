package fr.deepstonestudio.deepstone.Commands;

import fr.deepstonestudio.deepstone.Manager.InvSyncManager;
import fr.deepstonestudio.deepstone.storage.YamlInvStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class InvBackupCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final InvSyncManager manager;

    public InvBackupCommand(JavaPlugin plugin, InvSyncManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("deepstone.invbackup.restore")) {
            sender.sendMessage("§cPermission insuffisante.");
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("restore")) {
            sender.sendMessage("§cUsage: /invbackup restore <joueur> <date>");
            return true;
        }

        String playerName = args[1];
        String date = args[2];

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();

        File backupFile = new File(plugin.getDataFolder(),
                "inventories-backup/" + uuid + "/" + date + ".yml");

        if (!backupFile.exists()) {
            sender.sendMessage("§cBackup introuvable.");
            return true;
        }

        try {
            YamlConfiguration backupYaml = YamlConfiguration.loadConfiguration(backupFile);

            // On écrase inventories.yml avec le backup
            File mainFile = new File(plugin.getDataFolder(), "inventories.yml");
            backupYaml.save(mainFile);

            sender.sendMessage("§aBackup restauré pour " + playerName + ".");

            // Si le joueur est online → reload inventaire
            if (target.isOnline()) {
                Player online = target.getPlayer();
                manager.loadFor(online, online.getGameMode());
                sender.sendMessage("§aInventaire rechargé en jeu.");
            }

        } catch (Exception e) {
            sender.sendMessage("§cErreur restauration.");
            e.printStackTrace();
        }

        return true;
    }
}