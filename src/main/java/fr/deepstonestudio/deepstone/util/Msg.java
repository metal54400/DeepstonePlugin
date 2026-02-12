package fr.deepstonestudio.deepstone.util;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;


public class Msg {

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void broadcast(String msg) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    private Msg() {}

    public static @NotNull String ok(String s) {
        return Component.text("§7[§e?§7] " + s, NamedTextColor.GREEN);
    }

    public static String err(String s) {
        return Component.text("§7[§c!§7] " + s, NamedTextColor.RED);
    }

    public static Component info(String s) {
        return Component.text("§7[§d!§7] " + s, NamedTextColor.AQUA);
    }


}
