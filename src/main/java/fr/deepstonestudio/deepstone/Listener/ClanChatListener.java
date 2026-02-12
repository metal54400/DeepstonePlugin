package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.util.ClanService;
import fr.deepstonestudio.deepstone.util.Msg;
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
        if (!clans.isInClanChat(p.getUniqueId())) return;

        if (clans.getClanOf(p.getUniqueId()) == null) return;

        e.setCancelled(true);

        Set<UUID> recipients = clans.sharedChatRecipients(p.getUniqueId());
        String msg = String.valueOf(Msg.info("[Clan] " + p.getName() + " » " + e.getMessage()));

        // Re-sync vers le thread principal
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (UUID u : recipients) {
                Player t = Bukkit.getPlayer(u);
                if (t != null) t.sendMessage(msg);
            }
        });
    }
}