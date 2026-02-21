@EventHandler
public void onChat(AsyncPlayerChatEvent e) {
    Player p = e.getPlayer();
    UUID pu = p.getUniqueId();
    String content = e.getMessage();

    // Si pas en clan chat -> on laisse passer le chat normal
    if (!clans.isInClanChat(pu)) return;

    // Sinon on bloque le chat global
    e.setCancelled(true);

    // Envoi clan chat en sync
    Bukkit.getScheduler().runTask(plugin, () -> {
        UUID clanId = clans.getClanOf(pu);
        if (clanId == null) {
            p.sendMessage(Msg.error("Tu n'es pas dans un clan."));
            return;
        }

        Set<UUID> recipients = clans.sharedChatRecipients(pu);
        Component msg = Msg.info("[Clan] " + p.getName() + " » " + content);

        for (UUID u : recipients) {
            Player t = Bukkit.getPlayer(u);
            if (t != null && t.isOnline()) {
                t.sendMessage(msg);
            }
        }
    });
}
