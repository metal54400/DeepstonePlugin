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

    // ========================= LOAD FILE =========================

    public void reload() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer inventories.yml: " + e.getMessage());
            }
        }

        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    // ========================= SAVE FILE =========================

    public void saveFile() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder inventories.yml: " + e.getMessage());
        }
    }

    // ========================= INTERNAL =========================

    private String path(UUID uuid, InventoryGroup group) {
        return "players." + uuid + "." + group.name().toLowerCase();
    }

    private ConfigurationSection getOrCreateSection(UUID uuid, InventoryGroup group) {
        String path = path(uuid, group);

        ConfigurationSection sec = yaml.getConfigurationSection(path);
        if (sec == null) {
            sec = yaml.createSection(path);
        }

        return sec;
    }

    // ========================= LOAD SNAPSHOT =========================

    public InvSnapshot load(UUID uuid,
                            InventoryGroup group,
                            boolean armor,
                            boolean offhand,
                            boolean ender,
                            boolean xp,
                            boolean healthFood) {

        String path = path(uuid, group);
        ConfigurationSection sec = yaml.getConfigurationSection(path);

        if (sec == null) {
            return null; // ✅ Ne jamais créer snapshot vide
        }

        try {
            return InvSnapshot.load(sec, armor, offhand, ender, xp, healthFood);
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur chargement inventaire pour " + uuid + " (" + group + ")");
            e.printStackTrace();
            return null;
        }
    }

    // ========================= SAVE SNAPSHOT =========================

    public void save(UUID uuid,
                     InventoryGroup group,
                     InvSnapshot snapshot,
                     boolean armor,
                     boolean offhand,
                     boolean ender,
                     boolean xp,
                     boolean healthFood) {

        if (snapshot == null) {
            plugin.getLogger().warning("Tentative de sauvegarde d'un snapshot null pour " + uuid);
            return;
        }

        try {
            ConfigurationSection sec = getOrCreateSection(uuid, group);

            snapshot.save(sec, armor, offhand, ender, xp, healthFood);

            // ✅ Sauvegarde immédiate disque (anti crash perte data)
            saveFile();

        } catch (Exception e) {
            plugin.getLogger().severe("Erreur sauvegarde inventaire pour " + uuid + " (" + group + ")");
            e.printStackTrace();
        }
    }
}