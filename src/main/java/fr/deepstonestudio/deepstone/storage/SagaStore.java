package fr.deepstonestudio.deepstone.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class SagaStore {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public SagaStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "saga_progress.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void saveFile() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder saga_progress.yml: " + e.getMessage());
        }
    }

    public int getChapter(UUID uuid) {
        return yaml.getInt("players." + uuid + ".chapter", 0);
    }

    public void setChapter(UUID uuid, int chapter) {
        yaml.set("players." + uuid + ".chapter", Math.max(0, chapter));
    }
}