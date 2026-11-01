package com.example.spaceshootergamejavafx;

import javafx.scene.paint.Color;

/**
 * Tipe pesawat luar angkasa yang dapat dipilih pemain di Hangar.
 */
public enum ShipType {
    FALCON_X1(
            "FALCON X-1",
            "Tipe: Interceptor Lincah\nDirancang untuk manuver cepat dan respon instan.",
            Color.web("#00e5ff"), // Cyan Utama
            Color.web("#0288d1"), // Biru Aksen
            Color.web("#ffffff"), // Titanium White Body
            8.5,                  // Speed
            0.18,                 // Handling (Roll lerp)
            90.0                  // Magnet Range
    ),
    TITAN_AEGIS(
            "TITAN AEGIS",
            "Tipe: Heavy Cruiser Baja\nBodi kokoh berlapis armor titanium tebal dengan stabilitas tinggi.",
            Color.web("#ff1744"), // Merah Crimson
            Color.web("#ffea00"), // Emas Aksen
            Color.web("#37474f"), // Dark Metallic Gray Body
            6.5,                  // Speed
            0.12,                 // Handling
            130.0                 // Magnet Range
    ),
    PHANTOM_ECLIPSE(
            "PHANTOM ECLIPSE",
            "Tipe: Void Harvester\nMemiliki teknologi gravitasi kosmik untuk menyedot galon dari jarak jauh.",
            Color.web("#d500f9"), // Ungu Neon
            Color.web("#00e676"), // Hijau Emerald
            Color.web("#212121"), // Stealth Black Body
            7.5,                  // Speed
            0.15,                 // Handling
            220.0                 // Super Magnet Range!
    );

    private final String name;
    private final String description;
    private final Color primaryColor;
    private final Color accentColor;
    private final Color bodyColor;
    private final double speed;
    private final double handling;
    private final double magnetRange;

    ShipType(String name, String description, Color primaryColor, Color accentColor, Color bodyColor,
             double speed, double handling, double magnetRange) {
        this.name = name;
        this.description = description;
        this.primaryColor = primaryColor;
        this.accentColor = accentColor;
        this.bodyColor = bodyColor;
        this.speed = speed;
        this.handling = handling;
        this.magnetRange = magnetRange;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public Color getAccentColor() {
        return accentColor;
    }

    public Color getBodyColor() {
        return bodyColor;
    }

    public double getSpeed() {
        return speed;
    }

    public double getHandling() {
        return handling;
    }

    public double getMagnetRange() {
        return magnetRange;
    }
}

