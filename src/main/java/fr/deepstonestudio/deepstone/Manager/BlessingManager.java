package fr.deepstonestudio.deepstone.Manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BlessingManager {

    public static final long DAY_MS = 24L * 60L * 60L * 1000L; // 24h

    private final JavaPlugin plugin;

    private final File file;
    private YamlConfiguration yaml;

    // UUID -> BlessingData
    private final Map<UUID, BlessingData> blessings = new HashMap<>();

    private BukkitTask upkeepTask;

    public BlessingManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "blessings.yml");
        reload();
        startUpkeepTask();
    }

    /* ================================================== */
    /*                     DATA TYPES                     */
    /* ================================================== */

    public static class BlessingData {
        public long expiresAtMs;
        public List<PotionSpec> effects;

        public BlessingData(long expiresAtMs, List<PotionSpec> effects) {
            this.expiresAtMs = expiresAtMs;
            this.effects = effects;
        }
    }

    public static class PotionSpec {
        public String type;
        public int amplifier;
        public boolean ambient;
        public boolean particles;
        public boolean icon;

        public PotionSpec(String type, int amplifier, boolean ambient, boolean particles, boolean icon) {
            this.type = type;
            this.amplifier = amplifier;
            this.ambient = ambient;
            this.particles = particles;
            this.icon = icon;
        }
    }

    /* ================================================== */
    /*                    PERSISTENCE                     */
    /* ================================================== */

    public void reload() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer blessings.yml: " + e.getMessage());
            }
        }

        yaml = YamlConfiguration.loadConfiguration(file);
        blessings.clear();

        if (!yaml.isConfigurationSection("blessings")) return;

        for (String key : Objects.requireNonNull(yaml.getConfigurationSection("blessings")).getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long expiresAt = yaml.getLong("blessings." + key + ".expiresAtMs", 0L);

                List<Map<?, ?>> list = yaml.getMapList("blessings." + key + ".effects");
                List<PotionSpec> specs = new ArrayList<>();

                for (Map<?, ?> m : list) {
                    String type = String.valueOf(m.get("type"));

                    int amp = toInt(m.get("amplifier"), 0);
                    boolean ambient = toBool(m.get("ambient"), false);
                    boolean particles = toBool(m.get("particles"), true);
                    boolean icon = toBool(m.get("icon"), true);

                    specs.add(new PotionSpec(type, amp, ambient, particles, icon));
                }

                if (expiresAt > System.currentTimeMillis() && !specs.isEmpty()) {
                    blessings.put(uuid, new BlessingData(expiresAt, specs));
                }

            } catch (Exception ignored) {
                // Ignore entrée corrompue
            }
        }

        cleanupExpired(true);
    }

    public void save() {
        yaml.set("blessings", null);

        for (Map.Entry<UUID, BlessingData> entry : blessings.entrySet()) {
            String path = "blessings." + entry.getKey();
            BlessingData data = entry.getValue();

            yaml.set(path + ".expiresAtMs", data.expiresAtMs);

            List<Map<String, Object>> list = new ArrayList<>();
            for (PotionSpec spec : data.effects) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("type", spec.type);
                m.put("amplifier", spec.amplifier);
                m.put("ambient", spec.ambient);
                m.put("particles", spec.particles);
                m.put("icon", spec.icon);
                list.add(m);
            }
            yaml.set(path + ".effects", list);
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder blessings.yml: " + e.getMessage());
        }
    }

    /* ================================================== */
    /*                      LOGIC                         */
    /* ================================================== */

    public void setBlessing(Player player, List<PotionEffect> effects, long durationMs) {
        long expiresAt = System.currentTimeMillis() + durationMs;

        List<PotionSpec> specs = new ArrayList<>();
        for (PotionEffect e : effects) {
            PotionEffectType type = e.getType();
            if (type == null) continue;

            specs.add(new PotionSpec(
                    type.getName(),
                    e.getAmplifier(),
                    e.isAmbient(),
                    e.hasParticles(),
                    e.hasIcon()
            ));
        }

        blessings.put(player.getUniqueId(), new BlessingData(expiresAt, specs));
        save();

        applyBlessingNow(player);
    }

    /**
     * ✅ Temps restant en ms (0 si pas de bénédiction ou expirée)
     */
    public long getRemainingMs(UUID uuid) {
        BlessingData data = blessings.get(uuid);
        if (data == null) return 0L;
        long remaining = data.expiresAtMs - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    /**
     * ✅ Expiration exacte (null si pas de bénédiction)
     */
    public Long getExpiresAt(UUID uuid) {
        BlessingData data = blessings.get(uuid);
        return data == null ? null : data.expiresAtMs;
    }

    /**
     * ✅ Retire la bénédiction d’un joueur (return true si une bénédiction existait)
     */
    public boolean clearBlessing(UUID uuid) {
        BlessingData removed = blessings.remove(uuid);
        if (removed == null) return false;

        // Retire les effets de potion immédiatement si le joueur est en ligne
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            for (PotionSpec spec : removed.effects) {
                PotionEffectType type = PotionEffectType.getByName(spec.type);
                if (type != null) p.removePotionEffect(type);
            }
        }

        save();
        return true;
    }

    public void applyBlessingNow(Player player) {
        BlessingData data = blessings.get(player.getUniqueId());
        if (data == null) return;

        long remainingMs = data.expiresAtMs - System.currentTimeMillis();
        if (remainingMs <= 0) {
            blessings.remove(player.getUniqueId());
            save();
            return;
        }

        // ⚠️ Sécurité: (ms -> ticks), cap à int max, minimum 20 ticks
        long ticksLong = (remainingMs / 1000L) * 20L;
        int remainingTicks = (int) Math.max(20L, Math.min((long) Integer.MAX_VALUE, ticksLong));

        for (PotionSpec spec : data.effects) {
            PotionEffectType type = PotionEffectType.getByName(spec.type);
            if (type == null) continue;

            PotionEffect effect = new PotionEffect(
                    type,
                    remainingTicks,
                    spec.amplifier,
                    spec.ambient,
                    spec.particles,
                    spec.icon
            );

            player.addPotionEffect(effect, true);
        }
    }

    public void applyBlessingSoon(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyBlessingNow(player), 2L);
    }

    public void cleanupExpired(boolean saveIfChanged) {
        long now = System.currentTimeMillis();
        boolean changed = blessings.entrySet().removeIf(e -> e.getValue() == null || e.getValue().expiresAtMs <= now);
        if (changed && saveIfChanged) save();
    }

    public boolean hasBlessing(UUID uuid) {
        BlessingData data = blessings.get(uuid);
        return data != null && data.expiresAtMs > System.currentTimeMillis();
    }

    /* ================================================== */
    /*                AUTO UPKEEP (OPTION)                */
    /* ================================================== */

    /**
     * Réapplique aux joueurs connectés + nettoie les expirées.
     * Utile après mort / relog / certains resets d'effets.
     */
    private void startUpkeepTask() {
        if (upkeepTask != null) upkeepTask.cancel();

        upkeepTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            cleanupExpired(false);

            for (UUID uuid : new ArrayList<>(blessings.keySet())) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    applyBlessingNow(p);
                }
            }
        }, 40L, 40L); // toutes les 2 secondes
    }

    /**
     * À appeler dans onDisable()
     */
    public void shutdown() {
        if (upkeepTask != null) upkeepTask.cancel();
        save();
    }

    /* ================================================== */
    /*                  SAFE CONVERSION                   */
    /* ================================================== */

    private int toInt(Object obj, int def) {
        if (obj == null) return def;
        if (obj instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (Exception ignored) {
            return def;
        }
    }

    private boolean toBool(Object obj, boolean def) {
        if (obj == null) return def;
        if (obj instanceof Boolean b) return b;
        String s = String.valueOf(obj).trim().toLowerCase(Locale.ROOT);
        if (s.equals("true") || s.equals("yes") || s.equals("1")) return true;
        if (s.equals("false") || s.equals("no") || s.equals("0")) return false;
        return def;
    }
}