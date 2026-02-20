package fr.deepstonestudio.deepstone.Manager;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RuneProtectionManager {

    // On stocke les emplacements des runes posées
    private final Set<Location> runeBlocks = ConcurrentHashMap.newKeySet();

    public void markRune(Block block) {
        runeBlocks.add(block.getLocation());
    }

    public void unmarkRune(Block block) {
        runeBlocks.remove(block.getLocation());
    }

    public boolean isRune(Block block) {
        return runeBlocks.contains(block.getLocation());
    }

    /** Le bloc est protégé si c'est une rune OU si une rune est au-dessus */
    public boolean isProtectedBlock(Block block) {
        if (isRune(block)) return true;
        return isRune(block.getRelative(0, 1, 0)); // bloc support (rune au-dessus)
    }

    public Set<Location> getAllRunesView() {
        return Collections.unmodifiableSet(runeBlocks);
    }
}