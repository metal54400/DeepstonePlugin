package fr.deepstonestudio.deepstone.Listener;


import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

public class MobHeadDropListener implements Listener {

    private final JavaPlugin plugin;

    private boolean enabled;
    private int chancePercent;
    private boolean requireSneak;
    private boolean requireAxe;

    public MobHeadDropListener(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        FileConfiguration c = plugin.getConfig();
        this.enabled = c.getBoolean("mob-heads.enabled", true);
        this.chancePercent = clamp(c.getInt("mob-heads.chance-percent", 5), 0, 100);
        this.requireSneak = c.getBoolean("mob-heads.require-sneak", false);
        this.requireAxe = c.getBoolean("mob-heads.require-axe", false);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!enabled) return;

        LivingEntity mob = e.getEntity();
        Player killer = mob.getKiller();
        if (killer == null) return;

        if (requireSneak && !killer.isSneaking()) return;
        if (requireAxe && !isAxe(killer.getInventory().getItemInMainHand())) return;

        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll >= chancePercent) return;

        ItemStack head = headFor(mob);
        if (head == null) return;

        e.getDrops().add(head);
    }

    private ItemStack headFor(LivingEntity mob) {
        // Têtes vanilla disponibles
        if (mob instanceof Creeper) return new ItemStack(Material.CREEPER_HEAD);
        if (mob instanceof Skeleton) return new ItemStack(Material.SKELETON_SKULL);
        if (mob instanceof WitherSkeleton) return new ItemStack(Material.WITHER_SKELETON_SKULL);
        if (mob instanceof Zombie) return new ItemStack(Material.ZOMBIE_HEAD);
        if (mob instanceof Enderman) return new ItemStack(Material.DRAGON_HEAD); // (pas de tête enderman vanilla)
        // Ajoute d'autres mobs si tu veux

        return null; // pas de tête vanilla -> pas de drop
    }

    private boolean isAxe(ItemStack it) {
        if (it == null) return false;
        return switch (it.getType()) {
            case WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE -> true;
            default -> false;
        };
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}