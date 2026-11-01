package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.shape.ArcType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Background Luar Angkasa Parallax Multi-Layer dengan Nebula Kosmik dan Efek Bintang Berkilau.
 */
public class SpaceBackground {

    private static class Star {
        double x;
        double y;
        double speed;
        double size;
        double twinkleSpeed;
        double brightness;
        Color color;

        Star(double x, double y, double speed, double size, double twinkleSpeed, Color color) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.size = size;
            this.twinkleSpeed = twinkleSpeed;
            this.brightness = Math.random();
            this.color = color;
        }

        void update(double width, double height) {
            y += speed;
            if (y > height) {
                y = 0;
                x = Math.random() * width;
            }
            brightness += twinkleSpeed;
        }

        void render(GraphicsContext gc) {
            double alpha = 0.4 + 0.6 * (0.5 + 0.5 * Math.sin(brightness));
            gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            gc.fillOval(x - size / 2.0, y - size / 2.0, size, size);
        }
    }

    private final List<Star> stars = new ArrayList<>();
    private final int width;
    private final int height;
    private double nebulaAnim = 0;
    private final Random random = new Random();

    // Screen Shake effect
    private double shakeMagnitude = 0;

    public SpaceBackground(int width, int height) {
        this.width = width;
        this.height = height;

        // Layer 1: Bintang jauh (kecil, lambat, banyak)
        for (int i = 0; i < 90; i++) {
            stars.add(new Star(
                    random.nextDouble() * width,
                    random.nextDouble() * height,
                    0.35 + random.nextDouble() * 0.4,
                    1.2,
                    0.02 + random.nextDouble() * 0.04,
                    Color.web("#b0bec5")
            ));
        }

        // Layer 2: Bintang sedang (berwarna biru/cyan/kuning)
        for (int i = 0; i < 45; i++) {
            Color starCol = (random.nextDouble() < 0.5) ? Color.web("#80d8ff") : Color.web("#fff9c4");
            stars.add(new Star(
                    random.nextDouble() * width,
                    random.nextDouble() * height,
                    0.9 + random.nextDouble() * 0.8,
                    2.2,
                    0.05 + random.nextDouble() * 0.06,
                    starCol
            ));
        }

        // Layer 3: Bintang dekat (cepat, besar, berkilau)
        for (int i = 0; i < 20; i++) {
            stars.add(new Star(
                    random.nextDouble() * width,
                    random.nextDouble() * height,
                    2.2 + random.nextDouble() * 1.5,
                    3.5,
                    0.08 + random.nextDouble() * 0.08,
                    Color.web("#ffffff")
            ));
        }
    }

    public void triggerScreenShake(double intensity) {
        this.shakeMagnitude = Math.max(this.shakeMagnitude, intensity);
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

        for (Star star : stars) {
            star.update(width, height);
        }

        if (shakeMagnitude > 0) {
            shakeMagnitude *= 0.88; // Redam getaran
            if (shakeMagnitude < 0.2) shakeMagnitude = 0;
        }
    }

    public void render(GraphicsContext gc) {
        // --- 1. LATAR BELAKANG DEEP SPACE GELAP ---
        gc.setFill(Color.web("#050814"));
        gc.fillRect(0, 0, width, height);

        // --- 2. NEBULA KOSMIK BERCAHAYA (RADIAL CLOUDS) ---
        // Nebula Ungu Nebula 1
        double neb1X = width * 0.25 + Math.sin(nebulaAnim) * 30;
        double neb1Y = height * 0.35 + Math.cos(nebulaAnim) * 20;
        RadialGradient nebula1 = new RadialGradient(
                0, 0, neb1X, neb1Y, 320, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0.35, 0.05, 0.55, 0.28)),
                new Stop(0.5, Color.color(0.15, 0.02, 0.35, 0.14)),
                new Stop(1.0, Color.color(0.0, 0.0, 0.0, 0.0))
        );
        gc.setFill(nebula1);
        gc.fillOval(neb1X - 320, neb1Y - 320, 640, 640);

        // Nebula Cyan Kosmik 2
        double neb2X = width * 0.75 - Math.cos(nebulaAnim * 0.7) * 40;
        double neb2Y = height * 0.65 + Math.sin(nebulaAnim * 0.7) * 30;
        RadialGradient nebula2 = new RadialGradient(
                0, 0, neb2X, neb2Y, 280, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0.0, 0.35, 0.65, 0.25)),
                new Stop(0.5, Color.color(0.0, 0.15, 0.35, 0.12)),
                new Stop(1.0, Color.color(0.0, 0.0, 0.0, 0.0))
        );
        gc.setFill(nebula2);
        gc.fillOval(neb2X - 280, neb2Y - 280, 560, 560);

        // Planet Kosmik Berputar di Latar Belakang Jauh (Dekoratif)
        double planetX = width * 0.85;
        double planetY = height * 0.18;
        RadialGradient planetGrad = new RadialGradient(
                -45, 0.3, planetX, planetY, 45, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#80cbc4")),
                new Stop(0.4, Color.web("#00695c")),
                new Stop(0.9, Color.web("#004d40")),
                new Stop(1.0, Color.color(0.0, 0.1, 0.1, 0.0))
        );
        gc.setFill(planetGrad);
        gc.fillOval(planetX - 45, planetY - 45, 90, 90);

        // Cincin Planet
        gc.setStroke(Color.color(0.7, 0.9, 0.9, 0.3));
        gc.setLineWidth(3);
        gc.strokeArc(planetX - 70, planetY - 18, 140, 36, 170, 200, ArcType.OPEN);

        // --- 3. BINTANG-BINTANG PARALLAX ---
        for (Star star : stars) {
            star.render(gc);
        }
    }
}
