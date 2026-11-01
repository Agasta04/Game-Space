package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Random;

/**
 * Boss Alien Mothership Raksasa 3D yang mengambang di kedalaman horizon Sektor 5.
 */
public class BossMothership3D extends Entity3D {
    private int health = 30;
    private final int maxHealth = 30;
    private double animAngle = 0;
    private double moveSpeedX = 2.2;
    private final Random random = new Random();

    public BossMothership3D(double x, double y, double z) {
        super(x, y, z, 90.0);
    }

    public int getHealth() {
        return health;
    }

    public void takeDamage() {
        health--;
        if (health <= 0) {
            dead = true;
        }
    }

    @Override
    public void update() {
        animAngle += 0.04;

        // Bergerak melayang bolak-balik di horizon
        x += moveSpeedX;
        if (x < -240) {
            moveSpeedX = Math.abs(moveSpeedX);
        } else if (x > 240) {
            moveSpeedX = -Math.abs(moveSpeedX);
        }

        // Perlahan bergerak mendekat sedikit jika masih jauh
        if (z > 620) {
            z -= 1.0;
        }
    }

    public void shoot(List<EnemyPlasma3D> enemyPlasmas) {
        if (random.nextDouble() < 0.04) {
            // Tembakan plasma ganda dari meriam kiri dan kanan
            enemyPlasmas.add(new EnemyPlasma3D(this.x - 70, this.y + 10, this.z - 20, (random.nextDouble() - 0.5) * 1.5, 0.4, 18.0));
            enemyPlasmas.add(new EnemyPlasma3D(this.x + 70, this.y + 10, this.z - 20, (random.nextDouble() - 0.5) * 1.5, 0.4, 18.0));
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera3D camera) {
        if (z <= 25) return;

        double scale = camera.getScale(this.z);
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y, this.z);

        double bw = 240 * scale;
        double bh = 110 * scale;

        gc.save();

        // 1. Aura Reaktor Kosmik Ungu
        RadialGradient bossAura = new RadialGradient(
                0, 0, sx, sy, bw * 0.75, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0.8, 0.1, 0.9, 0.45)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(bossAura);
        gc.fillOval(sx - bw * 0.75, sy - bh * 0.65, bw * 1.5, bh * 1.3);

        // 2. Sayap Meriam Berat Kiri & Kanan
        gc.setFill(Color.web("#212121"));
        gc.fillRoundRect(sx - bw * 0.48, sy + bh * 0.1, 24 * scale, 35 * scale, 6 * scale, 6 * scale);
        gc.fillRoundRect(sx + bw * 0.48 - 24 * scale, sy + bh * 0.1, 24 * scale, 35 * scale, 6 * scale, 6 * scale);
        // Moncong Plasma Merah
        gc.setFill(Color.web("#ff1744"));
        gc.fillOval(sx - bw * 0.48 + 4 * scale, sy + bh * 0.1 + 28 * scale, 16 * scale, 8 * scale);
        gc.fillOval(sx + bw * 0.48 - 20 * scale, sy + bh * 0.1 + 28 * scale, 16 * scale, 8 * scale);

        // 3. Badan Piring Induk Titanium Ungu
        LinearGradient hullGrad = new LinearGradient(
                sx - bw / 2.0, sy - bh / 2.0, sx + bw / 2.0, sy + bh / 2.0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#ab47bc")),
                new Stop(0.35, Color.web("#e1bee7")), // Highlight 3D logam
                new Stop(0.7, Color.web("#4a148c")),
                new Stop(1.0, Color.web("#120024"))
        );
        gc.setFill(hullGrad);
        gc.fillOval(sx - bw / 2.0, sy - bh * 0.35, bw, bh * 0.75);

        gc.setStroke(Color.web("#ce93d8"));
        gc.setLineWidth(2.5 * scale);
        gc.strokeOval(sx - bw / 2.0, sy - bh * 0.35, bw, bh * 0.75);

        // 4. Kubah Komando Alien Mothership (Command Bridge)
        double domeW = bw * 0.45;
        double domeH = bh * 0.55;
        double domeX = sx - domeW / 2.0;
        double domeY = sy - bh * 0.45;

        LinearGradient domeGrad = new LinearGradient(
                domeX, domeY, domeX + domeW, domeY + domeH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#00e5ff")),
                new Stop(0.5, Color.web("#7c4dff")),
                new Stop(1.0, Color.web("#311b92"))
        );
        gc.setFill(domeGrad);
        gc.fillOval(domeX, domeY, domeW, domeH);

        // 5. Cincin Lampu Plasma Berputar
        int lights = 10;
        for (int i = 0; i < lights; i++) {
            double angle = animAngle + i * (2 * Math.PI / lights);
            double lx = sx + Math.cos(angle) * (bw * 0.42);
            double ly = sy + Math.sin(angle) * (bh * 0.22);
            gc.setFill(Color.web("#00e5ff"));
            gc.fillOval(lx - 4 * scale, ly - 4 * scale, 8 * scale, 8 * scale);
            gc.setFill(Color.WHITE);
            gc.fillOval(lx - 1.5 * scale, ly - 1.5 * scale, 3 * scale, 3 * scale);
        }

        // 6. Health Bar Sci-Fi di Atas Mothership
        double barW = 160 * scale;
        double barH = 10 * scale;
        double barX = sx - barW / 2.0;
        double barY = sy - bh * 0.65;

        gc.setFill(Color.color(0, 0, 0, 0.75));
        gc.fillRoundRect(barX - 2, barY - 2, barW + 4, barH + 4, 6, 6);
        gc.setStroke(Color.web("#ab47bc"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(barX - 2, barY - 2, barW + 4, barH + 4, 6, 6);

        double hpRatio = Math.max(0, (double) health / maxHealth);
        LinearGradient hpGrad = new LinearGradient(
                barX, 0, barX + barW, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#ff1744")),
                new Stop(1.0, Color.web("#ffea00"))
        );
        gc.setFill(hpGrad);
        gc.fillRoundRect(barX, barY, barW * hpRatio, barH, 4, 4);

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(10, 13 * scale)));
        gc.setFill(Color.WHITE);
        gc.fillText("BOSS ALIEN MOTHERSHIP", barX + 4, barY - 4);

        gc.restore();
    }
}

