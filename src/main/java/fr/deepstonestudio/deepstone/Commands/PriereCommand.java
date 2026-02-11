package fr.deepstonestudio.deepstone.Commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PriereCommand implements CommandExecutor {

    private final Economy economy; // peut être null si Vault/economy absent
    private final Map<UUID, Long> sacrificeMap;
    private final Random random = new Random();

    public PriereCommand(Economy economy, Map<UUID, Long> sacrificeMap) {
        this.economy = economy;
        this.sacrificeMap = sacrificeMap;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande uniquement joueur.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /priere <thor|odin|loki|freya|frey>");
            return true;
        }

        String god = args[0].toLowerCase(Locale.ROOT);
        if (!List.of("thor", "odin", "loki", "freya", "frey").contains(god)) {
            player.sendMessage(ChatColor.RED + "Dieu invalide.");
            return true;
        }

        long now = System.currentTimeMillis();
        Long expire = sacrificeMap.get(player.getUniqueId());
        if (expire == null || expire < now) {
            player.sendMessage(ChatColor.RED + "Tu dois faire un sacrifice avant de prier.");
            return true;
        }

        // Consomme le sacrifice
        sacrificeMap.remove(player.getUniqueId());

        player.sendMessage(ChatColor.GOLD + "Tu pries " + god.toUpperCase() + "...");
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 40);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f);

        int roll = random.nextInt(3);

        switch (roll) {
            case 0 -> { // Argent OU fallback fer
                if (economy != null) {
                    economy.depositPlayer(player, 100.0);
                    player.sendMessage(ChatColor.GREEN + "Les dieux te donnent 100€ !");
                } else {
                    giveIronFallback(player);
                    player.sendMessage(ChatColor.YELLOW + "Pas d’économie détectée: tu reçois 10 lingots de fer à la place.");
                }
            }
            case 1 -> {
                player.getInventory().addItem(new ItemStack(Material.DIAMOND, 15));
                player.sendMessage(ChatColor.AQUA + "Les dieux te donnent 15 diamants !");
            }
            case 2 -> {
                player.sendMessage(ChatColor.DARK_RED + "Les dieux te condamnent !");
                player.getWorld().strikeLightningEffect(player.getLocation());
                player.setHealth(0.0);
            }
        }

        return true;
    }

    private void giveIronFallback(Player player) {
        ItemStack iron = new ItemStack(Material.IRON_INGOT, 10);
        var leftover = player.getInventory().addItem(iron);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.1f);
    }
}