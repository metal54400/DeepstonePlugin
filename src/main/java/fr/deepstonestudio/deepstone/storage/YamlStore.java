package fr.deepstonestudio.deepstone.storage;

import fr.deepstonestudio.deepstone.model.Clan;
import fr.deepstonestudio.deepstone.model.Role;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class YamlStore {
    private final JavaPlugin plugin;
    private final File clansFile;
    private final File playersFile;

    public YamlStore(JavaPlugin plugin) {
        this.plugin = plugin;
        var folder = plugin.getDataFolder();
        if (!folder.exists()) folder.mkdirs();
        this.clansFile = new File(folder, "clans.yml");
        this.playersFile = new File(folder, "players.yml");
    }

    public Map<String, Clan> loadClans() throws IOException {
        if (!clansFile.exists()) clansFile.createNewFile();
        var cfg = YamlConfiguration.loadConfiguration(clansFile);

        Map<String, Clan> out = new HashMap<>();
        var root = cfg.getConfigurationSection("clans");
        if (root == null) return out;

        for (String key : root.getKeys(false)) {
            var sec = root.getConfigurationSection(key);
            if (sec == null) continue;

            Clan c = new Clan(sec.getString("display", key));
            c.setDisplayName(sec.getString("display", key));

            String king = sec.getString("king", null);
            String jarl = sec.getString("jarl", null);
            if (king != null) c.setKing(UUID.fromString(king));
            if (jarl != null) c.setJarl(UUID.fromString(jarl));

            var memSec = sec.getConfigurationSection("members");
            if (memSec != null) {
                for (String uuidStr : memSec.getKeys(false)) {
                    UUID u = UUID.fromString(uuidStr);
                    Role r = Role.from(memSec.getString(uuidStr, "PEASANT"));
                    c.addMember(u, r);
                }
            }
            out.put(c.getName(), c);
        }
        return out;
    }

    public void saveClans(Map<String, Clan> clans) throws IOException {
        var cfg = new YamlConfiguration();
        for (Clan c : clans.values()) {
            String path = "clans." + c.getName();
            cfg.set(path + ".display", c.getDisplayName());
            cfg.set(path + ".king", c.getKing() == null ? null : c.getKing().toString());
            cfg.set(path + ".jarl", c.getJarl() == null ? null : c.getJarl().toString());

            String memPath = path + ".members";
            for (var e : c.getMembers().entrySet()) {
                cfg.set(memPath + "." + e.getKey().toString(), e.getValue().name());
            }
        }
        cfg.save(clansFile);
    }

    public Map<UUID, String> loadPlayerClanIndex() throws IOException {
        if (!playersFile.exists()) playersFile.createNewFile();
        var cfg = YamlConfiguration.loadConfiguration(playersFile);

        Map<UUID, String> out = new HashMap<>();
        var sec = cfg.getConfigurationSection("players");
        if (sec == null) return out;

        for (String uuidStr : sec.getKeys(false)) {
            String clan = sec.getString(uuidStr, null);
            if (clan != null) out.put(UUID.fromString(uuidStr), clan);
        }
        return out;
    }

    public void savePlayerClanIndex(Map<UUID, String> index) throws IOException {
        var cfg = new YamlConfiguration();
        for (var e : index.entrySet()) {
            cfg.set("players." + e.getKey().toString(), e.getValue());
        }
        cfg.save(playersFile);
    }
}
