package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

import java.util.Random;

/**
 * Batu Asteroid Kosmik 3D yang melayang dan berputar di ruang hampa.
 */
public class Asteroid3D extends Entity3D {
    private final double speedZ;
    private double rotAngle;
    private final double rotSpeed;
    private final double[] vertexOffsets;
    private static final int NUM_VERTICES = 8;

    public Asteroid3D(double x, double y, double z, double speedZ) {
        super(x, y, z, 32.0);
        this.speedZ = speedZ;
        this.rotAngle = Math.random() * Math.PI * 2;
        this.rotSpeed = (Math.random() - 0.5) * 0.04;

        this.vertexOffsets = new double[NUM_VERTICES];
        for (int i = 0; i < NUM_VERTICES; i++) {
            vertexOffsets[i] = 0.8 + Math.random() * 0.4;
        }
    }

    @Override
    public void update() {
        z -= speedZ;
        rotAngle += rotSpeed;
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

        if (sx < -100 || sx > SpaceShooter.WIDTH + 100 || sy < -100 || sy > SpaceShooter.HEIGHT + 100) return;

        double rad = baseRadius * scale;

        gc.save();

        // 1. Bayangan di Lantai Kosmik (Y = 190)
        double floorY = 190.0;
        double shadowX = camera.projectX(this.x, this.z);
        double shadowY = camera.projectY(floorY, this.z);

        RadialGradient shadowGrad = new RadialGradient(
                0, 0, shadowX, shadowY, rad * 0.8, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0, 0, 0, 0.4)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(shadowGrad);
        gc.fillOval(shadowX - rad * 0.8, shadowY - rad * 0.25, rad * 1.6, rad * 0.5);

        // 2. Batu Asteroid 3D
        double[] px = new double[NUM_VERTICES];
        double[] py = new double[NUM_VERTICES];
        for (int i = 0; i < NUM_VERTICES; i++) {
            double angle = rotAngle + i * (2 * Math.PI / NUM_VERTICES);
            double r = rad * vertexOffsets[i];
            px[i] = sx + Math.cos(angle) * r;
            py[i] = sy + Math.sin(angle) * r;
        }

        RadialGradient rockGrad = new RadialGradient(
                -45, 0.4, sx, sy, rad * 1.2, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#8d6e63")),
                new Stop(0.5, Color.web("#4e342e")),
                new Stop(1.0, Color.web("#1b0000"))
        );
        gc.setFill(rockGrad);
        gc.fillPolygon(px, py, NUM_VERTICES);

        gc.setStroke(Color.web("#a1887f"));
        gc.setLineWidth(1.2 * scale);
        gc.strokePolygon(px, py, NUM_VERTICES);

        gc.restore();
    }
}
