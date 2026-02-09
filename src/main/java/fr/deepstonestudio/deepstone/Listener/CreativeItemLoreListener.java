package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.Deepstone;
import org.bukkit.event.Listener;
import org.bukkit.*;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class CreativeItemLoreListener implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey refundKey;

    // Items qui doivent rester échangeables avec les villageois (donc PAS de lore)
    private static final Set<Material> VILLAGER_TRADE_ITEMS = Set.of(
            Material.EMERALD, Material.WHEAT, Material.PAPER, Material.STRING,
            Material.COAL, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND,
            Material.ROTTEN_FLESH, Material.BREAD, Material.CARROT, Material.POTATO,
            Material.PUMPKIN, Material.MELON_SLICE, Material.COOKED_BEEF, Material.COOKED_CHICKEN,
            Material.BOOK
    );

    public CreativeItemLoreListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.refundKey = new NamespacedKey(plugin, "rembourse");
    }

    @EventHandler
    public void onCreativeItemClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.getGameMode() != GameMode.CREATIVE) return;
        if (event.getView().getType() != InventoryType.CREATIVE) return;
        if (!player.hasPermission("deepstone.rembourse") && !player.isOp()) return;
        if (!plugin.getConfig().getBoolean("addLoreOnCreative", true)) return;

        ItemStack item = event.getCursor();
        if (item == null || item.getType() == Material.AIR) return;

        markRefunded(item, player);
    }

    private void markRefunded(ItemStack item, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(refundKey, PersistentDataType.STRING)) return;

        // Tag invisible -> n’empêche pas les trades
        pdc.set(refundKey, PersistentDataType.STRING, player.getName());

        // Lore visible seulement si l’item n’est pas “trade-villagers”
        if (!VILLAGER_TRADE_ITEMS.contains(item.getType())) {
            String loreLine = plugin.getConfig()
                    .getString("loreitem", "&7Remboursé par &b{staff}")
                    .replace("{staff}", player.getName());
            loreLine = ChatColor.translateAlternateColorCodes('&', loreLine);

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            if (!lore.contains(loreLine)) {
                lore.add(loreLine);
                meta.setLore(lore);
            }
        }

        item.setItemMeta(meta);
        Bukkit.getLogger().info("[ItemsLog] Item remboursé par " + player.getName());
    }
}