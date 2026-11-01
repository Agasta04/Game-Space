package com.example.spaceshootergamejavafx;

import java.io.Serializable;

/**
 * Menyimpan data skor seorang pemain pada leaderboard.
 */
public class LeaderboardEntry implements Serializable, Comparable<LeaderboardEntry> {
    private static final long serialVersionUID = 1L;

    private final String playerName;
    private final int score;
    private final int galonCount;
    private final String timestamp;

    public LeaderboardEntry(String playerName, int score, int galonCount, String timestamp) {
        this.playerName = (playerName == null || playerName.trim().isEmpty()) ? "Astronaut" : playerName.trim();
        this.score = score;
        this.galonCount = galonCount;
        this.timestamp = timestamp;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public int getGalonCount() {
        return galonCount;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(LeaderboardEntry other) {
        // Peringkat utama berdasarkan jumlah galon terbanyak yang dikumpulkan
        if (other.galonCount != this.galonCount) {
            return Integer.compare(other.galonCount, this.galonCount);
        }
        return Integer.compare(other.score, this.score);
    }
}

