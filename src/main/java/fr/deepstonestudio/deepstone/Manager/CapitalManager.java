package fr.deepstonestudio.deepstone.Manager;

import fr.deepstonestudio.deepstone.model.Clan;
import fr.deepstonestudio.deepstone.util.ClanService;
import fr.deepstonestudio.deepstone.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public final class CapitalManager implements Listener, Runnable {

    private final JavaPlugin plugin;
    private final ClanService clans;

    // Réglages
    private final int capitalRadius = 18;                 // zone de capitale
    private final int gloryLossOnDestroy = 25;            // perte si coeur détruit
    private final int captureSeconds = 20;                // temps de capture
    private final int captureBuffSeconds = 180;           // bonus temporaire
    private final int kingProtectionRadius = 10;          // autour du roi (dans capitale)
    private final double kingDamageMultiplier = 0.65;     // réduction dégâts reçus

    // Capture state par clan défendu
    private static final class CaptureState {
        String attackerClanId; // id du clan attaquant
        int progress;          // en secondes
        long lastTick;         // anti-spam
        long buffUntil;        // timestamp
        String buffOwnerClanId;
    }

    private final Map<String, CaptureState> states = new HashMap<>();

    public CapitalManager(JavaPlugin plugin, ClanService clans) {
        this.plugin = plugin;
        this.clans = clans;
    }

    public void start() {
        // tick 1 seconde
        Bukkit.getScheduler().runTaskTimer(plugin, this, 20L, 20L);
    }

    // -------------------------
    // Destruction du coeur
    // -------------------------
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player breaker = e.getPlayer();
        Clan breakerClan = clans.getClanOf(breaker.getUniqueId());

        for (Clan defended : clans.listClans()) {
            Clan.Capital cap = defended.getCapital();
            if (cap == null) continue;

            Location capLoc = cap.toLocation();
            if (capLoc == null) continue;

            if (sameBlock(e.getBlock().getLocation(), capLoc)) {
                // Interdire au clan propriétaire de casser son coeur (optionnel)
                if (breakerClan != null && breakerClan.getName().equalsIgnoreCase(defended.getName())) {
                    e.setCancelled(true);
                    breaker.sendMessage(Msg.error("Tu ne peux pas détruire le coeur de ta capitale."));
                    return;
                }

                // coeur détruit => perte de gloire + capitale supprimée
                defended.removeGlory(gloryLossOnDestroy);
                defended.setCapital(null);

                Bukkit.broadcastMessage(String.valueOf(Msg.error("🔥 La capitale de " + defended.getDisplayName() + " a été détruite ! (-" + gloryLossOnDestroy + " gloire)")));
                breaker.playSound(breaker.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 1f);

                // reset capture state
                states.remove(defended.getName());

                try { clans.saveAll(); } catch (Exception ignored) {}
                return;
            }
        }
    }

    // -------------------------
    // Protection spéciale du Roi (dans la capitale)
    // -------------------------
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player damager)) return;

        Clan vc = clans.getClanOf(victim.getUniqueId());
        if (vc == null) return;
        if (vc.getKing() == null || !vc.getKing().equals(victim.getUniqueId())) return;

        Clan.Capital cap = vc.getCapital();
        if (cap == null) return;

        Location capLoc = cap.toLocation();
        if (capLoc == null) return;

        // protection uniquement si le roi est dans la zone de capitale
        if (!isInRadius(victim.getLocation(), capLoc, capitalRadius)) return;

        // si attaquant allié / même clan => (tu as déjà un système alliés ailleurs)
        // ici on applique juste réduction dégâts sur le roi
        e.setDamage(e.getDamage() * kingDamageMultiplier);
    }

    // -------------------------
    // Tick: capture + buff temporaire + aura roi
    // -------------------------
    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (Clan defended : clans.listClans()) {
            Clan.Capital cap = defended.getCapital();
            if (cap == null) continue;

            Location capLoc = cap.toLocation();
            if (capLoc == null) continue;

            // Capture state
            CaptureState st = states.computeIfAbsent(defended.getName(), k -> new CaptureState());

            // 1) déterminer qui est dans la zone
            Set<String> presentEnemyClans = new HashSet<>();
            boolean defenderPresent = false;

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!isInRadius(p.getLocation(), capLoc, capitalRadius)) continue;
                Clan pc = clans.getClanOf(p.getUniqueId());
                if (pc == null) continue;

                if (pc.getName().equalsIgnoreCase(defended.getName())) {
                    defenderPresent = true;
                } else {
                    // ennemi = pas allié + pas même clan
                    if (clans.isEnemy(p.getUniqueId(), uuidOfAnyMember(defended))) {
                        presentEnemyClans.add(pc.getName());
                    }
                }
            }

            // 2) si buff actif pour un clan, l’entretenir (simple: message + rien d’autre)
            if (st.buffUntil > now) {
                // bonus temporaire: regen/force aux membres du clan buffOwner quand ils sont dans la capitale (RP "bénédiction de pillage")
                Clan owner = clans.getClanByName(st.buffOwnerClanId);
                if (owner != null) {
                    for (UUID u : owner.getMembers().keySet()) {
                        Player p = Bukkit.getPlayer(u);
                        if (p == null) continue;
                        if (!isInRadius(p.getLocation(), capLoc, capitalRadius)) continue;
                        p.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 40, 0, true, false, true));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false, true));
                    }
                }
            } else {
                st.buffOwnerClanId = null;
                st.buffUntil = 0;
            }

            // 3) aura de protection du roi (si roi présent dans zone)
            if (defended.getKing() != null) {
                Player king = Bukkit.getPlayer(defended.getKing());
                if (king != null && isInRadius(king.getLocation(), capLoc, capitalRadius)) {
                    // Roi: résistance + absorption légère, et buff aux proches (option)
                    king.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, false, true));
                    king.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 60, 0, true, false, true));

                    for (UUID u : defended.getMembers().keySet()) {
                        Player m = Bukkit.getPlayer(u);
                        if (m == null) continue;
                        if (m.getLocation().distanceSquared(king.getLocation()) > (kingProtectionRadius * kingProtectionRadius)) continue;
                        m.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, true, false, true));
                    }
                }
            }

            // 4) capture logic
            if (presentEnemyClans.isEmpty() || defenderPresent) {
                // défenseurs présents ou pas d’ennemis => la capture retombe
                if (st.progress > 0) st.progress = Math.max(0, st.progress - 2);
                st.attackerClanId = null;
                continue;
            }

            // si plusieurs clans ennemis => pas de capture (ils se disputent)
            if (presentEnemyClans.size() != 1) {
                if (st.progress > 0) st.progress = Math.max(0, st.progress - 2);
                st.attackerClanId = null;
                continue;
            }

            String attackerId = presentEnemyClans.iterator().next();

            // si nouvel attaquant, reset progress
            if (st.attackerClanId == null || !st.attackerClanId.equalsIgnoreCase(attackerId)) {
                st.attackerClanId = attackerId;
                st.progress = 0;
            }

            // progression 1 sec par tick
            st.progress++;

            // feedback léger au clan défendu (toutes les 5 sec)
            if (st.progress % 5 == 0) {
                broadcastToClan(defended, String.valueOf(Msg.error("⚠ Capitale attaquée ! Capture: " + st.progress + "/" + captureSeconds + " sec")));
            }

            if (st.progress >= captureSeconds) {
                // CAPTURE !
                Clan attacker = clans.getClanByName(attackerId);
                if (attacker != null) {
                    st.buffOwnerClanId = attacker.getName();
                    st.buffUntil = now + captureBuffSeconds * 1000L;

                    Bukkit.broadcastMessage(String.valueOf(Msg.ok("🏴 " + attacker.getDisplayName() + " a capturé la capitale de " + defended.getDisplayName() +
                            " ! Bonus " + (captureBuffSeconds) + "s.")));
                }
                st.progress = 0;
                st.attackerClanId = null;
            }
        }
    }

    private void broadcastToClan(Clan c, String msg) {
        for (UUID u : c.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(msg);
        }
    }

    private boolean isInRadius(Location a, Location b, int r) {
        if (a == null || b == null) return false;
        if (a.getWorld() == null || b.getWorld() == null) return false;
        if (!a.getWorld().getName().equals(b.getWorld().getName())) return false;
        return a.distanceSquared(b) <= (r * r);
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null) return false;
        if (a.getWorld() == null || b.getWorld() == null) return false;
        if (!a.getWorld().getName().equals(b.getWorld().getName())) return false;
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    // hack simple pour isEnemy(): on prend un membre au hasard du clan défendu
    private UUID uuidOfAnyMember(Clan defended) {
        if (defended.getMembers().isEmpty()) return new UUID(0, 0);
        return defended.getMembers().keySet().iterator().next();
    }
}