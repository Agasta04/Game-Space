package com.example.spaceshootergamejavafx;

/**
 * Mengelola proyeksi perspektif 3D, orientasi kamera, dan titik hilang (Vanishing Point).
 */
public class Camera3D {
    public static final double FOV = 460.0; // Jarak fokal proyeksi
    public static final double VANISH_X = SpaceShooter.WIDTH / 2.0;
    public static final double VANISH_Y = SpaceShooter.HEIGHT / 2.0 + 30.0;

    private double x = 0;
    private double y = -50;
    private double z = -90;

    private double targetX = 0;
    private double targetY = -50;

    public void update(double targetShipX, double targetShipY) {
        // Kamera mengikuti posisi pesawat pemain dengan lerp halus
        this.targetX = targetShipX * 0.35;
        this.targetY = -50 + (targetShipY + 120) * 0.25;

        this.x += (this.targetX - this.x) * 0.12;
        this.y += (this.targetY - this.y) * 0.12;
    }

    public double projectX(double worldX, double worldZ) {
        double relativeZ = worldZ - this.z;
        if (relativeZ <= 5.0) relativeZ = 5.0;
        double scale = FOV / relativeZ;
        return VANISH_X + (worldX - this.x) * scale;
    }

    public double projectY(double worldY, double worldZ) {
        double relativeZ = worldZ - this.z;
        if (relativeZ <= 5.0) relativeZ = 5.0;
        double scale = FOV / relativeZ;
        return VANISH_Y + (worldY - this.y) * scale;
    }

    public double getScale(double worldZ) {
        double relativeZ = worldZ - this.z;
        if (relativeZ <= 5.0) relativeZ = 5.0;
        return FOV / relativeZ;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}

