package fr.deepstonestudio.deepstone.Manager;

import fr.deepstonestudio.deepstone.Deepstone;
import fr.deepstonestudio.deepstone.model.inv.InvSnapshot;
import fr.deepstonestudio.deepstone.model.inv.InventoryGroup;
import fr.deepstonestudio.deepstone.storage.YamlInvStore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InvSyncManager {

    private JavaPlugin plugin = Deepstone.getInstance();
    private final YamlInvStore store;

    private final boolean enabled;
    private final boolean syncArmor;
    private final boolean syncOffhand;
    private final boolean syncEnder;
    private final boolean syncXp;
    private final boolean syncHealthFood;

    private final int lockTicks;

    private final Map<UUID, Long> swapLockUntilTick = new ConcurrentHashMap<>();



    public InvSyncManager(JavaPlugin plugin) {


        this.plugin = plugin;
        this.store = new YamlInvStore(plugin);

        this.enabled = plugin.getConfig().getBoolean("inv-sync.enabled", true);
        this.syncArmor = plugin.getConfig().getBoolean("inv-sync.sync-armor", true);
        this.syncOffhand = plugin.getConfig().getBoolean("inv-sync.sync-offhand", true);
        this.syncEnder = plugin.getConfig().getBoolean("inv-sync.sync-enderchest", false);
        this.syncXp = plugin.getConfig().getBoolean("inv-sync.sync-xp", true);
        this.syncHealthFood = plugin.getConfig().getBoolean("inv-sync.sync-health-food", false);

        this.lockTicks = Math.max(2, plugin.getConfig().getInt("inv-sync.swap-lock-ticks", 8));
        if (plugin.getConfig().getBoolean("inv-sync.auto-save.enabled", true)) {
            int minutes = plugin.getConfig().getInt("inv-sync.auto-save.interval-minutes", 5);

            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                flush();
                plugin.getLogger().info("[InvSync] Auto-save exécuté.");
            }, 20L * 60 * minutes, 20L * 60 * minutes);
        }

    }

    public boolean isEnabled() {
        return enabled;
    }

    public void flush() {
        store.saveFile();
    }

    public boolean isLocked(Player p) {
        long now = Bukkit.getCurrentTick();
        Long until = swapLockUntilTick.get(p.getUniqueId());
        return until != null && until > now;
    }

    public void lock(Player p) {
        long now = Bukkit.getCurrentTick();
        swapLockUntilTick.put(p.getUniqueId(), now + lockTicks);
    }

    public void saveCurrent(Player p) {
        if (!enabled) return;
        debug("Save inventaire " + p.getName());
        InventoryGroup g = InventoryGroup.from(p.getGameMode());
        InvSnapshot snap = InvSnapshot.capture(p, syncArmor, syncOffhand, syncEnder, syncXp, syncHealthFood);

        store.save(p.getUniqueId(), g, snap,
                syncArmor, syncOffhand, syncEnder, syncXp, syncHealthFood);
    }

    public void loadFor(Player p, GameMode targetMode) {
        if (!enabled) return;

        InventoryGroup targetGroup = InventoryGroup.from(targetMode);
        UUID uuid = p.getUniqueId();

        InvSnapshot loaded = store.load(uuid, targetGroup,
                syncArmor, syncOffhand, syncEnder, syncXp, syncHealthFood);

        // ✅ Si aucune sauvegarde -> NE RIEN FAIRE
        if (loaded == null) {
            return;
        }

        loaded.apply(p, syncArmor, syncOffhand, syncEnder, syncXp, syncHealthFood);

        try {
            loaded.apply(p, syncArmor, syncOffhand, syncEnder, syncXp, syncHealthFood);
        } catch (Exception ex) {
            plugin.getLogger().severe("Erreur load inventaire pour " + p.getName());
            ex.printStackTrace();
        }

        debug("Load inventaire " + p.getName() + " group=" + targetGroup);
    }


    private final boolean debug = plugin.getConfig().getBoolean("inv-sync.debug", false);
    private void debug(String msg) {
        if (debug) {
            plugin.getLogger().info("[InvSync DEBUG] " + msg);
        }
    }



    /**
     * Swap SAFE:
     * 1) Lock
     * 2) Save ancien mode
     * 3) Après 1 tick → load nouveau mode
     * Aucun clear manuel.
     */
    public void swap(Player p, GameMode newMode) {
        if (!enabled) return;
        debug("Swap " + p.getName() + " -> " + newMode);
        lock(p);
        p.closeInventory();

        // Sauvegarde inventaire actuel
        saveCurrent(p);

        // Charge nouvel inventaire 1 tick après
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;

            loadFor(p, newMode);
        }, 1L);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}