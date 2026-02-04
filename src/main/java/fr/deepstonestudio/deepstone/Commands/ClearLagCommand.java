package fr.deepstonestudio.deepstone.Commands;

import fr.deepstonestudio.deepstone.util.ClearService;
import fr.deepstonestudio.deepstone.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearLagCommand implements CommandExecutor {

    private final ClearService clearService;

    public ClearLagCommand(ClearService clearService) {
        this.clearService = clearService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.hasPermission("deepstone.clearlag")) {
                p.sendMessage("§cTu n'as pas la permission.");
                return true;
            }
        }

        int removed = clearService.clearGroundItems();
        Msg.broadcast("§a" + removed + " items ont été supprimés.");
        return true;
    }
}
