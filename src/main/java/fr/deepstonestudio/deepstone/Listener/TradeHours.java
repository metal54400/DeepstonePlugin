package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.util.Msg;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TradeHours implements Listener {

    private final JavaPlugin plugin;

    public TradeHours(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteractVillager(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() != EntityType.VILLAGER) return;

        // ✅ Toggle dans config
        if (!plugin.getConfig().getBoolean("trade-hours.enabled", true)) return;

        int start = plugin.getConfig().getInt("trade-hours.start-hour", 7);
        int end = plugin.getConfig().getInt("trade-hours.end-hour", 19);

        Player player = event.getPlayer();
        World world = player.getWorld();

        long time = world.getTime();
        int hour = minecraftHour(time);

        // Autorisé de start inclus à end exclus
        if (hour < start || hour >= end) {
            event.setCancelled(true);

            String msg = plugin.getConfig().getString(
                    "trade-hours.message",
                    "&7[§e?&7] Les échanges avec les villageois sont autorisés uniquement de &d%start%h& à &b%end%h&."
            );

            msg = msg.replace("%start%", String.valueOf(start))
                    .replace("%end%", String.valueOf(end));

            boolean broadcast = plugin.getConfig().getBoolean("trade-hours.broadcast", true);

            if (broadcast) {
                Msg.broadcast(msg);
            } else {
                Msg.send(player, msg);
            }
        }
    }

    // Minecraft: 0 tick = 06:00, 1000 ticks = 1h
    private int minecraftHour(long ticks) {
        ticks = ticks % 24000;
        return (int) ((ticks / 1000 + 6) % 24);
    }
}