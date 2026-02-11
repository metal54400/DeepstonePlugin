package fr.deepstonestudio.deepstone.Commands;


import fr.deepstonestudio.deepstone.model.Role;
import fr.deepstonestudio.deepstone.util.ClanService;
import fr.deepstonestudio.deepstone.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class ClanCommand implements CommandExecutor, TabCompleter {
    private final ClanService clans;

    public ClanCommand(ClanService clans) {
        this.clans = clans;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }

        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                p.sendMessage(Msg.info("/clan create <nom>"));
                p.sendMessage(Msg.info("/clan disband"));
                p.sendMessage(Msg.info("/clan invite <joueur>"));
                p.sendMessage(Msg.info("/clan join"));
                p.sendMessage(Msg.info("/clan leave"));
                p.sendMessage(Msg.info("/clan kick <joueur>"));
                p.sendMessage(Msg.info("/clan role <joueur> <WARRIOR|PEASANT>"));
                p.sendMessage(Msg.info("/clan setking <joueur>"));
                p.sendMessage(Msg.info("/clan setjarl <joueur>"));
                p.sendMessage(Msg.info("/clan info [nom]"));
                p.sendMessage(Msg.info("/clan list"));
                return true;
            }

            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create" -> {
                    if (args.length < 2) throw new IllegalStateException("Usage: /clan create <nom>");
                    var c = clans.createClan(p, args[1]);
                    p.sendMessage(Msg.ok("Clan créé: " + c.getDisplayName() + " (Roi = toi)"));
                }
                case "disband" -> {
                    clans.disband(p);
                    p.sendMessage(Msg.ok("Clan supprimé."));
                }
                case "invite" -> {
                    if (args.length < 2) throw new IllegalStateException("Usage: /clan invite <joueur>");
                    Player t = Bukkit.getPlayerExact(args[1]);
                    if (t == null) throw new IllegalStateException("Joueur introuvable/ hors-ligne.");
                    clans.invite(p, t);
                    p.sendMessage(Msg.ok("Invitation envoyée à " + t.getName()));
                    t.sendMessage(Msg.info("Tu as une invitation de clan. Fais /clan join"));
                }
                case "join" -> {
                    var c = clans.acceptInvite(p);
                    p.sendMessage(Msg.ok("Tu as rejoint " + c.getDisplayName() + " en tant que PAYSAN."));
                }
                case "leave" -> {
                    clans.leave(p);
                    p.sendMessage(Msg.ok("Tu as quitté ton clan."));
                }
                case "kick" -> {
                    if (args.length < 2) throw new IllegalStateException("Usage: /clan kick <joueur>");
                    Player t = Bukkit.getPlayerExact(args[1]);
                    if (t == null) throw new IllegalStateException("Joueur introuvable/ hors-ligne.");
                    clans.kick(p, t);
                    p.sendMessage(Msg.ok("Joueur expulsé: " + t.getName()));
                    t.sendMessage(Msg.err("Tu as été expulsé du clan."));
                }
                case "role" -> {
                    if (args.length < 3) throw new IllegalStateException("Usage: /clan role <joueur> <WARRIOR|PEASANT>");
                    Player t = Bukkit.getPlayerExact(args[1]);
                    if (t == null) throw new IllegalStateException("Joueur introuvable/ hors-ligne.");
                    Role r = Role.from(args[2]);
                    if (r != Role.WARRIOR && r != Role.PEASANT) throw new IllegalStateException("Rôle autorisé: WARRIOR ou PEASANT.");
                    clans.setRole(p, t, r);
                    p.sendMessage(Msg.ok("Rôle mis à jour: " + t.getName() + " -> " + r));
                    t.sendMessage(Msg.info("Ton rôle de clan est maintenant: " + r));
                }
                case "setking" -> {
                    if (args.length < 2) throw new IllegalStateException("Usage: /clan setking <joueur>");
                    Player t = Bukkit.getPlayerExact(args[1]);
                    if (t == null) throw new IllegalStateException("Joueur introuvable/ hors-ligne.");
                    clans.setKing(p, t);
                    p.sendMessage(Msg.ok("Nouveau Roi: " + t.getName()));
                    t.sendMessage(Msg.ok("Tu es maintenant le ROI du clan."));
                }
                case "setjarl" -> {
                    if (args.length < 2) throw new IllegalStateException("Usage: /clan setjarl <joueur>");
                    Player t = Bukkit.getPlayerExact(args[1]);
                    if (t == null) throw new IllegalStateException("Joueur introuvable/ hors-ligne.");
                    clans.setJarl(p, t);
                    p.sendMessage(Msg.ok("Nouveau Jarl: " + t.getName()));
                    t.sendMessage(Msg.ok("Tu es maintenant le JARL (chef militaire)."));
                }
                case "info" -> {
                    var c = (args.length >= 2) ? clans.getClanByName(args[1]) : clans.getClanOf(p.getUniqueId());
                    if (c == null) throw new IllegalStateException("Clan introuvable.");
                    p.sendMessage(Msg.info("Clan: " + c.getDisplayName()));
                    p.sendMessage(Msg.info("Membres: " + c.size()));
                    p.sendMessage(Msg.info("Roi: " + nameOrUnknown(c.getKing())));
                    p.sendMessage(Msg.info("Jarl: " + nameOrUnknown(c.getJarl())));
                }
                case "list" -> {
                    p.sendMessage(Msg.info("Clans: " + String.join(", ", clans.clanNames())));
                }
                default -> p.sendMessage(Msg.err("Commande inconnue. /clan help"));
            }

            // autosave léger
            clans.saveAll();
            return true;

        } catch (Exception e) {
            p.sendMessage(Msg.err(e.getMessage() == null ? "Erreur." : e.getMessage()));
            return true;
        }
    }

    private String nameOrUnknown(UUID uuid) {
        if (uuid == null) return "—";
        var off = Bukkit.getOfflinePlayer(uuid);
        return off.getName() == null ? uuid.toString() : off.getName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("help","create","disband","invite","join","leave","kick","role","setking","setjarl","info","list"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("invite") || sub.equals("kick") || sub.equals("role") || sub.equals("setking") || sub.equals("setjarl")) {
                return null; // laisse le client suggérer les joueurs
            }
            if (sub.equals("info")) return filter(clans.clanNames(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("role")) {
            return filter(List.of("WARRIOR","PEASANT"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String start) {
        String s = start.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.toLowerCase(Locale.ROOT).startsWith(s)) out.add(o);
        return out;
    }
}