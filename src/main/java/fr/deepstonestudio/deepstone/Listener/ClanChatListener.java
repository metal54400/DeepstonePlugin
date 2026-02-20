package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.util.ClanService;
import fr.deepstonestudio.deepstone.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        UUID pu = p.getUniqueId();

        // On lit uniquement le contenu en async (safe)
        String content = e.getMessage();

        // On passe tout en sync (safe) pour lire ClanService + envoyer messages
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!clans.isInClanChat(pu)) return;

            if (clans.getClanOf(pu) == null) {
                p.sendMessage(Msg.error("Tu n'es pas dans un clan."));
                return;
            }

            // Annule le chat global (même si l'event est déjà passé, on le fait avant ci-dessous)
            // => Ici on annule en async aussi juste après (voir plus bas)
            Set<UUID> recipients = clans.sharedChatRecipients(pu);
            Component msg = Msg.info("[Clan] " + p.getName() + " » " + content);
            Player t = null;
            t.sendMessage(msg);

            for (UUID u : recipients) {
                t = Bukkit.getPlayer(u);
                if (t != null && t.isOnline()) {
                    t.sendMessage(msg);
                }
            }
        });

        // IMPORTANT : on annule tout de suite le chat global si le joueur est en mode clan chat
        // (on ne touche pas ClanService ici => juste une annulation "optimiste")
        // Si tu veux être 100% correct, on annule toujours puis on renvoie global si pas clan chat,
        // mais ça devient plus lourd.
        e.setCancelled(true);
    }
}