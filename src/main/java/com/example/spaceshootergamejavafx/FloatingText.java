package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Teks melayang animasi (+1 GALON, +50 SKOR, CRITICAL HIT, dll).
 */
public class FloatingText {
    private double x;
    private double y;
    private final String text;
    private final Color color;
    private double life;
    private final double maxLife;
    private final double fontSize;

    public FloatingText(String text, double x, double y, Color color, double fontSize, double durationSeconds) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.fontSize = fontSize;
        this.maxLife = durationSeconds;
        this.life = durationSeconds;
    }

    public void update() {
        y -= 0.8; // Melayang ke atas secara perlahan
        life -= 0.016;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public void render(GraphicsContext gc) {
        double alpha = Math.max(0, life / maxLife);
        gc.save();
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, fontSize));
        // Bayangan hitam di belakang teks untuk kontras maksimal
        gc.setFill(Color.color(0, 0, 0, alpha * 0.8));
        gc.fillText(text, x + 2, y + 2);

        // Teks utama bercahaya
        gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        gc.fillText(text, x, y);
        gc.restore();
    }
}

