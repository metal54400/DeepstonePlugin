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

            // ✅ 1) members d'abord (sinon setKing/setJarl throw)
            var memSec = sec.getConfigurationSection("members");
            if (memSec != null) {
                for (String uuidStr : memSec.getKeys(false)) {
                    UUID u = UUID.fromString(uuidStr);
                    Role r = Role.from(memSec.getString(uuidStr, "PEASANT"));
                    c.addMember(u, r);
                }
            }
            String cw = sec.getString("capital.world", null);
            if (cw != null) {
                int cx = sec.getInt("capital.x");
                int cy = sec.getInt("capital.y");
                int cz = sec.getInt("capital.z");
                c.setCapitalRaw(new Clan.Capital(cw, cx, cy, cz));
            }

            // ✅ 2) diplomatie
            for (String a : sec.getStringList("allies")) {
                if (a != null && !a.isBlank()) c.getAllies().add(a.toLowerCase(Locale.ROOT));
            }
            for (String t : sec.getStringList("truces")) {
                if (t != null && !t.isBlank()) c.getTruces().add(t.toLowerCase(Locale.ROOT));
            }

            // ✅ 3) king/jarl ensuite
            String king = sec.getString("king", null);
            if (king != null) {
                UUID k = UUID.fromString(king);
                if (c.isMember(k)) c.setKing(k);
            }

            String jarl = sec.getString("jarl", null);
            if (jarl != null) {
                UUID j = UUID.fromString(jarl);
                if (c.isMember(j)) c.setJarl(j);
            }

            // Fallback si fichier ancien ou incohérent
            if (c.getKing() == null && !c.getMembers().isEmpty()) {
                UUID any = c.getMembers().keySet().iterator().next();
                c.addMember(any, Role.KING);
                c.setKing(any);
                c.setJarl(any);
            } else if (c.getJarl() == null && c.getKing() != null) {
                c.setJarl(c.getKing());
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

            // ✅ diplomatie
            cfg.set(path + ".allies", new ArrayList<>(c.getAllies()));
            cfg.set(path + ".truces", new ArrayList<>(c.getTruces()));

            // ✅ capitale
            if (c.getCapital() == null) {
                cfg.set(path + ".capital", null);
            } else {
                cfg.set(path + ".capital.world", c.getCapital().getWorld());
                cfg.set(path + ".capital.x", c.getCapital().getX());
                cfg.set(path + ".capital.y", c.getCapital().getY());
                cfg.set(path + ".capital.z", c.getCapital().getZ());
            }


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
    public Map<UUID, Integer> loadMercenaryGlory() throws IOException {
        if (!playersFile.exists()) playersFile.createNewFile();
        var cfg = YamlConfiguration.loadConfiguration(playersFile);

        Map<UUID, Integer> out = new HashMap<>();
        var sec = cfg.getConfigurationSection("mercenary_glory");
        if (sec == null) return out;

        for (String uuidStr : sec.getKeys(false)) {
            int v = sec.getInt(uuidStr, 0);
            out.put(UUID.fromString(uuidStr), Math.max(0, v));
        }
        return out;
    }

    public void saveMercenaryGlory(Map<UUID, Integer> glory) throws IOException {
        if (!playersFile.exists()) playersFile.createNewFile();
        var cfg = YamlConfiguration.loadConfiguration(playersFile);

        // réécrit la section
        cfg.set("mercenary_glory", null);
        for (var e : glory.entrySet()) {
            cfg.set("mercenary_glory." + e.getKey().toString(), Math.max(0, e.getValue()));
        }
        cfg.save(playersFile);
    }
}
