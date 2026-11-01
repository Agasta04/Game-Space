package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;
import javafx.scene.shape.ArcType;

/**
 * Merender model visual pesawat pemain dengan presisi tinggi, solid, dan tidak mengalami distorsi poligon.
 * Digunakan di Hangar Preview dan saat gameplay berlangsung.
 */
public class ShipRenderer {

    /**
     * Merender model pesawat secara solid dan presisi.
     *
     * @param gc GraphicsContext canvas
     * @param ship Tipe pesawat yang dipilih
     * @param cx Posisi X pusat
     * @param cy Posisi Y pusat
     * @param scale Skala ukuran
     * @param roll Kemiringan roll (-1.0 kiri sampai +1.0 kanan)
     * @param pitch Kemiringan pitch (-1.0 atas sampai +1.0 bawah)
     * @param animTime Waktu animasi untuk pulse mesin & glow
     */
    public static void renderShip(GraphicsContext gc, ShipType ship, double cx, double cy,
                                  double scale, double roll, double pitch, double animTime) {
        gc.save();
        gc.translate(cx, cy);

        // Efek banking roll horizontal compression yang terkontrol
        double rollFactor = 1.0 - Math.abs(roll) * 0.22;
        double rollLean = roll * 18.0 * scale;
        double pitchLean = pitch * 10.0 * scale;

        // Dimensi dasar pesawat
        double bodyW = 28.0 * scale * rollFactor;
        double bodyH = 68.0 * scale;
        double wingSpan = 84.0 * scale;

        // --- 1. TWIN ENGINE THRUSTER FLAME (Api Pendorong Plasma) ---
        double flamePulse = Math.sin(animTime * 4.0) * (5.0 * scale);
        double engineSpacing = 16.0 * scale * rollFactor;
        double flameY = (bodyH * 0.35) + pitchLean;

        renderThrusterFlame(gc, -engineSpacing + rollLean * 0.4, flameY, 11.0 * scale, 22.0 * scale + flamePulse, ship.getPrimaryColor());
        renderThrusterFlame(gc, engineSpacing + rollLean * 0.4, flameY, 11.0 * scale, 22.0 * scale + flamePulse, ship.getPrimaryColor());

        // --- 2. SAYAP UTAMA SESUAI TIPE PESAWAT (SOLID WING GEOMETRY) ---
        if (ship == ShipType.FALCON_X1) {
            // Falcon: Sayap V-Wing Aerodinamis Cepat
            renderFalconWings(gc, ship, wingSpan, bodyH, scale, roll, pitchLean);
        } else if (ship == ShipType.TITAN_AEGIS) {
            // Titan: Sayap Armor Baja Kekar Bersudut
            renderTitanWings(gc, ship, wingSpan, bodyH, scale, roll, pitchLean);
        } else {
            // Phantom: Forward-Swept Wings Sci-Fi
            renderPhantomWings(gc, ship, wingSpan, bodyH, scale, roll, pitchLean);
        }

        // --- 3. BADAN UTAMA PESAWAT (FUSELAGE MONOCOQUE HULL) ---
        // Sasis Utama
        double fLeft = -bodyW / 2.0 + rollLean * 0.3;
        double fTop = -bodyH * 0.5 + pitchLean;

        // Moncong Depan Ramping Aerodinamis
        double[] noseX = {rollLean * 0.5, fLeft, fLeft + bodyW};
        double[] noseY = {fTop - 16.0 * scale, fTop + 24.0 * scale, fTop + 24.0 * scale};

        LinearGradient noseGrad = new LinearGradient(
                fLeft, 0, fLeft + bodyW, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, ship.getBodyColor().darker()),
                new Stop(0.35, Color.WHITE),
                new Stop(0.7, ship.getBodyColor()),
                new Stop(1.0, ship.getBodyColor().darker())
        );
        gc.setFill(noseGrad);
        gc.fillPolygon(noseX, noseY, 3);
        gc.setStroke(ship.getAccentColor());
        gc.setLineWidth(1.5 * scale);
        gc.strokePolygon(noseX, noseY, 3);

