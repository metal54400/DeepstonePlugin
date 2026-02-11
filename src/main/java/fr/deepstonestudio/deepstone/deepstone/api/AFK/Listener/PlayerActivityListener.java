package fr.deepstonestudio.deepstone.api.AFK.Listener;

import fr.deepstonestudio.deepstone.api.AFK.AfkService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class PlayerActivityListener implements Listener {

    private final AfkService afk;
    private final boolean cancelOnMove;
    private final boolean cancelOnChat;
    private final boolean cancelOnInteract;

    public PlayerActivityListener(AfkService afk, boolean cancelOnMove, boolean cancelOnChat, boolean cancelOnInteract) {
        this.afk = afk;
        this.cancelOnMove = cancelOnMove;
        this.cancelOnChat = cancelOnChat;
        this.cancelOnInteract = cancelOnInteract;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        afk.markActive(e.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!cancelOnMove) return;
        if (e.getTo() == null) return;

        // Ignore si pas de déplacement réel (évite spam)
        if (e.getFrom().getX() == e.getTo().getX()
                && e.getFrom().getY() == e.getTo().getY()
                && e.getFrom().getZ() == e.getTo().getZ()) {
            return;
        }

        afk.markActive(e.getPlayer());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!cancelOnChat) return;
        afk.markActive(e.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        // Même si cancelOnChat=false, beaucoup de serveurs veulent sortir AFK sur commande.
        // Si tu veux le rendre configurable aussi, dis-moi.
        afk.markActive(e.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!cancelOnInteract) return;
        afk.markActive(e.getPlayer());
    }
}
