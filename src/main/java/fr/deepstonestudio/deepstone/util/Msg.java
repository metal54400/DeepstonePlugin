package fr.deepstonestudio.deepstone.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Msg {

    private Msg() {}

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void send(Player player, String msg) {
        player.sendMessage(color(msg));
    }

    public static void broadcast(String msg) {
        String colored = color(msg);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(colored));
        Bukkit.getConsoleSender().sendMessage(colored);
    }

    public static Component ok(String s) {
        return Component.text("§7[§e?§7] " + s, NamedTextColor.GREEN);
    }

    public static Component err(String s) {
        return Component.text("§7[§c!§7] " + s, NamedTextColor.RED);
    }

    public static Component info(String s) {
        return Component.text("§7[§d!§7] " + s, NamedTextColor.AQUA);
    }
}