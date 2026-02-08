package fr.deepstonestudio.deepstone.api.AFK.Listener;

import fr.deepstonestudio.deepstone.api.AFK.AfkService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public final class PlayerActivityListener implements Listener {

    private final AfkService afk;

    public PlayerActivityListener(AfkService afk) {
        this.afk = afk;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        afk.markActive(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // pas nécessaire, le service se base sur online players
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        // Évite de spam si le joueur ne change pas de bloc/position
        if (e.getFrom().getX() == e.getTo().getX()
                && e.getFrom().getY() == e.getTo().getY()
                && e.getFrom().getZ() == e.getTo().getZ()
                && e.getFrom().getYaw() == e.getTo().getYaw()
                && e.getFrom().getPitch() == e.getTo().getPitch()) {
            return;
        }
        afk.markActive(e.getPlayer());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        afk.markActive(e.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        afk.markActive(e.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        afk.markActive(e.getPlayer());
    }
}
