package fr.deepstonestudio.deepstone.api.events;

import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class AelleDeathEvent implements Listener {

    private final AelleEvents aelleEvents;

    public AelleDeathEvent(AelleEvents aelleEvents) {
        this.aelleEvents = aelleEvents;
    }

    @EventHandler
    public void onAelleDeath(EntityDeathEvent event) {

        LivingEntity aelle = aelleEvents.getAelle();
        if (aelle == null) return;
        if (!event.getEntity().equals(aelle)) return;

        Location loc = aelle.getLocation();
        World world = loc.getWorld();

        // 🩸 Message global
        Bukkit.broadcastMessage("§4☠ Ælle est tombé !");
        Bukkit.broadcastMessage("§6La vengeance des Ragnarson est accomplie !");

        // Réaction des frères et Grande Armée
        for (Player p : world.getPlayers()) {
            p.sendMessage("§7Ivar : Justice est faite !");
            p.sendMessage("§7Ubbe : Le tyran est tombé !");
            p.sendMessage("§7Hvitserk : Les fils de Ragnar sont victorieux !");
            p.sendMessage("§7Sigurd : Northumbrie est à nous !");
            p.sendMessage("§7Bjorn : Que la gloire des Ragnarson se répande !");
            p.sendMessage("§6La Grande Armée célèbre la victoire !");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 2f, 0.8f);
            p.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 5);
        }

        // 💀 Drop spécial : Couronne
        ItemStack crown = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta meta = crown.getItemMeta();
        meta.setDisplayName("§6Couronne du Roi Ælle");
        meta.setLore(Collections.singletonList("§7Symbole de la chute de Northumbrie"));
        crown.setItemMeta(meta);

        world.dropItemNaturally(loc, crown);
    }
}