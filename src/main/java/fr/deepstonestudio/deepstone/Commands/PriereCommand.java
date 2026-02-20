package fr.deepstonestudio.deepstone.Commands;

import fr.deepstonestudio.deepstone.Manager.BlessingManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PriereCommand implements CommandExecutor {

    private final Economy economy; // peut être null si Vault absent
    private final Map<UUID, Long> sacrificeMap;
    private final Map<UUID, String> priereDeathCauseMap;
    private final BlessingManager blessingManager;

    private final Map<UUID, Long> priereCooldownMap = new HashMap<>();
    private final Random random = new Random();

    private static final long COOLDOWN_MS = 24L * 60L * 60L * 1000L; // 24 heures

    public PriereCommand(Economy economy,
                         Map<UUID, Long> sacrificeMap,
                         Map<UUID, String> priereDeathCauseMap,
                         BlessingManager blessingManager) {
        this.economy = economy;
        this.sacrificeMap = sacrificeMap;
        this.priereDeathCauseMap = priereDeathCauseMap;
        this.blessingManager = blessingManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§7[§c!§7] Commande uniquement joueur.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§7[§c!§7] Usage: /priere <thor|odin|loki|freya|frey|status|clear>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // ✅ /priere status
        if (sub.equals("status")) {
            long remaining = blessingManager.getRemainingMs(player.getUniqueId());
            if (remaining <= 0) {
                player.sendMessage("§7[§e?§7] Tu n’as aucune bénédiction active.");
            } else {
                player.sendMessage("§7[§e?§7] Bénédiction active : §6" + formatDuration(remaining) + "§7 restantes.");
            }
            return true;
        }

        // ✅ /priere clear <joueur> (admin)
        if (sub.equals("clear")) {
            if (!player.hasPermission("deepstone.priere.admin")) {
                player.sendMessage("§7[§c!§7] Tu n’as pas la permission.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§7[§c!§7] Usage: /priere clear <joueur>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage("§7[§c!§7] Joueur introuvable ou hors-ligne.");
                return true;
            }

            boolean removed = blessingManager.clearBlessing(target.getUniqueId());
            if (removed) {
                player.sendMessage("§7[§e?§7] Bénédiction retirée à §e" + target.getName() + "§7.");
                target.sendMessage("§7[§c!§7] Ta bénédiction a été retirée par un admin.");
            } else {
                player.sendMessage("§7[§e?§7] §e" + target.getName() + "§7 n’a aucune bénédiction active.");
            }
            return true;
        }

        // ✅ Sinon: prière classique /priere <dieu>
        if (args.length != 1) {
            player.sendMessage("§7[§c!§7] Usage: /priere <thor|odin|loki|freya|frey>");
            return true;
        }

        String god = sub;
        if (!List.of("thor", "odin", "loki", "freya", "frey").contains(god)) {
            player.sendMessage("§7[§c!§7] Dieu invalide.");
            return true;
        }

        long now = System.currentTimeMillis();

        // 🔒 Cooldown avant de consommer le sacrifice
        Long cdExpire = priereCooldownMap.get(player.getUniqueId());
        if (cdExpire != null && cdExpire > now) {
            player.sendMessage("§7[§c!§7] Tu dois attendre encore §6" + formatDuration(cdExpire - now) + "§7.");
            return true;
        }

        // 🔥 Sacrifice obligatoire
        Long sacrificeExpire = sacrificeMap.get(player.getUniqueId());
        if (sacrificeExpire == null || sacrificeExpire < now) {
            player.sendMessage("§7[§c!§7] Tu dois faire un sacrifice avant de prier.");
            return true;
        }

        // Consomme + cooldown
        sacrificeMap.remove(player.getUniqueId());
        priereCooldownMap.put(player.getUniqueId(), now + COOLDOWN_MS);

        player.sendMessage("§7[§e?§7] Tu pries §e" + god.toUpperCase(Locale.ROOT) + "§7...");
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 40);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f);

        // 0..4
        int roll = random.nextInt(5);
        switch (roll) {
            case 0 -> rewardMoney(player);
            case 1 -> rewardDiamonds(player);
            case 2 -> rewardFood(player, god);
            case 3 -> rewardBlessing24h(player, god); // 🧪 effets 24h persistants
            case 4 -> punish(player, god);
        }

        return true;
    }

    /* ========================= */
    /*        RECOMPENSES        */
    /* ========================= */

    private void rewardMoney(Player player) {
        if (economy != null) {
            economy.depositPlayer(player, 100.0);
            player.sendMessage("§7[§e?§7] §aLes dieux te donnent §a100€§7 !");
        } else {
            giveItem(player, new ItemStack(Material.IRON_INGOT, 10));
            player.sendMessage("§7[§e?§7] §ePas d’économie détectée: §710 lingots de fer.");
        }
    }

    private void rewardDiamonds(Player player) {
        giveItem(player, new ItemStack(Material.DIAMOND, 15));
        player.sendMessage("§7[§e?§7] §bLes dieux te donnent §b15 diamants§7 !");
    }

    private void rewardFood(Player player, String god) {
        int chance = random.nextInt(100);
        List<ItemStack> reward;
        String msg;

        if (chance < 5) { // 5% rare
            reward = List.of(
                    new ItemStack(Material.GOLDEN_APPLE, 2),
                    new ItemStack(Material.COOKED_BEEF, 16)
            );
            msg = "§dUn festin divin descend des cieux...";
        } else if (chance < 30) { // 25%
            reward = List.of(
                    new ItemStack(Material.COOKED_PORKCHOP, 20),
                    new ItemStack(Material.BAKED_POTATO, 16)
            );
            msg = "§aUn repas généreux t’est offert.";
        } else if (chance < 60) { // 30%
            reward = List.of(
                    new ItemStack(Material.COOKED_CHICKEN, 24),
                    new ItemStack(Material.CARROT, 16)
            );
            msg = "§aUne bénédiction de nourriture.";
        } else { // 40%
            reward = List.of(
                    new ItemStack(Material.BREAD, 16),
                    new ItemStack(Material.COOKED_COD, 20)
            );
            msg = "§aUn repas simple, mais honnête.";
        }

        for (ItemStack item : reward) giveItem(player, item);

        player.sendMessage("§7[§e?§7] " + msg + " §8(" + god.toUpperCase(Locale.ROOT) + ")");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
    }

    /**
     * 🧪 Effets RP par dieu, durée 24h IRL, persistants même après mort + reboot.
     * On donne des effets "modèles" (durée courte) : BlessingManager gère la persistance.
     */
    private void rewardBlessing24h(Player player, String god) {
        String g = god.toLowerCase(Locale.ROOT);

        List<PotionEffect> effects;

        switch (g) {
            case "thor" -> {
                effects = List.of(
                        new PotionEffect(PotionEffectType.STRENGTH, 20, 0, false, true, true),
                        new PotionEffect(PotionEffectType.RESISTANCE, 20, 0, false, true, true)
                );
                player.getWorld().strikeLightningEffect(player.getLocation());
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 40);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.1f);
                player.sendMessage("§7[§e?§7] §eTHOR §7t’accorde §cForce§7 et §fRésistance §7pour §61 journée§7.");
            }

            case "odin" -> {
                effects = List.of(
                        new PotionEffect(PotionEffectType.NIGHT_VISION, 20, 0, false, true, true),
                        new PotionEffect(PotionEffectType.SPEED, 20, 0, false, true, true)
                );
                player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation().add(0, 1, 0), 18);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.6f, 0.9f);
                player.sendMessage("§7[§e?§7] §eODIN §7t’offre la vision du corbeau pour §61 journée§7.");
            }

            case "freya" -> {
                effects = List.of(
                        new PotionEffect(PotionEffectType.REGENERATION, 20, 0, false, true, true),
                        new PotionEffect(PotionEffectType.ABSORPTION, 20, 1, false, true, true)
                );
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 18);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.2f);
                player.sendMessage("§7[§e?§7] §dFREYA §7te bénit pour §61 journée§7.");
            }

            case "frey" -> {
                // SATURATION existe en versions modernes
                effects = List.of(
                        new PotionEffect(PotionEffectType.SATURATION, 20, 0, false, true, true),
                        new PotionEffect(PotionEffectType.HASTE, 20, 1, false, true, true)
                );
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 25);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
                player.sendMessage("§7[§e?§7] §aFREY §7t’apporte abondance et labeur pour §61 journée§7.");
            }

            case "loki" -> {
                int r = random.nextInt(100);

                if (r < 35) {
                    effects = List.of(
                            new PotionEffect(PotionEffectType.SLOWNESS, 20, 0, false, true, true),
                            new PotionEffect(PotionEffectType.WEAKNESS, 20, 0, false, true, true),
                            new PotionEffect(PotionEffectType.NAUSEA, 20 * 60, 0, false, true, true)
                    );
                    player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.6f, 1.0f);
                    player.sendMessage("§7[§c!§7] §cLOKI §7rit… et te maudit pour §61 journée§7.");
                } else if (r < 75) {
                    effects = List.of(
                            new PotionEffect(PotionEffectType.SPEED, 20, 1, false, true, true),
                            new PotionEffect(PotionEffectType.HUNGER, 20, 0, false, true, true)
                    );
                    player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1, 0), 40);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);
                    player.sendMessage("§7[§e?§7] §eLOKI §7t’aide… mais pas gratuitement. §8(§61 journée§8)");
                } else {
                    effects = List.of(
                            new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0, false, true, true),
                            new PotionEffect(PotionEffectType.SPEED, 20, 0, false, true, true)
                    );
                    player.getWorld().spawnParticle(Particle.BUBBLE, player.getLocation().add(0, 1, 0), 35);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.6f, 1.3f);
                    player.sendMessage("§7[§e?§7] §aLOKI §7te couvre d’illusions pour §61 journée§7.");
                }
            }

            default -> {
                effects = List.of(new PotionEffect(PotionEffectType.LUCK, 20, 0, false, true, true));
                player.sendMessage("§7[§e?§7] §aUne bénédiction étrange dure §61 journée§7.");
            }
        }

        // ✅ Stockage persistant 24h + réapplication via BlessingManager
        blessingManager.setBlessing(player, effects, BlessingManager.DAY_MS);
    }

    private void punish(Player player, String god) {
        player.sendMessage("§7[§c!§7] §4Les dieux te condamnent !");
        player.getWorld().strikeLightningEffect(player.getLocation());

        String cause = "Colère de " + god.toUpperCase(Locale.ROOT);

        List<String> messages = List.of(
                "§c☠ §f" + player.getName() + " §7a été foudroyé pour avoir offensé §e" + god.toUpperCase(Locale.ROOT) + "§7.",
                "§c☠ §f" + player.getName() + " §7a murmuré une prière impure… §e" + god.toUpperCase(Locale.ROOT) + " §7l’a fait taire.",
                "§c☠ §f" + player.getName() + " §7a été jugé indigne par §e" + god.toUpperCase(Locale.ROOT) + "§7.",
                "§c☠ §f" + player.getName() + " §7a été consumé par la volonté de §e" + god.toUpperCase(Locale.ROOT) + "§7."
        );

        String deathMessage = messages.get(random.nextInt(messages.size()));
        priereDeathCauseMap.put(player.getUniqueId(), cause + "||" + deathMessage);

        player.setHealth(0.0);
    }

    /* ========================= */
    /*        UTILITAIRES        */
    /* ========================= */

    private void giveItem(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
        }
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        long hours = minutes / 60L;
        long mins = minutes % 60L;

        if (hours > 0) return hours + "h " + mins + "m";
        if (mins > 0) return mins + "m " + seconds + "s";
        return seconds + "s";
    }
}