package fr.deepstonestudio.deepstone.storage;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class TipsStore {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public TipsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tips.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer tips.yml: " + e.getMessage());
            }
        }
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isTipsEnabled(UUID uuid) {
        // défaut: enabled (donc disabled=false)
        return !yaml.getBoolean("disabled." + uuid, false);
    }

    public void setTipsEnabled(UUID uuid, boolean enabled) {
        yaml.set("disabled." + uuid, enabled ? null : true);
        save();
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder tips.yml: " + e.getMessage());
        }
    }
}