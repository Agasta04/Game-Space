package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.shape.ArcType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Latar Belakang Luar Angkasa 3D Hyperspace Warp dengan Nebula Kosmik yang berubah sesuai Level.
 */
public class SpaceBackground3D {

    private static class Star3D {
        double x;
        double y;
        double z;
        double pz; // Posisi Z frame sebelumnya untuk efek warp streaks
        Color color;

        Star3D(double x, double y, double z, Color color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.pz = z;
            this.color = color;
        }

        void reset(Random rand) {
            this.x = (rand.nextDouble() * 2.0 - 1.0) * 1400.0;
            this.y = (rand.nextDouble() * 2.0 - 1.0) * 1000.0;
            this.z = 1750.0 + rand.nextDouble() * 200.0;
            this.pz = this.z;
        }
    }

    private final List<Star3D> stars = new ArrayList<>();
    private final int numStars = 220;
    private final Random random = new Random();

    private double warpSpeed = 16.0;
    private double targetWarpSpeed = 16.0;
    private boolean inWarpTransition = false;
    private double shakeMagnitude = 0.0;
    private double nebulaAnim = 0.0;

    private Color currentNebula1 = Color.web("#0d47a1");
    private Color currentNebula2 = Color.web("#00bcd4");

    public SpaceBackground3D() {
        for (int i = 0; i < numStars; i++) {
            Color c = (random.nextDouble() < 0.3) ? Color.web("#80d8ff") : (random.nextDouble() < 0.6 ? Color.web("#fff9c4") : Color.WHITE);
            Star3D star = new Star3D(
                    (random.nextDouble() * 2.0 - 1.0) * 1400.0,
                    (random.nextDouble() * 2.0 - 1.0) * 1000.0,
                    random.nextDouble() * 1800.0,
                    c
            );
            stars.add(star);
        }
    }

    public void setLevelColors(Color neb1, Color neb2) {
        this.currentNebula1 = neb1;
        this.currentNebula2 = neb2;
    }

    public void triggerScreenShake(double intensity) {
        this.shakeMagnitude = Math.max(this.shakeMagnitude, intensity);
    }

    public void triggerWarpJump(double durationSeconds) {
        this.inWarpTransition = true;
        this.targetWarpSpeed = 120.0; // Kecepatan hyperspace warp!
    }

    public void stopWarpJump() {
        this.inWarpTransition = false;
        this.targetWarpSpeed = 16.0;
    }

    public double getShakeX() {
        if (shakeMagnitude <= 0.1) return 0;
        return (random.nextDouble() * 2.0 - 1.0) * shakeMagnitude;
    }

    public double getShakeY() {
        if (shakeMagnitude <= 0.1) return 0;
        return (random.nextDouble() * 2.0 - 1.0) * shakeMagnitude;
    }

    public void update() {
        nebulaAnim += 0.008;

        // Interpolasi kecepatan warp
        warpSpeed += (targetWarpSpeed - warpSpeed) * 0.08;

        for (Star3D star : stars) {
            star.pz = star.z;
            star.z -= warpSpeed;
            if (star.z <= 25.0) {
                star.reset(random);
            }
        }

        if (shakeMagnitude > 0) {
            shakeMagnitude *= 0.88;
            if (shakeMagnitude < 0.2) shakeMagnitude = 0;
        }
    }

    public void render(GraphicsContext gc, Camera3D camera) {
        double width = SpaceShooter.WIDTH;
        double height = SpaceShooter.HEIGHT;

        // 1. Langit Ruang Hampa Dalam
        gc.setFill(Color.web("#030611"));
        gc.fillRect(0, 0, width, height);

        // 2. Kabut Nebula Kosmik Bergaya Halus Sesuai Level
        double neb1X = width * 0.3 + Math.sin(nebulaAnim) * 40;
        double neb1Y = height * 0.4 + Math.cos(nebulaAnim) * 30;
        RadialGradient nebGrad1 = new RadialGradient(
                0, 0, neb1X, neb1Y, 360, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(currentNebula1.getRed(), currentNebula1.getGreen(), currentNebula1.getBlue(), 0.32)),
                new Stop(0.6, Color.color(currentNebula1.getRed(), currentNebula1.getGreen(), currentNebula1.getBlue(), 0.12)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(nebGrad1);
        gc.fillOval(neb1X - 360, neb1Y - 360, 720, 720);

        double neb2X = width * 0.75 - Math.cos(nebulaAnim * 0.8) * 45;
        double neb2Y = height * 0.65 + Math.sin(nebulaAnim * 0.8) * 35;
        RadialGradient nebGrad2 = new RadialGradient(
                0, 0, neb2X, neb2Y, 320, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(currentNebula2.getRed(), currentNebula2.getGreen(), currentNebula2.getBlue(), 0.26)),
                new Stop(0.6, Color.color(currentNebula2.getRed(), currentNebula2.getGreen(), currentNebula2.getBlue(), 0.08)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(nebGrad2);
        gc.fillOval(neb2X - 320, neb2Y - 320, 640, 640);

        // 3. Garis Bintang 3D Hyperspace Warp
        for (Star3D star : stars) {
            double sx = camera.projectX(star.x, star.z);
            double sy = camera.projectY(star.y, star.z);

            if (sx < -20 || sx > width + 20 || sy < -20 || sy > height + 20) continue;

            double scale = camera.getScale(star.z);

            if (warpSpeed > 25.0) {
                // Efek Hyperspace Warp Streaks memanjang
                double prevSx = camera.projectX(star.x, star.pz);
                double prevSy = camera.projectY(star.y, star.pz);

                gc.setStroke(star.color);
                gc.setLineWidth(Math.max(1.0, 3.5 * scale));
                gc.strokeLine(sx, sy, prevSx, prevSy);
            } else {
                // Bintang 3D normal dengan kilau kedalaman
                double dotSize = Math.max(1.2, 4.0 * scale);
                double alpha = Math.min(1.0, Math.max(0.2, scale * 1.5));
                gc.setFill(Color.color(star.color.getRed(), star.color.getGreen(), star.color.getBlue(), alpha));
                gc.fillOval(sx - dotSize / 2.0, sy - dotSize / 2.0, dotSize, dotSize);
            }
        }
    }
}
