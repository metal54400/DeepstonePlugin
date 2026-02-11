package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.util.Msg;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class TradeHours implements Listener {
    private static final int START = 7;
    private static final int END = 19;
    @EventHandler
    public void onInteractVillager(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() != EntityType.VILLAGER) return;

        Player player = event.getPlayer();
        World world = player.getWorld();

        // Heure Minecraft: 0..23999 ticks (0 = 06:00)
        long time = world.getTime();
        int hour = minecraftHour(time);

        // Autorisé de 07:00 inclus à 19:00 exclus (tu peux ajuster)
        if (hour < START || hour >= END) {
            event.setCancelled(true);
            Msg.broadcast("&7[§c!&7] Les échanges avec les villageois sont autorisés uniquement de &d07h& à &b19h.");
        }
    }

    // Conversion simple ticks -> heure (approx)
    // Minecraft: 0 tick = 06:00, 1000 ticks = 1h
    private int minecraftHour(long ticks) {
        // on ramène 0..23999
        ticks = ticks % 24000;
        // 0 = 06:00, donc heure = (ticks/1000 + 6) % 24
        return (int)((ticks / 1000 + 6) % 24);
    }
}
