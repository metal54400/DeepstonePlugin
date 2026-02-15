package fr.deepstonestudio.deepstone.api;

import com.Acrobot.ChestShop.Events.PreShopCreationEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ShopPriceListener implements Listener {

    @EventHandler
    public void onShopCreate(PreShopCreationEvent event) {

        String[] lines = event.getSignLines();
        Player player = event.getPlayer();

        if (lines.length < 4) return;

        // Ligne 2 = prix (ex: B 100:50)
        String priceLine = lines[2];
        double buyPrice = extractBuyPrice(priceLine);

        if (buyPrice <= 0) return;

        // Ligne 3 = item
        Material material = Material.matchMaterial(lines[3]);
        if (material == null) return;

        double minPrice = getMinimumPrice(material);

        if (buyPrice < minPrice) {
            event.setOutcome(PreShopCreationEvent.CreationOutcome.BUY_PRICE_BELOW_MIN);
            player.sendMessage("§cPrix minimum pour §e" + material.name() + " §c: §6" + minPrice + "€");
        }
    }

    private double extractBuyPrice(String line) {
        if (line == null) return -1;

        line = line.replace(" ", "").toUpperCase();

        if (!line.contains("B")) return -1;

        try {
            String pricePart = line.split("B")[1];

            if (pricePart.contains(":")) {
                return Double.parseDouble(pricePart.split(":")[0]);
            }

            return Double.parseDouble(pricePart);

        } catch (Exception e) {
            return -1;
        }
    }

    private double getMinimumPrice(Material material) {
        switch (material) {

            case ENCHANTED_BOOK: return 200;
            case NETHERITE_INGOT: return 400;
            case NETHERITE_HELMET: return 500;
            case NETHERITE_CHESTPLATE: return 560;
            case NETHERITE_LEGGINGS: return 540;
            case NETHERITE_BOOTS: return 580;
            case NETHERITE_SWORD: return 400;
            case NETHERITE_PICKAXE: return 400;
            case NETHERITE_AXE: return 400;
            case NETHERITE_HOE: return 200;

            case DIAMOND: return 20;
            case DIAMOND_HELMET: return 100;
            case DIAMOND_CHESTPLATE: return 160;
            case DIAMOND_LEGGINGS: return 140;
            case DIAMOND_BOOTS: return 80;
            case DIAMOND_SWORD: return 40;
            case DIAMOND_PICKAXE: return 60;
            case DIAMOND_AXE: return 60;
            case DIAMOND_HOE: return 40;

            case TRIDENT: return 600;
            case TOTEM_OF_UNDYING: return 450;
            case SHULKER_BOX: return 1200;
            case WITHER_SKELETON_SKULL: return 400;
            case NETHER_STAR: return 1200;
            case BEACON: return 1200;

            case IRON_BLOCK: return 7;
            case IRON_INGOT: return 0.7;
            case IRON_NUGGET: return 0.07;

            case GOLD_BLOCK: return 9;
            case GOLD_INGOT: return 0.9;
            case GOLD_NUGGET: return 0.09;

            case PLAYER_HEAD: return 20;
        }

        return 0;
    }
}
