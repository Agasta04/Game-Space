package com.example.spaceshootergamejavafx;

import java.util.*;

/**
 * Representasi Geometri 3D Nyata untuk Pesawat Luar Angkasa.
 * Menggunakan Vertex 3D (X, Y, Z), Transformasi Matriks 3D (Pitch, Roll, Yaw),
 * Normal Permukaan (Face Normal), Directional Shading, dan Painter's Algorithm Depth Sort.
 */
public class True3DShipModel {

    public static class Vec3 {
        public double x, y, z;
        public Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class Face3D implements Comparable<Face3D> {
        public int[] vertexIndices;
        public javafx.scene.paint.Color baseColor;
        public boolean isGlow;
        public double avgZ;
        public double lighting;

        public Face3D(int[] vertexIndices, javafx.scene.paint.Color baseColor, boolean isGlow) {
            this.vertexIndices = vertexIndices;
            this.baseColor = baseColor;
            this.isGlow = isGlow;
        }

        @Override
        public int compareTo(Face3D o) {
            // Urutkan Z dari paling jauh ke paling dekat (Painter's Algorithm)
            return Double.compare(o.avgZ, this.avgZ);
        }
    }

    public final List<Vec3> baseVertices = new ArrayList<>();
    public final List<Face3D> faces = new ArrayList<>();

    public True3DShipModel() {
        buildModel();
    }

    private int addVertex(double x, double y, double z) {
        baseVertices.add(new Vec3(x, y, z));
        return baseVertices.size() - 1;
    }

    private void addQuad(int a, int b, int c, int d, javafx.scene.paint.Color col, boolean glow) {
        faces.add(new Face3D(new int[]{a, b, c, d}, col, glow));
    }

    private void addTri(int a, int b, int c, javafx.scene.paint.Color col, boolean glow) {
        faces.add(new Face3D(new int[]{a, b, c}, col, glow));
    }

