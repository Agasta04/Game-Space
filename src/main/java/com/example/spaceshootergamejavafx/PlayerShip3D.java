package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

import java.util.List;
import java.util.Random;

/**
 * Pesawat Pemain dalam Perspektif 3D Chase Camera View.
 * Menggunakan ShipRenderer profesional untuk render presisi tanpa distorsi.
 */
public class PlayerShip3D extends Entity3D {

    private final ShipType shipType;
    private final DroneType droneType;

    private boolean moveLeft;
    private boolean moveRight;
    private boolean moveUp;
    private boolean moveDown;

    private double currentRoll = 0.0;
    private double currentPitch = 0.0;
    private double engineAnim = 0.0;
    private double droneAnim = 0.0;

    private int health = 3;
    private final int maxHealth = 3;
    private double invulnerableTimer = 0.0;

    private final Random random = new Random();

    public PlayerShip3D(ShipType shipType, DroneType droneType) {
        super(0, 115, 110, 36.0); // Z=110 dekat di depan kamera
        this.shipType = shipType;
        this.droneType = droneType;
    }

    public ShipType getShipType() {
        return shipType;
    }

    public DroneType getDroneType() {
        return droneType;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public boolean takeDamage() {
        if (invulnerableTimer <= 0) {
            health--;
            invulnerableTimer = 0.8;
            SoundManager.playHit();
            if (health <= 0) {
                dead = true;
            }
            return true;
        }
        return false;
    }

    public double getMagnetRange() {
        double range = shipType.getMagnetRange();
        if (droneType == DroneType.MAGNET_DRONE) {
            range *= droneType.getBoostFactor();
        }
        return range;
    }

    @Override
    public void update() {
        double speed = shipType.getSpeed();

        // Kontrol X (Kiri-Kanan)
        double targetRoll = 0.0;
        if (moveLeft) {
            x -= speed;
            targetRoll = -0.55;
        }
        if (moveRight) {
            x += speed;
            targetRoll = 0.55;
        }

        // Kontrol Y (Atas-Bawah)
        double targetPitch = 0.0;
        if (moveUp) {
            y -= speed * 0.85;
            targetPitch = -0.3;
        }
        if (moveDown) {
            y += speed * 0.85;
            targetPitch = 0.3;
        }

        // Batas gerak 3D di layar
        if (x < -360) x = -360;
        if (x > 360) x = 360;
        if (y < -160) y = -160;
        if (y > 165) y = 165;

        // Smooth Interpolation
        double handling = shipType.getHandling();
        currentRoll += (targetRoll - currentRoll) * handling;
        currentPitch += (targetPitch - currentPitch) * handling;

        engineAnim += 0.35;
        droneAnim += 0.05;

        if (invulnerableTimer > 0) {
            invulnerableTimer -= 0.016;
        }
    }

    public void emitThrusterParticles(List<Particle> particles, Camera3D camera) {
        double scale = camera.getScale(this.z);
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y, this.z);

        double spacing = 16 * scale;
        double engineY = sy + (20 * scale);

        double[] engineXs = {sx - spacing + currentRoll * 6, sx + spacing + currentRoll * 6};

        for (double eX : engineXs) {
            double pVx = (random.nextDouble() - 0.5) * 1.5 - currentRoll * 3.0;
            double pVy = 3.5 + random.nextDouble() * 3.5;
            Color flameCol = (random.nextDouble() < 0.6)
                    ? shipType.getPrimaryColor()
                    : Color.web("#ffffff");
            particles.add(new Particle(eX, engineY, pVx, pVy, (5 + random.nextDouble() * 4) * scale, 0.22, flameCol));
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera3D camera) {
        // Kedip saat kebal setelah terkena tabrakan
        if (invulnerableTimer > 0 && ((int) (invulnerableTimer * 20) % 2 == 0)) {
            return;
        }

        double scale = camera.getScale(this.z);
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y, this.z);

        // 1. Crosshair Bidik 3D di Horizon
        renderCrosshair3D(gc, camera);

        // 2. Drone Pendukung
        if (droneType != null) {
            renderCompanionDrone(gc, sx, sy, scale);
        }

        // 3. Render Pesawat Pemain Menggunakan ShipRenderer Solid
        ShipRenderer.renderShip(gc, shipType, sx, sy, scale, currentRoll, currentPitch, engineAnim);

        // 4. Perisai Pelindung (Aegis Drone atau saat kebal)
        if (droneType == DroneType.SHIELD_DRONE || invulnerableTimer > 0) {
            gc.save();
            gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.5 + Math.sin(engineAnim) * 0.25));
            gc.setLineWidth(2.2 * scale);
            gc.strokeOval(sx - 48 * scale, sy - 40 * scale, 96 * scale, 80 * scale);
            gc.restore();
        }
    }

    private void renderCrosshair3D(GraphicsContext gc, Camera3D camera) {
        double targetZ = 700.0;
        double cx = camera.projectX(this.x * 0.6, targetZ);
        double cy = camera.projectY(this.y * 0.6, targetZ);
        double cSize = 18.0;

        gc.save();
        Color col = shipType.getPrimaryColor();
        gc.setStroke(Color.color(col.getRed(), col.getGreen(), col.getBlue(), 0.75));
        gc.setLineWidth(1.6);
        gc.strokeOval(cx - cSize / 2.0, cy - cSize / 2.0, cSize, cSize);

        gc.strokeLine(cx - cSize, cy, cx - cSize * 0.45, cy);
        gc.strokeLine(cx + cSize * 0.45, cy, cx + cSize, cy);
        gc.strokeLine(cx, cy - cSize, cx, cy - cSize * 0.45);
        gc.strokeLine(cx, cy + cSize * 0.45, cx, cy + cSize);

        gc.setFill(Color.WHITE);
        gc.fillOval(cx - 1.5, cy - 1.5, 3, 3);
        gc.restore();
    }

    private void renderCompanionDrone(GraphicsContext gc, double sx, double sy, double scale) {
        double droneOrbitX = sx + Math.cos(droneAnim) * (58 * scale);
        double droneOrbitY = sy - 18 * scale + Math.sin(droneAnim * 1.5) * (14 * scale);
        double droneSize = 15 * scale;

        gc.save();
        RadialGradient droneGrad = new RadialGradient(
                -45, 0.4, droneOrbitX, droneOrbitY, droneSize / 2.0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),
                new Stop(0.5, droneType.getColor()),
                new Stop(1.0, Color.BLACK)
        );
        gc.setFill(droneGrad);
        gc.fillOval(droneOrbitX - droneSize / 2.0, droneOrbitY - droneSize / 2.0, droneSize, droneSize);

        gc.setStroke(droneType.getColor());
        gc.setLineWidth(1.5 * scale);
        gc.strokeOval(droneOrbitX - droneSize * 0.8, droneOrbitY - droneSize * 0.35, droneSize * 1.6, droneSize * 0.7);
        gc.restore();
    }

    public void shoot(List<Laser3D> lasers) {
        double leftWingX = this.x - 28;
        double rightWingX = this.x + 28;
        double startY = this.y + 4;
        double startZ = this.z + 10;

        lasers.add(new Laser3D(leftWingX, startY, startZ, (this.x * 0.6 - leftWingX) * 0.05, (this.y * 0.6 - startY) * 0.05, shipType.getPrimaryColor()));
        lasers.add(new Laser3D(rightWingX, startY, startZ, (this.x * 0.6 - rightWingX) * 0.05, (this.y * 0.6 - startY) * 0.05, shipType.getPrimaryColor()));

        if (droneType == DroneType.BLASTER_DRONE) {
            lasers.add(new Laser3D(this.x, startY - 18, startZ, 0, -0.2, droneType.getColor()));
        }

        SoundManager.playLaser();
    }

    public void setMoveLeft(boolean moveLeft) {
        this.moveLeft = moveLeft;
    }

    public void setMoveRight(boolean moveRight) {
        this.moveRight = moveRight;
    }

    public void setMoveUp(boolean moveUp) {
        this.moveUp = moveUp;
    }

    public void setMoveDown(boolean moveDown) {
        this.moveDown = moveDown;
    }
}
