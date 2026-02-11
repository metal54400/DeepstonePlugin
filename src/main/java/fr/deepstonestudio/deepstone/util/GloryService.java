package fr.deepstonestudio.deepstone.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GloryService {

    private final Map<UUID, Integer> playerGlory = new HashMap<>();
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration config;

    public GloryService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "glory.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        if (!config.contains("players")) return;

        for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            int glory = config.getInt("players." + uuidStr);
            playerGlory.put(uuid, glory);
        }
    }

    public void save() {
        for (var entry : playerGlory.entrySet()) {
            config.set("players." + entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int get(UUID uuid) {
        return playerGlory.getOrDefault(uuid, 0);
    }

    public void add(UUID uuid, int amount) {
        playerGlory.put(uuid, get(uuid) + amount);
    }

    public void remove(UUID uuid, int amount) {
        playerGlory.put(uuid, Math.max(0, get(uuid) - amount));
    }
}