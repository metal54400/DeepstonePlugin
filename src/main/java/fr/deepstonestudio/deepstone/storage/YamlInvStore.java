package fr.deepstonestudio.deepstone.storage;

import fr.deepstonestudio.deepstone.model.inv.InvSnapshot;
import fr.deepstonestudio.deepstone.model.inv.InventoryGroup;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class YamlInvStore {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public YamlInvStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "inventories.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void saveFile() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder inventories.yml: " + e.getMessage());
        }
    }

    private ConfigurationSection section(UUID uuid, InventoryGroup group) {
        String path = "players." + uuid + "." + group.name().toLowerCase();
        ConfigurationSection sec = yaml.getConfigurationSection(path);
        if (sec == null) sec = yaml.createSection(path);
        return sec;
    }

    public InvSnapshot load(UUID uuid, InventoryGroup group,
                            boolean armor, boolean offhand, boolean ender, boolean xp, boolean healthFood) {
        ConfigurationSection sec = yaml.getConfigurationSection("players." + uuid + "." + group.name().toLowerCase());
        if (sec == null) return null;
        return InvSnapshot.load(sec, armor, offhand, ender, xp, healthFood);
    }

    public void save(UUID uuid, InventoryGroup group, InvSnapshot snapshot,
                     boolean armor, boolean offhand, boolean ender, boolean xp, boolean healthFood) {
        ConfigurationSection sec = section(uuid, group);
        snapshot.save(sec, armor, offhand, ender, xp, healthFood);
    }
}