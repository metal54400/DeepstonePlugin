package fr.deepstonestudio.deepstone.Listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;

public class CommandBlockListener implements Listener {

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String cmd = event.getMessage().toLowerCase();

        if (cmd.equals("/pl") || cmd.equals("/plugins")) {
            if (!player.hasPermission("deepstone.pl")) {
                event.setCancelled(true);
                player.sendMessage("§7[§c!&7] §fTu n'as pas la permission d'exécuter cette commande.");
            }
        }
    }
}
