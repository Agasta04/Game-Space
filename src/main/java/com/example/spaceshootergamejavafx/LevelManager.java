package com.example.spaceshootergamejavafx;

import javafx.scene.paint.Color;

/**
 * Mengelola progres 5 Level permainan dari Orbit Bumi hingga Alien Mothership Core.
 */
public class LevelManager {
    public static final int MAX_LEVEL = 5;

    private static final LevelData[] LEVELS = new LevelData[] {
            new LevelData(
                    1,
                    "SEKTOR 1: ORBIT BUMI",
                    "Orbit Rendah - Mulai adaptasi perburuan galon di angkasa",
                    10,    // Target 10 galon
                    3.8,   // Kecepatan UFO
                    1.4,   // Interval spawn detik
                    Color.web("#0d47a1"),
                    Color.web("#00bcd4"),
                    false, // Tanpa asteroid
                    false  // Bukan level boss
            ),
            new LevelData(
                    2,
                    "SEKTOR 2: SABUK ASTEROID",
                    "Navigasi di antara puing meteorit dan armada UFO lincah",
                    15,    // Target 15 galon
                    4.4,
                    1.2,
                    Color.web("#3e2723"),
                    Color.web("#ff8f00"),
                    true,  // Muncul asteroid 3D
                    false
            ),
            new LevelData(
                    3,
                    "SEKTOR 3: CYBER NEBULA",
                    "Kabut kosmik ultraviolet, UFO alien mulai menembak laser",
                    20,    // Target 20 galon
                    5.0,
                    1.0,
                    Color.web("#4a148c"),
                    Color.web("#e91e63"),
                    true,
                    false
            ),
            new LevelData(
                    4,
                    "SEKTOR 4: BADAI SURYA",
                    "Radiasi matahari ekstrem, kecepatan armada alien maksimum",
                    25,    // Target 25 galon
                    5.8,
                    0.85,
                    Color.web("#bf360c"),
                    Color.web("#ffd600"),
                    true,
                    false
            ),
            new LevelData(
                    5,
                    "SEKTOR 5: ALIEN CORE",
                    "Duel final melawan UFO Mothership raksasa di pusat galaksi!",
                    35,    // Target 35 galon
                    6.4,
                    0.75,
                    Color.web("#311b92"),
                    Color.web("#00e5ff"),
                    true,
                    true   // Final Boss Mothership muncul!
            )
    };

    private int currentLevel = 1;

    public LevelData getCurrentLevelData() {
        return LEVELS[Math.min(currentLevel - 1, LEVELS.length - 1)];
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void reset() {
        currentLevel = 1;
    }

    public boolean canLevelUp(int levelGalonCollected) {
        if (currentLevel >= MAX_LEVEL) return false;
        return levelGalonCollected >= getCurrentLevelData().getTargetGalon();
    }

    public boolean advanceLevel() {
        if (currentLevel < MAX_LEVEL) {
            currentLevel++;
            return true;
        }
        return false;
    }

    public boolean isFinalLevel() {
        return currentLevel == MAX_LEVEL;
    }
}

