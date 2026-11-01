package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Partikel dinamis untuk efek ledakan, debu kosmik, dan api pendorong mesin roket.
 */
public class Particle {
    private double x;
    private double y;
    private double vx;
    private double vy;
    private double size;
    private double life;
    private final double maxLife;
    private final Color baseColor;

    public Particle(double x, double y, double vx, double vy, double size, double maxLife, Color baseColor) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.size = size;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.baseColor = baseColor;
    }

    public void update() {
        x += vx;
        y += vy;
        life -= 0.016; // Asumsi ~60 FPS
    }

    public boolean isDead() {
        return life <= 0;
    }

    public void render(GraphicsContext gc) {
        double opacity = Math.max(0, life / maxLife);
        Color colorWithAlpha = Color.color(
                baseColor.getRed(),
                baseColor.getGreen(),
                baseColor.getBlue(),
                opacity * baseColor.getOpacity()
        );
        gc.setFill(colorWithAlpha);
        double currentSize = size * (0.5 + 0.5 * (life / maxLife));
        gc.fillOval(x - currentSize / 2, y - currentSize / 2, currentSize, currentSize);
    }
}

