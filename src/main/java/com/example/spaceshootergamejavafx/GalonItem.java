package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.shape.ArcType;

/**
 * Item Galon Air Mineral Kosmik 3D yang harus dikumpulkan pemain.
 * Mendukung Galon Reguler (Biru) dan Galon Emas Langka (Golden).
 */
public class GalonItem extends GameObject {
    public static final int WIDTH = 36;
    public static final int HEIGHT = 48;
    private static final double SPEED = 2.4;

    private boolean isDead = false;
    private final boolean golden;
    private double animAngle = 0;
    private double bobbingOffset = 0;

    public GalonItem(double x, double y, boolean golden) {
        super(x, y, WIDTH, HEIGHT);
        this.golden = golden;
        this.animAngle = Math.random() * Math.PI * 2;
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
        y += SPEED;
        animAngle += 0.06;
        bobbingOffset = Math.sin(animAngle * 2) * 3;
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();

        double drawX = x - WIDTH / 2.0;
        double drawY = y - HEIGHT / 2.0 + bobbingOffset;

        // --- 1. AURA CAHAYA (NEON GLOW) ---
        Color auraCore = golden ? Color.color(1.0, 0.85, 0.1, 0.6) : Color.color(0.2, 0.7, 1.0, 0.45);
        Color auraFade = golden ? Color.color(0.9, 0.5, 0.0, 0.0) : Color.color(0.1, 0.4, 0.9, 0.0);

        RadialGradient aura = new RadialGradient(
                0, 0, x, y + bobbingOffset, WIDTH * 1.3, false, CycleMethod.NO_CYCLE,
                new Stop(0, auraCore),
                new Stop(1, auraFade)
        );
        gc.setFill(aura);
        gc.fillOval(x - WIDTH * 1.3, y - HEIGHT * 1.3 + bobbingOffset, WIDTH * 2.6, HEIGHT * 2.6);

        // --- 2. BADAN GALON 3D (Silinder Polycarbonate) ---
        LinearGradient bottleShading;
        if (golden) {
            bottleShading = new LinearGradient(
                    drawX, 0, drawX + WIDTH, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#e65100")),
                    new Stop(0.2, Color.web("#ffb300")),
                    new Stop(0.5, Color.web("#fff9c4")), // Specular highlight emas
                    new Stop(0.8, Color.web("#ffa000")),
                    new Stop(1.0, Color.web("#ff6f00"))
            );
        } else {
            bottleShading = new LinearGradient(
                    drawX, 0, drawX + WIDTH, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#0e5a8a")),
                    new Stop(0.2, Color.web("#42b6f5")),
                    new Stop(0.5, Color.web("#e1f5fe")), // Specular highlight plastik
                    new Stop(0.8, Color.web("#299edb")),
                    new Stop(1.0, Color.web("#0d4870"))
            );
        }
        gc.setFill(bottleShading);
        gc.fillRoundRect(drawX, drawY + 12, WIDTH, HEIGHT - 14, 10, 10);

        // Garis Lekukan Ring Galon
        gc.setStroke(Color.color(1, 1, 1, 0.5));
        gc.setLineWidth(1.5);
        gc.strokeLine(drawX + 3, drawY + 21, drawX + WIDTH - 3, drawY + 21);
        gc.strokeLine(drawX + 3, drawY + 29, drawX + WIDTH - 3, drawY + 29);
        gc.strokeLine(drawX + 3, drawY + 37, drawX + WIDTH - 3, drawY + 37);

        // Cairan di Dalam Galon
        LinearGradient fluidGradient;
        if (golden) {
            fluidGradient = new LinearGradient(
                    0, drawY + 16, 0, drawY + HEIGHT - 4, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.color(1.0, 0.9, 0.2, 0.8)),
                    new Stop(1.0, Color.color(0.9, 0.55, 0.0, 0.9))
            );
        } else {
            fluidGradient = new LinearGradient(
                    0, drawY + 16, 0, drawY + HEIGHT - 4, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.color(0.15, 0.75, 1.0, 0.75)),
                    new Stop(1.0, Color.color(0.05, 0.4, 0.85, 0.85))
            );
        }
        gc.setFill(fluidGradient);
        gc.fillRoundRect(drawX + 3, drawY + 16, WIDTH - 6, HEIGHT - 20, 6, 6);

        // Gelembung Udara
        gc.setFill(Color.color(1, 1, 1, 0.85));
        double bubbleY = drawY + 25 + Math.sin(animAngle * 3) * 4;
        gc.fillOval(drawX + WIDTH * 0.38, bubbleY, 3, 3);
        gc.fillOval(drawX + WIDTH * 0.65, bubbleY + 6, 2, 2);

        // Leher Galon
        LinearGradient neckGradient = new LinearGradient(
                drawX + 9, 0, drawX + WIDTH - 9, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, golden ? Color.web("#ffb300") : Color.web("#299edb")),
                new Stop(0.5, Color.WHITE),
                new Stop(1, golden ? Color.web("#ff6f00") : Color.web("#0e5a8a"))
        );
        gc.setFill(neckGradient);
        gc.fillRoundRect(drawX + 9, drawY + 4, WIDTH - 18, 9, 4, 4);

        // Tutup Galon (Cap)
        LinearGradient capGradient = new LinearGradient(
                drawX + 8, 0, drawX + WIDTH - 8, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, golden ? Color.web("#b71c1c") : Color.web("#c62828")),
                new Stop(0.4, Color.web("#ef5350")),
                new Stop(1, Color.web("#b71c1c"))
        );
        gc.setFill(capGradient);
        gc.fillRoundRect(drawX + 8, drawY, WIDTH - 16, 6, 3, 3);

        // Pegangan samping galon
        gc.setStroke(golden ? Color.web("#ff8f00") : Color.web("#1e88e5"));
        gc.setLineWidth(2.5);
        gc.strokeArc(drawX - 2, drawY + 18, 8, 15, 90, 180, ArcType.OPEN);

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

    @Override
    public boolean isDead() {
        return isDead;
    }

    public void setDead(boolean dead) {
        this.isDead = dead;
    }
}
