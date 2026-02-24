package fr.deepstonestudio.deepstone.Commands;

import fr.deepstonestudio.deepstone.api.events.RagnarDeathEvent;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RagnarDeathCommand implements CommandExecutor {

    private final RagnarDeathEvent event;

    public RagnarDeathCommand(RagnarDeathEvent event) {
        this.event = event;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(color("&cUtilise: &7/ragnardeath start &8| &7/ragnardeath stop &8| &7/ragnardeath status"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                World w;

                if (sender instanceof Player p) w = p.getWorld();
                else {
                    sender.sendMessage("Console: précise un monde (ex: /ragnardeath start world)");
                    return true;
                }

                if (event.isRunning()) {
                    sender.sendMessage(color("&cEvent déjà en cours."));
                    return true;
                }

                event.start(w);
                sender.sendMessage(color("&aEvent lancé dans le monde: &e" + w.getName() + " &7(pendant 1 journée MC)"));
                return true;
            }

            case "stop" -> {
                if (!event.isRunning()) {
                    sender.sendMessage(color("&cAucun event en cours."));
                    return true;
                }
                event.stop();
                sender.sendMessage(color("&cEvent stoppé."));
                return true;
            }

            case "status" -> {
                if (!event.isRunning()) {
                    sender.sendMessage(color("&7Event: &cOFF"));
                    return true;
                }
                World w = event.getWorld();
                sender.sendMessage(color("&7Event: &aON &8| &7Monde: &e" + (w == null ? "?" : w.getName())));
                return true;
            }

            default -> {
                sender.sendMessage(color("&cUtilise: &7/ragnardeath start/stop/status"));
                return true;
            }
        }
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}