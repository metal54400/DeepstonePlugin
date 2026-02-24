package fr.deepstonestudio.deepstone.api.events;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class RagnarDeathEvent {

    private final JavaPlugin plugin;

    private boolean running = false;
    private UUID worldId = null;

    private int endTaskId = -1;
    private int tickTaskId = -1;

    public static final long DURATION_TICKS = 24000L;

    // PDC keys (pour marquer/éviter double buff + stocker multipliers)
    private final NamespacedKey KEY_BUFFED;
    private final NamespacedKey KEY_HP_MULT;
    private final NamespacedKey KEY_DMG_MULT;
    private final NamespacedKey KEY_SPD_MULT;

    // Narration random
    private static final List<String> NARRATIONS = List.of(
            "&8[&6Saga&8] &7Le vent tourne… &cRagnar approche de sa fin.",
            "&8[&6Saga&8] &7Les dieux observent. &7Même les rois saignent.",
            "&8[&6Saga&8] &7On dit que l’ambition de Ragnar était plus grande que la mer.",
            "&8[&6Saga&8] &7Ses ennemis sourient… &7Mais ils ne savent pas ce qui vient.",
            "&8[&6Saga&8] &7Un nom peut mourir… &6mais pas une légende.",
            "&8[&6Saga&8] &7La peur change de camp. &cLes créatures se déchaînent.",
            "&8[&6Saga&8] &e« Ne pleurez pas… réjouissez-vous. » &7— Ragnar",
            "&8[&6Saga&8] &7Les Ragnarson se lèveront. &cLa tempête est inévitable."
    );

    // Messages spéciaux “vague”
    private static final List<String> WAVES = List.of(
            "&8[&6EVENT&8] &cVague de rage ! &7Les monstres deviennent incontrôlables.",
            "&8[&6EVENT&8] &4Les serpents du destin s’éveillent…",
            "&8[&6EVENT&8] &cL’ombre de Ragnar plane… &7La nuit mord plus fort."
    );

    // Quand envoyer une narration (random interval)
    private long nextNarrationTick = 0;
    private long nextWaveTick = 0;

    public RagnarDeathEvent(JavaPlugin plugin) {
        this.plugin = plugin;
        this.KEY_BUFFED = new NamespacedKey(plugin, "ragnar_buffed");
        this.KEY_HP_MULT = new NamespacedKey(plugin, "ragnar_hp");
        this.KEY_DMG_MULT = new NamespacedKey(plugin, "ragnar_dmg");
        this.KEY_SPD_MULT = new NamespacedKey(plugin, "ragnar_spd");
    }

    public boolean isRunning() { return running; }

    public World getWorld() {
        return worldId == null ? null : Bukkit.getWorld(worldId);
    }

    public void start(World world) {
        if (running) return;

        running = true;
        worldId = world.getUID();

        // setup random timers
        nextNarrationTick = 40 + rand(200, 600); // 10s -> 30s après start
        nextWaveTick = 200 + rand(1200, 2400);   // 10s -> 2min après start

        // Start message
        broadcast(world, "&8[&6EVENT&8] &cLa Mort de Ragnar commence… &7(1 journée Minecraft)");
        titleNearSpawn(world, "&6⚔ La Mort de Ragnar", "&7La saga s’assombrit…", 10, 50, 10);
        soundNearSpawn(world, Sound.ITEM_GOAT_HORN_SOUND_0, 1f, 0.8f);

        // Main loop (1 tick)
        tickTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            long t = 0;

            @Override
            public void run() {
                if (!running) return;

                World w = getWorld();
                if (w == null) { stop(); return; }

                // Refresh buffs sur mobs existants (toutes les 10s)
                if (t % 200 == 0) refreshExistingMobs(w);

                // Narration random
                if (t >= nextNarrationTick) {
                    broadcast(w, pick(NARRATIONS));
                    // prochain msg dans 30s à 2min
                    nextNarrationTick = t + rand(600, 2400);
                }

                // Vague random (rare, + buff supplémentaire temporaire)
                if (t >= nextWaveTick) {
                    String waveMsg = pick(WAVES);
                    broadcast(w, waveMsg);

                    // petit effet ambiance
                    if (chance(35)) w.strikeLightningEffect(w.getSpawnLocation());
                    if (chance(50)) soundNearSpawn(w, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9f, 1f);

                    // Buff temporaire “rage” sur mobs hostiles actuels (sans retoucher les attributs)
                    rageWave(w);

                    // prochaine vague dans 2 à 5 minutes
                    nextWaveTick = t + rand(2400, 6000);
                }

                t++;
            }
        }, 0L, 1L);

        // Stop auto après 24000 ticks
        endTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            World w = getWorld();
            if (w != null) broadcast(w, "&8[&6EVENT&8] &aFin de l’event : &eLa Mort de Ragnar&7.");
            stop();
        }, DURATION_TICKS);
    }

    public void stop() {
        if (!running) return;
        running = false;

        if (tickTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        if (endTaskId != -1) {
            Bukkit.getScheduler().cancelTask(endTaskId);
            endTaskId = -1;
        }

        worldId = null;
        nextNarrationTick = 0;
        nextWaveTick = 0;
    }

    // À appeler depuis CreatureSpawnEvent
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!running) return;

        World w = getWorld();
        if (w == null) return;
        if (!event.getLocation().getWorld().equals(w)) return;

        LivingEntity ent = event.getEntity();
        if (!isHostile(ent)) return;

        buffHostileRandom(ent);
    }

    // ---------------- Buff logic ----------------

    private void refreshExistingMobs(World w) {
        for (LivingEntity le : w.getLivingEntities()) {
            if (!isHostile(le)) continue;
            // refresh effets courts (sans re-multiplier attributs)
            refreshPotions(le);
        }
    }

    private void rageWave(World w) {
        for (LivingEntity le : w.getLivingEntities()) {
            if (!isHostile(le)) continue;
            // Rage courte (20s)
            le.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 20 * 20, 0, true, true, true));
            if (chance(50)) le.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 0, true, true, true));
            if (chance(25)) le.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 15, 0, true, true, true));
        }
    }

    private boolean isHostile(LivingEntity e) {
        if (e instanceof Monster) return true;
        return (e instanceof Slime && ((Slime) e).getSize() > 0)
                || (e instanceof Phantom)
                || (e instanceof Hoglin)
                || (e instanceof Zoglin);
    }

    private void buffHostileRandom(LivingEntity mob) {
        PersistentDataContainer pdc = mob.getPersistentDataContainer();

        // Déjà buffé -> juste refresh potions
        if (pdc.has(KEY_BUFFED, PersistentDataType.BYTE)) {
            refreshPotions(mob);
            return;
        }

        // Marqueur
        pdc.set(KEY_BUFFED, PersistentDataType.BYTE, (byte) 1);

        // Multipliers random (tu peux ajuster les plages)
        double hpMult  = randDouble(1.15, 1.65); // +15% à +65%
        double dmgMult = randDouble(1.10, 1.45); // +10% à +45%
        double spdMult = randDouble(1.03, 1.18); // +3% à +18%

        // Stocke pour debug / futur
        pdc.set(KEY_HP_MULT,  PersistentDataType.DOUBLE, hpMult);
        pdc.set(KEY_DMG_MULT, PersistentDataType.DOUBLE, dmgMult);
        pdc.set(KEY_SPD_MULT, PersistentDataType.DOUBLE, spdMult);

        // Applique 1 seule fois
        scaleAttribute(mob, Attribute.MAX_HEALTH, hpMult);
        scaleAttribute(mob, Attribute.ATTACK_DAMAGE, dmgMult);
        scaleAttribute(mob, Attribute.MOVEMENT_SPEED, spdMult);

        // Heal au max après hp up
        AttributeInstance maxHp = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) mob.setHealth(Math.min(maxHp.getValue(), maxHp.getValue()));

        // Effets courts random
        refreshPotions(mob);
    }

    private void refreshPotions(LivingEntity mob) {
        // 10s, refresh régulièrement
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 10, chance(35) ? 1 : 0, true, true, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 20 * 10, chance(25) ? 1 : 0, true, true, true));

        // Un peu de résistance parfois
        if (chance(20)) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 10, 0, true, true, true));
        }

        // Rare: invis (mobs “sournois”)
        if (chance(5)) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 8, 0, true, false, true));
        }
    }

    private void scaleAttribute(LivingEntity mob, Attribute attr, double multiplier) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        inst.setBaseValue(inst.getBaseValue() * multiplier);
    }

    // ---------------- Messaging helpers ----------------

    private void broadcast(World w, String msg) {
        String colored = ChatColor.translateAlternateColorCodes('&', msg);
        for (Player p : w.getPlayers()) p.sendMessage(colored);
    }

    private void titleNearSpawn(World w, String title, String sub, int in, int stay, int out) {
        String t = ChatColor.translateAlternateColorCodes('&', title);
        String s = ChatColor.translateAlternateColorCodes('&', sub);
        Location spawn = w.getSpawnLocation();
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(spawn) <= 250 * 250) {
                p.sendTitle(t, s, in, stay, out);
            }
        }
    }

    private void soundNearSpawn(World w, Sound sound, float vol, float pitch) {
        Location spawn = w.getSpawnLocation();
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(spawn) <= 250 * 250) {
                p.playSound(p.getLocation(), sound, vol, pitch);
            }
        }
    }

    // ---------------- Random helpers ----------------

    private static int rand(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static double randDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    private static boolean chance(int percent) {
        return ThreadLocalRandom.current().nextInt(100) < percent;
    }

    private static String pick(List<String> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}