package fr.deepstonestudio.deepstone.api.events;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AelleEvents {

    private LivingEntity aelle;
    private final JavaPlugin plugin;

    public AelleEvents(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // 🔥 Spawn Ælle + narration des frères + Grande Armée
    public void spawnAelle(Location loc) {

        World world = loc.getWorld();

        // Spawn Ælle
        aelle = world.spawn(loc, WitherSkeleton.class);
        aelle.setCustomName("§4Ælle, Roi de Northumbrie");
        aelle.setCustomNameVisible(true);
        aelle.setGlowing(true);

        aelle.getAttribute(Attribute.MAX_HEALTH).setBaseValue(200);
        aelle.setHealth(200);
        aelle.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(12);

        // Sons et particules de spawn
        world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 2f, 1f);
        world.spawnParticle(Particle.FLAME, loc, 150, 1,1,1,0.1);
        world.spawnParticle(Particle.SMOKE, loc, 100, 1,1,1,0.05);

        // Texte dramatique initial
        for (Player p : world.getPlayers()) {
            p.sendTitle("§0La Colère des Ragnarson", "§cÆlle doit payer...", 10, 60, 20);
            p.playSound(p.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 2f, 1f);
        }

        // Narration progressive des frères (Ivar, Ubbe, Hvitserk, Sigurd, Bjorn + Grande Armée)
        new BukkitRunnable() {

            int step = 0;

            @Override
            public void run() {
                step++;

                for (Player p : world.getPlayers()) {
                    switch (step) {
                        case 1 -> p.sendMessage("§7Ivar : Notre père a été trahi… il faut venger Ragnar !");
                        case 2 -> p.sendMessage("§7Ubbe : Ælle ne doit pas rester en vie après ce qu'il a fait !");
                        case 3 -> p.sendMessage("§7Hvitserk : Que le sang coule et que notre vengeance soit accomplie !");
                        case 4 -> p.sendMessage("§7Sigurd : Ensemble, nous ferons tomber le tyran !");
                        case 5 -> p.sendMessage("§7Bjorn : La guerre des Ragnarson commence maintenant !");
                        case 6 -> {
                            p.sendMessage("§6La Grande Armée des frères Ragnarson se tient prête !");
                            p.sendMessage("§6Les guerriers de tous les clans Vikings marchent derrière eux !");
                        }
                    }
                }

                // Terminer la narration après le dernier message
                if (step >= 6) cancel();
            }

        }.runTaskTimer(plugin, 40L, 60L); // 2 secondes entre chaque message
    }

    // ✅ Getter pour savoir si Ælle est vivant
    public LivingEntity getAelle() {
        return aelle;
    }
}