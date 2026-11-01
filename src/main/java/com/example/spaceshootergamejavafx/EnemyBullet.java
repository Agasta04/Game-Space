package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

/**
 * Peluru Plasma Merah Musuh/Boss UFO dengan Efek Bercahaya.
 */
public class EnemyBullet extends GameObject {

    public static final int WIDTH = 6;
    public static final int HEIGHT = 20;
    private static final double SPEED = 4.5;
    private boolean dead = false;

    public EnemyBullet(double x, double y) {
        super(x, y, WIDTH, HEIGHT);
    }

    @Override
    public void update() {
        y += SPEED;
        if (y > SpaceShooter.HEIGHT + 30) {
            dead = true;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();

        // Glow Plasma Merah
        RadialGradient glow = new RadialGradient(
                0, 0, x, y, WIDTH * 2.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1.0, 0.1, 0.2, 0.7)),
                new Stop(1, Color.color(0.8, 0.0, 0.0, 0.0))
        );
        gc.setFill(glow);
        gc.fillOval(x - WIDTH * 2.5, y - HEIGHT / 2.0, WIDTH * 5, HEIGHT);

        // Inti Plasma
        LinearGradient plasmaGrad = new LinearGradient(
                0, y - HEIGHT / 2.0, 0, y + HEIGHT / 2.0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(1.0, 0.3, 0.0, 0.2)),
                new Stop(0.7, Color.web("#ff1744")),
                new Stop(1.0, Color.WHITE)
        );
        gc.setFill(plasmaGrad);
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
