package fr.deepstonestudio.deepstone.model;

public class War {

    private final String clanA;
    private final String clanB;
    private final long startTime;
    private boolean active;
    private boolean accepted;

    public War(String clanA, String clanB) {
        this.clanA = clanA;
        this.clanB = clanB;
        this.startTime = System.currentTimeMillis();
        this.active = false;
        this.accepted = false;
    }

    public String getClanA() { return clanA; }
    public String getClanB() { return clanB; }

    public boolean isActive() { return active; }
    public boolean isAccepted() { return accepted; }

    public long getStartTime() { return startTime; }

    public void accept() {
        this.accepted = true;
        this.active = true;
    }

    public void end() {
        this.active = false;
    }

    public boolean involves(String clan) {
        return clan.equalsIgnoreCase(clanA) || clan.equalsIgnoreCase(clanB);
    }

    public String getEnemy(String clan) {
        return clan.equalsIgnoreCase(clanA) ? clanB : clanA;
    }
}