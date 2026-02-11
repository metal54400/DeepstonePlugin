package fr.deepstonestudio.deepstone.util;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;


public class Msg {

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void broadcast(String msg) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    private Msg() {}

    public static Component ok(String s) {
        return Component.text("✔ " + s, NamedTextColor.GREEN);
    }

    public static Component err(String s) {
        return Component.text("✖ " + s, NamedTextColor.RED);
    }

    public static Component info(String s) {
        return Component.text("➤ " + s, NamedTextColor.AQUA);
    }


}
