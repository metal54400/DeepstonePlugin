package fr.deepstonestudio.deepstone.Listener;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class DecapitationListener implements Listener {

    private final JavaPlugin plugin;

    // Config
    private boolean enabled;
    private long markDurationMs;
    private long markCooldownMs;
    private int dropChancePercent;
    private boolean requireSneak;
    private boolean requireAxe;
    private boolean onlyVillagerTypes;

    // player -> cooldown clic
    private final Map<UUID, Long> clickCd = new HashMap<>();

    // clé PDC sur l'entité pour stocker l'armement + qui l'a armée
    private final NamespacedKey keyMarkedUntil;
    private final NamespacedKey keyMarkedBy;

    public DecapitationListener(JavaPlugin plugin) {
        this.plugin = plugin;

        this.keyMarkedUntil = new NamespacedKey(plugin, "decap_mark_until");
        this.keyMarkedBy = new NamespacedKey(plugin, "decap_mark_by");

        reloadFromConfig();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Recharge les options depuis config.yml */
    public void reloadFromConfig() {
        FileConfiguration c = plugin.getConfig();

        this.enabled = c.getBoolean("decapitation.enabled", true);

        long markDurationSeconds = c.getLong("decapitation.mark-duration-seconds", 10L);
        long markCooldownSeconds = c.getLong("decapitation.mark-cooldown-seconds", 2L);

        this.markDurationMs = Math.max(1L, markDurationSeconds) * 1000L;
        this.markCooldownMs = Math.max(0L, markCooldownSeconds) * 1000L;

        this.dropChancePercent = clamp(c.getInt("decapitation.drop-chance-percent", 15), 0, 100);

        this.requireSneak = c.getBoolean("decapitation.require-sneak", true);
        this.requireAxe = c.getBoolean("decapitation.require-axe", true);
        this.onlyVillagerTypes = c.getBoolean("decapitation.only-villager-types", true);
    }

    /* ========================= */
    /*      MARQUAGE AU CLIC     */
    /* ========================= */

    @EventHandler
    public void onRightClickEntity(PlayerInteractEntityEvent e) {
        if (!enabled) return;

        // Évite double event main-hand/off-hand
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player p = e.getPlayer();
        Entity target = e.getRightClicked();

        if (onlyVillagerTypes) {
            if (!(target instanceof Villager) && !(target instanceof ZombieVillager)) return;
        } else {
            if (!(target instanceof LivingEntity)) return;
        }

        if (requireAxe && !isAxe(p.getInventory().getItemInMainHand())) return;

        // Cooldown anti-spam clic
        long now = System.currentTimeMillis();
        Long cd = clickCd.get(p.getUniqueId());
        if (cd != null && cd > now) return;
        clickCd.put(p.getUniqueId(), now + markCooldownMs);

        // Marque l'entité
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        pdc.set(keyMarkedUntil, PersistentDataType.LONG, now + markDurationMs);
        pdc.set(keyMarkedBy, PersistentDataType.STRING, p.getUniqueId().toString());

        // Feedback
        p.playSound(p.getLocation(), Sound.ITEM_AXE_STRIP, 0.8f, 1.2f);
        p.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.2, 0), 10, 0.2, 0.2, 0.2, 0.01);
        p.sendMessage("§7[§e?§7] Cible marquée. §eAccroupis-toi§7 et tue-la à la hache pour tenter de récupérer sa tête.");
    }

    /* ========================= */
    /*         DROP À LA MORT    */
    /* ========================= */

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (!enabled) return;

        LivingEntity ent = e.getEntity();

        if (onlyVillagerTypes) {
            if (!(ent instanceof Villager) && !(ent instanceof ZombieVillager)) return;
        }

        Player killer = ent.getKiller();
        if (killer == null) return;

        if (requireSneak && !killer.isSneaking()) return;

        if (requireAxe && !isAxe(killer.getInventory().getItemInMainHand())) return;

        // Vérifie que l'entité était marquée et par ce joueur, et pas expirée
        PersistentDataContainer pdc = ent.getPersistentDataContainer();
        Long until = pdc.get(keyMarkedUntil, PersistentDataType.LONG);
        String by = pdc.get(keyMarkedBy, PersistentDataType.STRING);

        long now = System.currentTimeMillis();
        if (until == null || until < now) return;
        if (by == null || !by.equals(killer.getUniqueId().toString())) return;

        // Chance
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll >= dropChancePercent) return;

        // Drop la tête
        ItemStack head = makeHead(ent);
        if (head == null) return;

        e.getDrops().add(head);

        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.1f);
        killer.sendMessage("§7[§a✓§7] §aDécapitation réussie !§7 Tu as récupéré une tête.");
    }

    /* ========================= */
    /*           UTILS           */
    /* ========================= */

    private boolean isAxe(ItemStack it) {
        if (it == null) return false;
        Material m = it.getType();
        return m == Material.WOODEN_AXE
                || m == Material.STONE_AXE
                || m == Material.IRON_AXE
                || m == Material.GOLDEN_AXE
                || m == Material.DIAMOND_AXE
                || m == Material.NETHERITE_AXE;
    }

    private ItemStack makeHead(LivingEntity ent) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return null;

        if (ent instanceof Villager) {
            meta.setDisplayName("§fTête de Villageois");
        } else if (ent instanceof ZombieVillager) {
            meta.setDisplayName("§aTête de Zombie Villageois");
        } else {
            meta.setDisplayName("§fTête");
        }

        skull.setItemMeta(meta);
        return skull;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}