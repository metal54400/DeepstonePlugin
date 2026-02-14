package fr.deepstonestudio.deepstone.util;


import fr.deepstonestudio.deepstone.Deepstone;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RunePlacer {

    private final Deepstone plugin;
    private final Random rng = new Random();

    private final int radius = 15;
    private final int ringThickness = 1;
    private final int spokes = 8;
    private final Material runeMaterial = Material.REDSTONE_WIRE;

    // 5 minutes en ticks
    private final long despawnDelay = 6000L;

    public RunePlacer(Deepstone plugin) {
        this.plugin = plugin;
    }

    public void placeNordicDeath(Location deathLoc) {
        if (deathLoc == null || deathLoc.getWorld() == null) return;

        World world = deathLoc.getWorld();
        int cx = deathLoc.getBlockX();
        int cy = deathLoc.getBlockY();
        int cz = deathLoc.getBlockZ();

        Set<Block> placedBlocks = new HashSet<>();

        // Son + ambiance
        world.playSound(deathLoc, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.7f);
        world.spawnParticle(Particle.SOUL, deathLoc.clone().add(0, 1, 0), 35, 1.0, 0.5, 1.0, 0.02);

        int rOuter = radius;
        int rInner = Math.max(0, radius - ringThickness);
        int rOuter2 = rOuter * rOuter;
        int rInner2 = rInner * rInner;

        // ANNEAU
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {

                int d2 = x * x + z * z;
                if (d2 > rOuter2 || d2 < rInner2) continue;

                int bx = cx + x;
                int bz = cz + z;

                int groundY = findGroundY(world, bx, cy, bz);
                if (groundY == Integer.MIN_VALUE) continue;

                tryPlace(world, bx, groundY + 1, bz, placedBlocks);
            }
        }

        // RAYONS
        for (int i = 0; i < spokes; i++) {
            double angle = (Math.PI * 2.0) * i / spokes + (rng.nextDouble() * 0.25);

            for (int r = 2; r <= radius; r++) {
                int bx = cx + (int) Math.round(Math.cos(angle) * r);
                int bz = cz + (int) Math.round(Math.sin(angle) * r);

                int groundY = findGroundY(world, bx, cy, bz);
                if (groundY == Integer.MIN_VALUE) continue;

                if (rng.nextDouble() < 0.20) continue;

                tryPlace(world, bx, groundY + 1, bz, placedBlocks);
            }
        }

        // 🔥 SUPPRESSION APRÈS 5 MINUTES
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : placedBlocks) {
                    if (block.getType() == runeMaterial) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }.runTaskLater(plugin, despawnDelay);
    }

    private void tryPlace(World world, int x, int y, int z, Set<Block> placedBlocks) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) return;

        Block target = world.getBlockAt(x, y, z);
        Block below = world.getBlockAt(x, y - 1, z);

        if (target.getType().isAir() && below.getType().isSolid()) {
            target.setType(runeMaterial, false);
            placedBlocks.add(target);
        }
    }

    private int findGroundY(World world, int x, int startY, int z) {
        int y = Math.max(world.getMinHeight(), Math.min(startY, world.getMaxHeight() - 2));

        if (world.getBlockAt(x, y, z).getType().isSolid()) return y;

        for (int i = 0; i < 16 && y - 1 >= world.getMinHeight(); i++) {
            y--;
            if (world.getBlockAt(x, y, z).getType().isSolid()) return y;
        }

        return Integer.MIN_VALUE;
    }
}
