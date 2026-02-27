package fr.deepstonestudio.deepstone.model.monitoring;

import java.util.HashMap;
import java.util.Map;

public class Stats {

    public Map<String, Integer> monthlyJoins = new HashMap<>();
    public int totalJoins = 0;
    public double tps = 20.0;
    public int onlinePlayers = 0;
    public long uptime = 0;
    public long lastUpdate = System.currentTimeMillis();
}