package fr.deepstonestudio.deepstone.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class InvBackupManager {

    private final JavaPlugin plugin;
    private final File backupFolder;
    private final int maxBackups;
    private final boolean enabled;

    public InvBackupManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("inv-sync.backup.enabled", true);
        this.maxBackups = plugin.getConfig().getInt("inv-sync.backup.max-per-player", 5);

        this.backupFolder = new File(plugin.getDataFolder(), "inventories-backup");
        if (!backupFolder.exists()) backupFolder.mkdirs();
    }

    public void backup(UUID uuid, YamlConfiguration sourceYaml) {
        if (!enabled) return;

        try {
            File playerFolder = new File(backupFolder, uuid.toString());
            if (!playerFolder.exists()) playerFolder.mkdirs();

            String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File file = new File(playerFolder, time + ".yml");

            sourceYaml.save(file);

            cleanupOldBackups(playerFolder);

        } catch (IOException e) {
            plugin.getLogger().severe("Backup inventaire impossible pour " + uuid);
            e.printStackTrace();
        }
    }

    private void cleanupOldBackups(File folder) {
        File[] files = folder.listFiles();
        if (files == null || files.length <= maxBackups) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        for (int i = 0; i < files.length - maxBackups; i++) {
            files[i].delete();
        }
    }
}