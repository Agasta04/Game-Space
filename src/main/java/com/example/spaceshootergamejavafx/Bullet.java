package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

/**
 * Peluru Laser Neon Cyan Pemain dengan efek Trail Bercahaya.
 */
public class Bullet extends GameObject {

    public static final int WIDTH = 6;
    public static final int HEIGHT = 22;
    private static final double SPEED = 9.5;
    private boolean dead = false;

    public Bullet(double x, double y) {
        super(x, y, WIDTH, HEIGHT);
    }

    @Override
    public void update() {
        y -= SPEED;
        if (y < -30) {
            dead = true;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();

        // Neon Glow di sekeliling laser
        RadialGradient glow = new RadialGradient(
                0, 0, x, y, WIDTH * 2.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.0, 0.9, 1.0, 0.7)),
                new Stop(1, Color.color(0.0, 0.4, 1.0, 0.0))
        );
        gc.setFill(glow);
        gc.fillOval(x - WIDTH * 2.5, y - HEIGHT / 2.0, WIDTH * 5, HEIGHT);

        // Badan Laser Gradasi Inti Terang
        LinearGradient laserGrad = new LinearGradient(
                0, y - HEIGHT / 2.0, 0, y + HEIGHT / 2.0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),
                new Stop(0.3, Color.web("#00e5ff")),
                new Stop(1.0, Color.color(0.0, 0.5, 1.0, 0.2))
        );
        gc.setFill(laserGrad);
        gc.fillRoundRect(x - WIDTH / 2.0, y - HEIGHT / 2.0, WIDTH, HEIGHT, 4, 4);

        gc.restore();
    }

    @Override
    public double getWidth() {
        return WIDTH;
    }

    @Override
    public double getHeight() {
        return HEIGHT;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    @Override
    public boolean isDead() {
        return dead;
    }
}
