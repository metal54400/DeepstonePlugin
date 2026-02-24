package fr.deepstonestudio.deepstone.Commands;


import fr.deepstonestudio.deepstone.storage.TipsStore;
import fr.deepstonestudio.deepstone.util.TipsService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TipsCommand implements CommandExecutor {

    private final TipsStore store;
    private final TipsService service;
    private final JavaPlugin plugin;

    public TipsCommand(JavaPlugin plugin, TipsStore store, TipsService service) {
        this.store = store;
        this.service = service;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // -------- RELOAD --------
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("deepstone.tips.reload")) {
                sender.sendMessage(color("&cTu n'as pas la permission."));
                return true;
            }

            service.getPlugin().reloadConfig();
            service.restart();

            sender.sendMessage(color("&f[Server] &7[&e?&7] &aConfiguration des messages d'aide rechargée."));
            return true;
        }

        // -------- PLAYER TOGGLE --------
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (!p.hasPermission("deepstone.tips")) {
            p.sendMessage(color("&cTu n'as pas la permission."));
            return true;
        }

        boolean enabled = store.isTipsEnabled(p.getUniqueId());
        store.setTipsEnabled(p.getUniqueId(), !enabled);

        if (enabled) {
            p.sendMessage(color("&f[Server] &7[&e?&7] &cTu as désactivé les messages d'aide."));
        } else {
            p.sendMessage(color("&f[Server] &7[&e?&7] &aTu as activé les messages d'aide."));
        }

        return true;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}