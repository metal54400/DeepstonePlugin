package fr.deepstonestudio.deepstone.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class Msg {

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void broadcast(String msg) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }
}
