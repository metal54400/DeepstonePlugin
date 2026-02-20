package fr.deepstonestudio.deepstone.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class Msg {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private Msg() {}

    /* ========================= */
    /*       STRING SUPPORT      */
    /* ========================= */

    public static Component color(String s) {
        return LEGACY.deserialize(s);
    }

    public static void send(Player player, String msg) {
        player.sendMessage(color(msg));
    }

    public static void send(Player player, Component component) {
        player.sendMessage(component);
    }

    public static void broadcast(String msg) {
        Component c = color(msg);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(c));
        Bukkit.getConsoleSender().sendMessage(c);
    }

    public static void broadcast(Component component) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(component));
        Bukkit.getConsoleSender().sendMessage(component);
    }

    /* ========================= */
    /*        PREFIX TYPES       */
    /* ========================= */

    public static Component ok(String s) {
        return prefix("✓", NamedTextColor.GREEN)
                .append(Component.text(" "))
                .append(color(s));
    }

    public static Component error(String s) {
        return prefix("!", NamedTextColor.RED)
                .append(Component.text(" "))
                .append(color(s));
    }

    public static Component info(String s) {
        return prefix("!", NamedTextColor.AQUA)
                .append(Component.text(" "))
                .append(color(s));
    }

    private static Component prefix(String symbol, NamedTextColor color) {
        return Component.text("[", NamedTextColor.GRAY)
                .append(Component.text(symbol, color))
                .append(Component.text("]", NamedTextColor.GRAY));
    }
}