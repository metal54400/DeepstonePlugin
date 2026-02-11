package fr.deepstonestudio.deepstone.util;

import fr.deepstonestudio.deepstone.model.War;

import java.util.*;

public class WarService {

    private final Map<String, War> pendingWars = new HashMap<>();
    private final List<War> activeWars = new ArrayList<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    private static final long WAR_DURATION = 1000L * 60 * 60; // 1h
    private static final long WAR_COOLDOWN = 1000L * 60 * 30; // 30min

    public boolean isInWar(String clan) {
        return getWar(clan) != null;
    }

    public War getWar(String clan) {
        return activeWars.stream()
                .filter(w -> w.isActive() && w.involves(clan))
                .findFirst()
                .orElse(null);
    }

    public War declare(String attacker, String target) {
        if (cooldowns.containsKey(attacker) &&
                System.currentTimeMillis() < cooldowns.get(attacker)) {
            throw new IllegalStateException("Cooldown actif.");
        }

        War war = new War(attacker, target);
        pendingWars.put(target, war);
        return war;
    }

    public War accept(String clan) {
        War war = pendingWars.remove(clan);
        if (war == null) return null;

        war.accept();
        activeWars.add(war);
        return war;
    }

    public void endWar(War war) {
        war.end();
        activeWars.remove(war);
        cooldowns.put(war.getClanA(), System.currentTimeMillis() + WAR_COOLDOWN);
        cooldowns.put(war.getClanB(), System.currentTimeMillis() + WAR_COOLDOWN);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (Iterator<War> it = activeWars.iterator(); it.hasNext();) {
            War war = it.next();
            if (now - war.getStartTime() > WAR_DURATION) {
                war.end();
                it.remove();
            }
        }
    }
}
