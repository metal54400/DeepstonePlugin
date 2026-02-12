package fr.deepstonestudio.deepstone.Commands;

import fr.deepstonestudio.deepstone.model.Clan;
import fr.deepstonestudio.deepstone.util.MercenaryService;
import fr.deepstonestudio.deepstone.util.Msg;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class MercenaryCommand implements CommandExecutor, TabCompleter {

    private final MercenaryService mercs;

    public MercenaryCommand(MercenaryService mercs) {
        this.mercs = mercs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }

        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                p.sendMessage(Msg.info("/mercenary join <clan>"));
                p.sendMessage(Msg.info("/mercenary leave"));
                p.sendMessage(Msg.info("/mercenary status"));
                p.sendMessage(Msg.info("/mercenary balance"));
                return true;
            }

            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "join" -> {
                    if (args.length < 2) throw new IllegalStateException("Usage: /mercenary join <clan>");
                    mercs.join(p, args[1]);
                    Clan emp = mercs.getEmployerClan(p.getUniqueId());
                    p.sendMessage(Msg.ok("Contrat signé. Tu combats pour " + (emp == null ? args[1] : emp.getDisplayName()) + "."));
                }
                case "leave" -> {
                    mercs.leave(p);
                    p.sendMessage(Msg.ok("Contrat terminé."));
                }
                case "status" -> {
                    Clan emp = mercs.getEmployerClan(p.getUniqueId());
                    if (emp == null) {
                        p.sendMessage(Msg.info("Tu n’as aucun contrat."));
                    } else {
                        long ms = mercs.getRemainingMs(p.getUniqueId());
                        long min = ms / 60_000L;
                        p.sendMessage(Msg.info("Employeur: " + emp.getDisplayName()));
                        p.sendMessage(Msg.info("Temps restant: " + min + " min"));
                    }
                }
                case "balance" -> {
                    p.sendMessage(Msg.info("Bourse mercenaire: " + mercs.getWallet(p.getUniqueId()) + " gloire."));
                }
                default -> p.sendMessage(Msg.err("Commande inconnue. /mercenary help"));
            }

            return true;

        } catch (Exception e) {
            p.sendMessage(Msg.err(e.getMessage() == null ? "Erreur." : e.getMessage()));
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("help","join","leave","status","balance"), args[0]);
        // le tab des clans : tu peux brancher clans.clanNames() si tu veux, là on laisse vide
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String start) {
        String s = start.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.toLowerCase(Locale.ROOT).startsWith(s)) out.add(o);
        return out;
    }
}
