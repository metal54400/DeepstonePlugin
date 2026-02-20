package fr.deepstonestudio.deepstone.Listener;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RaidListener implements Listener {

    private final JavaPlugin plugin;

    private final Map<UUID, Long> raidCooldown = new ConcurrentHashMap<>();

    // Config
    private boolean enabled;
    private long cooldownMs;
    private Material rewardMaterial;
    private int rewardAmount;
    private String msgCooldown;
    private String msgReward;

    public RaidListener(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        FileConfiguration c = plugin.getConfig();

        this.enabled = c.getBoolean("raid.enabled", true);

        long cooldownSeconds = c.getLong("raid.cooldown-seconds", 3600L);
        this.cooldownMs = Math.max(0L, cooldownSeconds) * 1000L;

        String matName = c.getString("raid.reward.material", "TOTEM_OF_UNDYING");
        Material mat = Material.matchMaterial(matName == null ? "" : matName);
        this.rewardMaterial = (mat != null) ? mat : Material.TOTEM_OF_UNDYING;

        this.rewardAmount = Math.max(1, c.getInt("raid.reward.amount", 2));

        this.msgCooldown = c.getString(
                "raid.message-cooldown",
                "§7[§e?§7] Vous devez attendre encore §6{minutes}§7 minutes (§6{seconds}§7s) avant un nouveau raid."
        );

        this.msgReward = c.getString(
                "raid.message-reward",
                "§7[§e?§7] §aRécompense de raid reçue: §e{amount}x {item}§7."
        );
    }

    @EventHandler
    public void onRaidFinish(RaidFinishEvent event) {
        if (!enabled) return;

        List<Player> winners = event.getWinners();
        long now = System.currentTimeMillis();

        for (Player player : winners) {
            UUID uuid = player.getUniqueId();

            Long last = raidCooldown.get(uuid);
            if (last != null && cooldownMs > 0) {
                long elapsed = now - last;
                if (elapsed < cooldownMs) {
                    long remainingMs = cooldownMs - elapsed;
                    long remainingSeconds = Math.max(0L, remainingMs / 1000L);
                    long remainingMinutes = (remainingSeconds + 59) / 60; // arrondi au dessus

                    player.sendMessage(msgCooldown
                            .replace("{minutes}", String.valueOf(remainingMinutes))
                            .replace("{seconds}", String.valueOf(remainingSeconds))
                    );
                    continue;
                }
            }

            // Met à jour le cooldown
            raidCooldown.put(uuid, now);

            // Donne la récompense
            ItemStack reward = new ItemStack(rewardMaterial, rewardAmount);
            var leftover = player.getInventory().addItem(reward);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
            }

            player.sendMessage(msgReward
                    .replace("{amount}", String.valueOf(rewardAmount))
                    .replace("{item}", rewardMaterial.name())
            );
        }
    }
}