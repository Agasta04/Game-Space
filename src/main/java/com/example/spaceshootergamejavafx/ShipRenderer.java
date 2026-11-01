package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.*;
import javafx.scene.effect.DropShadow;

import java.io.InputStream;
import java.util.Random;

/**
 * Merender model visual pesawat pemain dengan elemen 3D nyata,
 * tekstur definisi tinggi (High-Definition 3D Model),
 * triple engine thruster plasma, dan aura bercahaya persis sesuai referensi.
 */
public class ShipRenderer {

    private static Image shipUserExact;
    private static Image ship3DHighRes;
    private static final Random random = new Random();

    static {
        try {
            InputStream isUser = ShipRenderer.class.getResourceAsStream("/player_ship_user_exact.png");
            if (isUser != null) {
                shipUserExact = new Image(isUser);
            }
            InputStream is3D = ShipRenderer.class.getResourceAsStream("/player_ship_3d.png");
            if (is3D != null) {
                ship3DHighRes = new Image(is3D);
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat tekstur 3D kapal: " + e.getMessage());
        }
    }

    /**
     * Merender model pesawat 3D dengan perspektif chase camera dari belakang.
     */
    public static void renderShip(GraphicsContext gc, ShipType ship, double cx, double cy,
                                  double scale, double roll, double pitch, double animTime) {
        gc.save();
        gc.translate(cx, cy);

        // Banking roll rotation
        double rollAngle = roll * 24.0;
        gc.rotate(rollAngle);

        // Pitch lean
        double pitchLean = pitch * 12.0 * scale;
        gc.translate(0, pitchLean);

        // Pilih tekstur yang paling pas
        Image shipImage = (ship == ShipType.TITAN_AEGIS && ship3DHighRes != null)
                ? ship3DHighRes
                : (shipUserExact != null ? shipUserExact : ship3DHighRes);

        // Dimensi rendering pesawat di layar
        double renderW = 120.0 * scale;
        double renderH = 74.0 * scale;
        if (shipImage == ship3DHighRes) {
            renderW = 135.0 * scale;
            renderH = 96.0 * scale;
        }

        // --- 1. EFEK TRIPLE PLASMA ENGINE THRUSTERS (Persis Foto Referensi) ---
        // Foto memiliki 3 mesin: 1 Mesin Pusat Besar + 2 Mesin Samping
        double nozzleY = (renderH * 0.22);
        double centerSpacing = renderW * 0.17;

        double flamePulse = Math.sin(animTime * 6.0) * (4.0 * scale);
        double boostFlame = Math.max(0, -pitch) * 16.0 * scale; // memanjang saat W (maju) ditekan

        // Mesin Kiri & Kanan (Turbo Nozzles)
        renderSingleThrusterFlame(gc, -centerSpacing, nozzleY, 9.0 * scale, 22.0 * scale + flamePulse + boostFlame * 0.7, Color.web("#00e5ff"), Color.web("#80d8ff"));
        renderSingleThrusterFlame(gc, centerSpacing, nozzleY, 9.0 * scale, 22.0 * scale + flamePulse + boostFlame * 0.7, Color.web("#00e5ff"), Color.web("#80d8ff"));

        // Mesin Tengah Utama (Giant Center Plasma Arc)
        renderMainCenterThruster(gc, 0, nozzleY + (2.0 * scale), 14.0 * scale, 34.0 * scale + flamePulse * 1.5 + boostFlame, animTime);

        // --- 2. AURA / OUTER GLOW CAHAYA PESAWAT (PERSIS FOTO REFERENSI) ---
        // Di foto terdapat halo putih/cyan terang yang membingkai siluet pesawat
        RadialGradient auraGrad = new RadialGradient(
                0, 0, 0, 0, renderW * 0.65, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0.0, 0.9, 1.0, 0.35)),
                new Stop(0.7, Color.color(0.4, 0.95, 1.0, 0.15)),
                new Stop(1.0, Color.color(0.0, 0.0, 0.0, 0))
        );
        gc.setFill(auraGrad);
        gc.fillOval(-renderW * 0.65, -renderH * 0.65, renderW * 1.3, renderH * 1.3);

