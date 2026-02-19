package fr.deepstonestudio.deepstone.Listener;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimActionbarListener implements Listener {

    private final JavaPlugin plugin;

    // true = dans un claim, false = hors claim
    private final Map<UUID, Boolean> lastProtectedState = new HashMap<>();

    public ClaimActionbarListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("claim-actionbar.enabled", true)) return;

        Player player = event.getPlayer();
        boolean inClaim = isInClaim(player);
        lastProtectedState.put(player.getUniqueId(), inClaim);

        // Optionnel: afficher direct à la connexion
        sendActionbar(player, inClaim
                ? plugin.getConfig().getString("claim-actionbar.protected-message", "&aZone Protégé")
                : plugin.getConfig().getString("claim-actionbar.unprotected-message", "&cZone non protégé"));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("claim-actionbar.enabled", true)) return;

        // anti spam: seulement si changement de bloc
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        boolean inClaim = isInClaim(player);
        boolean last = lastProtectedState.getOrDefault(uuid, false);

        // seulement si l'état change
        if (inClaim == last) return;

        lastProtectedState.put(uuid, inClaim);

        String msg = inClaim
                ? plugin.getConfig().getString("claim-actionbar.protected-message", "&aZone Protégé")
                : plugin.getConfig().getString("claim-actionbar.unprotected-message", "&cZone non protégé");

        sendActionbar(player, msg);
    }

    private boolean isInClaim(Player player) {
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        return claim != null; // claim joueur OU admin
    }

    private void sendActionbar(Player player, String message) {
        if (message == null) return;
        String colored = translateColors(message);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colored));
    }

    private String translateColors(String s) {
        return s.replace("&", "§");
    }
}