    private void buildModel() {
        // --- 1. MONCONG & BODI FUSELAGE DEPAN (Z: +20 sampai +75) ---
        // Sumbu: +Z = Depan/Moncong, -Z = Belakang/Mesin, -Y = Atas/Punggung, +Y = Bawah/Perut, +X = Sayap Kanan, -X = Sayap Kiri
        int noseTip = addVertex(0, -2, 75); // Ujung moncong lancip
        int noseBottom = addVertex(0, 8, 55);

        int fTopLeft = addVertex(-8, -8, 35);
        int fTopRight = addVertex(8, -8, 35);
        int fBotLeft = addVertex(-10, 8, 35);
        int fBotRight = addVertex(10, 8, 35);

        // Segitiga Moncong Atas
        addTri(noseTip, fTopLeft, fTopRight, javafx.scene.paint.Color.web("#cfd8dc"), false);
        // Sisi Moncong Kiri & Kanan
        addTri(noseTip, fBotLeft, fTopLeft, javafx.scene.paint.Color.web("#90a4ae"), false);
        addTri(noseTip, fTopRight, fBotRight, javafx.scene.paint.Color.web("#90a4ae"), false);
        // Bawah Moncong
        addTri(noseTip, fBotRight, fBotLeft, javafx.scene.paint.Color.web("#546e7a"), false);

        // --- 2. KOKPIT GLASS BUBBLE (Elevated) ---
        int cpNose = addVertex(0, -9, 38);
        int cpApex = addVertex(0, -16, 15);
        int cpLeft = addVertex(-5, -12, 18);
        int cpRight = addVertex(5, -12, 18);
        int cpRear = addVertex(0, -12, -2);

        // Kaca Kokpit Reflektif Cyan
        addTri(cpNose, cpLeft, cpApex, javafx.scene.paint.Color.web("#00e5ff"), true);
        addTri(cpNose, cpApex, cpRight, javafx.scene.paint.Color.web("#80d8ff"), true);
        addTri(cpApex, cpLeft, cpRear, javafx.scene.paint.Color.web("#0091ea"), true);
        addTri(cpApex, cpRear, cpRight, javafx.scene.paint.Color.web("#00b0ff"), true);

        // --- 3. BADAN TENGAH & PUNGGUNG (Z: +35 sampai -30) ---
        int midTopLeft = addVertex(-14, -10, -10);
        int midTopRight = addVertex(14, -10, -10);
        int midBotLeft = addVertex(-16, 9, -10);
        int midBotRight = addVertex(16, 9, -10);

        // Panel Deck Atas Fuselage
        addQuad(fTopLeft, midTopLeft, midTopRight, fTopRight, javafx.scene.paint.Color.web("#b0bec5"), false);
        // Panel Samping Kiri & Kanan
        addQuad(fTopLeft, fBotLeft, midBotLeft, midTopLeft, javafx.scene.paint.Color.web("#78909c"), false);
        addQuad(fTopRight, midTopRight, midBotRight, fBotRight, javafx.scene.paint.Color.web("#78909c"), false);
        // Panel Perut Bawah
        addQuad(fBotLeft, fBotRight, midBotRight, midBotLeft, javafx.scene.paint.Color.web("#37474f"), false);

        // --- 4. ENGINE COWLING DORSAL NACELLES (Kiri & Kanan Kokpit) ---
        int nacL_Top = addVertex(-11, -15, 5);
        int nacL_Rear = addVertex(-12, -14, -28);
        int nacR_Top = addVertex(11, -15, 5);
        int nacR_Rear = addVertex(12, -14, -28);

        addTri(fTopLeft, nacL_Top, midTopLeft, javafx.scene.paint.Color.web("#607d8b"), false);
        addTri(fTopRight, midTopRight, nacR_Top, javafx.scene.paint.Color.web("#607d8b"), false);
        addQuad(nacL_Top, nacL_Rear, midTopLeft, midTopLeft, javafx.scene.paint.Color.web("#455a64"), false);
        addQuad(nacR_Top, midTopRight, midTopRight, nacR_Rear, javafx.scene.paint.Color.web("#455a64"), false);

        // --- 5. BURITAN BELAKANG MESIN (Rear Fuselage Z: -10 sampai -36) ---
        int rearTopLeft = addVertex(-16, -9, -36);
        int rearTopRight = addVertex(16, -9, -36);
        int rearBotLeft = addVertex(-16, 8, -36);
        int rearBotRight = addVertex(16, 8, -36);

        addQuad(midTopLeft, rearTopLeft, rearTopRight, midTopRight, javafx.scene.paint.Color.web("#90a4ae"), false);
        addQuad(midTopLeft, midBotLeft, rearBotLeft, rearTopLeft, javafx.scene.paint.Color.web("#607d8b"), false);
        addQuad(midTopRight, rearTopRight, rearBotRight, midBotRight, javafx.scene.paint.Color.web("#607d8b"), false);
        addQuad(midBotLeft, rearBotLeft, rearBotRight, midBotRight, javafx.scene.paint.Color.web("#263238"), false);

        // Panel Dinding Belakang (Rear Bulkhead)
        addQuad(rearTopLeft, rearTopRight, rearBotRight, rearBotLeft, javafx.scene.paint.Color.web("#1c2833"), false);

        // --- 6. SAYAP INVERTED-GULL 3D (PERSIS FOTO REFERENSI) ---
        // Sayap Kiri: Root -> Elbow (Menukik ke Bawah) -> Tip (Mencuat Luar dengan Rel Senjata)
        int wRootFrontL = addVertex(-14, 0, 18);
        int wRootRearL = addVertex(-16, 2, -26);
        int wElbowFrontL = addVertex(-38, 7, 0); // Menukik ke bawah (Y positif)
        int wElbowRearL = addVertex(-40, 9, -24);
        int wTipFrontL = addVertex(-72, 4, -8);  // Ujung sayap mencuat
        int wTipRearL = addVertex(-70, 6, -32);
        int wTipRailL = addVertex(-71, -4, -18); // Rel pod senjata atas

        // Sayap Atas Kiri (Inner & Outer)
        addQuad(wRootFrontL, wElbowFrontL, wElbowRearL, wRootRearL, javafx.scene.paint.Color.web("#78909c"), false);
        addQuad(wElbowFrontL, wTipFrontL, wTipRearL, wElbowRearL, javafx.scene.paint.Color.web("#90a4ae"), false);
        addTri(wTipFrontL, wTipRailL, wTipRearL, javafx.scene.paint.Color.web("#37474f"), false);

        // Sayap Bawah Kiri
        addQuad(wRootRearL, wElbowRearL, wElbowFrontL, wRootFrontL, javafx.scene.paint.Color.web("#37474f"), false);
        addQuad(wElbowRearL, wTipRearL, wTipFrontL, wElbowFrontL, javafx.scene.paint.Color.web("#263238"), false);

        // Sayap Kanan (Simetris Sumbu X)
        int wRootFrontR = addVertex(14, 0, 18);
        int wRootRearR = addVertex(16, 2, -26);
        int wElbowFrontR = addVertex(38, 7, 0);
        int wElbowRearR = addVertex(40, 9, -24);
        int wTipFrontR = addVertex(72, 4, -8);
        int wTipRearR = addVertex(70, 6, -32);
        int wTipRailR = addVertex(71, -4, -18);

        // Sayap Atas Kanan
        addQuad(wRootFrontR, wRootRearR, wElbowRearR, wElbowFrontR, javafx.scene.paint.Color.web("#78909c"), false);
        addQuad(wElbowFrontR, wElbowRearR, wTipRearR, wTipFrontR, javafx.scene.paint.Color.web("#90a4ae"), false);
        addTri(wTipFrontR, wTipRearR, wTipRailR, javafx.scene.paint.Color.web("#37474f"), false);

        // Sayap Bawah Kanan
        addQuad(wRootRearR, wRootFrontR, wElbowFrontR, wElbowRearR, javafx.scene.paint.Color.web("#37474f"), false);
        addQuad(wElbowRearR, wElbowFrontR, wTipFrontR, wTipRearR, javafx.scene.paint.Color.web("#263238"), false);

        // --- 7. TIGA NOZZLE MESIN 3D (TRIPLE REAR THRUSTER NOZZLES) ---
        // Mesin Tengah (Center Nozzle Besar Z: -36 sampai -44)
        buildNozzleRing(0, 0, -36, -44, 9.5, 7.5, javafx.scene.paint.Color.web("#e0f7fa"), true);

        // Mesin Samping Kiri (Z: -36 sampai -42)
        buildNozzleRing(-11, 0, -36, -42, 6.5, 5.0, javafx.scene.paint.Color.web("#80d8ff"), true);

        // Mesin Samping Kanan (Z: -36 sampai -42)
        buildNozzleRing(11, 0, -36, -42, 6.5, 5.0, javafx.scene.paint.Color.web("#80d8ff"), true);
    }

    private void buildNozzleRing(double cx, double cy, double zFront, double zBack, double rFront, double rBack, javafx.scene.paint.Color innerCol, boolean isCenter) {
        int segs = 8;
        int[] fv = new int[segs];
        int[] bv = new int[segs];

        for (int i = 0; i < segs; i++) {
            double ang = i * Math.PI * 2 / segs;
            double cos = Math.cos(ang);
            double sin = Math.sin(ang);
            fv[i] = addVertex(cx + cos * rFront, cy + sin * rFront, zFront);
            bv[i] = addVertex(cx + cos * rBack, cy + sin * rBack, zBack);
        }

        // Quads Cincin Nozzle Luar Silinder
        for (int i = 0; i < segs; i++) {
            int next = (i + 1) % segs;
            addQuad(fv[i], fv[next], bv[next], bv[i], javafx.scene.paint.Color.web("#455a64"), false);
        }

        // Tutup Nozzle Dalam (Plasma Core Glowing Face)
        int centerBack = addVertex(cx, cy, zBack);
        for (int i = 0; i < segs; i++) {
            int next = (i + 1) % segs;
            addTri(bv[i], bv[next], centerBack, innerCol, true);
        }
    }
}

