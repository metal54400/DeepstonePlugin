package fr.deepstonestudio.deepstone.Listener;


import fr.deepstonestudio.deepstone.Manager.ProtectionManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvpListener implements Listener {

    private final ProtectionManager protectionManager;

    public PvpListener(ProtectionManager protectionManager) {
        this.protectionManager = protectionManager;
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        // Victime protégée => annule dégâts
        if (protectionManager.isProtected(victim)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "§7[§c!&7] Ce joueur est protégé par L'anti-Tpkill.");
            return;
        }

        // Anti-abus : si l'attaquant est protégé et tape => retire sa protection
        if (protectionManager.isProtected(attacker)) {
            protectionManager.remove(attacker, "attaque détectée");
        }
    }
}