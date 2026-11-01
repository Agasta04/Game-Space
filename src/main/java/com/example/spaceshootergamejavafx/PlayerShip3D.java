package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

import java.util.List;
import java.util.Random;

/**
 * Pesawat Pemain dalam Ruang 3D dengan Kontrol Alami:
 * - A / D: Belok ke KIRI / KANAN (Steer & Banking Roll 3D)
 * - W / S: Gerak MAJU / MUNDUR (Throttle & Brake pada sumbu Z)
 * - Visual 3D dengan Triple Plasma Thrusters & Dynamic Shadow.
 */
public class PlayerShip3D extends Entity3D {

    private final ShipType shipType;
    private final DroneType droneType;

    private boolean moveLeft;
    private boolean moveRight;
    private boolean moveForward;
    private boolean moveBackward;

    private double currentRoll = 0.0;
    private double currentPitch = 0.0;
    private double engineAnim = 0.0;
    private double droneAnim = 0.0;
    private double hoverAnim = 0.0;

    private int health = 3;
    private final int maxHealth = 3;
    private double invulnerableTimer = 0.0;

    // Kecepatan maju mundur relatif
    private double forwardSpeed = 0.0;

    private final Random random = new Random();

    public PlayerShip3D(ShipType shipType, DroneType droneType) {
        super(0, 75, 120, 38.0); // Ketinggian Y=75 stabil di atas lantai kosmik
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

        // 1. KONTROL KIRI - KANAN (Sumbu X)
        double targetRoll = 0.0;
        if (moveLeft) {
            x -= speed;
            targetRoll = -0.65;
        }
        if (moveRight) {
            x += speed;
            targetRoll = 0.65;
        }

        // Batas lintasan kiri-kanan
        if (x < -360) x = -360;
        if (x > 360) x = 360;

        // 2. KONTROL MAJU - MUNDUR (Sumbu Z)
        double targetSpeedZ = 0.0;
        double targetPitch = 0.0;
        if (moveForward) {
            targetSpeedZ = speed * 1.1; // Maju ke depan (Z bertambah)
            targetPitch = -0.45;        // Condong ke depan (menukik masuk arena)
        }
        if (moveBackward) {
            targetSpeedZ = -speed * 0.9; // Mundur / Mengerem (Z berkurang)
            targetPitch = 0.40;          // Menengadah saat rem
        }

        forwardSpeed += (targetSpeedZ - forwardSpeed) * 0.25;
        z += forwardSpeed;

        // Batas manuver maju-mundur di arena 3D (antara Z=60 hingga Z=320)
        if (z < 60) z = 60;
        if (z > 320) z = 320;

        // 3. Ketinggian Y Melayang Stabil (Hovering Bobbing Lembut)
        hoverAnim += 0.06;
        y = 75.0 + Math.sin(hoverAnim) * 4.0;

        // Smooth Interpolation Roll & Pitch
        double handling = shipType.getHandling();
        currentRoll += (targetRoll - currentRoll) * handling;
        currentPitch += (targetPitch - currentPitch) * 0.20;

        engineAnim += (moveForward ? 0.65 : 0.35);
        droneAnim += 0.05;

        if (invulnerableTimer > 0) {
            invulnerableTimer -= 0.016;
        }
    }

    /**
     * Menyemburkan partikel plasma dari 3 nozzle mesin (Kiri, Tengah, Kanan).
     */
    public void emitThrusterParticles(List<Particle> particles, Camera3D camera) {
        double scale = camera.getScale(this.z);
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y, this.z);

        double spacing = 20 * scale;
        double engineY = sy + (16 * scale);

        // 1. Mesin Samping Kiri & Kanan
        double[] sideXs = {sx - spacing + currentRoll * 6, sx + spacing + currentRoll * 6};
        double extraFlame = moveForward ? 4.0 : 0.0;

        for (double sideX : sideXs) {
            double pVx = (random.nextDouble() - 0.5) * 1.5 - currentRoll * 3.0;
            double pVy = (3.5 + extraFlame) + random.nextDouble() * 3.5;
            Color flameCol = (random.nextDouble() < 0.7) ? Color.web("#00e5ff") : Color.WHITE;
            particles.add(new Particle(sideX, engineY, pVx, pVy, (5 + random.nextDouble() * 3) * scale, 0.22, flameCol));
        }

        // 2. Mesin Pusat Utama (Lidah Petir & Plasma Terang)
        double centerVx = (random.nextDouble() - 0.5) * 2.0 - currentRoll * 2.5;
        double centerVy = (5.5 + extraFlame * 1.5) + random.nextDouble() * 5.0;
        Color centerCol = (random.nextDouble() < 0.5) ? Color.WHITE : Color.web("#80d8ff");
        particles.add(new Particle(sx, engineY + (2 * scale), centerVx, centerVy, (7 + random.nextDouble() * 4) * scale, 0.28, centerCol));
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

        // 1. Bayangan 3D di Lantai Grid Kosmik (Y = 190)
        double floorY = 190.0;
        double shadowX = camera.projectX(this.x, this.z);
        double shadowY = camera.projectY(floorY, this.z);
        double shadowW = 75.0 * scale;
        double shadowH = 20.0 * scale;

