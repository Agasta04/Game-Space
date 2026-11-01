package com.example.spaceshootergamejavafx;

import javafx.scene.paint.Color;

/**
 * Data konfigurasi untuk setiap level permainan (Total 5 Level).
 */
public class LevelData {
    private final int levelNumber;
    private final String name;
    private final String subtitle;
    private final int targetGalon;
    private final double enemySpeed;
    private final double enemySpawnInterval;
    private final Color nebulaColor1;
    private final Color nebulaColor2;
    private final boolean spawnAsteroids;
    private final boolean isBossLevel;

    public LevelData(int levelNumber, String name, String subtitle, int targetGalon,
                     double enemySpeed, double enemySpawnInterval,
                     Color nebulaColor1, Color nebulaColor2,
                     boolean spawnAsteroids, boolean isBossLevel) {
        this.levelNumber = levelNumber;
        this.name = name;
        this.subtitle = subtitle;
        this.targetGalon = targetGalon;
        this.enemySpeed = enemySpeed;
        this.enemySpawnInterval = enemySpawnInterval;
        this.nebulaColor1 = nebulaColor1;
        this.nebulaColor2 = nebulaColor2;
        this.spawnAsteroids = spawnAsteroids;
        this.isBossLevel = isBossLevel;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public String getName() {
        return name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getTargetGalon() {
        return targetGalon;
    }

    public double getEnemySpeed() {
        return enemySpeed;
    }

    public double getEnemySpawnInterval() {
        return enemySpawnInterval;
    }

    public Color getNebulaColor1() {
        return nebulaColor1;
    }

    public Color getNebulaColor2() {
        return nebulaColor2;
    }

    public boolean isSpawnAsteroids() {
        return spawnAsteroids;
    }

    public boolean isBossLevel() {
        return isBossLevel;
    }
}

