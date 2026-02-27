package fr.deepstonestudio.deepstone.model.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Stats.java
public class Stats {
    public double tps = 20.0;
    public int onlinePlayers = 0;
    public long uptime = 0; // en secondes
    public int totalJoins = 0;
    public Map<String,Integer> monthlyJoins = new HashMap<>();
    public long lastUpdate = 0;
    public List<Long> restarts = new ArrayList<>(); // <-- stocke timestamps des redémarrages
}