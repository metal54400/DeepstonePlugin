package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.model.Clan;
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
        final String content = e.getMessage();

        // ✅ Si pas en mode clan chat => on laisse le chat normal fonctionner
        if (!clans.isInClanChat(pu)) return;

        // ✅ Sinon on bloque le chat global
        e.setCancelled(true);

        // ✅ Tout ce qui touche Bukkit + ClanService + envoi de messages => en SYNC
        Bukkit.getScheduler().runTask(plugin, () -> {
            // double-check (au cas où le joueur toggle off juste après)
            if (!clans.isInClanChat(pu)) return;

            Clan clan = clans.getClanOf(pu);
            if (clan == null) {
                p.sendMessage(Msg.error("Tu n'es pas dans un clan."));
                return;
            }

            Set<UUID> recipients = clans.sharedChatRecipients(pu);

            Component msg = Msg.info("[Clan] " + p.getName() + " » " + content);

            // ✅ envoie à tous les destinataires (membres + alliés selon ton service)
            for (UUID u : recipients) {
                Player t = Bukkit.getPlayer(u);
                if (t != null && t.isOnline()) {
                    t.sendMessage(msg);
                }
            }

            // ✅ au cas où le sender n'est pas inclus (normalement il l'est), on assure
            if (!recipients.contains(pu)) {
                p.sendMessage(msg);
            }
        });
    }
}
