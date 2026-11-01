package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

import java.util.List;
import java.util.Random;

/**
 * Musuh UFO (Piring Terbang Alien) 3D dengan Kubah Kaca, Alien Pilot, dan Lampu LED Berputar.
 */
public class Enemy extends GameObject {

    public static final int WIDTH = 48;
    public static final int HEIGHT = 32;
    public static double SPEED = 1.6;

    private boolean dead = false;
    private double animAngle = 0;
    private final double swayOffset;
    private final double swaySpeed;
    private final Random random = new Random();

    public Enemy(double x, double y) {
        super(x, y, WIDTH, HEIGHT);
        this.animAngle = Math.random() * Math.PI * 2;
        this.swayOffset = (Math.random() - 0.5) * 1.5;
        this.swaySpeed = 0.03 + Math.random() * 0.03;
    }

    @Override
    public void update() {
        y += SPEED;
        animAngle += 0.08;
        x += Math.sin(animAngle * 0.8) * swayOffset;

        // Cegah UFO keluar batas layar saat swaying
        if (x < WIDTH / 2.0) x = WIDTH / 2.0;
        if (x > SpaceShooter.WIDTH - WIDTH / 2.0) x = SpaceShooter.WIDTH - WIDTH / 2.0;
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();

        double drawX = x - WIDTH / 2.0;
        double drawY = y - HEIGHT / 2.0;

        // Aura energi UFO (Tractor Beam / Glow bawah piring)
        RadialGradient bottomBeam = new RadialGradient(
                0, 0, x, y + 4, WIDTH * 0.6, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.2, 1.0, 0.4, 0.4)),
                new Stop(1, Color.color(0.1, 0.8, 0.2, 0.0))
        );
        gc.setFill(bottomBeam);
        gc.fillOval(x - WIDTH * 0.6, y, WIDTH * 1.2, HEIGHT * 0.9);

        // --- 1. KUBAH KACA ALIEN (COCKPIT DOME) ---
        double domeW = WIDTH * 0.46;
        double domeH = HEIGHT * 0.55;
        double domeX = x - domeW / 2.0;
        double domeY = drawY + 2;

        // Kaca kubah hijau toska futuristik
        LinearGradient domeGrad = new LinearGradient(
                domeX, domeY, domeX + domeW, domeY + domeH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#69f0ae")),
                new Stop(0.4, Color.web("#00bfa5")),
                new Stop(1.0, Color.web("#004d40"))
        );
        gc.setFill(domeGrad);
        gc.fillOval(domeX, domeY, domeW, domeH);

        // Kepala Alien Kecil di dalam kubah
        gc.setFill(Color.web("#76ff03"));
        gc.fillOval(x - 5, domeY + 4, 10, 9);
        // Mata hitam besar alien
        gc.setFill(Color.BLACK);
        gc.fillOval(x - 4, domeY + 6, 3, 4);
        gc.fillOval(x + 1, domeY + 6, 3, 4);

        // Pantulan cahaya kubah kaca
        gc.setFill(Color.color(1, 1, 1, 0.75));
        gc.fillOval(domeX + 3, domeY + 2, domeW * 0.35, 4);

        // --- 2. PIRING LOGAM UTAMA UFO (SAUCER BODY 3D) ---
        LinearGradient saucerGrad = new LinearGradient(
                drawX, drawY + 10, drawX, drawY + HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#cfd8dc")),
                new Stop(0.3, Color.web("#90a4ae")),
                new Stop(0.7, Color.web("#455a64")),
                new Stop(1.0, Color.web("#263238"))
        );
        gc.setFill(saucerGrad);
        gc.fillOval(drawX, drawY + 10, WIDTH, HEIGHT - 10);

        // Rim / Cincin Logam Tengah dengan Shading Logam Berkilau
        gc.setStroke(Color.web("#b0bec5"));
        gc.setLineWidth(1.5);
        gc.strokeOval(drawX, drawY + 10, WIDTH, HEIGHT - 10);

        // --- 3. LAMPU LED BERPUTAR DI TEPI PIRING (ROTATING LIGHTS) ---
        int numLights = 6;
        for (int i = 0; i < numLights; i++) {
            double angle = animAngle + (i * (2 * Math.PI / numLights));
            double lightX = x + Math.cos(angle) * (WIDTH * 0.44);
            double lightY = y + 7 + Math.sin(angle) * (HEIGHT * 0.18);

            // Lampu berkedip oranye / cyan / kuning neon
            Color lightCol = (i % 2 == 0) ? Color.web("#ffea00") : Color.web("#ff3d00");
            gc.setFill(lightCol);
            gc.fillOval(lightX - 2.5, lightY - 2, 5, 4);

            // Kilau cahaya titik LED
            gc.setFill(Color.color(1, 1, 1, 0.8));
            gc.fillOval(lightX - 1, lightY - 1, 2, 2);
        }

        gc.restore();
    }

    /**
     * Memunculkan partikel puing saat UFO meledak.
     */
    public void spawnExplosionParticles(List<Particle> particles) {
        for (int i = 0; i < 22; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.5 + random.nextDouble() * 5.0;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            Color pColor = (random.nextDouble() < 0.4)
                    ? Color.web("#ff3d00") // Api merah
                    : (random.nextDouble() < 0.7 ? Color.web("#ffea00") : Color.web("#00e5ff")); // Puing neon
            particles.add(new Particle(x, y, vx, vy, 4 + random.nextDouble() * 5, 0.45 + random.nextDouble() * 0.35, pColor));
        }
    }

    @Override
    public double getWidth() {
        return WIDTH;
    }

    @Override
    public double getHeight() {
        return HEIGHT;
    }

    public void hit() {
        setDead(true);
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    @Override
    public boolean isDead() {
        return dead;
    }
}
