package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

import java.util.List;
import java.util.Random;

/**
 * Musuh UFO Alien Saucer 3D yang terbang dari horizon jauh mendekati kamera.
 */
public class UfoEnemy3D extends Entity3D {
    private final double speedZ;
    private double animAngle;
    private final double swayAmplitude;
    private final double swaySpeed;
    private final Random random = new Random();

    public UfoEnemy3D(double x, double y, double z, double speedZ) {
        super(x, y, z, 32.0);
        this.speedZ = speedZ;
        this.animAngle = Math.random() * Math.PI * 2;
        this.swayAmplitude = 0.8 + Math.random() * 1.4;
        this.swaySpeed = 0.04 + Math.random() * 0.03;
    }

    @Override
    public void update() {
        z -= speedZ;
        animAngle += 0.08;

        // Gerakan meliuk halus di angkasa
        x += Math.sin(animAngle) * swayAmplitude;
        y += Math.cos(animAngle * 0.7) * (swayAmplitude * 0.5);

        // Jika sudah melewati kamera ke belakang pemain
        if (z < 25) {
            dead = true; // Lolos dengan damai tanpa merusak pemain
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera3D camera) {
        if (z <= 25) return;

        double scale = camera.getScale(this.z);
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y, this.z);

        // Jangan render jika di luar layar
        if (sx < -100 || sx > SpaceShooter.WIDTH + 100 || sy < -100 || sy > SpaceShooter.HEIGHT + 100) return;

        double ufoW = 72 * scale;
        double ufoH = 40 * scale;

        gc.save();

        // 1. Aura Traktor Beam Bawah
        RadialGradient tractorGlow = new RadialGradient(
                0, 0, sx, sy + (4 * scale), ufoW * 0.65, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.2, 1.0, 0.4, 0.35)),
                new Stop(1, Color.color(0, 0, 0, 0))
        );
        gc.setFill(tractorGlow);
        gc.fillOval(sx - ufoW * 0.65, sy, ufoW * 1.3, ufoH);

        // 2. Kubah Kaca Alien 3D (Cockpit Dome)
        double domeW = ufoW * 0.48;
        double domeH = ufoH * 0.65;
        double domeX = sx - domeW / 2.0;
        double domeY = sy - ufoH * 0.45;

        LinearGradient domeGrad = new LinearGradient(
                domeX, domeY, domeX + domeW, domeY + domeH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#69f0ae")),
                new Stop(0.5, Color.web("#00bfa5")),
                new Stop(1.0, Color.web("#004d40"))
        );
        gc.setFill(domeGrad);
        gc.fillOval(domeX, domeY, domeW, domeH);

        // Alien Pilot di dalam kubah (tampak jika cukup dekat)
        if (scale > 0.4) {
            gc.setFill(Color.web("#76ff03"));
            gc.fillOval(sx - 4 * scale, domeY + 4 * scale, 8 * scale, 8 * scale);
            gc.setFill(Color.BLACK);
            gc.fillOval(sx - 3 * scale, domeY + 6 * scale, 2.5 * scale, 3 * scale);
            gc.fillOval(sx + 0.5 * scale, domeY + 6 * scale, 2.5 * scale, 3 * scale);
        }

        // Pantulan Kaca Kubah
        gc.setFill(Color.color(1, 1, 1, 0.7));
        gc.fillOval(domeX + 3 * scale, domeY + 2 * scale, domeW * 0.35, 4 * scale);

        // 3. Piring Logam Utama UFO 3D
        LinearGradient saucerGrad = new LinearGradient(
                sx - ufoW / 2.0, sy - ufoH / 2.0, sx + ufoW / 2.0, sy + ufoH / 2.0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#cfd8dc")),
                new Stop(0.3, Color.web("#90a4ae")),
                new Stop(0.7, Color.web("#455a64")),
                new Stop(1.0, Color.web("#212121"))
        );
        gc.setFill(saucerGrad);
        gc.fillOval(sx - ufoW / 2.0, sy - ufoH * 0.2, ufoW, ufoH * 0.7);

        // Rim Cincin Logam
        gc.setStroke(Color.web("#eceff1"));
        gc.setLineWidth(1.2 * scale);
        gc.strokeOval(sx - ufoW / 2.0, sy - ufoH * 0.2, ufoW, ufoH * 0.7);

        // 4. Lampu LED Berputar 3D di Tepian Piring
        int numLights = 7;
        for (int i = 0; i < numLights; i++) {
            double angle = animAngle + (i * (2 * Math.PI / numLights));
            double lx = sx + Math.cos(angle) * (ufoW * 0.44);
            double ly = sy + 3 * scale + Math.sin(angle) * (ufoH * 0.18);

            Color lightCol = (i % 2 == 0) ? Color.web("#ffea00") : Color.web("#ff3d00");
            gc.setFill(lightCol);
            double lSize = 4.5 * scale;
            gc.fillOval(lx - lSize / 2.0, ly - lSize / 2.0, lSize, lSize);
        }

        gc.restore();
    }

    public void spawnExplosion(List<Particle> particles, Camera3D camera) {
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y, this.z);
        double scale = camera.getScale(this.z);

        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double spd = 2.0 + random.nextDouble() * 5.0;
            Color col = (random.nextDouble() < 0.4) ? Color.web("#ff3d00") : Color.web("#ffea00");
            particles.add(new Particle(sx, sy, Math.cos(angle) * spd, Math.sin(angle) * spd, (4 + random.nextDouble() * 4) * scale, 0.45, col));
        }
    }
}

