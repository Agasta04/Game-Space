package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Random;

/**
 * Boss Alien Mothership UFO Raksasa 3D dengan Meriam Laser Ganda dan Health Bar Sci-Fi.
 */
public class BossEnemy extends Enemy {

    public static final int BOSS_WIDTH = 110;
    public static final int BOSS_HEIGHT = 65;
    private int health = 15;
    private final int maxHealth = 15;
    private double horizontalSpeed = 2.4;
    private double animAngle = 0;
    private final Random random = new Random();

    public BossEnemy(double x, double y) {
        super(x, y);
        this.width = BOSS_WIDTH;
        this.height = BOSS_HEIGHT;
    }

    @Override
    public double getWidth() {
        return BOSS_WIDTH;
    }

    @Override
    public double getHeight() {
        return BOSS_HEIGHT;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void takeDamage() {
        health--;
        if (health <= 0) {
            setDead(true);
        }
    }

    @Override
    public void hit() {
        takeDamage();
    }

    @Override
    public void update() {
        animAngle += 0.06;

        // Turun perlahan sampai posisi tempur
        if (y < 90) {
            y += 1.2;
        }

        // Bergerak bolak-balik horizontal
        x += horizontalSpeed;
        if (x - BOSS_WIDTH / 2.0 <= 20) {
            horizontalSpeed = Math.abs(horizontalSpeed);
        } else if (x + BOSS_WIDTH / 2.0 >= SpaceShooter.WIDTH - 20) {
            horizontalSpeed = -Math.abs(horizontalSpeed);
        }
    }

    public void shoot(List<GameObject> newObjects) {
        // Peluang menembak laser dari kedua meriam
        if (random.nextDouble() < 0.035) {
            double cannonOffset = BOSS_WIDTH * 0.35;
            newObjects.add(new EnemyBullet(x - cannonOffset, y + BOSS_HEIGHT / 2.0));
            newObjects.add(new EnemyBullet(x + cannonOffset, y + BOSS_HEIGHT / 2.0));
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();

        double drawX = x - BOSS_WIDTH / 2.0;
        double drawY = y - BOSS_HEIGHT / 2.0;

        // --- 1. AURA ENERGI KOSMIK UNGU DI SEKITAR BOSS ---
        RadialGradient bossAura = new RadialGradient(
                0, 0, x, y + 8, BOSS_WIDTH * 0.7, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.8, 0.1, 0.9, 0.45)),
                new Stop(1, Color.color(0.4, 0.0, 0.5, 0.0))
        );
        gc.setFill(bossAura);
        gc.fillOval(x - BOSS_WIDTH * 0.7, y - 10, BOSS_WIDTH * 1.4, BOSS_HEIGHT * 1.2);

        // --- 2. MERIAM PLASMA KIRI & KANAN ---
        double cannonW = 12;
        double cannonH = 22;
        gc.setFill(Color.web("#212121"));
        gc.fillRoundRect(drawX + 10, drawY + BOSS_HEIGHT - 12, cannonW, cannonH, 4, 4);
        gc.fillRoundRect(drawX + BOSS_WIDTH - 10 - cannonW, drawY + BOSS_HEIGHT - 12, cannonW, cannonH, 4, 4);
        // Ujung laras plasma merah menyala
        gc.setFill(Color.web("#ff1744"));
        gc.fillOval(drawX + 11, drawY + BOSS_HEIGHT + 6, cannonW - 2, 5);
        gc.fillOval(drawX + BOSS_WIDTH - 9 - cannonW, drawY + BOSS_HEIGHT + 6, cannonW - 2, 5);

        // --- 3. BADAN UTAMA PIRING INDUK 3D (MOTHERSHIP HULL) ---
        LinearGradient hullGrad = new LinearGradient(
                drawX, drawY + 16, drawX, drawY + BOSS_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#9c27b0")), // Titanium violet armor
                new Stop(0.3, Color.web("#e1bee7")), // Specular highlight metallic
                new Stop(0.7, Color.web("#4a148c")),
                new Stop(1.0, Color.web("#1a0033"))
        );
        gc.setFill(hullGrad);
        gc.fillOval(drawX, drawY + 16, BOSS_WIDTH, BOSS_HEIGHT - 16);

        // Armor Plate Ring
        gc.setStroke(Color.web("#ba68c8"));
        gc.setLineWidth(2.2);
        gc.strokeOval(drawX, drawY + 16, BOSS_WIDTH, BOSS_HEIGHT - 16);

        // --- 4. KUBAH KOMANDO ALIEN BOSS (COMMAND BRIDGE DOME) ---
        double domeW = BOSS_WIDTH * 0.48;
        double domeH = BOSS_HEIGHT * 0.55;
        double domeX = x - domeW / 2.0;
        double domeY = drawY + 4;
        LinearGradient domeGrad = new LinearGradient(
                domeX, domeY, domeX + domeW, domeY + domeH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#ea80fc")),
                new Stop(0.5, Color.web("#aa00ff")),
                new Stop(1.0, Color.web("#311b92"))
        );
        gc.setFill(domeGrad);
        gc.fillOval(domeX, domeY, domeW, domeH);

        // Kilau kaca kubah komando
        gc.setFill(Color.color(1, 1, 1, 0.7));
        gc.fillOval(domeX + 6, domeY + 4, domeW * 0.4, 7);

        // --- 5. LAMPU PLASMA BERPUTAR DI SEKITAR BOSS ---
        int lightsCount = 10;
        for (int i = 0; i < lightsCount; i++) {
            double angle = animAngle + (i * (2 * Math.PI / lightsCount));
            double lX = x + Math.cos(angle) * (BOSS_WIDTH * 0.45);
            double lY = y + 15 + Math.sin(angle) * (BOSS_HEIGHT * 0.22);

            gc.setFill(Color.web("#00e5ff"));
            gc.fillOval(lX - 3.5, lY - 3, 7, 6);
            gc.setFill(Color.WHITE);
            gc.fillOval(lX - 1.5, lY - 1.5, 3, 3);
        }

        // --- 6. HEALTH BAR SCI-FI DI ATAS KEPALA BOSS ---
        double barW = 100;
        double barH = 8;
        double barX = x - barW / 2.0;
        double barY = drawY - 14;

        // Background bar gelap
        gc.setFill(Color.color(0, 0, 0, 0.7));
        gc.fillRoundRect(barX - 2, barY - 2, barW + 4, barH + 4, 6, 6);
        gc.setStroke(Color.web("#ab47bc"));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(barX - 2, barY - 2, barW + 4, barH + 4, 6, 6);

        // Progress bar darah
        double hpRatio = Math.max(0, (double) health / maxHealth);
        LinearGradient hpGrad = new LinearGradient(
                barX, 0, barX + barW, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#ff1744")),
                new Stop(1.0, Color.web("#ffea00"))
        );
        gc.setFill(hpGrad);
        gc.fillRoundRect(barX, barY, barW * hpRatio, barH, 4, 4);

        // Label teks Boss HP
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        gc.setFill(Color.WHITE);
        gc.fillText("BOSS UFO MOTHERSHIP", barX + 3, barY - 4);

        gc.restore();
    }

    @Override
    public void spawnExplosionParticles(List<Particle> particles) {
        // Ledakan spektakuler mega boss
        for (int i = 0; i < 70; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.5 + random.nextDouble() * 8.0;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            Color pColor = (random.nextDouble() < 0.4)
                    ? Color.web("#ea80fc")
                    : (random.nextDouble() < 0.7 ? Color.web("#ff1744") : Color.web("#ffea00"));
            particles.add(new Particle(x, y, vx, vy, 5 + random.nextDouble() * 7, 0.6 + random.nextDouble() * 0.5, pColor));
        }
    }
}