        // --- 3. RENDER 3D MODEL TEXTURE HD ---
        if (shipImage != null) {
            double drawX = -renderW / 2.0;
            double drawY = -renderH / 2.0;
            gc.drawImage(shipImage, drawX, drawY, renderW, renderH);

            // Lapisan Aksen Warna Pesawat (Falcon Cyan / Titan Merah / Phantom Ungu)
            if (ship != ShipType.FALCON_X1) {
                gc.save();
                gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.OVERLAY);
                gc.setFill(Color.color(ship.getPrimaryColor().getRed(), ship.getPrimaryColor().getGreen(), ship.getPrimaryColor().getBlue(), 0.35));
                gc.fillOval(drawX, drawY, renderW, renderH);
                gc.restore();
            }
        } else {
            // Fallback procedur jika gambar gagal termuat
            renderFallbackProceduralShip(gc, ship, renderW, renderH, scale);
        }

        // --- 4. KOKPIT GLASS REFLECTION GLOW ---
        gc.save();
        double cockpitX = 0;
        double cockpitY = -renderH * 0.15;
        RadialGradient cockpitGlow = new RadialGradient(
                -45, 0.3, cockpitX, cockpitY, 12 * scale, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),
                new Stop(0.4, Color.web("#80d8ff")),
                new Stop(0.8, Color.web("#0091ea")),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(cockpitGlow);
        gc.fillOval(cockpitX - 6 * scale, cockpitY - 10 * scale, 12 * scale, 20 * scale);
        gc.restore();

        gc.restore();
    }

    /**
     * Merender semburan api plasma mesin samping.
     */
    private static void renderSingleThrusterFlame(GraphicsContext gc, double x, double y, double width, double height, Color outerCol, Color innerCol) {
        gc.save();
        // Cincin Nozzle
        gc.setFill(Color.web("#e0f7fa"));
        gc.fillOval(x - width / 2.0, y - width * 0.35, width, width * 0.7);

        // Api Plasma Lonjong
        LinearGradient flameGrad = new LinearGradient(
                x, y, x, y + height, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),
                new Stop(0.25, innerCol),
                new Stop(0.7, outerCol),
                new Stop(1.0, Color.color(outerCol.getRed(), outerCol.getGreen(), outerCol.getBlue(), 0.0))
        );
        gc.setFill(flameGrad);
        gc.fillOval(x - width / 2.0, y, width, height);
        gc.restore();
    }

    /**
     * Merender mesin pusat utama yang sangat bertenaga dengan lidah petir plasma biru-putih ke bawah.
     */
    private static void renderMainCenterThruster(GraphicsContext gc, double x, double y, double width, double height, double animTime) {
        gc.save();
        // Cincin Nozzle Inti Putih Terang
        gc.setFill(Color.WHITE);
        gc.fillOval(x - width / 2.0, y - width * 0.35, width, width * 0.7);

        // Semburan Berkas Plasma Utama
        LinearGradient mainGrad = new LinearGradient(
                x, y, x, y + height, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),
                new Stop(0.2, Color.web("#e0f7fa")),
                new Stop(0.5, Color.web("#00e5ff")),
                new Stop(0.85, Color.web("#0091ea")),
                new Stop(1.0, Color.color(0, 0.4, 1.0, 0))
        );
        gc.setFill(mainGrad);
        gc.fillOval(x - width / 2.0, y, width, height);

        // Lidah Petir Listrik Plasma Biru ke Bawah (persis foto referensi!)
        gc.setStroke(Color.web("#80d8ff"));
        gc.setLineWidth(2.0);
        double prevX = x;
        double prevY = y + width * 0.5;
        int segments = 6;
        double segH = height / segments;

        for (int i = 1; i <= segments; i++) {
            double nextX = x + Math.sin(animTime * 12.0 + i) * (width * 0.35);
            double nextY = y + i * segH;
            gc.strokeLine(prevX, prevY, nextX, nextY);
            prevX = nextX;
            prevY = nextY;
        }

        gc.restore();
    }

    private static void renderFallbackProceduralShip(GraphicsContext gc, ShipType ship, double w, double h, double scale) {
        gc.setFill(Color.web("#263238"));
        double[] px = {-w / 2.0, 0, w / 2.0};
        double[] py = {h / 2.0, -h / 2.0, h / 2.0};
        gc.fillPolygon(px, py, 3);
        gc.setStroke(ship.getPrimaryColor());
        gc.setLineWidth(2.0 * scale);
        gc.strokePolygon(px, py, 3);
    }
}
