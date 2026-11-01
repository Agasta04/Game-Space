package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;

/**
 * Kelas dasar untuk seluruh objek di dunia 3D (Pesawat, UFO, Galon, Asteroid, Laser).
 */
public abstract class Entity3D implements Comparable<Entity3D> {
    protected double x;
    protected double y;
    protected double z;
    protected double baseRadius;
    protected boolean dead = false;

    public Entity3D(double x, double y, double z, double baseRadius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.baseRadius = baseRadius;
    }

    public abstract void update();

    public abstract void render(GraphicsContext gc, Camera3D camera);

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getBaseRadius() {
        return baseRadius;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    /**
     * Pengecekan tabrakan di ruang 3D (X, Y, dan kedalaman Z).
     */
    public boolean checkCollision3D(Entity3D other, double zTolerance) {
        if (Math.abs(this.z - other.z) > zTolerance) return false;
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double distSq = dx * dx + dy * dy;
        double totalRadius = this.baseRadius + other.baseRadius;
        return distSq <= (totalRadius * totalRadius);
    }

    /**
     * Urutan rendering Z-Sorting (Painter's Algorithm):
     * Objek dengan Z paling besar (paling jauh) dirender terlebih dahulu,
     * sehingga objek yang lebih dekat ke kamera menimpa objek di belakangnya secara alami.
     */
    @Override
    public int compareTo(Entity3D other) {
        return Double.compare(other.z, this.z);
    }
}

