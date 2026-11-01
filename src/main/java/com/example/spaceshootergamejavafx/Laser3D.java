package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

/**
 * Berkas Laser 3D Berenergi Tinggi yang melesat dari rel sayap pesawat ke kedalaman horizon.
 * Sesuai visual referensi: Berkas sinar lonjong tebal dengan inti putih terang dan aura cyan menyilaukan.
 */
public class Laser3D extends Entity3D {
    private final double vx;
    private final double vy;
    private static final double SPEED_Z = 64.0;
    private static final double LASER_LENGTH_Z = 65.0;
    private final Color color;

    public Laser3D(double x, double y, double z, double vx, double vy, Color color) {
        super(x, y, z, 16.0);
        this.vx = vx;
        this.vy = vy;
        this.color = color;
    }

    @Override
    public void update() {
        x += vx;
        y += vy;
        z += SPEED_Z;
        if (z > 1750) {
            dead = true;
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera3D camera) {
        if (z <= 10) return;

        double sx1 = camera.projectX(this.x, this.z);
        double sy1 = camera.projectY(this.y, this.z);
        double sx2 = camera.projectX(this.x, this.z + LASER_LENGTH_Z);
        double sy2 = camera.projectY(this.y, this.z + LASER_LENGTH_Z);

        double scale = camera.getScale(this.z);
        double coreWidth = Math.max(2.2, 7.0 * scale);
        double auraWidth = coreWidth * 2.6;

        gc.save();

        // 1. Aura Cahaya Luar Neon Cyan/Biru
        gc.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.55));
        gc.setLineWidth(auraWidth);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeLine(sx1, sy1, sx2, sy2);

        // 2. Berkas Menengah Terang
        gc.setStroke(Color.web("#e0f7fa"));
        gc.setLineWidth(coreWidth * 1.5);
        gc.strokeLine(sx1, sy1, sx2, sy2);

        // 3. Inti Putih Murni Bersinar
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(coreWidth);
        gc.strokeLine(sx1, sy1, sx2, sy2);

        gc.restore();
    }
}
