package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.util.ClanService;
import fr.deepstonestudio.deepstone.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;

public final class ClanChatListener implements Listener {

    private final JavaPlugin plugin;
    private final ClanService clans;

    public ClanChatListener(JavaPlugin plugin, ClanService clans) {
        this.plugin = plugin;
        this.clans = clans;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        final UUID pu = p.getUniqueId();

        // Si le joueur n'est pas en mode clan chat -> on ne touche à rien (chat classique OK)
        if (!clans.isInClanChat(pu)) return;

        // On bloque le chat global uniquement pour le clan chat
        e.setCancelled(true);

        // Lecture du message (safe en async)
        final String content = e.getMessage();

        // Tout ce qui touche Bukkit/ClanService/envoi en sync
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Double-check (au cas où l'état a changé entre temps)
            if (!clans.isInClanChat(pu)) return;

            UUID clanId = clans.getClanOf(pu);
            if (clanId == null) {
                p.sendMessage(Msg.error("Tu n'es pas dans un clan."));
                return;
            }

            Set<UUID> recipients = clans.sharedChatRecipients(pu);
            if (recipients == null || recipients.isEmpty()) {
                // Optionnel : au moins renvoyer au joueur
                p.sendMessage(Msg.error("Aucun destinataire de clan chat trouvé."));
                return;
            }

            Component msg = Msg.info("[Clan] " + p.getName() + " » " + content);

            for (UUID u : recipients) {
                Player t = Bukkit.getPlayer(u);
                if (t != null && t.isOnline()) {
                    t.sendMessage(msg);
                }
            }
        });
    }
}
