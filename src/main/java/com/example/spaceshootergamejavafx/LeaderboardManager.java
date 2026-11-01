package com.example.spaceshootergamejavafx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Mengelola penyimpanan dan pembacaan Leaderboard.
 */
public class LeaderboardManager {
    private static final String FILE_NAME = "leaderboard.txt";
    private static final int MAX_ENTRIES = 10;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final List<LeaderboardEntry> entries = new ArrayList<>();

    public LeaderboardManager() {
        load();
    }

    public synchronized List<LeaderboardEntry> getTopEntries() {
        return new ArrayList<>(entries);
    }

    public synchronized void addScore(String playerName, int score, int galonCount) {
        String now = DATE_FORMAT.format(new Date());
        LeaderboardEntry entry = new LeaderboardEntry(playerName, score, galonCount, now);
        entries.add(entry);
        Collections.sort(entries);
        if (entries.size() > MAX_ENTRIES) {
            entries.subList(MAX_ENTRIES, entries.size()).clear();
        }
        save();
    }

    private synchronized void load() {
        entries.clear();
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            // Berikan skor default awal agar leaderboard tidak kosong
            entries.add(new LeaderboardEntry("Captain Nova", 1500, 15, DATE_FORMAT.format(new Date())));
            entries.add(new LeaderboardEntry("Galaxy Ace", 1000, 10, DATE_FORMAT.format(new Date())));
            entries.add(new LeaderboardEntry("Star Ranger", 600, 6, DATE_FORMAT.format(new Date())));
            save();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(";;;");
                if (parts.length >= 4) {
                    String name = parts[0];
                    int score = Integer.parseInt(parts[1]);
                    int galon = Integer.parseInt(parts[2]);
                    String timestamp = parts[3];
                    entries.add(new LeaderboardEntry(name, score, galon, timestamp));
                }
            }
            Collections.sort(entries);
            if (entries.size() > MAX_ENTRIES) {
                entries.subList(MAX_ENTRIES, entries.size()).clear();
            }
        } catch (Exception e) {
            System.err.println("Gagal membaca leaderboard: " + e.getMessage());
        }
    }

    private synchronized void save() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FILE_NAME), StandardCharsets.UTF_8))) {
            writer.println("# Space Shooter Leaderboard: Name;;;Score;;;GalonCount;;;Date");
            for (LeaderboardEntry entry : entries) {
                writer.println(entry.getPlayerName() + ";;;" + entry.getScore() + ";;;" + entry.getGalonCount() + ";;;" + entry.getTimestamp());
            }
        } catch (Exception e) {
            System.err.println("Gagal menyimpan leaderboard: " + e.getMessage());
        }
    }
}

