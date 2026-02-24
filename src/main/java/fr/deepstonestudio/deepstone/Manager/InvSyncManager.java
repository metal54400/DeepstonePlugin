package fr.deepstonestudio.deepstone.Manager;

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

    private final JavaPlugin plugin;
    private final YamlInvStore store;

    private final boolean enabled;
    private final boolean syncArmor;
    private final boolean syncOffhand;
    private final boolean syncEnder;
    private final boolean syncXp;
    private final boolean syncHealthFood;

    private final int lockTicks;

    // Anti-duplication: lock during swap
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
        InventoryGroup g = InventoryGroup.from(p.getGameMode());
        InvSnapshot snap = InvSnapshot.capture(p, syncArmor, syncOffhand, syncEnder, syncXp, syncHealthFood);
        store.save(p.getUniqueId(), g, snap, syncArmor, syncOffhand, syncEnder, syncXp, syncHealthFood);
    }

    public void loadFor(Player p, GameMode targetMode) {
        if (!enabled) return;

        InventoryGroup targetGroup = InventoryGroup.from(targetMode);
        UUID uuid = p.getUniqueId();

        InvSnapshot loaded = store.load(uuid, targetGroup, syncArmor, syncOffhand, syncEnder, syncXp, syncHealthFood);

        // Si aucune data encore -> snapshot vide
        if (loaded == null) {
            loaded = new InvSnapshot();
            loaded.storageContents = new org.bukkit.inventory.ItemStack[36];
            if (syncArmor) loaded.armorContents = new org.bukkit.inventory.ItemStack[4];
            if (syncEnder) loaded.enderContents = new org.bukkit.inventory.ItemStack[27];
            if (syncOffhand) loaded.offhandItem = null;
        }

        loaded.apply(p, syncArmor, syncOffhand, syncEnder, syncXp, syncHealthFood);
    }

    /**
     * ✅ Swap SAFE: save old -> lock -> close inv -> load new AFTER 1 tick
     * ❌ Aucun clear() ici => survie jamais vidée par le plugin
     */
    public void swap(Player p, GameMode newMode) {
        if (!enabled) return;

        lock(p);
        p.closeInventory();

        // Sauvegarde l'état du mode actuel (SURV/ADV ou CREA/SPEC)
        saveCurrent(p);

        // Charge l'inventaire du nouveau groupe après 1 tick
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            loadFor(p, newMode);
        }, 1L);
    }
}