package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.shape.ArcType;

import java.util.List;
import java.util.Random;

/**
 * Pesawat Pemain dalam Perspektif 3D Chase Camera View.
 * Menampilkan model pesawat 3D dengan banking roll, pitch, twin plasma engine, crosshair 3D, dan drone pendukung.
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
        super(0, 110, 120, 36.0); // Berada di Z=120, agak di bawah tengah layar
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

        // Pergerakan X (Kiri - Kanan)
        double targetRoll = 0.0;
        if (moveLeft) {
            x -= speed;
            targetRoll = -0.65;
        }
        if (moveRight) {
            x += speed;
            targetRoll = 0.65;
        }

        // Pergerakan Y (Atas - Bawah)
        double targetPitch = 0.0;
        if (moveUp) {
            y -= speed * 0.85;
            targetPitch = -0.35;
        }
        if (moveDown) {
            y += speed * 0.85;
            targetPitch = 0.35;
        }

        // Batasi ruang gerak 3D
        if (x < -360) x = -360;
        if (x > 360) x = 360;
        if (y < -180) y = -180;
        if (y > 170) y = 170;

        // Smooth Interpolation Roll & Pitch
        double handling = shipType.getHandling();
        currentRoll += (targetRoll - currentRoll) * handling;
        currentPitch += (targetPitch - currentPitch) * handling;

        engineAnim += 0.35;
        droneAnim += 0.05;

        if (invulnerableTimer > 0) {
            invulnerableTimer -= 0.016;
        }
    }

    /**
     * Memancarkan partikel api mesin plasma roket ke belakang kamera.
     */
    public void emitThrusterParticles(List<Particle> particles, Camera3D camera) {
        double scale = camera.getScale(this.z);
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y, this.z);

        double rollFactor = 1.0 - Math.abs(currentRoll) * 0.25;
        double spacing = 16 * scale * rollFactor;
        double engineY = sy + (18 * scale);

        double[] engineXs = {sx - spacing + currentRoll * 6, sx + spacing + currentRoll * 6};

        for (double eX : engineXs) {
            double pVx = (random.nextDouble() - 0.5) * 1.5 - currentRoll * 3.0;
            double pVy = 3.0 + random.nextDouble() * 3.5;
            Color flameCol = (random.nextDouble() < 0.6)
                    ? shipType.getPrimaryColor()
                    : Color.web("#ffffff");
            particles.add(new Particle(eX, engineY, pVx, pVy, (5 + random.nextDouble() * 4) * scale, 0.22, flameCol));
        }
    }

    @Override
    public void render(GraphicsContext gc, Camera3D camera) {
        gc.save();

        // Kedip saat kebal setelah terkena tabrakan
        if (invulnerableTimer > 0 && ((int) (invulnerableTimer * 20) % 2 == 0)) {
            gc.restore();
            return;
        }

        double scale = camera.getScale(this.z);
        double sx = camera.projectX(this.x, this.z);
        double sy = camera.projectY(this.y, this.z);

        double w = 80 * scale;
        double h = 60 * scale;

        // --- 1. RENDER RETICLE / CROSSHAIR 3D DI HORIZON ---
        renderCrosshair3D(gc, camera);

        // --- 2. RENDER DRONE PENDUKUNG (JIKA DIPILIH) ---
        if (droneType != null) {
            renderCompanionDrone(gc, camera, sx, sy, scale);
        }

        // --- 3. PERSPEKTIF ROLL & SHADING 3D PESAWAT ---
        double rollFactor = 1.0 - Math.abs(currentRoll) * 0.35;
        double rollOffset = currentRoll * 24 * scale;
        double pitchOffset = currentPitch * 15 * scale;

        // Api Pendorong Belakang (Twin Thruster Nozzles)
        double flamePulse = Math.sin(engineAnim) * (6 * scale);
        double engineSpacing = 16 * scale * rollFactor;
        renderThrusterGlow(gc, sx - engineSpacing + rollOffset * 0.5, sy + 16 * scale + pitchOffset, 12 * scale, 22 * scale + flamePulse);
        renderThrusterGlow(gc, sx + engineSpacing + rollOffset * 0.5, sy + 16 * scale + pitchOffset, 12 * scale, 22 * scale + flamePulse);

        // Sayap Kiri 3D
        double leftSpan = (w * 0.65) * (1.0 - currentRoll * 0.45);
        double[] lWingX = {sx - 10 * scale * rollFactor, sx - leftSpan, sx - 12 * scale * rollFactor};
        double[] lWingY = {sy - 6 * scale + pitchOffset, sy + 16 * scale + pitchOffset, sy + 10 * scale + pitchOffset};
        LinearGradient lWingGrad = new LinearGradient(
                sx - leftSpan, 0, sx, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, shipType.getAccentColor()),
                new Stop(0.6, shipType.getBodyColor()),
                new Stop(1.0, Color.BLACK)
        );
        gc.setFill(lWingGrad);
        gc.fillPolygon(lWingX, lWingY, 3);
        gc.setStroke(shipType.getPrimaryColor());
        gc.setLineWidth(1.2 * scale);
        gc.strokePolygon(lWingX, lWingY, 3);

        // Sayap Kanan 3D
        double rightSpan = (w * 0.65) * (1.0 + currentRoll * 0.45);
        double[] rWingX = {sx + 10 * scale * rollFactor, sx + rightSpan, sx + 12 * scale * rollFactor};
        double[] rWingY = {sy - 6 * scale + pitchOffset, sy + 16 * scale + pitchOffset, sy + 10 * scale + pitchOffset};
        LinearGradient rWingGrad = new LinearGradient(
                sx, 0, sx + rightSpan, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.BLACK),
                new Stop(0.4, shipType.getBodyColor()),
                new Stop(1.0, shipType.getAccentColor())
        );
        gc.setFill(rWingGrad);
        gc.fillPolygon(rWingX, rWingY, 3);
        gc.setStroke(shipType.getPrimaryColor());
        gc.setLineWidth(1.2 * scale);
        gc.strokePolygon(rWingX, rWingY, 3);

        // Badan Utama Pesawat 3D (Fuselage Silinder Menghadap Depan)
        double bodyW = 28 * scale * rollFactor;
        double bodyLeft = sx - bodyW / 2.0 + rollOffset * 0.3;
        double bodyTop = sy - 26 * scale + pitchOffset;

        // Moncong Depan
        double[] noseX = {sx + rollOffset * 0.6, bodyLeft, bodyLeft + bodyW};
        double[] noseY = {bodyTop - 12 * scale, bodyTop + 14 * scale, bodyTop + 14 * scale};
        LinearGradient noseGrad = new LinearGradient(
                bodyLeft, 0, bodyLeft + bodyW, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, shipType.getBodyColor()),
                new Stop(0.4, Color.WHITE),
                new Stop(1.0, shipType.getBodyColor())
        );
        gc.setFill(noseGrad);
        gc.fillPolygon(noseX, noseY, 3);

        // Bodi Belakang
        LinearGradient bodyGrad = new LinearGradient(
                bodyLeft, 0, bodyLeft + bodyW, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, shipType.getBodyColor().darker()),
                new Stop(0.35, Color.WHITE),
                new Stop(0.7, shipType.getBodyColor()),
                new Stop(1.0, Color.BLACK)
        );
        gc.setFill(bodyGrad);
        gc.fillRoundRect(bodyLeft, bodyTop + 12 * scale, bodyW, 30 * scale, 6 * scale, 6 * scale);

        // Kanopi Kokpit Kaca 3D
        double cockpitW = bodyW * 0.65;
        double cockpitX = sx - cockpitW / 2.0 + rollOffset * 0.45;
        double cockpitY = bodyTop + 2 * scale;
        LinearGradient cockpitGrad = new LinearGradient(
                cockpitX, cockpitY, cockpitX + cockpitW, cockpitY + 16 * scale, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, shipType.getPrimaryColor()),
                new Stop(0.5, Color.WHITE),
                new Stop(1.0, shipType.getAccentColor())
        );
        gc.setFill(cockpitGrad);
        gc.fillOval(cockpitX, cockpitY, cockpitW, 16 * scale);

        // Perisai Energi Heksagonal (Jika Aegis Drone atau sedang kebal)
        if (droneType == DroneType.SHIELD_DRONE || invulnerableTimer > 0) {
            gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.45 + Math.sin(engineAnim) * 0.25));
            gc.setLineWidth(2.0 * scale);
            gc.strokeOval(sx - w * 0.65, sy - h * 0.7, w * 1.3, h * 1.4);
        }

        gc.restore();
    }

    private void renderThrusterGlow(GraphicsContext gc, double tx, double ty, double tw, double th) {
        RadialGradient glow = new RadialGradient(
                0, 0, tx, ty, tw * 1.4, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(0.3, shipType.getPrimaryColor()),
                new Stop(0.8, shipType.getAccentColor()),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(glow);
        gc.fillOval(tx - tw / 2.0, ty, tw, th);
    }

    private void renderCrosshair3D(GraphicsContext gc, Camera3D camera) {
        // Crosshair diproyeksikan di depan pesawat pada jarak horizon Z = 750
        double targetZ = 750;
        double cx = camera.projectX(this.x * 0.6, targetZ);
        double cy = camera.projectY(this.y * 0.6, targetZ);
        double cSize = 18;

        gc.setStroke(Color.color(shipType.getPrimaryColor().getRed(), shipType.getPrimaryColor().getGreen(), shipType.getPrimaryColor().getBlue(), 0.75));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - cSize / 2.0, cy - cSize / 2.0, cSize, cSize);

        // 4 Garis Bidik
        gc.strokeLine(cx - cSize, cy, cx - cSize * 0.5, cy);
        gc.strokeLine(cx + cSize * 0.5, cy, cx + cSize, cy);
        gc.strokeLine(cx, cy - cSize, cx, cy - cSize * 0.5);
        gc.strokeLine(cx, cy + cSize * 0.5, cx, cy + cSize);

        // Titik tengah
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - 1.5, cy - 1.5, 3, 3);
    }

    private void renderCompanionDrone(GraphicsContext gc, Camera3D camera, double sx, double sy, double scale) {
        double droneOrbitX = sx + Math.cos(droneAnim) * (55 * scale);
        double droneOrbitY = sy - 20 * scale + Math.sin(droneAnim * 1.5) * (14 * scale);
        double droneSize = 14 * scale;

        // Drone Sphere 3D
        RadialGradient droneGrad = new RadialGradient(
                -45, 0.4, droneOrbitX, droneOrbitY, droneSize / 2.0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),
                new Stop(0.5, droneType.getColor()),
                new Stop(1.0, Color.BLACK)
        );
        gc.setFill(droneGrad);
        gc.fillOval(droneOrbitX - droneSize / 2.0, droneOrbitY - droneSize / 2.0, droneSize, droneSize);

        // Drone Aura Ring
        gc.setStroke(droneType.getColor());
        gc.setLineWidth(1.2 * scale);
        gc.strokeOval(droneOrbitX - droneSize * 0.8, droneOrbitY - droneSize * 0.35, droneSize * 1.6, droneSize * 0.7);
    }

    /**
     * Menembakkan laser berkas energi ke horizon 3D.
     */
    public void shoot(List<Laser3D> lasers) {
        double leftWingX = this.x - 28;
        double rightWingX = this.x + 28;
        double startY = this.y + 4;
        double startZ = this.z + 10;

        // Twin Lasers melesat ke target crosshair di horizon
        lasers.add(new Laser3D(leftWingX, startY, startZ, (this.x * 0.6 - leftWingX) * 0.05, (this.y * 0.6 - startY) * 0.05, shipType.getPrimaryColor()));
        lasers.add(new Laser3D(rightWingX, startY, startZ, (this.x * 0.6 - rightWingX) * 0.05, (this.y * 0.6 - startY) * 0.05, shipType.getPrimaryColor()));

        // Jika ada Blaster Drone, tambahkan tembakan drone
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
