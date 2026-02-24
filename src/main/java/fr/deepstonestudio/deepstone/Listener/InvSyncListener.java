package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.Manager.InvSyncManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

public final class InvSyncListener implements Listener {

    private final InvSyncManager manager;

    public InvSyncListener(InvSyncManager manager) {
        this.manager = manager;
    }

    // ===================== GAMEMODE SWAP =====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGamemodeChange(PlayerGameModeChangeEvent e) {

        // On attend 1 tick pour être sûr que le gamemode a changé
        Bukkit.getScheduler().runTask(manager.getPlugin(), () ->
                manager.swap(e.getPlayer(), e.getNewGameMode())
        );
    }

    // ===================== JOIN =====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {

        // On attend 1 tick que tout soit chargé
        Bukkit.getScheduler().runTask(manager.getPlugin(), () -> {
            manager.lock(e.getPlayer());
            manager.loadFor(e.getPlayer(), e.getPlayer().getGameMode());
        });
    }

    // ===================== QUIT =====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        manager.saveCurrent(e.getPlayer());
        manager.flush();
    }

    // ===================== ANTI DUPE =====================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (manager.isLocked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.Player p) {
            if (manager.isLocked(p)) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof org.bukkit.entity.Player p) {
            if (manager.isLocked(p)) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (manager.isLocked(e.getPlayer())) e.setCancelled(true);
    }
}

