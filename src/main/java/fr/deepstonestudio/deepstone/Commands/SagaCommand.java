package fr.deepstonestudio.deepstone.Commands;


import fr.deepstonestudio.deepstone.saga.SagaChapters;
import fr.deepstonestudio.deepstone.storage.SagaStore;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SagaCommand implements CommandExecutor {

    private final SagaStore store;

    public SagaCommand(SagaStore store) {
        this.store = store;
    }

    private static String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private void sendChapter(Player p, int chapterIndex) {
        chapterIndex = Math.max(0, Math.min(chapterIndex, SagaChapters.maxChapterIndex()));

        List<String> lines = SagaChapters.CHAPTERS.get(chapterIndex);

        p.sendMessage(c("&8"));
        for (String line : lines) p.sendMessage(c(line));
        p.sendMessage(c("&8"));
        p.sendMessage(c("&7Chapitre: &e" + (chapterIndex + 1) + "&7/&e" + (SagaChapters.maxChapterIndex() + 1)
                + " &8| &a/saga next &8| &c/saga prev &8| &7/saga chapter <num>"));

        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        int current = store.getChapter(p.getUniqueId());

        if (args.length == 0) {
            sendChapter(p, current);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start", "reset" -> {
                store.setChapter(p.getUniqueId(), 0);
                store.saveFile();
                sendChapter(p, 0);
            }
            case "next" -> {
                int next = Math.min(current + 1, SagaChapters.maxChapterIndex());
                store.setChapter(p.getUniqueId(), next);
                store.saveFile();
                sendChapter(p, next);
            }
            case "prev" -> {
                int prev = Math.max(current - 1, 0);
                store.setChapter(p.getUniqueId(), prev);
                store.saveFile();
                sendChapter(p, prev);
            }
            case "chapter" -> {
                if (args.length < 2) {
                    p.sendMessage(c("&cUtilise: &7/saga chapter <num>"));
                    return true;
                }
                int n;
                try { n = Integer.parseInt(args[1]); }
                catch (NumberFormatException e) {
                    p.sendMessage(c("&cNombre invalide."));
                    return true;
                }
                int idx = Math.max(0, Math.min(n - 1, SagaChapters.maxChapterIndex()));
                store.setChapter(p.getUniqueId(), idx);
                store.saveFile();
                sendChapter(p, idx);
            }
            default -> p.sendMessage(c("&cUtilise: &7/saga &8| &7/saga next&8|&7prev&8|&7start&8|&7chapter <num>&8|&7reset"));
        }

        return true;
    }
}