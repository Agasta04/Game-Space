package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.*;

import java.io.InputStream;
import java.util.Random;

/**
 * Musuh UFO Alien Saucer 3D yang terbang dari horizon jauh mendekati kamera di lintasan arena.
 * Menggunakan tekstur 3D definisi tinggi dengan kubah cockpit alien, lampu putar LED, dan tractor beam.
 */
public class UfoEnemy3D extends Entity3D {

    private static Image ufoImg;

    static {
        try {
            InputStream is = UfoEnemy3D.class.getResourceAsStream("/ufo_3d.png");
            if (is != null) {
                ufoImg = new Image(is);
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat UFO 3D: " + e.getMessage());
        }
    }

    private final double speedZ;
    private double animAngle;
    private final double swayAmplitude;
    private final Random random = new Random();

    public UfoEnemy3D(double x, double y, double z, double speedZ) {
        super(x, y, z, 36.0);
        this.speedZ = speedZ;
        this.animAngle = Math.random() * Math.PI * 2;
        this.swayAmplitude = 0.8 + Math.random() * 1.5;
    }

    @Override
    public void update() {
        z -= speedZ;
        animAngle += 0.08;

        // Gerakan meliuk horizontal di lintasan
        x += Math.sin(animAngle) * swayAmplitude;

        // Jika sudah melewati kamera ke belakang pemain
        if (z < 25) {
            dead = true; // Lolos dengan damai tanpa merusak pemain
        }
    }

    public void spawnExplosion(java.util.List<Particle> particles, Camera3D camera) {
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y, this.z);
        double scale = camera.getScale(this.z);
        for (int i = 0; i < 24; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double spd = (1.5 + random.nextDouble() * 5.5);
            double vx = Math.cos(angle) * spd;
            double vy = Math.sin(angle) * spd;
            Color c = (random.nextDouble() < 0.5) ? Color.web("#d500f9") : (random.nextDouble() < 0.8 ? Color.web("#00e5ff") : Color.WHITE);
            particles.add(new Particle(sx, sy, vx, vy, (4 + random.nextDouble() * 5) * scale, 0.45, c));
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera3D camera) {
        if (z <= 25) return;

        double scale = camera.getScale(this.z);
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y, this.z);

        if (sx < -100 || sx > SpaceShooter.WIDTH + 100 || sy < -100 || sy > SpaceShooter.HEIGHT + 100) return;

        double ufoW = 88 * scale;
        double ufoH = 52 * scale;

        gc.save();

        // 1. Bayangan 3D di Lantai Grid Kosmik (Y = 190)
        double floorY = 190.0;
        double shadowX = camera.projectX(this.x, this.z);
        double shadowY = camera.projectY(floorY, this.z);
        double shadowW = ufoW * 0.95;
        double shadowH = ufoH * 0.32;

        RadialGradient shadowGrad = new RadialGradient(
                0, 0, shadowX, shadowY, shadowW / 2.0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0, 0, 0, 0.55)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(shadowGrad);
        gc.fillOval(shadowX - shadowW / 2.0, shadowY - shadowH / 2.0, shadowW, shadowH);

        // 2. Traktor Beam / Aura Energi Piring Bawah
        RadialGradient tractorGlow = new RadialGradient(
                0, 0, sx, sy + (6 * scale), ufoW * 0.65, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.6, 0.0, 1.0, 0.40)),
                new Stop(0.7, Color.color(0.2, 0.9, 1.0, 0.15)),
                new Stop(1, Color.color(0, 0, 0, 0))
        );
        gc.setFill(tractorGlow);
        gc.fillOval(sx - ufoW * 0.65, sy, ufoW * 1.3, ufoH * 1.1);

        // 3. Tekstur Piring Terbang UFO 3D HD
        if (ufoImg != null) {
            double drawX = sx - ufoW / 2.0;
            double drawY = sy - ufoH / 2.0;
            gc.drawImage(ufoImg, drawX, drawY, ufoW, ufoH);

            // Lampu LED Berputar di Keliling Piring
            int numLeds = 6;
            for (int i = 0; i < numLeds; i++) {
                double angle = animAngle * 2.0 + (i * Math.PI * 2 / numLeds);
                double ledX = sx + Math.cos(angle) * (ufoW * 0.38);
                double ledY = sy + (ufoH * 0.22) + Math.sin(angle) * (ufoH * 0.10);

                Color ledColor = (i % 2 == 0) ? Color.web("#d500f9") : Color.web("#00e5ff");
                gc.setFill(ledColor);
                gc.fillOval(ledX - 2.5 * scale, ledY - 2.5 * scale, 5.0 * scale, 5.0 * scale);
            }
        } else {
            // Fallback render
            renderFallbackUfo(gc, sx, sy, ufoW, ufoH, scale);
        }

        gc.restore();
    }

    private void renderFallbackUfo(GraphicsContext gc, double sx, double sy, double ufoW, double ufoH, double scale) {
        gc.setFill(Color.web("#9c27b0"));
        gc.fillOval(sx - ufoW / 2.0, sy - ufoH / 2.0, ufoW, ufoH);
    }
}
