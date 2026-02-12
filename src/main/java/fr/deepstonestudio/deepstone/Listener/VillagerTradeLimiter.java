package fr.deepstonestudio.deepstone.Listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.MerchantRecipe;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerTradeLimiter implements Listener {
    private final Map<UUID, Long> bookTradeCooldown = new HashMap<>();

    @EventHandler
    public void onTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof Villager villager)) return;


        MerchantRecipe trade = villager.getRecipe(event.getSlot());
        Material result = trade.getResult().getType();
        UUID playerUUID = player.getUniqueId();


// Limitation des livres enchantés (1 par heure)
        if (result == Material.ENCHANTED_BOOK) {
            long lastTrade = bookTradeCooldown.getOrDefault(playerUUID, 0L);
            if (System.currentTimeMillis() - lastTrade < 3600000) {
                event.setCancelled(true);
                player.sendMessage("§7[§e?§7] Vous devez attendre avant d'acheter un autre livre enchanté !");
                return;
            }
            bookTradeCooldown.put(playerUUID, System.currentTimeMillis());
        }
    }


    @EventHandler
    public void onVillagerTradeChange(VillagerAcquireTradeEvent event) {
        MerchantRecipe trade = event.getRecipe();
        Material result = trade.getResult().getType();


// Appliquer les limitations sur certains échanges
        if (result == Material.DIAMOND_CHESTPLATE || result == Material.DIAMOND_PICKAXE) {
            trade.setMaxUses(1); // 1 trade par refill
        } else if (result == Material.COAL || result == Material.STICK) {
            trade.setMaxUses(5); // 5 trades par refill
        }
    }
}