        gc.save();
        RadialGradient shadowGrad = new RadialGradient(
                0, 0, shadowX, shadowY, shadowW / 2.0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0, 0, 0, 0.65)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(shadowGrad);
        gc.fillOval(shadowX - shadowW / 2.0, shadowY - shadowH / 2.0, shadowW, shadowH);
        gc.restore();

        // 2. Crosshair Bidik 3D di Horizon
        renderCrosshair3D(gc, camera);

        // 3. Drone Pendukung di Sayap
        if (droneType != null) {
            renderCompanionDrone(gc, sx, sy, scale);
        }

        // 4. Render Pesawat Menggunakan Model 3D HD (ShipRenderer)
        ShipRenderer.renderShip(gc, shipType, sx, sy, scale, currentRoll, currentPitch, engineAnim);

        // 5. Perisai Pelindung Cyber
        if (droneType == DroneType.SHIELD_DRONE || invulnerableTimer > 0) {
            gc.save();
            gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.5 + Math.sin(engineAnim) * 0.25));
            gc.setLineWidth(2.5 * scale);
            gc.strokeOval(sx - 70 * scale, sy - 45 * scale, 140 * scale, 90 * scale);
            gc.restore();
        }
    }

    public boolean isMoveForward() {
        return moveForward;
    }

    public boolean isMoveBackward() {
        return moveBackward;
    }

    public double getForwardSpeed() {
        return forwardSpeed;
    }

    private void renderCrosshair3D(GraphicsContext gc, Camera3D camera) {
        double crossZ = 1200.0;
        double crossX = camera.projectX(this.x * 0.35, crossZ);
        double crossY = camera.projectY(this.y * 0.45, crossZ);

        gc.save();
        // Cincin Luar Berputar Lembut
        double ringR = 14.0;
        gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.55));
        gc.setLineWidth(1.4);
        gc.strokeOval(crossX - ringR, crossY - ringR, ringR * 2, ringR * 2);

        // Brackets Pengunci Sudut [ ]
        double bLen = 8.0;
        double bDist = 20.0;
        gc.setStroke(Color.web("#80d8ff"));
        gc.setLineWidth(1.6);
        // Kiri-Atas
        gc.strokeLine(crossX - bDist, crossY - bDist, crossX - bDist + bLen, crossY - bDist);
        gc.strokeLine(crossX - bDist, crossY - bDist, crossX - bDist, crossY - bDist + bLen);
        // Kanan-Atas
        gc.strokeLine(crossX + bDist, crossY - bDist, crossX + bDist - bLen, crossY - bDist);
        gc.strokeLine(crossX + bDist, crossY - bDist, crossX + bDist, crossY - bDist + bLen);
        // Kiri-Bawah
        gc.strokeLine(crossX - bDist, crossY + bDist, crossX - bDist + bLen, crossY + bDist);
        gc.strokeLine(crossX - bDist, crossY + bDist, crossX - bDist, crossY + bDist - bLen);
        // Kanan-Bawah
        gc.strokeLine(crossX + bDist, crossY + bDist, crossX + bDist - bLen, crossY + bDist);
        gc.strokeLine(crossX + bDist, crossY + bDist, crossX + bDist, crossY + bDist - bLen);

        // Titik Pip Inti
        gc.setFill(Color.WHITE);
        gc.fillOval(crossX - 2, crossY - 2, 4, 4);

        // Telemetri Jarak Sensor 3D
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 9));
        gc.setFill(Color.web("#00e5ff", 0.75));
        gc.fillText("LOCK 1.2KM", crossX - 24, crossY + 33);

        gc.restore();
    }

    private void renderCompanionDrone(GraphicsContext gc, double sx, double sy, double scale) {
        double droneOrbitX = sx - 68 * scale + Math.cos(droneAnim) * (18 * scale);
        double droneOrbitY = sy - 20 * scale + Math.sin(droneAnim * 1.5) * (14 * scale);
        double droneSize = 16 * scale;

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

    /**
     * Menembakkan dual laser berkas biru-putih dari rel ujung sayap ke arah horizon.
     */
    public void shoot(List<Laser3D> lasers) {
        // Posisi ujung sayap kiri dan kanan
        double leftWingX = this.x - 42;
        double rightWingX = this.x + 42;
        double startY = this.y + 4;
        double startZ = this.z + 15;

        // Vektor laser meluncur masuk ke horizon 3D
        double targetZ = 1200.0;
        double targetX = this.x * 0.35;
        double targetY = this.y * 0.45;

        double vLx = (targetX - leftWingX) / (targetZ / 58.0);
        double vLy = (targetY - startY) / (targetZ / 58.0);
        double vRx = (targetX - rightWingX) / (targetZ / 58.0);
        double vRy = (targetY - startY) / (targetZ / 58.0);

        lasers.add(new Laser3D(leftWingX, startY, startZ, vLx, vLy, shipType.getPrimaryColor()));
        lasers.add(new Laser3D(rightWingX, startY, startZ, vRx, vRy, shipType.getPrimaryColor()));

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

    public void setMoveForward(boolean moveForward) {
        this.moveForward = moveForward;
    }

    public void setMoveBackward(boolean moveBackward) {
        this.moveBackward = moveBackward;
    }
}
