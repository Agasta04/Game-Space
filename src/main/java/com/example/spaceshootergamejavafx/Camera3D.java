package com.example.spaceshootergamejavafx;

/**
 * Kamera 3D Chase Camera dinamis yang mengikuti pesawat pemain di sumbu X dan Z (Maju-Mundur).
 */
public class Camera3D {
    public static final double FOV = 440.0;
    public static final double VANISH_X = SpaceShooter.WIDTH / 2.0;
    public static final double VANISH_Y = SpaceShooter.HEIGHT / 2.0 + 35.0;

    private double x = 0;
    private double y = -40;
    private double z = -140;

    public void update(double shipX, double shipY, double shipZ) {
        // Kamera berada di belakang dan sedikit di atas pesawat
        double targetCamX = shipX * 0.35;
        double targetCamY = shipY - 80.0;
        double targetCamZ = shipZ - 150.0;

        // Smooth camera lerp
        this.x += (targetCamX - this.x) * 0.15;
        this.y += (targetCamY - this.y) * 0.15;
        this.z += (targetCamZ - this.z) * 0.20;
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
