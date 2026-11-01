package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

/**
 * Bola Plasma Energi Merah yang ditembakkan musuh UFO / Boss ke arah pemain.
 */
public class EnemyPlasma3D extends Entity3D {
    private final double vx;
    private final double vy;
    private final double speedZ;

    public EnemyPlasma3D(double x, double y, double z, double vx, double vy, double speedZ) {
        super(x, y, z, 16.0);
        this.vx = vx;
        this.vy = vy;
        this.speedZ = speedZ;
    }

    @Override
    public void update() {
        x += vx;
        y += vy;
        z -= speedZ;
        if (z < 25) {
            dead = true;
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera3D camera) {
        if (z <= 25) return;

        double scale = camera.getScale(this.z);
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y, this.z);

        double pSize = 18 * scale;

        gc.save();
        // Plasma Glow
        RadialGradient glow = new RadialGradient(
                0, 0, sx, sy, pSize * 1.5, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),
                new Stop(0.35, Color.web("#ff1744")),
                new Stop(0.8, Color.web("#d50000")),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(glow);
        gc.fillOval(sx - pSize * 1.5, sy - pSize * 1.5, pSize * 3.0, pSize * 3.0);
        gc.restore();
    }
}

