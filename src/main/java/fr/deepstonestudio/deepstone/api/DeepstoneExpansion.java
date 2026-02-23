package fr.deepstonestudio.deepstone.api;

import fr.deepstonestudio.deepstone.Deepstone;
import fr.deepstonestudio.deepstone.api.AFK.AfkService;
import fr.deepstonestudio.deepstone.model.clan.Clan;
import fr.deepstonestudio.deepstone.util.ClanService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DeepstoneExpansion extends PlaceholderExpansion {

    private final Deepstone plugin;
    private final AfkService afkService;
    private final ClanService clanService;
    private final EssentialsHook essentialsHook; // nullable

    public DeepstoneExpansion(Deepstone plugin, AfkService afkService, ClanService clanService, EssentialsHook essentialsHook) {
        this.plugin = plugin;
        this.afkService = afkService;
        this.clanService = clanService;
        this.essentialsHook = essentialsHook;
    }

    @Override public @NotNull String getIdentifier() { return "deepstone"; }
    @Override public @NotNull String getAuthor() { return "Deepstone"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        // NOTE: PlaceholderAPI peut appeler avec player = null selon le contexte
        // (console, placeholders globaux, etc.)
        if (params == null || params.isEmpty()) return "";

        // -----------------------------
        // ONLINE LIST / COUNT
        // -----------------------------

        // %deepstone_online_count%
        if (params.equalsIgnoreCase("online_count")) {
            return String.valueOf(Bukkit.getOnlinePlayers().size());
        }

        // %deepstone_online_1% ... %deepstone_online_40%
        // (trié par nom pour que l'ordre bouge moins)
        if (params.toLowerCase().startsWith("online_")) {
            int idx;
            try {
                idx = Integer.parseInt(params.substring("online_".length()));
            } catch (Exception ignored) {
                return "";
            }

            if (idx < 1) return "";

            List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
            online.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

            if (idx > online.size()) return "";
            return online.get(idx - 1).getName();
        }

        // Si pas de joueur (ou placeholder contextuel), on laisse passer les placeholders non liés au joueur
        if (player == null) return null;

        // -----------------------------
        // AFK
        // -----------------------------

        // %deepstone_afk%
        if (params.equalsIgnoreCase("afk")) {
            if (!player.isOnline()) return "";
            Player p = player.getPlayer();
            if (p == null) return "";

            boolean afk = (essentialsHook != null)
                    ? essentialsHook.isAfk(p.getUniqueId())
                    : afkService.isAfk(p);

            return afk ? "§7[AFK]" : "";
        }

        // -----------------------------
        // CLAN
        // -----------------------------

        //
        if (params.equalsIgnoreCase("clan")) {
            Clan clan = clanService.getClanOf(player.getUniqueId());
            return clan != null ? clan.getDisplayName() : "";
        }

        // %deepstone_clan_id%
        if (params.equalsIgnoreCase("clan_id")) {
            Clan clan = clanService.getClanOf(player.getUniqueId());
            return clan != null ? clan.getName() : "";
        }

        return null; // placeholder inconnu
    }
}