package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.*;

import java.io.InputStream;

/**
 * Galon Air Mineral Kosmik 3D yang melayang dari horizon mendekati pemain di lintasan.
 * Menggunakan tekstur 3D definisi tinggi dengan silinder berkilau, gelembung air, dan tutup merah.
 */
public class Galon3D extends Entity3D {

    private static Image galonImg;

    static {
        try {
            InputStream is = Galon3D.class.getResourceAsStream("/galon_3d.png");
            if (is != null) {
                galonImg = new Image(is);
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat galon 3D: " + e.getMessage());
        }
    }

    private final boolean golden;
    private final double speedZ;
    private double rotAngle;
    private double bobbingOffset;

    public Galon3D(double x, double y, double z, double speedZ, boolean golden) {
        super(x, y, z, 36.0);
        this.speedZ = speedZ;
        this.golden = golden;
        this.rotAngle = Math.random() * Math.PI * 2;
    }

    public boolean isGolden() {
        return golden;
    }

    public int getGalonValue() {
        return golden ? 3 : 1;
    }

    public int getScoreValue() {
        return golden ? 300 : 100;
    }

    @Override
    public void update() {
        z -= speedZ;
        rotAngle += 0.05;
        bobbingOffset = Math.sin(rotAngle * 2.0) * 8.0;

        if (z < 25) {
            dead = true;
        }
    }

    public void attractTowards(double targetX, double targetY, double targetZ, double pullSpeed) {
        double dx = targetX - this.x;
        double dy = targetY - this.y;
        this.x += dx * pullSpeed;
        this.y += dy * pullSpeed;
    }

    @Override
    public void render(GraphicsContext gc, Camera3D camera) {
        if (z <= 25) return;

        double scale = camera.getScale(this.z);
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y + bobbingOffset, this.z);

        if (sx < -80 || sx > SpaceShooter.WIDTH + 80 || sy < -80 || sy > SpaceShooter.HEIGHT + 80) return;

        double gw = 64 * scale;
        double gh = 86 * scale;

        gc.save();

        // 1. Bayangan 3D di Lantai Grid Kosmik (Y = 190)
        double floorY = 190.0;
        double shadowX = camera.projectX(this.x, this.z);
        double shadowY = camera.projectY(floorY, this.z);
        double shadowW = gw * 0.9;
        double shadowH = gh * 0.28;

        RadialGradient shadowGrad = new RadialGradient(
                0, 0, shadowX, shadowY, shadowW / 2.0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0, 0, 0, 0.55)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(shadowGrad);
        gc.fillOval(shadowX - shadowW / 2.0, shadowY - shadowH / 2.0, shadowW, shadowH);

        // 2. Aura Cahaya Neon 3D
        Color auraCore = golden ? Color.color(1.0, 0.85, 0.1, 0.65) : Color.color(0.0, 0.85, 1.0, 0.50);
        RadialGradient aura = new RadialGradient(
                0, 0, sx, sy, gw * 1.1, false, CycleMethod.NO_CYCLE,
                new Stop(0, auraCore),
                new Stop(0.65, Color.color(auraCore.getRed(), auraCore.getGreen(), auraCore.getBlue(), 0.15)),
                new Stop(1, Color.color(0, 0, 0, 0))
        );
        gc.setFill(aura);
        gc.fillOval(sx - gw * 1.1, sy - gh * 0.9, gw * 2.2, gh * 1.8);

        // 3. Tekstur Galon 3D HD
        if (galonImg != null) {
            double drawX = sx - gw / 2.0;
            double drawY = sy - gh / 2.0;

            gc.drawImage(galonImg, drawX, drawY, gw, gh);

            if (golden) {
                // Kilauan Emas Galon Emas
                gc.save();
                gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.OVERLAY);
                gc.setFill(Color.color(1.0, 0.85, 0.0, 0.65));
                gc.fillRoundRect(drawX, drawY, gw, gh, 10 * scale, 10 * scale);
                gc.restore();
            }
        } else {
            // Fallback render
            renderFallbackGalon(gc, sx, sy, gw, gh, scale);
        }

        gc.restore();
    }

    private void renderFallbackGalon(GraphicsContext gc, double sx, double sy, double gw, double gh, double scale) {
        double drawX = sx - gw / 2.0;
        double drawY = sy - gh / 2.0;
        gc.setFill(golden ? Color.GOLD : Color.web("#00e5ff"));
        gc.fillRoundRect(drawX, drawY, gw, gh, 8 * scale, 8 * scale);
    }
}
