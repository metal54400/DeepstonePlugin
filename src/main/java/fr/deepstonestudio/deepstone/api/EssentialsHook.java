package fr.deepstonestudio.deepstone.api;

import net.ess3.api.IEssentials;
import net.ess3.api.IUser;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class EssentialsHook {
    private final IEssentials essentials;

    private EssentialsHook(IEssentials essentials) {
        this.essentials = essentials;
    }

    public static EssentialsHook tryCreate(Plugin essentialsPlugin) {
        if (essentialsPlugin instanceof IEssentials ie) {
            return new EssentialsHook(ie);
        }
        return null;
    }

    public boolean isAfk(UUID uuid) {
        IUser user = essentials.getUser(uuid);
        return user != null && user.isAfk();
    }
}