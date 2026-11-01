package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.shape.ArcType;

import java.util.List;
import java.util.Random;

/**
 * Roket Luar Angkasa Pemain dengan Animasi 3D Procedural, Banking Roll, dan Twin Plasma Thruster.
 */
public class Player extends GameObject {

    public static final int BASE_WIDTH = 54;
    public static final int BASE_HEIGHT = 70;
    private static final double SPEED = 6.5;

    private boolean moveLeft;
    private boolean moveRight;
    private boolean moveForward;
    private boolean moveBackward;

    private int health = 3;
    private int maxHealth = 3;
    private boolean dead = false;

    // Nilai tilt/roll 3D (-1.0 = miring penuh ke kiri, +1.0 = miring penuh ke kanan)
    private double currentRoll = 0.0;
    private double thrusterAnim = 0.0;
    private final Random random = new Random();

    // Invulnerability timer setelah terkena hit
    private double invulnerableTime = 0.0;

    public Player(double x, double y) {
        super(x, y, BASE_WIDTH, BASE_HEIGHT);
    }

    @Override
    public double getWidth() {
        return BASE_WIDTH;
    }

    @Override
    public double getHeight() {
        return BASE_HEIGHT;
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

    public boolean isInvulnerable() {
        return invulnerableTime > 0;
    }

    public boolean takeDamage() {
        if (invulnerableTime <= 0) {
            health--;
            invulnerableTime = 0.8; // Jeda singkat (0.8 detik) agar pemain tidak terkena multi-damage instan
            SoundManager.playHit();
            if (health <= 0) {
                dead = true;
            }
            return true;
        }
        return false;
    }

    public void hit() {
        takeDamage();
    }

    @Override
    public void update() {
        // Gerakan mulus
        if (moveLeft && x - BASE_WIDTH / 2.0 - SPEED >= 10) {
            x -= SPEED;
        }
        if (moveRight && x + BASE_WIDTH / 2.0 + SPEED <= SpaceShooter.WIDTH - 10) {
            x += SPEED;
        }
        if (moveForward && y - BASE_HEIGHT / 2.0 - SPEED >= 20) {
            y -= SPEED;
        }
        if (moveBackward && y + BASE_HEIGHT / 2.0 + SPEED <= SpaceShooter.HEIGHT - 20) {
            y += SPEED;
        }

        // Animasi 3D Banking Roll (Transisi sudut kemiringan roket)
        double targetRoll = 0.0;
        if (moveLeft) targetRoll = -0.7;
        if (moveRight) targetRoll = 0.7;
        currentRoll += (targetRoll - currentRoll) * 0.15;

        // Animasi pulse pendorong roket
        thrusterAnim += 0.25;

        // Kurangi waktu kebal
        if (invulnerableTime > 0) {
            invulnerableTime -= 0.016;
        }
    }

    /**
     * Menghasilkan partikel api dan asap dari nozzle pendorong mesin roket.
     */
    public void emitThrusterParticles(List<Particle> particles) {
        double rollFactor = 1.0 - Math.abs(currentRoll) * 0.2;
        double nozzleSpacing = 12 * rollFactor;
        double nozzleY = y + BASE_HEIGHT / 2.0 - 4;

        // Nozzle Kiri & Kanan
        double[] nozzleXs = {x - nozzleSpacing + currentRoll * 4, x + nozzleSpacing + currentRoll * 4};

        for (double nX : nozzleXs) {
            double pVx = (random.nextDouble() - 0.5) * 1.5 - currentRoll * 2.0;
            double pVy = 3.5 + random.nextDouble() * 4.0;
            Color flameColor = (random.nextDouble() < 0.6)
                    ? Color.rgb(40, 180, 255, 0.9)   // Cyan-blue plasma core
                    : Color.rgb(255, 200, 50, 0.85);  // Hot gold sparks
            particles.add(new Particle(nX, nozzleY, pVx, pVy, 5 + random.nextDouble() * 4, 0.25, flameColor));
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();

        // Efek kedip jika sedang invulnerable
        if (invulnerableTime > 0 && ((int) (invulnerableTime * 20) % 2 == 0)) {
            gc.restore();
            return;
        }

        double rollFactor = 1.0 - Math.abs(currentRoll) * 0.3; // Efek kompresi 3D saat roll
        double rollOffset = currentRoll * 14; // Pergeseran perspektif cockpit

        // --- 1. TWIN THRUSTER FLAMES (Api Pendorong Plasma) ---
        double nozzleLeftX = x - 12 * rollFactor + rollOffset * 0.5;
        double nozzleRightX = x + 12 * rollFactor + rollOffset * 0.5;
        double flameBaseY = y + BASE_HEIGHT / 2.0 - 4;
        double flamePulse = Math.sin(thrusterAnim) * 5;

        // Render lidah api pendorong kiri
        renderPlasmaFlame(gc, nozzleLeftX, flameBaseY, 10 * rollFactor, 22 + flamePulse);
        // Render lidah api pendorong kanan
        renderPlasmaFlame(gc, nozzleRightX, flameBaseY, 10 * rollFactor, 22 + flamePulse);

        // --- 2. SAYAP DELTA 3D (WINGS) DENGAN PERSPEKTIF ROLL ---
        double wingSpanLeft = (BASE_WIDTH / 2.0 + 8) * (1.0 - currentRoll * 0.5);
        double wingSpanRight = (BASE_WIDTH / 2.0 + 8) * (1.0 + currentRoll * 0.5);
        double wingY = y + 10;
        double wingTipY = y + BASE_HEIGHT / 2.0 + 2;

        // Sayap Kiri (Poligon)
        double[] leftWingX = {x - 8 * rollFactor, x - wingSpanLeft, x - 10 * rollFactor};
        double[] leftWingY = {wingY, wingTipY, wingTipY - 6};
        LinearGradient leftWingGrad = new LinearGradient(
                x - wingSpanLeft, 0, x, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#d32f2f")), // Aksen merah sporty
                new Stop(0.6, Color.web("#78909c")),
                new Stop(1.0, Color.web("#37474f"))
        );
        gc.setFill(leftWingGrad);
        gc.fillPolygon(leftWingX, leftWingY, 3);
        gc.setStroke(Color.web("#ef5350"));
        gc.setLineWidth(1.2);
        gc.strokePolygon(leftWingX, leftWingY, 3);

        // Sayap Kanan (Poligon)
        double[] rightWingX = {x + 8 * rollFactor, x + wingSpanRight, x + 10 * rollFactor};
        double[] rightWingY = {wingY, wingTipY, wingTipY - 6};
        LinearGradient rightWingGrad = new LinearGradient(
                x, 0, x + wingSpanRight, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#37474f")),
                new Stop(0.4, Color.web("#78909c")),
                new Stop(1.0, Color.web("#d32f2f"))
        );
        gc.setFill(rightWingGrad);
        gc.fillPolygon(rightWingX, rightWingY, 3);
        gc.setStroke(Color.web("#ef5350"));
        gc.setLineWidth(1.2);
        gc.strokePolygon(rightWingX, rightWingY, 3);

        // Laser Cannon Pods di Ujung Sayap
        gc.setFill(Color.web("#263238"));
        gc.fillRect(x - wingSpanLeft, wingTipY - 14, 4, 12);
        gc.fillRect(x + wingSpanRight - 4, wingTipY - 14, 4, 12);
        // Ujung laras laser neon cyan
        gc.setFill(Color.web("#00e5ff"));
        gc.fillRect(x - wingSpanLeft, wingTipY - 16, 4, 3);
        gc.fillRect(x + wingSpanRight - 4, wingTipY - 16, 4, 3);

        // --- 3. BADAN UTAMA ROKET 3D (FUSELAGE) ---
        double bodyW = (BASE_WIDTH * 0.45) * rollFactor;
        double bodyLeft = x - bodyW / 2.0 + rollOffset * 0.3;
        double bodyTop = y - BASE_HEIGHT / 2.0;

        // Moncong Roket Kerucut Aerodinamis 3D
        double[] noseX = {x + rollOffset * 0.5, bodyLeft, bodyLeft + bodyW};
        double[] noseY = {bodyTop, bodyTop + 24, bodyTop + 24};
        LinearGradient noseGrad = new LinearGradient(
                bodyLeft, 0, bodyLeft + bodyW, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#b0bec5")),
                new Stop(0.4, Color.web("#ffffff")), // Highlighting specular 3D
                new Stop(0.8, Color.web("#cfd8dc")),
                new Stop(1.0, Color.web("#78909c"))
        );
        gc.setFill(noseGrad);
        gc.fillPolygon(noseX, noseY, 3);

        // Tabung Badan Roket (Cylindrical Body)
        LinearGradient bodyGrad = new LinearGradient(
                bodyLeft, 0, bodyLeft + bodyW, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#90a4ae")),
                new Stop(0.3, Color.web("#ffffff")), // Kilau silinder logam
                new Stop(0.7, Color.web("#eceff1")),
                new Stop(1.0, Color.web("#607d8b"))
        );
        gc.setFill(bodyGrad);
        gc.fillRoundRect(bodyLeft, bodyTop + 22, bodyW, BASE_HEIGHT - 28, 6, 6);

        // Garis Panel Sci-Fi di Badan Roket
        gc.setStroke(Color.web("#0288d1"));
        gc.setLineWidth(1.5);
        gc.strokeLine(bodyLeft + 2, bodyTop + 40, bodyLeft + bodyW - 2, bodyTop + 40);

        // --- 4. KACA KOKPIT 3D (COCKPIT CANOPY) ---
        double cockpitW = bodyW * 0.6;
        double cockpitX = x - cockpitW / 2.0 + rollOffset * 0.4;
        double cockpitY = bodyTop + 14;
        LinearGradient cockpitGrad = new LinearGradient(
                cockpitX, cockpitY, cockpitX + cockpitW, cockpitY + 22, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#00e5ff")),
                new Stop(0.5, Color.web("#0288d1")),
                new Stop(1.0, Color.web("#01579b"))
        );
        gc.setFill(cockpitGrad);
        gc.fillOval(cockpitX, cockpitY, cockpitW, 20);

        // Pantulan Cahaya Kaca (Gleam highlight)
        gc.setFill(Color.color(1, 1, 1, 0.75));
        gc.fillOval(cockpitX + 2, cockpitY + 3, cockpitW * 0.4, 6);

        // --- 5. PERISAI SHIELD GLOW (Jika Ada) ---
        if (invulnerableTime > 0) {
            gc.setStroke(Color.color(0.2, 0.8, 1.0, 0.7));
            gc.setLineWidth(2.5);
            gc.strokeOval(x - BASE_WIDTH / 2.0 - 6, y - BASE_HEIGHT / 2.0 - 6, BASE_WIDTH + 12, BASE_HEIGHT + 12);
        }

        gc.restore();
    }

    private void renderPlasmaFlame(GraphicsContext gc, double fX, double fY, double fW, double fH) {
        // Inti api plasma dalam
        LinearGradient flameGrad = new LinearGradient(
                0, fY, 0, fY + fH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#ffffff")),
                new Stop(0.25, Color.web("#00e5ff")),
                new Stop(0.7, Color.web("#2979ff")),
                new Stop(1.0, Color.color(0.1, 0.3, 1.0, 0.0))
        );
        gc.setFill(flameGrad);
        gc.fillOval(fX - fW / 2.0, fY, fW, fH);

        // Inti api putih terkonsentrasi
        gc.setFill(Color.color(1, 1, 1, 0.85));
        gc.fillOval(fX - (fW * 0.4) / 2.0, fY, fW * 0.4, fH * 0.5);
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

    /**
     * Tembakan Laser Ganda (Twin Laser Cannons) dari sayap kiri dan kanan!
     */
    public void shoot(List<GameObject> newObjects) {
        double rollFactor = 1.0 - Math.abs(currentRoll) * 0.3;
        double wingSpan = (BASE_WIDTH / 2.0 + 6) * rollFactor;

        Bullet bulletLeft = new Bullet(x - wingSpan, y - 6);
        Bullet bulletRight = new Bullet(x + wingSpan, y - 6);
        newObjects.add(bulletLeft);
        newObjects.add(bulletRight);

        SoundManager.playLaser();
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    @Override
    public boolean isDead() {
        return dead;
    }
}
