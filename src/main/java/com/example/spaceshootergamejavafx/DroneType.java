package com.example.spaceshootergamejavafx;

import javafx.scene.paint.Color;

/**
 * Drone pendukung (Companion / Support) yang bisa dipilih pemain.
 */
public enum DroneType {
    MAGNET_DRONE(
            "GRAVITY DRONE",
            "Membantu menyedot galon air kosmik dari radius lebih jauh.",
            Color.web("#00e5ff"),
            1.6 // Magnet multiplier
    ),
    BLASTER_DRONE(
            "PLASMA DRONE",
            "Memberikan tembakan peluru plasma otomatis ke horizon setiap beberapa detik.",
            Color.web("#ff9100"),
            1.0
    ),
    SHIELD_DRONE(
            "AEGIS DRONE",
            "Membentuk proyeksi perisai energi heksagonal di sekitar pesawat.",
            Color.web("#00e676"),
            1.0
    );

    private final String title;
    private final String desc;
    private final Color color;
    private final double boostFactor;

    DroneType(String title, String desc, Color color, double boostFactor) {
        this.title = title;
        this.desc = desc;
        this.color = color;
        this.boostFactor = boostFactor;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }

    public Color getColor() {
        return color;
    }

    public double getBoostFactor() {
        return boostFactor;
    }
}

