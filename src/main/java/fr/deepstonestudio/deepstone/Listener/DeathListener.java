package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.Deepstone;
import fr.deepstonestudio.deepstone.util.RunePlacer;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {
    private final RunePlacer runePlacer;
    private final Deepstone plugin;
    public DeathListener(Deepstone plugin) {
        this.runePlacer = new RunePlacer(plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("runes.enabled", true)) return;

        // Seulement si tué par un joueur
        if (plugin.getConfig().getBoolean("runes.require-player-kill", true)) {
            Player killer = event.getEntity().getKiller();
            if (killer == null) return;
        }

        // Option: seulement mobs hostiles
        if (plugin.getConfig().getBoolean("runes.only-hostile-mobs", true)) {
            if (!(event.getEntity() instanceof Monster)) return;
        }

        Location loc = event.getEntity().getLocation();
        if (loc.getWorld() == null) return;

        // GriefPrevention checks
        boolean blockAdmin = plugin.getConfig().getBoolean("runes.griefprevention.block-admin-claims", true);
        boolean blockPlayer = plugin.getConfig().getBoolean("runes.griefprevention.block-player-claims", false);

        if (blockAdmin || blockPlayer) {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(loc, true, null);
            if (claim != null) {
                if (claim.isAdminClaim() && blockAdmin) return;
                if (!claim.isAdminClaim() && blockPlayer) return;
            }
        }

        // ✅ Place la rune
        runePlacer.placeNordicDeath(loc);
    }
}
