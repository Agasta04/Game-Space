package com.example.spaceshootergamejavafx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

import java.util.*;

/**
 * Merender model pesawat tempur 3D Asli (True 3D Polyhedral Aerospace Geometry):
 * - Menggunakan koordinat vertex 3D asli (X, Y, Z).
 * - Matriks rotasi 3D lengkap: Roll (Belok A/D), Pitch (Maju/Mundur W/S), dan Yaw (Hangar Turntable).
 * - Proyeksi perspektif 3D, Face Normal, Pencahayaan Directional Shading, dan Painter's Algorithm Depth Sorting.
 * - Triple plasma rocket thrusters yang terpasang presisi di ujung 3 nozzle silinder 3D.
 */
public class ShipRenderer {

    private static final True3DShipModel shipModel = new True3DShipModel();
    private static final Random random = new Random();

    // Vektor Sumber Cahaya 3D (dari atas-depan sedikit kiri)
    private static final double LIGHT_X = 0.25;
    private static final double LIGHT_Y = -0.85;
    private static final double LIGHT_Z = -0.45;
    private static final double LIGHT_LEN = Math.sqrt(LIGHT_X * LIGHT_X + LIGHT_Y * LIGHT_Y + LIGHT_Z * LIGHT_Z);
    private static final double NL_X = LIGHT_X / LIGHT_LEN;
    private static final double NL_Y = LIGHT_Y / LIGHT_LEN;
    private static final double NL_Z = LIGHT_Z / LIGHT_LEN;

    /**
     * Merender pesawat dalam ruang 3D.
     *
     * @param gc GraphicsContext canvas
     * @param ship Tipe pesawat yang dipilih
     * @param cx Posisi X layar
     * @param cy Posisi Y layar
     * @param scale Skala ukuran
     * @param roll Kemiringan roll (banking saat A/D)
     * @param pitch Kemiringan pitch (tunduk/tengadah saat W/S)
     * @param animTime Waktu animasi pulse mesin / rotasi
     */
    public static void renderShip(GraphicsContext gc, ShipType ship, double cx, double cy,
                                  double scale, double roll, double pitch, double animTime) {
        double coordinatedYaw = -roll * 0.35;
        renderShipFull(gc, ship, cx, cy, scale, roll, pitch, coordinatedYaw, animTime, true);
    }

    /**
     * Render lengkap dengan sudut Yaw (untuk Hangar 360 Turntable).
     */
    public static void renderShipFull(GraphicsContext gc, ShipType ship, double cx, double cy,
                                      double scale, double roll, double pitch, double yaw,
                                      double animTime, boolean drawThrusters) {

        List<True3DShipModel.Vec3> verts = shipModel.baseVertices;
        int numVerts = verts.size();

        // Array koordinat tertransformasi 3D
        double[] tx = new double[numVerts];
        double[] ty = new double[numVerts];
        double[] tz = new double[numVerts];

        // Array proyeksi layar 2D
        double[] sx = new double[numVerts];
        double[] sy = new double[numVerts];

        // Precompute Trigonometri Rotasi 3D
        // 1. Yaw (Rotasi sumbu Y)
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);

