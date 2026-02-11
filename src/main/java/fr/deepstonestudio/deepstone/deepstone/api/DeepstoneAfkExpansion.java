package fr.deepstonestudio.deepstone.api;

import fr.deepstonestudio.deepstone.Deepstone;
import fr.deepstonestudio.deepstone.api.AFK.AfkService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DeepstoneAfkExpansion extends PlaceholderExpansion {

    private final Deepstone plugin;
    private final AfkService afkService;
    private final EssentialsHook essentialsHook; // nullable

    public DeepstoneAfkExpansion(Deepstone plugin, AfkService afkService, EssentialsHook essentialsHook) {
        this.plugin = plugin;
        this.afkService = afkService;
        this.essentialsHook = essentialsHook;
    }

    @Override public @NotNull String getIdentifier() { return "deepstone"; }
    @Override public @NotNull String getAuthor() { return "Deepstone"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (!params.equalsIgnoreCase("afk")) return null;
        if (player == null || !player.isOnline()) return "";

        Player p = player.getPlayer();
        if (p == null) return "";

        boolean afk = (essentialsHook != null)
                ? essentialsHook.isAfk(p.getUniqueId())
                : afkService.isAfk(p);

        return afk ? "§7[AFK]" : "";
    }
}