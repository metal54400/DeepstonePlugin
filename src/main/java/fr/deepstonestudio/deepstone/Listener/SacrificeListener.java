package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.util.Msg;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.*;

public class SacrificeListener implements Listener {

    private final Map<UUID, Long> sacrificeMap;
    private final Set<EntityType> allowed = Set.of(
            EntityType.COW,
            EntityType.SHEEP,
            EntityType.PIG,
            EntityType.CHICKEN,
            EntityType.PLAYER
    );

    public SacrificeListener(Map<UUID, Long> sacrificeMap) {
        this.sacrificeMap = sacrificeMap;
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {

        if (!(event.getEntity().getKiller() instanceof Player player)) return;
        if (!allowed.contains(event.getEntity().getType())) return;

        // 60 secondes pour prier
        sacrificeMap.put(player.getUniqueId(),
                System.currentTimeMillis() + 60000);

        player.sendMessage(ChatColor.DARK_RED + "§7[§e?§7] Sacrifice accepté ! Tu as 60 secondes pour faire /priere.");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1f, 0.8f);
    }
}