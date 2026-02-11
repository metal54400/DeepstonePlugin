package fr.deepstonestudio.deepstone.Commands;

import fr.deepstonestudio.deepstone.util.ClanService;
import fr.deepstonestudio.deepstone.util.WarService;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class WarCommand implements CommandExecutor {

    private final ClanService clans;
    private final WarService wars;

    public WarCommand(ClanService clans, WarService wars) {
        this.clans = clans;
        this.wars = wars;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) return true;

        if (args.length < 1) {
            player.sendMessage("§7[§e?§7] §c/war <declare|accept> <clan>");
            return true;
        }

        var clan = clans.getClanOf(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§7[§e?§7] §cTu n'as pas de clan.");
            return true;
        }

        if (args[0].equalsIgnoreCase("declare")) {

            if (args.length < 2) {
                player.sendMessage("§7[§e?§7] §c/war declare <clan>");
                return true;
            }

            var target = clans.getClanByName(args[1]);
            if (target == null) {
                player.sendMessage("§7[§e?§7] §cClan introuvable.");
                return true;
            }

            wars.declare(clan.getName(), target.getName());

            player.getServer().broadcastMessage("§7[§e?§7] §6"
                    + clan.getDisplayName()
                    + " déclare la guerre à "
                    + target.getDisplayName());

        } else if (args[0].equalsIgnoreCase("accept")) {

            var war = wars.accept(clan.getName());
            if (war == null) {
                player.sendMessage("§7[§c!§7] Aucune guerre en attente.");
                return true;
            }

            player.getServer().broadcastMessage("§7[§c!§7] La guerre commence !");
        }

        return true;
    }
}