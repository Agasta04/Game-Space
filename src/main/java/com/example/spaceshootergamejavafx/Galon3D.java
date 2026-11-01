package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.shape.ArcType;

/**
 * Galon Air Mineral Kosmik 3D yang melayang dari horizon mendekati pemain.
 * Mendukung rotasi 3D dan efek magnet gravitasi ke pesawat pemain.
 */
public class Galon3D extends Entity3D {
    private final boolean golden;
    private final double speedZ;
    private double rotAngle;
    private double bobbingOffset;

    public Galon3D(double x, double y, double z, double speedZ, boolean golden) {
        super(x, y, z, 34.0);
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
        bobbingOffset = Math.sin(rotAngle * 2.0) * 12.0;

        if (z < 25) {
            dead = true;
        }
    }

    /**
     * Efek magnet hisap galon ke arah posisi pesawat pemain jika berada dalam jangkauan.
     */
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

        double gw = 54 * scale;
        double gh = 72 * scale;

        gc.save();

        // 1. Aura Cahaya Neon 3D
        Color auraCore = golden ? Color.color(1.0, 0.85, 0.1, 0.6) : Color.color(0.2, 0.7, 1.0, 0.45);
        RadialGradient aura = new RadialGradient(
                0, 0, sx, sy, gw * 1.1, false, CycleMethod.NO_CYCLE,
                new Stop(0, auraCore),
                new Stop(1, Color.color(0, 0, 0, 0))
        );
        gc.setFill(aura);
        gc.fillOval(sx - gw * 1.1, sy - gh * 0.9, gw * 2.2, gh * 1.8);

        // 2. Badan Galon 3D (Silinder Polycarbonate)
        double drawX = sx - gw / 2.0;
        double drawY = sy - gh / 2.0;

        LinearGradient bodyGrad;
        if (golden) {
            bodyGrad = new LinearGradient(
                    drawX, 0, drawX + gw, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#e65100")),
                    new Stop(0.25, Color.web("#ffb300")),
                    new Stop(0.5, Color.web("#fff9c4")), // Specular highlight emas
                    new Stop(0.8, Color.web("#ffa000")),
                    new Stop(1.0, Color.web("#ff6f00"))
            );
        } else {
            bodyGrad = new LinearGradient(
                    drawX, 0, drawX + gw, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#0e5a8a")),
                    new Stop(0.25, Color.web("#42b6f5")),
                    new Stop(0.5, Color.web("#e1f5fe")), // Specular highlight plastik
                    new Stop(0.8, Color.web("#299edb")),
                    new Stop(1.0, Color.web("#0d4870"))
            );
        }
        gc.setFill(bodyGrad);
        gc.fillRoundRect(drawX, drawY + 16 * scale, gw, gh - 18 * scale, 12 * scale, 12 * scale);

        // Garis Lekukan Khas Galon
        gc.setStroke(Color.color(1, 1, 1, 0.55));
        gc.setLineWidth(1.8 * scale);
        gc.strokeLine(drawX + 4 * scale, drawY + 28 * scale, drawX + gw - 4 * scale, drawY + 28 * scale);
        gc.strokeLine(drawX + 4 * scale, drawY + 40 * scale, drawX + gw - 4 * scale, drawY + 40 * scale);
        gc.strokeLine(drawX + 4 * scale, drawY + 52 * scale, drawX + gw - 4 * scale, drawY + 52 * scale);

        // Cairan di Dalam Galon
        LinearGradient fluidGrad;
        if (golden) {
            fluidGrad = new LinearGradient(
                    0, drawY + 20 * scale, 0, drawY + gh - 4 * scale, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.color(1.0, 0.9, 0.2, 0.8)),
                    new Stop(1.0, Color.color(0.9, 0.55, 0.0, 0.9))
            );
        } else {
            fluidGrad = new LinearGradient(
                    0, drawY + 20 * scale, 0, drawY + gh - 4 * scale, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.color(0.15, 0.75, 1.0, 0.8)),
                    new Stop(1.0, Color.color(0.05, 0.4, 0.85, 0.9))
            );
        }
        gc.setFill(fluidGrad);
        gc.fillRoundRect(drawX + 4 * scale, drawY + 22 * scale, gw - 8 * scale, gh - 28 * scale, 8 * scale, 8 * scale);

        // Leher Galon
        LinearGradient neckGrad = new LinearGradient(
                drawX + 14 * scale, 0, drawX + gw - 14 * scale, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, golden ? Color.web("#ffb300") : Color.web("#299edb")),
                new Stop(0.5, Color.WHITE),
                new Stop(1, golden ? Color.web("#ff6f00") : Color.web("#0e5a8a"))
        );
        gc.setFill(neckGrad);
        gc.fillRoundRect(drawX + 14 * scale, drawY + 6 * scale, gw - 28 * scale, 12 * scale, 5 * scale, 5 * scale);

        // Tutup Segel Galon (Merah Khas)
        gc.setFill(Color.web("#d32f2f"));
        gc.fillRoundRect(drawX + 12 * scale, drawY, gw - 24 * scale, 9 * scale, 4 * scale, 4 * scale);

        // Handle Samping
        gc.setStroke(golden ? Color.web("#ff8f00") : Color.web("#1e88e5"));
        gc.setLineWidth(3.0 * scale);
        gc.strokeArc(drawX - 3 * scale, drawY + 26 * scale, 10 * scale, 20 * scale, 90, 180, ArcType.OPEN);

        gc.restore();
    }
}

