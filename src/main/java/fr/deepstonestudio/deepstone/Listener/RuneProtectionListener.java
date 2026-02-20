package fr.deepstonestudio.deepstone.Listener;

import fr.deepstonestudio.deepstone.Manager.RuneProtectionManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class RuneProtectionListener implements Listener {

    private final RuneProtectionManager runes;

    public RuneProtectionListener(RuneProtectionManager runes) {
        this.runes = runes;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();

        // ❌ Casser une rune
        if (runes.isRune(b)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§7[§c!§7] §cTu ne peux pas casser cette rune.");
            return;
        }

        // ❌ Casser le bloc support (si une rune est au-dessus)
        if (runes.isRune(b.getRelative(0, 1, 0))) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§7[§c!§7] §cTu ne peux pas casser ce bloc tant qu'une rune est au-dessus.");
        }
    }

    // ✅ Explosions: on retire les blocs protégés de la liste
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> runes.isProtectedBlock(b));
    }

    // ✅ Pistons: on annule si un bloc protégé serait déplacé
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(runes::isProtectedBlock)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream().anyMatch(runes::isProtectedBlock)) {
            e.setCancelled(true);
        }
    }
}