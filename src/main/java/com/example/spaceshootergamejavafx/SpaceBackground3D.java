package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Latar Belakang Luar Angkasa 3D Asli:
 * Menggunakan Deep Space Cosmic Void, Kabut Nebula Dinamis, Bintang Hyperspace Warp,
 * dan 3D Perspective Cosmic Grid Floor yang memberikan sensasi kedalaman ruang angkasa sejati.
 */
public class SpaceBackground3D {

    private static class Star3D {
        double x;
        double y;
        double z;
        double pz;
        Color color;

        Star3D(double x, double y, double z, Color color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.pz = z;
            this.color = color;
        }

        void reset(Random rand) {
            this.x = (rand.nextDouble() * 2.0 - 1.0) * 1500.0;
            this.y = -600.0 + rand.nextDouble() * 700.0;
            this.z = 1750.0 + rand.nextDouble() * 200.0;
            this.pz = this.z;
        }
    }

    private final List<Star3D> stars = new ArrayList<>();
    private final int numStars = 240;
    private final Random random = new Random();

    private double warpSpeed = 18.0;
    private double targetWarpSpeed = 18.0;
    private double gridOffset = 0.0;
    private double shakeMagnitude = 0.0;
    private double nebulaAnim = 0.0;

    private Color currentNebula1 = Color.web("#0d47a1");
    private Color currentNebula2 = Color.web("#00bcd4");

    public SpaceBackground3D() {
        for (int i = 0; i < numStars; i++) {
            Color c = (random.nextDouble() < 0.35)
                    ? Color.web("#80d8ff")
                    : (random.nextDouble() < 0.65 ? Color.web("#fff9c4") : Color.WHITE);
            Star3D star = new Star3D(
                    (random.nextDouble() * 2.0 - 1.0) * 1500.0,
                    -600.0 + random.nextDouble() * 700.0,
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
        this.targetWarpSpeed = 110.0;
    }

    public void stopWarpJump() {
        this.targetWarpSpeed = 18.0;
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
        warpSpeed += (targetWarpSpeed - warpSpeed) * 0.08;

        // Pergerakan lantai grid 3D kosmik
        gridOffset = (gridOffset + warpSpeed * 0.02) % 1.0;

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
        double horizonY = Camera3D.VANISH_Y;

        // 1. Langit Ruang Hampa Deep Space Asli
        gc.setFill(Color.web("#030713"));
        gc.fillRect(0, 0, width, height);

        // 2. Kabut Nebula Kosmik Mengambang Dinamis
        double neb1X = width * 0.25 + Math.sin(nebulaAnim) * 40;
        double neb1Y = height * 0.35 + Math.cos(nebulaAnim) * 30;
        RadialGradient nebGrad1 = new RadialGradient(
                0, 0, neb1X, neb1Y, 380, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(currentNebula1.getRed(), currentNebula1.getGreen(), currentNebula1.getBlue(), 0.35)),
                new Stop(0.6, Color.color(currentNebula1.getRed(), currentNebula1.getGreen(), currentNebula1.getBlue(), 0.10)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(nebGrad1);
        gc.fillOval(neb1X - 380, neb1Y - 380, 760, 760);

        double neb2X = width * 0.78 - Math.cos(nebulaAnim * 0.75) * 40;
        double neb2Y = height * 0.55 + Math.sin(nebulaAnim * 0.75) * 35;
        RadialGradient nebGrad2 = new RadialGradient(
                0, 0, neb2X, neb2Y, 320, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(currentNebula2.getRed(), currentNebula2.getGreen(), currentNebula2.getBlue(), 0.28)),
                new Stop(0.6, Color.color(currentNebula2.getRed(), currentNebula2.getGreen(), currentNebula2.getBlue(), 0.08)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(nebGrad2);
        gc.fillOval(neb2X - 320, neb2Y - 320, 640, 640);

        // 3. 3D PERSPECTIVE COSMIC GRID DI LANTAI RUANG ANGKASA (KUNCI KEDALAMAN 3D NYATA)
        renderCosmicPerspectiveGrid(gc, camera, width, height, horizonY);

        // 4. Bintang-Bintang 3D Hyperspace Warp
        for (Star3D star : stars) {
            double sx = camera.projectX(star.x, star.z);
            double sy = camera.projectY(star.y, star.z);

            if (sx < -20 || sx > width + 20 || sy < -20 || sy > height + 20) continue;

            double scale = camera.getScale(star.z);

            if (warpSpeed > 25.0) {
                double prevSx = camera.projectX(star.x, star.pz);
                double prevSy = camera.projectY(star.y, star.pz);
                gc.setStroke(star.color);
                gc.setLineWidth(Math.max(1.0, 3.5 * scale));
                gc.strokeLine(sx, sy, prevSx, prevSy);
            } else {
                double dotSize = Math.max(1.2, 4.2 * scale);
                double alpha = Math.min(1.0, Math.max(0.2, scale * 1.6));
                gc.setFill(Color.color(star.color.getRed(), star.color.getGreen(), star.color.getBlue(), alpha));
                gc.fillOval(sx - dotSize / 2.0, sy - dotSize / 2.0, dotSize, dotSize);
            }
        }
    }

    private void renderCosmicPerspectiveGrid(GraphicsContext gc, Camera3D camera, double width, double height, double horizonY) {
        double floorY = 220.0;

        // Garis Horizon bercahaya
        LinearGradient horizGlow = new LinearGradient(
                0, horizonY - 10, 0, horizonY + 20, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0, 0, 0, 0)),
                new Stop(0.4, Color.color(currentNebula2.getRed(), currentNebula2.getGreen(), currentNebula2.getBlue(), 0.5)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(horizGlow);
        gc.fillRect(0, horizonY - 10, width, 30);

        // Garis-garis Bujur Membujur ke Horizon (Longitudinal lines)
        gc.setLineWidth(1.2);
        for (int i = -10; i <= 10; i++) {
            double worldX = i * 85.0;
            double nearX = camera.projectX(worldX, 60.0);
            double farX = camera.projectX(worldX, 1500.0);
            double nearY = camera.projectY(floorY, 60.0);
            double farY = camera.projectY(floorY, 1500.0);

            double alpha = Math.max(0.08, 0.35 - Math.abs(i) * 0.025);
            gc.setStroke(Color.color(currentNebula2.getRed(), currentNebula2.getGreen(), currentNebula2.getBlue(), alpha));
            gc.strokeLine(nearX, nearY, farX, farY);
        }

        // Garis-garis Lintang Melintang (Latitudinal horizontal lines bergerak mundur)
        int numLats = 14;
        for (int j = 0; j < numLats; j++) {
            double fraction = (j + gridOffset) / numLats;
            double z = 70.0 + Math.pow(fraction, 2.2) * 1400.0;
            if (z < 70.0 || z > 1500.0) continue;

            double sy = camera.projectY(floorY, z);
            double leftX = camera.projectX(-850.0, z);
            double rightX = camera.projectX(850.0, z);

            double alpha = (1.0 - fraction) * 0.38;
            gc.setStroke(Color.color(currentNebula2.getRed(), currentNebula2.getGreen(), currentNebula2.getBlue(), alpha));
            gc.strokeLine(leftX, sy, rightX, sy);
        }
    }
}
