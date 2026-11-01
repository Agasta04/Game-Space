package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Latar Belakang Luar Angkasa 3D Epik:
 * Menampilkan Planet Magma Vulkanik Raksasa di Horizon (persis referensi foto),
 * Grid Kosmik Berkedalaman 3D, Nebula Mengambang, dan Bintang Hyperspace Warp.
 */
public class SpaceBackground3D {

    private static Image volcanicPlanetImg;

    static {
        try {
            InputStream is = SpaceBackground3D.class.getResourceAsStream("/space_volcanic_planet.jpg");
            if (is != null) {
                volcanicPlanetImg = new Image(is);
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat latar planet: " + e.getMessage());
        }
    }

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
            this.x = (rand.nextDouble() * 2.0 - 1.0) * 1600.0;
            this.y = -600.0 + rand.nextDouble() * 700.0;
            this.z = 1750.0 + rand.nextDouble() * 200.0;
            this.pz = this.z;
        }
    }

    private final List<Star3D> stars = new ArrayList<>();
    private final int numStars = 220;
    private final Random random = new Random();

    private double warpSpeed = 18.0;
    private double targetWarpSpeed = 18.0;
    private double gridOffset = 0.0;
    private double shakeMagnitude = 0.0;
    private double animPulse = 0.0;

    private Color currentNebula1 = Color.web("#ff6f00"); // Nuansa Api Vulkanik
    private Color currentNebula2 = Color.web("#00e5ff"); // Cyan Kontras

    public SpaceBackground3D() {
        for (int i = 0; i < numStars; i++) {
            Color c = (random.nextDouble() < 0.35)
                    ? Color.web("#80d8ff")
                    : (random.nextDouble() < 0.65 ? Color.web("#ffe082") : Color.WHITE);
            Star3D star = new Star3D(
                    (random.nextDouble() * 2.0 - 1.0) * 1600.0,
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
        this.targetWarpSpeed = 120.0;
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
        animPulse += 0.02;
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

        // 1. Langit Luar Angkasa Deep Space
        gc.setFill(Color.web("#030712"));
        gc.fillRect(0, 0, width, height);

        // 2. RENDER PLANET MAGMA VULKANIK RAKSASA DI HORIZON (PERSIS REFERENSI FOTO)
        renderVolcanicPlanetHorizon(gc, width, height, horizonY, camera);

        // 3. PERSPECTIVE COSMIC GRID DI LANTAI RUANG ANGKASA (KUNCI KEDALAMAN 3D)
        renderCosmicPerspectiveGrid(gc, camera, width, height, horizonY);

        // 4. Bintang-Bintang 3D Hyperspace
        renderStars(gc, camera, width, height);
    }

    private void renderVolcanicPlanetHorizon(GraphicsContext gc, double width, double height, double horizonY, Camera3D camera) {
        gc.save();

        // Parallax lembut mengikuti kamera
        double parallaxX = -camera.getX() * 0.04;
        double parallaxY = -camera.getY() * 0.02;

        if (volcanicPlanetImg != null) {
            // Gambar Planet Menutupi Bagian Tengah dan Atas Horizon
            double imgW = width * 1.08;
            double imgH = height * 0.88;
            double imgX = (width - imgW) / 2.0 + parallaxX;
            double imgY = -imgH * 0.18 + parallaxY;

            gc.drawImage(volcanicPlanetImg, imgX, imgY, imgW, imgH);

            // Lapisan Gradien Atmosfer Pijar Oranye Mengalir
            RadialGradient coronaGrad = new RadialGradient(
                    0, 0, width / 2.0 + parallaxX, horizonY - 40 + parallaxY, width * 0.55, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.color(1.0, 0.45, 0.0, 0.25)),
                    new Stop(0.65, Color.color(0.9, 0.2, 0.0, 0.10)),
                    new Stop(1.0, Color.color(0, 0, 0, 0))
            );
            gc.setFill(coronaGrad);
            gc.fillRect(0, 0, width, height);
        } else {
            // Procedural Volcanic Sun/Planet fallback
            double planetR = width * 0.45;
            double planetCx = width / 2.0 + parallaxX;
            double planetCy = horizonY + parallaxY - 20;

            RadialGradient sunGrad = new RadialGradient(
                    0, 0, planetCx, planetCy, planetR, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#fff3e0")),
                    new Stop(0.25, Color.web("#ff9800")),
                    new Stop(0.65, Color.web("#d84315")),
                    new Stop(0.9, Color.web("#bf360c")),
                    new Stop(1.0, Color.color(0.1, 0, 0, 0))
            );
            gc.setFill(sunGrad);
            gc.fillOval(planetCx - planetR, planetCy - planetR, planetR * 2, planetR * 2);
        }

        gc.restore();
    }

    private void renderCosmicPerspectiveGrid(GraphicsContext gc, Camera3D camera, double width, double height, double horizonY) {
        double floorY = 220.0;

        // Garis Horizon bercahaya
        LinearGradient horizGlow = new LinearGradient(
                0, horizonY - 12, 0, horizonY + 24, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0, 0, 0, 0)),
                new Stop(0.4, Color.color(currentNebula2.getRed(), currentNebula2.getGreen(), currentNebula2.getBlue(), 0.6)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(horizGlow);
        gc.fillRect(0, horizonY - 12, width, 36);

        // Garis Bujur Membujur ke Horizon
        gc.setLineWidth(1.3);
        for (int i = -11; i <= 11; i++) {
            double worldX = i * 85.0;
            double nearX = camera.projectX(worldX, 60.0);
            double farX = camera.projectX(worldX, 1500.0);
            double nearY = camera.projectY(floorY, 60.0);
            double farY = camera.projectY(floorY, 1500.0);

            double alpha = Math.max(0.08, 0.40 - Math.abs(i) * 0.025);
            gc.setStroke(Color.color(currentNebula2.getRed(), currentNebula2.getGreen(), currentNebula2.getBlue(), alpha));
            gc.strokeLine(nearX, nearY, farX, farY);
        }

        // Garis Lintang Melintang Meluncur Mundur
        int numLats = 14;
        for (int j = 0; j < numLats; j++) {
            double fraction = (j + gridOffset) / numLats;
            double z = 70.0 + Math.pow(fraction, 2.2) * 1400.0;
            if (z < 70.0 || z > 1500.0) continue;

            double sy = camera.projectY(floorY, z);
            double leftX = camera.projectX(-880.0, z);
            double rightX = camera.projectX(880.0, z);

            double alpha = (1.0 - fraction) * 0.42;
            gc.setStroke(Color.color(currentNebula2.getRed(), currentNebula2.getGreen(), currentNebula2.getBlue(), alpha));
            gc.strokeLine(leftX, sy, rightX, sy);
        }
    }

    private void renderStars(GraphicsContext gc, Camera3D camera, double width, double height) {
        for (Star3D star : stars) {
            double sx = camera.projectX(star.x, star.z);
            double sy = camera.projectY(star.y, star.z);

            if (sx < -20 || sx > width + 20 || sy < -20 || sy > height + 20) continue;

            double scale = camera.getScale(star.z);

            if (warpSpeed > 25.0) {
                double prevSx = camera.projectX(star.x, star.pz);
                double prevSy = camera.projectY(star.y, star.pz);
                gc.setStroke(star.color);
                gc.setLineWidth(Math.max(1.0, 3.8 * scale));
                gc.strokeLine(sx, sy, prevSx, prevSy);
            } else {
                double dotSize = Math.max(1.2, 4.2 * scale);
                double alpha = Math.min(1.0, Math.max(0.25, scale * 1.8));
                gc.setFill(Color.color(star.color.getRed(), star.color.getGreen(), star.color.getBlue(), alpha));
                gc.fillOval(sx - dotSize / 2.0, sy - dotSize / 2.0, dotSize, dotSize);
            }
        }
    }
}
