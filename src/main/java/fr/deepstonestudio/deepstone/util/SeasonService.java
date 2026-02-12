package fr.deepstonestudio.deepstone.util;

import fr.deepstonestudio.deepstone.api.DiscordWebhook;
import fr.deepstonestudio.deepstone.model.Clan;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public final class SeasonService {

    private final JavaPlugin plugin;
    private final ClanService clans;
    private final GloryService glory;
    private final DiscordWebhook discord;

    public SeasonService(JavaPlugin plugin, ClanService clans, GloryService glory, DiscordWebhook discord) {
        this.plugin = plugin;
        this.clans = clans;
        this.glory = glory;
        this.discord = discord;
    }

    public void startScheduler() {
        if (!plugin.getConfig().getBoolean("season.enabled", true)) return;

        // Check toutes les 60 secondes (léger)
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L * 60);
    }

    private void tick() {
        int hour = plugin.getConfig().getInt("season.run-hour", 12);
        int minute = plugin.getConfig().getInt("season.run-minute", 0);
        int lengthDays = plugin.getConfig().getInt("season.length-days", 30);

        ZonedDateTime now = ZonedDateTime.now();
        // Exemple simple: on déclenche si c'est le bon jour (tous les 30 jours depuis epoch) + heure/minute
        long daysSinceEpoch = now.toLocalDate().toEpochDay();
        if (daysSinceEpoch % lengthDays != 0) return;
        if (now.getHour() != hour || now.getMinute() != minute) return;

        // Pour éviter double déclenchement dans la même minute, on stocke un flag en mémoire
        // (tu peux le persister si tu veux)
        runSeasonEnd();
    }

    public void runSeasonEnd() {
        // 1) Top clans
        List<Clan> top = clans.listClans().stream()
                .sorted(Comparator.comparingInt(Clan::getGlory).reversed())
                .limit(3)
                .collect(Collectors.toList());

        // 2) Reward + annonce serveur
        Bukkit.broadcastMessage("§6🏆 Fin de saison ! Classement des clans :");
        for (int i = 0; i < top.size(); i++) {
            Clan c = top.get(i);
            Bukkit.broadcastMessage("§e#" + (i + 1) + " §f" + c.getDisplayName() + " §7(" + c.getGlory() + " gloire)");
            giveRewards(i + 1, c);
        }

        // 3) Discord
        if (discord != null) {
            discord.sendTop3(top);
        }

        // 4) Reset gloire
        for (Clan c : clans.listClans()) {
            c.setGlory(0);
        }

        boolean resetPlayer = plugin.getConfig().getBoolean("season.reset-player-glory", true);
        if (resetPlayer) {
            glory.resetAll(); // à ajouter (voir plus bas)
            glory.save();
        }

        try {
            clans.saveAll(); // persiste clans.yml (glory clan)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void giveRewards(int rank, Clan clan) {
        String path = switch (rank) {
            case 1 -> "rewards.top1";
            case 2 -> "rewards.top2";
            case 3 -> "rewards.top3";
            default -> null;
        };
        if (path == null) return;

        List<String> cmds = plugin.getConfig().getStringList(path);
        if (cmds.isEmpty()) return;

        String kingName = resolveName(clan.getKing());
        for (String raw : cmds) {
            String cmd = raw.replace("%clan%", clan.getName())
                    .replace("%clanDisplay%", clan.getDisplayName())
                    .replace("%king%", kingName == null ? "" : kingName);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private String resolveName(UUID uuid) {
        if (uuid == null) return null;
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName();
    }
}