        // 2. Pitch (Rotasi sumbu X)
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);

        // 3. Roll (Rotasi sumbu Z)
        double cosR = Math.cos(roll);
        double sinR = Math.sin(roll);

        double cameraDist = 260.0;
        double focalLength = 260.0;
        double baseScale = scale * 0.95;

        // Transformasi semua titik vertex 3D
        for (int i = 0; i < numVerts; i++) {
            True3DShipModel.Vec3 v = verts.get(i);
            double x0 = v.x;
            double y0 = v.y;
            double z0 = v.z;

            // Yaw: X-Z plane
            double x1 = x0 * cosY + z0 * sinY;
            double y1 = y0;
            double z1 = -x0 * sinY + z0 * cosY;

            // Pitch: Y-Z plane
            double x2 = x1;
            double y2 = y1 * cosP - z1 * sinP;
            double z2 = y1 * sinP + z1 * cosP;

            // Roll: X-Y plane
            double x3 = x2 * cosR - y2 * sinR;
            double y3 = x2 * sinR + y2 * cosR;
            double z3 = z2;

            tx[i] = x3;
            ty[i] = y3;
            tz[i] = z3;

            // Proyeksi Perspektif 3D ke Layar
            // Z positif menjauh dari kamera, Z negatif mendekat ke kamera
            double dist = cameraDist - z3;
            if (dist < 10.0) dist = 10.0;
            double pScale = (focalLength / dist) * baseScale;

            sx[i] = cx + x3 * pScale;
            sy[i] = cy + y3 * pScale;
        }

        // Siapkan Wajah Poligon 3D untuk Depth Sorting
        List<True3DShipModel.Face3D> sortedFaces = new ArrayList<>(shipModel.faces);

        for (True3DShipModel.Face3D face : sortedFaces) {
            int[] idx = face.vertexIndices;
            int n = idx.length;

            // Hitung rata-rata Z untuk Painter's Algorithm
            double sumZ = 0;
            for (int id : idx) {
                sumZ += tz[id];
            }
            face.avgZ = sumZ / n;

            // Hitung Normal Permukaan 3D (Face Normal)
            double ux = tx[idx[1]] - tx[idx[0]];
            double uy = ty[idx[1]] - ty[idx[0]];
            double uz = tz[idx[1]] - tz[idx[0]];

            double vx = tx[idx[2]] - tx[idx[0]];
            double vy = ty[idx[2]] - ty[idx[0]];
            double vz = tz[idx[2]] - tz[idx[0]];

            double nx = uy * vz - uz * vy;
            double ny = uz * vx - ux * vz;
            double nz = ux * vy - uy * vx;

            double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0.0001) {
                nx /= len;
                ny /= len;
                nz /= len;
            }

            // Directional Shading (Lambertian dot product)
            double dot = nx * NL_X + ny * NL_Y + nz * NL_Z;
            double light = 0.38 + Math.max(0, dot) * 0.62;
            face.lighting = Math.min(1.0, Math.max(0.22, light));
        }

        // Urutkan Wajah dari yang paling belakang ke depan (Back to Front)
        Collections.sort(sortedFaces);

        gc.save();

        // 1. AURA CAHAYA HALO 3D SEKITAR PESAWAT
        double shipSpan = 140 * scale;
        RadialGradient shipHalo = new RadialGradient(
                0, 0, cx, cy, shipSpan * 0.65, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0.0, 0.85, 1.0, 0.28)),
                new Stop(0.7, Color.color(0.0, 0.5, 1.0, 0.08)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        gc.setFill(shipHalo);
        gc.fillOval(cx - shipSpan * 0.65, cy - shipSpan * 0.5, shipSpan * 1.3, shipSpan);

        // 2. GAMBAR SEMUA WAJAH POLIGON 3D
        double[] polyX = new double[4];
        double[] polyY = new double[4];

        Color primaryCol = ship.getPrimaryColor();
        Color bodyCol = ship.getBodyColor();
        Color accentCol = ship.getAccentColor();

        for (True3DShipModel.Face3D face : sortedFaces) {
            int[] idx = face.vertexIndices;
            int n = idx.length;

            for (int j = 0; j < n; j++) {
                polyX[j] = sx[idx[j]];
                polyY[j] = sy[idx[j]];
            }

            if (face.isGlow) {
                // Kaca kokpit / Inti plasma mesin
                gc.setFill(face.baseColor);
                gc.fillPolygon(polyX, polyY, n);

                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.0 * scale);
                gc.strokePolygon(polyX, polyY, n);
            } else {
                // Panel Baja Pesawat dengan Pencahayaan 3D Nyata
                Color base = face.baseColor;
                // Tint panel dengan warna tema pesawat
                if (ship != ShipType.FALCON_X1) {
                    base = blendColors(base, bodyCol, 0.35);
                }

                Color litColor = applyLighting(base, face.lighting);
                gc.setFill(litColor);
                gc.fillPolygon(polyX, polyY, n);

                // Garis panel cyber halus bersudut
                Color seamColor = Color.color(
                        primaryCol.getRed() * 0.6 + litColor.getRed() * 0.4,
                        primaryCol.getGreen() * 0.6 + litColor.getGreen() * 0.4,
                        primaryCol.getBlue() * 0.6 + litColor.getBlue() * 0.4,
                        0.45
                );
                gc.setStroke(seamColor);
                gc.setLineWidth(0.85 * scale);
                gc.strokePolygon(polyX, polyY, n);
            }
        }

        // 3. TRIPLE PLASMA ENGINE FLAMES DARI 3 NOZZLE 3D
        if (drawThrusters && (tz[verts.size() - 1] < 150)) { // Hanya tampak jika menghadap menjauh/sebagian
            render3DThrusters(gc, verts, sx, sy, scale, animTime, pitch, primaryCol);
        }

        gc.restore();
    }

    private static void render3DThrusters(GraphicsContext gc, List<True3DShipModel.Vec3> verts,
                                          double[] sx, double[] sy, double scale,
                                          double animTime, double pitch, Color flameColor) {
        // Ambil titik pusat 3 nozzle dari vertex terakhir yang dibentuk di True3DShipModel
        // Sesuai konstruksi:
        // Center Nozzle: vertex index (numVerts - 1)
        // Right Nozzle: vertex index (numVerts - 18)
        // Left Nozzle: vertex index (numVerts - 35)
        int numVerts = verts.size();
        int centerIdx = numVerts - 1;
        int rightIdx = numVerts - 18;
        int leftIdx = numVerts - 35;

        double flamePulse = Math.sin(animTime * 6.5) * (4.5 * scale);
        double boost = Math.max(0, -pitch) * 18.0 * scale;

        // 1. Mesin Kiri & Kanan (Twin Turbo Thrusters)
        renderSingleJetFlame(gc, sx[leftIdx], sy[leftIdx], 9.0 * scale, 22.0 * scale + flamePulse + boost * 0.7, flameColor);
        renderSingleJetFlame(gc, sx[rightIdx], sy[rightIdx], 9.0 * scale, 22.0 * scale + flamePulse + boost * 0.7, flameColor);

        // 2. Mesin Pusat Utama (Giant Center Plasma Jet)
        renderMainPlasmaFlame(gc, sx[centerIdx], sy[centerIdx], 14.0 * scale, 36.0 * scale + flamePulse * 1.5 + boost, animTime);
    }

    private static void renderSingleJetFlame(GraphicsContext gc, double x, double y, double width, double height, Color col) {
        gc.save();
        // Cincin Inti Putih
        gc.setFill(Color.WHITE);
        gc.fillOval(x - width / 2.0, y - width * 0.35, width, width * 0.7);

        // Lidah Api Plasma
        LinearGradient flameGrad = new LinearGradient(
                x, y, x, y + height, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),
                new Stop(0.25, Color.web("#80d8ff")),
                new Stop(0.65, Color.web("#00e5ff")),
                new Stop(1.0, Color.color(0, 0.8, 1.0, 0))
        );
        gc.setFill(flameGrad);
        gc.fillOval(x - width / 2.0, y, width, height);
        gc.restore();
    }

    private static void renderMainPlasmaFlame(GraphicsContext gc, double x, double y, double width, double height, double animTime) {
        gc.save();
        // Cincin Inti Nozzle Putih Menyilaukan
        gc.setFill(Color.WHITE);
        gc.fillOval(x - width / 2.0, y - width * 0.35, width, width * 0.7);

        // Semburan Berkas Plasma Raksasa
        LinearGradient mainGrad = new LinearGradient(
                x, y, x, y + height, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),
                new Stop(0.2, Color.web("#e0f7fa")),
                new Stop(0.5, Color.web("#00e5ff")),
                new Stop(0.8, Color.web("#0091ea")),
                new Stop(1.0, Color.color(0, 0.4, 1.0, 0))
        );
        gc.setFill(mainGrad);
        gc.fillOval(x - width / 2.0, y, width, height);

        // Lidah Petir Listrik Plasma Biru Menjalar ke Bawah (persis foto referensi!)
        gc.setStroke(Color.web("#80d8ff"));
        gc.setLineWidth(2.2);
        double prevX = x;
        double prevY = y + width * 0.4;
        int segments = 6;
        double segH = height / segments;

        for (int i = 1; i <= segments; i++) {
            double nextX = x + Math.sin(animTime * 12.0 + i * 1.5) * (width * 0.4);
            double nextY = y + i * segH;
            gc.strokeLine(prevX, prevY, nextX, nextY);
            prevX = nextX;
            prevY = nextY;
        }

        gc.restore();
    }

    private static Color applyLighting(Color c, double factor) {
        double r = Math.min(1.0, c.getRed() * factor);
        double g = Math.min(1.0, c.getGreen() * factor);
        double b = Math.min(1.0, c.getBlue() * factor);
        return Color.color(r, g, b, c.getOpacity());
    }

    private static Color blendColors(Color c1, Color c2, double ratio) {
        double r = c1.getRed() * (1 - ratio) + c2.getRed() * ratio;
        double g = c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio;
        double b = c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio;
        return Color.color(r, g, b);
    }
}