        // Bodi Silinder Utama
        LinearGradient bodyGrad = new LinearGradient(
                fLeft, 0, fLeft + bodyW, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, ship.getBodyColor().darker()),
                new Stop(0.3, Color.web("#f5f5f5")),
                new Stop(0.65, ship.getBodyColor()),
                new Stop(1.0, ship.getBodyColor().darker())
        );
        gc.setFill(bodyGrad);
        gc.fillRoundRect(fLeft, fTop + 20.0 * scale, bodyW, bodyH * 0.65, 8.0 * scale, 8.0 * scale);
        gc.setStroke(ship.getAccentColor());
        gc.setLineWidth(1.5 * scale);
        gc.strokeRoundRect(fLeft, fTop + 20.0 * scale, bodyW, bodyH * 0.65, 8.0 * scale, 8.0 * scale);

        // Panel Garis Sci-Fi
        gc.setStroke(ship.getPrimaryColor());
        gc.setLineWidth(1.8 * scale);
        gc.strokeLine(fLeft + 3 * scale, fTop + 38 * scale, fLeft + bodyW - 3 * scale, fTop + 38 * scale);

        // --- 4. KOKPIT KACA KRISTAL 3D (CANOPY) ---
        double cpW = bodyW * 0.62;
        double cpH = 22.0 * scale;
        double cpX = -cpW / 2.0 + rollLean * 0.45;
        double cpY = fTop + 4.0 * scale;

        LinearGradient canopyGrad = new LinearGradient(
                cpX, cpY, cpX + cpW, cpY + cpH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, ship.getPrimaryColor()),
                new Stop(0.4, Color.WHITE),
                new Stop(1.0, ship.getAccentColor())
        );
        gc.setFill(canopyGrad);
        gc.fillOval(cpX, cpY, cpW, cpH);

        // Kilau pantulan cahaya kaca
        gc.setFill(Color.color(1, 1, 1, 0.75));
        gc.fillOval(cpX + 2 * scale, cpY + 2 * scale, cpW * 0.45, 6 * scale);

        // --- 5. MERIAM LASER PODS PADA UJUNG SAYAP ---
        double cannonOffset = (wingSpan * 0.5) * (1.0 - Math.abs(roll) * 0.2);
        renderCannonPod(gc, -cannonOffset, fTop + 35 * scale, scale, ship.getPrimaryColor());
        renderCannonPod(gc, cannonOffset, fTop + 35 * scale, scale, ship.getPrimaryColor());

        gc.restore();
    }

    private static void renderFalconWings(GraphicsContext gc, ShipType ship, double wingSpan, double bodyH, double scale, double roll, double pitchLean) {
        double leftSpan = (wingSpan * 0.5) * (1.0 - roll * 0.35);
        double rightSpan = (wingSpan * 0.5) * (1.0 + roll * 0.35);
        double wingY = 4.0 * scale + pitchLean;

        // Sayap Kiri
        double[] lx = {-8 * scale, -leftSpan, -14 * scale};
        double[] ly = {wingY, wingY + 24 * scale, wingY + 16 * scale};
        gc.setFill(ship.getAccentColor());
        gc.fillPolygon(lx, ly, 3);
        gc.setStroke(ship.getPrimaryColor());
        gc.setLineWidth(1.8 * scale);
        gc.strokePolygon(lx, ly, 3);

        // Sayap Kanan
        double[] rx = {8 * scale, rightSpan, 14 * scale};
        double[] ry = {wingY, wingY + 24 * scale, wingY + 16 * scale};
        gc.setFill(ship.getAccentColor());
        gc.fillPolygon(rx, ry, 3);
        gc.setStroke(ship.getPrimaryColor());
        gc.setLineWidth(1.8 * scale);
        gc.strokePolygon(rx, ry, 3);
    }

    private static void renderTitanWings(GraphicsContext gc, ShipType ship, double wingSpan, double bodyH, double scale, double roll, double pitchLean) {
        double leftSpan = (wingSpan * 0.55) * (1.0 - roll * 0.3);
        double rightSpan = (wingSpan * 0.55) * (1.0 + roll * 0.3);
        double wingY = -2.0 * scale + pitchLean;

        // Armor sayap bersudut tebal
        double[] lx = {-10 * scale, -leftSpan, -leftSpan + 10 * scale, -12 * scale};
        double[] ly = {wingY, wingY + 14 * scale, wingY + 34 * scale, wingY + 26 * scale};
        gc.setFill(ship.getBodyColor());
        gc.fillPolygon(lx, ly, 4);
        gc.setStroke(ship.getPrimaryColor());
        gc.setLineWidth(2.2 * scale);
        gc.strokePolygon(lx, ly, 4);

        double[] rx = {10 * scale, rightSpan, rightSpan - 10 * scale, 12 * scale};
        double[] ry = {wingY, wingY + 14 * scale, wingY + 34 * scale, wingY + 26 * scale};
        gc.setFill(ship.getBodyColor());
        gc.fillPolygon(rx, ry, 4);
        gc.setStroke(ship.getPrimaryColor());
        gc.setLineWidth(2.2 * scale);
        gc.strokePolygon(rx, ry, 4);
    }

    private static void renderPhantomWings(GraphicsContext gc, ShipType ship, double wingSpan, double bodyH, double scale, double roll, double pitchLean) {
        double leftSpan = (wingSpan * 0.52) * (1.0 - roll * 0.35);
        double rightSpan = (wingSpan * 0.52) * (1.0 + roll * 0.35);
        double wingY = 10.0 * scale + pitchLean;

        // Forward-swept wing (sayap maju ke depan)
        double[] lx = {-8 * scale, -leftSpan, -leftSpan + 8 * scale, -10 * scale};
        double[] ly = {wingY, wingY - 14 * scale, wingY + 16 * scale, wingY + 14 * scale};
        gc.setFill(ship.getAccentColor());
        gc.fillPolygon(lx, ly, 4);
        gc.setStroke(ship.getPrimaryColor());
        gc.setLineWidth(1.8 * scale);
        gc.strokePolygon(lx, ly, 4);

        double[] rx = {8 * scale, rightSpan, rightSpan - 8 * scale, 10 * scale};
        double[] ry = {wingY, wingY - 14 * scale, wingY + 16 * scale, wingY + 14 * scale};
        gc.setFill(ship.getAccentColor());
        gc.fillPolygon(rx, ry, 4);
        gc.setStroke(ship.getPrimaryColor());
        gc.setLineWidth(1.8 * scale);
        gc.strokePolygon(rx, ry, 4);
    }

    private static void renderCannonPod(GraphicsContext gc, double cx, double cy, double scale, Color laserColor) {
        gc.setFill(Color.web("#212121"));
        gc.fillRoundRect(cx - 3 * scale, cy - 8 * scale, 6 * scale, 16 * scale, 2 * scale, 2 * scale);
        // Ujung laras bercahaya
        gc.setFill(laserColor);
        gc.fillOval(cx - 2.5 * scale, cy - 10 * scale, 5 * scale, 4 * scale);
    }

    private static void renderThrusterFlame(GraphicsContext gc, double tx, double ty, double tw, double th, Color flameColor) {
        RadialGradient flameGrad = new RadialGradient(
                0, 0, tx, ty + th * 0.3, th * 0.7, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),
                new Stop(0.3, flameColor),
                new Stop(0.7, Color.color(flameColor.getRed(), flameColor.getGreen(), flameColor.getBlue(), 0.35)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(flameGrad);
        gc.fillOval(tx - tw / 2.0, ty, tw, th);
    }
}

