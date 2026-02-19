package fr.deepstonestudio.deepstone.Listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerTradeLimiter implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> bookTradeCooldown = new HashMap<>();

    public VillagerTradeLimiter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Config: enabled global
        if (!plugin.getConfig().getBoolean("villager-trade-limiter.enabled", true)) return;

        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();

        // Le holder peut être Merchant plutôt que Villager selon versions/serveurs
        if (!(holder instanceof Villager) && !(inv instanceof MerchantInventory)) return;

        // Pour éviter NPE / mauvais slots
        if (event.getSlot() < 0) return;

        // On essaie de récupérer la recette de manière safe
        MerchantRecipe trade = null;

        if (inv instanceof MerchantInventory merchantInv) {
            Merchant merchant = merchantInv.getMerchant();
            int selected = merchantInv.getSelectedRecipeIndex();
            if (selected >= 0 && selected < merchant.getRecipes().size()) {
                trade = merchant.getRecipes().get(selected);
            }
        } else if (holder instanceof Villager villager) {
            // Fallback (mais getRecipe(event.getSlot()) n'est pas fiable avec InventoryClick)
            int index = safeRecipeIndexFromSlot(event.getSlot());
            if (index >= 0 && index < villager.getRecipes().size()) {
                trade = villager.getRecipes().get(index);
            }
        }

        if (trade == null) return;

        Material result = trade.getResult().getType();
        UUID playerUUID = player.getUniqueId();

        // ✅ Limitation livres enchantés
        if (result == Material.ENCHANTED_BOOK) {
            if (!plugin.getConfig().getBoolean("villager-trade-limiter.enchanted-book.enabled", true)) return;

            long cooldownSeconds = plugin.getConfig().getLong("villager-trade-limiter.enchanted-book.cooldown-seconds", 3600L);
            long cooldownMs = cooldownSeconds * 1000L;

            long lastTrade = bookTradeCooldown.getOrDefault(playerUUID, 0L);
            long now = System.currentTimeMillis();

            if (now - lastTrade < cooldownMs) {
                event.setCancelled(true);
                String msg = plugin.getConfig().getString(
                        "villager-trade-limiter.enchanted-book.message",
                        "§7[§e?§7] Vous devez attendre avant d'acheter un autre livre enchanté !"
                );
                player.sendMessage(msg);
                return;
            }

            bookTradeCooldown.put(playerUUID, now);
        }
    }

    @EventHandler
    public void onVillagerTradeChange(VillagerAcquireTradeEvent event) {
        if (!plugin.getConfig().getBoolean("villager-trade-limiter.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("villager-trade-limiter.max-uses.enabled", true)) return;

        MerchantRecipe trade = event.getRecipe();
        Material result = trade.getResult().getType();

        int diamondMax = plugin.getConfig().getInt("villager-trade-limiter.max-uses.diamond-items-max-uses", 1);
        int cheapMax = plugin.getConfig().getInt("villager-trade-limiter.max-uses.cheap-items-max-uses", 5);

        // Appliquer les limitations sur certains échanges
        if (result == Material.DIAMOND_CHESTPLATE || result == Material.DIAMOND_PICKAXE) {
            trade.setMaxUses(diamondMax);
        } else if (result == Material.COAL || result == Material.STICK) {
            trade.setMaxUses(cheapMax);
        }
    }

    /**
     * InventoryClickEvent slot ne correspond pas directement à l'index recette.
     * Ici on met un fallback simple (souvent inutile si MerchantInventory géré).
     */
    private int safeRecipeIndexFromSlot(int slot) {
        // Sur beaucoup de versions, le résultat est slot 2 (inputs 0/1)
        // et la recette sélectionnée est gérée par MerchantInventory.
        // Donc on renvoie -1 pour éviter de casser.
        return -1;
    }
}