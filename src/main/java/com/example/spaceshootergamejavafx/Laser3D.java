package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

/**
 * Berkas Laser 3D yang melesat dari moncong pesawat ke horizon kedalaman.
 */
public class Laser3D extends Entity3D {
    private final double vx;
    private final double vy;
    private static final double SPEED_Z = 58.0;
    private static final double LASER_LENGTH_Z = 45.0;
    private final Color color;

    public Laser3D(double x, double y, double z, double vx, double vy, Color color) {
        super(x, y, z, 14.0);
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
        double width = Math.max(1.8, 6.0 * scale);

        gc.save();
        // Berkas sinar luar neon
        gc.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.5));
        gc.setLineWidth(width * 2.2);
        gc.strokeLine(sx1, sy1, sx2, sy2);

        // Inti putih terang
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(width);
        gc.strokeLine(sx1, sy1, sx2, sy2);
        gc.restore();
    }
}

