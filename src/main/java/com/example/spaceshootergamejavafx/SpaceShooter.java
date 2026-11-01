package com.example.spaceshootergamejavafx;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.*;

/**
 * Game Space Invader 3D Profesional: Misi Pemburu UFO & Galon Kosmik.
 * Menggunakan Full 3D Chase Camera Perspective dengan Kontrol Alami Maju-Mundur & Kiri-Kanan,
 * Sistem Hangar Pemilihan Pesawat, 5 Sektor Level Progresif, dan Arena 3D Bebas Bug.
 */
public class SpaceShooter extends Application {

    public static final int WIDTH = 1050;
    public static final int HEIGHT = 700;

    private Stage primaryStage;
    private Scene gameScene;
    private Pane gameRoot;
    private Canvas canvas;
    private GraphicsContext gc;

    // Sistem 3D & Kamera
    private Camera3D camera = new Camera3D();
    private SpaceBackground3D spaceBackground = new SpaceBackground3D();
    private LevelManager levelManager = new LevelManager();

    // Pemilihan Pesawat & Drone di Hangar
    private ShipType selectedShip = ShipType.FALCON_X1;
    private DroneType selectedDrone = DroneType.MAGNET_DRONE;
    private int selectedShipIndex = 0;
    private double hangarAnimAngle = 0.0;

    // Entitas Dunia 3D
    private PlayerShip3D player;
    private final List<Entity3D> worldEntities = new ArrayList<>();
    private final List<Entity3D> newEntities = new ArrayList<>();
    private final List<Laser3D> lasers = new ArrayList<>();
    private final List<EnemyPlasma3D> enemyPlasmas = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    // Leaderboard
    private final LeaderboardManager leaderboardManager = new LeaderboardManager();
    private String currentPlayerName = "Astronaut-1";

    // Game State
    private int totalGalonCount = 0;
    private int levelGalonCount = 0;
    private int score = 0;
    private boolean gameRunning = false;
    private boolean isPaused = false;
    private boolean inLevelTransition = false;
    private double warpTimer = 0.0;
    private final Random random = new Random();

    // Game Loop
    private AnimationTimer gameLoop;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Space Invader 3D - Ultimate Cosmic Mission");
        stage.setResizable(false);

        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/player.png"))));
        } catch (Exception ignored) {}

        initGameScene();
        SoundManager.startBGM();

        // Tampilkan Menu Hangar 3D Utama
        showHangarMenu();
        stage.show();
    }

    private void initGameScene() {
        gameRoot = new Pane();
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        gameRoot.getChildren().add(canvas);

        gameScene = new Scene(gameRoot, WIDTH, HEIGHT, Color.BLACK);

        // KONTROL MAJU-MUNDUR (W/S) DAN KIRI-KANAN (A/D)
        gameScene.setOnKeyPressed(event -> {
            if (player == null) return;
            KeyCode code = event.getCode();
            if (code == KeyCode.A || code == KeyCode.LEFT) player.setMoveLeft(true);
            if (code == KeyCode.D || code == KeyCode.RIGHT) player.setMoveRight(true);
            if (code == KeyCode.W || code == KeyCode.UP) player.setMoveForward(true);     // MAJU KE DEPAN
            if (code == KeyCode.S || code == KeyCode.DOWN) player.setMoveBackward(true);  // MUNDUR / REM
            if (code == KeyCode.SPACE && gameRunning && !isPaused && !inLevelTransition) {
                player.shoot(lasers);
            }
            if (code == KeyCode.M) {
                SoundManager.toggleMusic();
            }
            if (code == KeyCode.ESCAPE) {
                isPaused = !isPaused;
            }
        });

        gameScene.setOnKeyReleased(event -> {
            if (player == null) return;
            KeyCode code = event.getCode();
            if (code == KeyCode.A || code == KeyCode.LEFT) player.setMoveLeft(false);
            if (code == KeyCode.D || code == KeyCode.RIGHT) player.setMoveRight(false);
            if (code == KeyCode.W || code == KeyCode.UP) player.setMoveForward(false);
            if (code == KeyCode.S || code == KeyCode.DOWN) player.setMoveBackward(false);
        });

        gameLoop = new AnimationTimer() {
            private long lastEnemySpawn = 0;
            private long lastGalonSpawn = 0;
            private long lastAsteroidSpawn = 0;

            @Override
            public void handle(long now) {
                if (!gameRunning) return;

                if (isPaused) {
                    renderPauseOverlay();
                    return;
                }

                // Update Kamera Mengikuti Posisi Pesawat (X, Y, dan Z Maju-Mundur)
                if (player != null) {
                    camera.update(player.getX(), player.getY(), player.getZ());
                }
                spaceBackground.update();

                // Transisi Warp Speed Naik Level
                if (inLevelTransition) {
                    warpTimer -= 0.016;
                    if (warpTimer <= 0) {
                        inLevelTransition = false;
                        spaceBackground.stopWarpJump();
                    }
                }

                LevelData currentLevel = levelManager.getCurrentLevelData();

                // Spawning Objek 3D dari Horizon
                if (!inLevelTransition) {
                    // Spawn UFO Musuh
                    long enemyInterval = (long) (currentLevel.getEnemySpawnInterval() * 1_000_000_000L);
                    if (now - lastEnemySpawn > enemyInterval) {
                        spawnUfo3D(currentLevel.getEnemySpeed());
                        lastEnemySpawn = now;
                    }

                    // Spawn Galon Kosmik 3D
                    if (now - lastGalonSpawn > 1_400_000_000L) {
                        spawnGalon3D();
                        lastGalonSpawn = now;
                    }

                    // Spawn Asteroid jika level mengizinkan
                    if (currentLevel.isSpawnAsteroids() && now - lastAsteroidSpawn > 2_200_000_000L) {
                        spawnAsteroid3D();
                        lastAsteroidSpawn = now;
                    }

                    // Final Boss di Level 5
                    if (currentLevel.isBossLevel() && worldEntities.stream().noneMatch(e -> e instanceof BossMothership3D)) {
                        BossMothership3D boss = new BossMothership3D(0, 60, 950);
                        worldEntities.add(boss);
                        SoundManager.playBossAlert();
                        spaceBackground.triggerScreenShake(18.0);
                        floatingTexts.add(new FloatingText("⚠️ PERINGATAN: FINAL BOSS ALIEN MOTHERSHIP!", WIDTH / 2.0 - 250, HEIGHT / 2.0 - 60, Color.web("#ff1744"), 24, 3.5));
                    }
                }

                // Render Canvas
                gc.save();
                gc.translate(spaceBackground.getShakeX(), spaceBackground.getShakeY());

                // 1. Render Background Luar Angkasa 3D dengan Perspective Grid Lantai
                spaceBackground.render(gc, camera);

                // 2. Partikel Mesin Pemain
                if (player != null && !player.isDead()) {
                    player.update();
                    player.emitThrusterParticles(particles, camera);
                }

                // 3. Update Partikel
                Iterator<Particle> pIt = particles.iterator();
                while (pIt.hasNext()) {
                    Particle p = pIt.next();
                    p.update();
                    p.render(gc);
                    if (p.isDead()) pIt.remove();
                }

                // 4. Update Lasers
                Iterator<Laser3D> lIt = lasers.iterator();
                while (lIt.hasNext()) {
                    Laser3D laser = lIt.next();
                    laser.update();
                    laser.render(gc, camera);
                    if (laser.isDead()) lIt.remove();
                }

                // 5. Update Enemy Plasmas
                Iterator<EnemyPlasma3D> epIt = enemyPlasmas.iterator();
                while (epIt.hasNext()) {
                    EnemyPlasma3D plasma = epIt.next();
                    plasma.update();
                    plasma.render(gc, camera);
                    if (plasma.isDead()) epIt.remove();
                }

                // 6. Collision & Magnet Attraction
                handlePhysicsAndCollisions();

                // 7. Z-Ordering / Painter's Algorithm untuk Objek 3D
                worldEntities.addAll(newEntities);
                newEntities.clear();

                List<Entity3D> renderList = new ArrayList<>(worldEntities);
                if (player != null && !player.isDead()) {
                    renderList.add(player);
                }
                Collections.sort(renderList); // Mengurutkan Z dari yang paling jauh ke yang paling dekat

                for (Entity3D entity : renderList) {
                    if (entity != player) {
                        entity.update();
                    }
                    entity.render(gc, camera);

                    if (entity instanceof BossMothership3D) {
                        ((BossMothership3D) entity).shoot(enemyPlasmas);
                    }
                }

                // Hapus entitas mati
                worldEntities.removeIf(Entity3D::isDead);

                // 8. Update & Render Floating Texts
                Iterator<FloatingText> tIt = floatingTexts.iterator();
                while (tIt.hasNext()) {
                    FloatingText ft = tIt.next();
                    ft.update();
                    ft.render(gc);
                    if (ft.isDead()) tIt.remove();
                }

                gc.restore();

                // 9. Render HUD Sci-Fi Profesional
                renderProfessionalHUD();

                // Cek Naik Level
                checkLevelProgress();

                // Cek Game Over
                if (player != null && player.isDead()) {
                    onGameOver();
                }
            }
        };
    }

    private void showHangarMenu() {
        gameRunning = false;

        VBox rootBox = new VBox(14);
        rootBox.setAlignment(Pos.CENTER);
        rootBox.setStyle("-fx-background-color: linear-gradient(to bottom, #030713, #091a36, #140826);");
        rootBox.setPadding(new Insets(16));

        // Header Title
        Text titleText = new Text("★ STAR INVADER 3D ★");
        titleText.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 46));
        titleText.setFill(Color.web("#00e5ff"));
        titleText.setEffect(new DropShadow(26, Color.web("#00b0ff")));

        Text subText = new Text("[ // TACTICAL AEROSPACE DOCK // SECTOR 01-05 READY // ]");
        subText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        subText.setFill(Color.web("#ffd600"));

        HBox mainHangarBox = new HBox(24);
        mainHangarBox.setAlignment(Pos.CENTER);
        mainHangarBox.setMaxWidth(920);

        // Preview Canvas Pesawat 3D di Hangar
        Canvas previewCanvas = new Canvas(340, 240);
        GraphicsContext pgc = previewCanvas.getGraphicsContext2D();

        AnimationTimer turntableTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                hangarAnimAngle += 0.035;
                renderHangarShipPreview(pgc, selectedShip, hangarAnimAngle);
            }
        };
        turntableTimer.start();

        VBox leftBox = new VBox(10);
        leftBox.setAlignment(Pos.CENTER);
        leftBox.setStyle(
                "-fx-background-color: rgba(8, 20, 48, 0.85);"
                + "-fx-padding: 16;"
                + "-fx-background-radius: 16;"
                + "-fx-border-color: #00e5ff;"
                + "-fx-border-width: 1.6;"
                + "-fx-border-radius: 16;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0, 229, 255, 0.35), 14, 0, 0, 0);"
        );

        Label previewTitle = new Label("// 3D HOLOGRAPHIC DOCK //");
        previewTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        previewTitle.setTextFill(Color.web("#80d8ff"));

        // Tombol Ganti Pesawat < dan >
        HBox shipSelectorBox = new HBox(12);
        shipSelectorBox.setAlignment(Pos.CENTER);

        Button prevBtn = createMiniButton("◀");
        Button nextBtn = createMiniButton("▶");

        Label shipNameLabel = new Label(selectedShip.getName());
        shipNameLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 20));
        shipNameLabel.setTextFill(selectedShip.getPrimaryColor());
        shipNameLabel.setPrefWidth(190);
        shipNameLabel.setAlignment(Pos.CENTER);

        // Right Info Box
        VBox rightBox = new VBox(10);
        rightBox.setAlignment(Pos.CENTER_LEFT);
        rightBox.setPrefWidth(460);
        rightBox.setStyle(
                "-fx-background-color: rgba(8, 20, 48, 0.85);"
                + "-fx-padding: 18;"
                + "-fx-background-radius: 16;"
                + "-fx-border-color: #00e5ff;"
                + "-fx-border-width: 1.6;"
                + "-fx-border-radius: 16;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0, 229, 255, 0.35), 14, 0, 0, 0);"
        );

        // Input Nama Pemain / Callsign
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label pNameLbl = new Label("👨‍🚀 CALLSIGN:");
        pNameLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        pNameLbl.setTextFill(Color.web("#80d8ff"));

        TextField nameInput = new TextField(currentPlayerName);
        nameInput.setPrefWidth(270);
        nameInput.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nameInput.setStyle(
                "-fx-background-color: #06152d;"
                + "-fx-text-fill: white;"
                + "-fx-border-color: #00e5ff;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 6 10 6 10;"
        );

        nameBox.getChildren().addAll(pNameLbl, nameInput);

        Label descLabel = new Label(selectedShip.getDescription());
        descLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        descLabel.setTextFill(Color.web("#cfd8dc"));
        descLabel.setWrapText(true);

        // Bar Statistik Pesawat
        ProgressBar speedBar = createStatBar(selectedShip.getSpeed() / 10.0, "#00e5ff");
        ProgressBar handleBar = createStatBar(selectedShip.getHandling() / 0.22, "#ffea00");
        ProgressBar magnetBar = createStatBar(selectedShip.getMagnetRange() / 250.0, "#d500f9");

        Label speedPct = new Label((int)(selectedShip.getSpeed() * 10) + "%");
        speedPct.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        speedPct.setTextFill(Color.web("#00e5ff"));

        Label handlePct = new Label((int)(selectedShip.getHandling() / 0.22 * 100) + "%");
        handlePct.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        handlePct.setTextFill(Color.web("#ffea00"));

        Label magnetPct = new Label((int)(selectedShip.getMagnetRange() / 250.0 * 100) + "%");
        magnetPct.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        magnetPct.setTextFill(Color.web("#d500f9"));

        VBox statsBox = new VBox(6);
        statsBox.getChildren().addAll(
                createStatRow("WARP SPEED", speedBar, speedPct),
                createStatRow("3D MANEUVER", handleBar, handlePct),
                createStatRow("MAGNET GRAVITY", magnetBar, magnetPct)
        );

        // Pilihan Drone Pendukung Interaktif (Cyber Cards)
        Label droneLabel = new Label("PILIH COMPANION DRONE:");
        droneLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        droneLabel.setTextFill(Color.web("#80d8ff"));

        HBox droneCardsBox = new HBox(8);
        droneCardsBox.setAlignment(Pos.CENTER_LEFT);
        List<VBox> droneCardsList = new ArrayList<>();

        for (DroneType dt : DroneType.values()) {
            VBox card = new VBox(3);
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(8, 10, 8, 10));
            card.setPrefWidth(136);
            card.setCursor(javafx.scene.Cursor.HAND);

            Label dIcon = new Label(dt == DroneType.MAGNET_DRONE ? "🧲" : (dt == DroneType.SHIELD_DRONE ? "🛡️" : "⚡"));
            dIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

            Label dName = new Label(dt.getTitle());
            dName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            dName.setTextFill(Color.WHITE);

            String perk = dt == DroneType.MAGNET_DRONE ? "+35% Tarik Galon" : (dt == DroneType.SHIELD_DRONE ? "Perisai Kinetik" : "Laser Tambahan");
            Label dPerk = new Label(perk);
            dPerk.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
            dPerk.setTextFill(Color.web("#80d8ff"));

            card.getChildren().addAll(dIcon, dName, dPerk);
            droneCardsList.add(card);

            card.setOnMouseClicked(e -> {
                selectedDrone = dt;
                SoundManager.playClick();
                for (int i = 0; i < DroneType.values().length; i++) {
                    DroneType t = DroneType.values()[i];
                    VBox c = droneCardsList.get(i);
                    boolean isSel = (t == selectedDrone);
                    c.setStyle(
                            "-fx-background-color: " + (isSel ? "linear-gradient(to bottom, rgba(0, 229, 255, 0.35), rgba(10, 30, 70, 0.85));" : "rgba(10, 20, 45, 0.65);")
                            + "-fx-border-color: " + (isSel ? "#00e5ff;" : "rgba(0, 229, 255, 0.25);")
                            + "-fx-border-width: " + (isSel ? "1.8;" : "1.0;")
                            + "-fx-border-radius: 8;"
                            + "-fx-background-radius: 8;"
                    );
                    c.setEffect(isSel ? new DropShadow(10, Color.web("#00e5ff")) : null);
                }
            });

            boolean isSel = (dt == selectedDrone);
            card.setStyle(
                    "-fx-background-color: " + (isSel ? "linear-gradient(to bottom, rgba(0, 229, 255, 0.35), rgba(10, 30, 70, 0.85));" : "rgba(10, 20, 45, 0.65);")
                    + "-fx-border-color: " + (isSel ? "#00e5ff;" : "rgba(0, 229, 255, 0.25);")
                    + "-fx-border-width: " + (isSel ? "1.8;" : "1.0;")
                    + "-fx-border-radius: 8;"
                    + "-fx-background-radius: 8;"
            );
            if (isSel) card.setEffect(new DropShadow(10, Color.web("#00e5ff")));
            droneCardsBox.getChildren().add(card);
        }

        prevBtn.setOnAction(e -> {
            ShipType[] types = ShipType.values();
            selectedShipIndex = (selectedShipIndex - 1 + types.length) % types.length;
            selectedShip = types[selectedShipIndex];
            shipNameLabel.setText(selectedShip.getName());
            shipNameLabel.setTextFill(selectedShip.getPrimaryColor());
            descLabel.setText(selectedShip.getDescription());
            speedBar.setProgress(selectedShip.getSpeed() / 10.0);
            handleBar.setProgress(selectedShip.getHandling() / 0.22);
            magnetBar.setProgress(selectedShip.getMagnetRange() / 250.0);
            speedPct.setText((int)(selectedShip.getSpeed() * 10) + "%");
            handlePct.setText((int)(selectedShip.getHandling() / 0.22 * 100) + "%");
            magnetPct.setText((int)(selectedShip.getMagnetRange() / 250.0 * 100) + "%");
            SoundManager.playClick();
        });

        nextBtn.setOnAction(e -> {
            ShipType[] types = ShipType.values();
            selectedShipIndex = (selectedShipIndex + 1) % types.length;
            selectedShip = types[selectedShipIndex];
            shipNameLabel.setText(selectedShip.getName());
            shipNameLabel.setTextFill(selectedShip.getPrimaryColor());
            descLabel.setText(selectedShip.getDescription());
            speedBar.setProgress(selectedShip.getSpeed() / 10.0);
            handleBar.setProgress(selectedShip.getHandling() / 0.22);
            magnetBar.setProgress(selectedShip.getMagnetRange() / 250.0);
            speedPct.setText((int)(selectedShip.getSpeed() * 10) + "%");
            handlePct.setText((int)(selectedShip.getHandling() / 0.22 * 100) + "%");
            magnetPct.setText((int)(selectedShip.getMagnetRange() / 250.0 * 100) + "%");
            SoundManager.playClick();
        });

        shipSelectorBox.getChildren().addAll(prevBtn, shipNameLabel, nextBtn);
        leftBox.getChildren().addAll(previewTitle, previewCanvas, shipSelectorBox);

        rightBox.getChildren().addAll(nameBox, descLabel, statsBox, droneLabel, droneCardsBox);
        mainHangarBox.getChildren().addAll(leftBox, rightBox);

        // Tombol Aksi Bawah
        HBox buttonsBox = new HBox(14);
        buttonsBox.setAlignment(Pos.CENTER);

        Button launchBtn = createSciFiButton("🚀 LUNCURKAN MISI", "#00e676", "#00c853", 260);
        launchBtn.setOnAction(e -> {
            turntableTimer.stop();
            String entered = nameInput.getText().trim();
            if (!entered.isEmpty()) currentPlayerName = entered;
            SoundManager.playWarp();
            startNewGame();
        });

        Button leaderboardBtn = createSciFiButton("🏆 LEADERBOARD", "#2979ff", "#1565c0", 200);
        leaderboardBtn.setOnAction(e -> {
            turntableTimer.stop();
            showLeaderboardScreen();
        });

        Button instructionsBtn = createSciFiButton("📜 PANDUAN", "#ab47bc", "#7b1fa2", 170);
        instructionsBtn.setOnAction(e -> showInstructionsDialog());

        Button musicBtn = createSciFiButton("🎵 MUSIK: " + (SoundManager.isMusicEnabled() ? "ON" : "OFF"), "#ff9100", "#e65100", 160);
        musicBtn.setOnAction(e -> {
            SoundManager.toggleMusic();
            musicBtn.setText("🎵 MUSIK: " + (SoundManager.isMusicEnabled() ? "ON" : "OFF"));
        });

        Button exitBtn = createSciFiButton("❌ KELUAR", "#ef5350", "#c62828", 140);
        exitBtn.setOnAction(e -> System.exit(0));

        buttonsBox.getChildren().addAll(launchBtn, leaderboardBtn, instructionsBtn, musicBtn, exitBtn);

        rootBox.getChildren().addAll(titleText, subText, mainHangarBox, buttonsBox);

        Scene hangarScene = new Scene(rootBox, WIDTH, HEIGHT);
        primaryStage.setScene(hangarScene);
    }

    private void renderHangarShipPreview(GraphicsContext pgc, ShipType ship, double angle) {
        pgc.clearRect(0, 0, 340, 240);

        double cx = 170;
        double cy = 180;
        double platW = 200;
        double platH = 46;

        // 1. Grid Lantai Pedestal Hangar Hologram
        pgc.setStroke(Color.color(0.0, 0.9, 1.0, 0.20));
        pgc.setLineWidth(1.0);
        for (int i = -4; i <= 4; i++) {
            pgc.strokeLine(cx + i * 24, cy - 8, cx + i * 28, cy + 26);
        }

        // 2. Cincin Kompas Hologram Berputar
        pgc.setStroke(Color.web("#00e5ff", 0.70));
        pgc.setLineWidth(1.8);
        pgc.strokeOval(cx - platW / 2.0, cy - platH / 2.0, platW, platH);

        // Ticks Derajat di Sekeliling Cincin
        for (int i = 0; i < 12; i++) {
            double tickAng = angle * 0.4 + (i * Math.PI * 2 / 12);
            double tx = cx + Math.cos(tickAng) * (platW * 0.48);
            double ty = cy + Math.sin(tickAng) * (platH * 0.48);
            pgc.setStroke(Color.color(0.0, 0.9, 1.0, 0.45));
            pgc.strokeLine(tx, ty, tx + Math.cos(tickAng) * 4, ty + Math.sin(tickAng) * 2);
        }

        // Cincin Luar Berdenyut
        double pulseW = platW + Math.sin(angle * 2.5) * 8.0;
        double pulseH = platH + Math.sin(angle * 2.5) * 3.0;
        pgc.setStroke(Color.web("#00e5ff", 0.30));
        pgc.strokeOval(cx - pulseW / 2.0, cy - pulseH / 2.0, pulseW, pulseH);

        // 3. Berkas Proyeksi Laser Hologram Memancar ke Atas
        LinearGradient beamGrad = new LinearGradient(
                0, cy, 0, 30, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0.0, 0.9, 1.0, 0.25)),
                new Stop(0.7, Color.color(0.0, 0.5, 1.0, 0.08)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        pgc.setFill(beamGrad);
        double[] bx = {cx - platW / 2.0, cx + platW / 2.0, cx + 55, cx - 55};
        double[] by = {cy, cy, 30, 30};
        pgc.fillPolygon(bx, by, 4);

        // Scanline Laser Horizontal Naik-Turun
        double scanY = 55 + ((Math.sin(angle * 1.8) + 1.0) / 2.0) * 105;
        pgc.setStroke(Color.color(0.4, 1.0, 1.0, 0.45));
        pgc.setLineWidth(1.2);
        pgc.strokeLine(cx - 75, scanY, cx + 75, scanY);

        // 4. Model Pesawat 3D Turntable 360 Derajat
        double shipY = 115 + Math.sin(angle * 2.0) * 6.0;
        double gentleRoll = Math.sin(angle * 0.7) * 0.12;
        ShipRenderer.renderShipFull(pgc, ship, cx, shipY, 1.35, gentleRoll, -0.22, angle, angle, false);

        // 5. Teks Telemetri Hologram Bawah
        pgc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        pgc.setFill(Color.web("#00e5ff", 0.85));
        pgc.fillText("HULL: 100% // WARP CORE: ONLINE", cx - 85, cy + 32);
    }

    private void startNewGame() {
        worldEntities.clear();
        newEntities.clear();
        lasers.clear();
        enemyPlasmas.clear();
        particles.clear();
        floatingTexts.clear();

        totalGalonCount = 0;
        levelGalonCount = 0;
        score = 0;
        isPaused = false;
        inLevelTransition = false;

        levelManager.reset();
        LevelData lvl = levelManager.getCurrentLevelData();
        spaceBackground.setLevelColors(lvl.getNebulaColor1(), lvl.getNebulaColor2());

        player = new PlayerShip3D(selectedShip, selectedDrone);

        gameRunning = true;
        gameLoop.start();
        primaryStage.setScene(gameScene);

        floatingTexts.add(new FloatingText("MISI DIMULAI: " + lvl.getName(), WIDTH / 2.0 - 200, HEIGHT / 2.0, Color.web("#00e5ff"), 24, 3.0));
    }

    private void spawnUfo3D(double speedZ) {
        double spawnX = (random.nextDouble() * 2.0 - 1.0) * 340.0;
        double spawnY = 60.0 + random.nextDouble() * 25.0; // Sejajar zona terbang pemain
        worldEntities.add(new UfoEnemy3D(spawnX, spawnY, 1650.0, speedZ));
    }

    private void spawnGalon3D() {
        double spawnX = (random.nextDouble() * 2.0 - 1.0) * 320.0;
        double spawnY = 60.0 + random.nextDouble() * 20.0; // Sejajar zona terbang pemain
        boolean golden = (random.nextDouble() < 0.20);
        worldEntities.add(new Galon3D(spawnX, spawnY, 1650.0, 4.2, golden));
    }

    private void spawnAsteroid3D() {
        double spawnX = (random.nextDouble() * 2.0 - 1.0) * 350.0;
        double spawnY = 65.0 + random.nextDouble() * 25.0;
        worldEntities.add(new Asteroid3D(spawnX, spawnY, 1700.0, 3.6));
    }

    private void handlePhysicsAndCollisions() {
        if (player == null || player.isDead()) return;

        double shipX = player.getX();
        double shipY = player.getY();
        double shipZ = player.getZ();
        double magnetDist = player.getMagnetRange();

        // 1. Tarikan Magnet Gravitasi & Pengumpulan Galon (Maju mengejar galon)
        for (Entity3D entity : worldEntities) {
            if (entity instanceof Galon3D && !entity.isDead()) {
                Galon3D galon = (Galon3D) entity;

                double dx = shipX - galon.getX();
                double dy = shipY - galon.getY();
                double dz = shipZ - galon.getZ();
                double dist3D = Math.sqrt(dx * dx + dy * dy + dz * dz);

                // Magnet menyedot galon ke arah posisi pemain
                if (dist3D < magnetDist) {
                    galon.attractTowards(shipX, shipY, shipZ, 0.10);
                }

                // Ambil galon jika berada di zona fisik pesawat (Z toleransi 48)
                if (Math.abs(galon.getZ() - shipZ) < 48.0) {
                    double dist2D = Math.sqrt(dx * dx + dy * dy);
                    if (dist2D < 48.0) {
                        collectGalon3D(galon);
                    }
                }
            }
        }

        // 2. Tabrakan Laser Pemain vs UFO & Boss
        for (Laser3D laser : lasers) {
            if (laser.isDead()) continue;

            for (Entity3D entity : worldEntities) {
                if (entity.isDead()) continue;

                if (entity instanceof UfoEnemy3D) {
                    UfoEnemy3D ufo = (UfoEnemy3D) entity;
                    if (laser.checkCollision3D(ufo, 45.0)) {
                        laser.setDead(true);
                        ufo.setDead(true);
                        score += 30;
                        ufo.spawnExplosion(particles, camera);
                        SoundManager.playExplosion();
                        spaceBackground.triggerScreenShake(5.0);
                        floatingTexts.add(new FloatingText("UFO HANCUR", camera.projectX(ufo.getX(), ufo.getZ()), camera.projectY(ufo.getY(), ufo.getZ()), Color.web("#69f0ae"), 14, 0.8));
                    }
                } else if (entity instanceof BossMothership3D) {
                    BossMothership3D boss = (BossMothership3D) entity;
                    if (laser.checkCollision3D(boss, 65.0)) {
                        laser.setDead(true);
                        boss.takeDamage();
                        score += 50;
                        SoundManager.playHit();
                        if (boss.isDead()) {
                            score += 1000;
                            totalGalonCount += 10;
                            levelGalonCount += 10;
                            SoundManager.playExplosion();
                            spaceBackground.triggerScreenShake(20.0);
                            floatingTexts.add(new FloatingText("💥 BOSS ALIEN MOTHERSHIP HANCUR! +10 GALON BONUS!", WIDTH / 2.0 - 240, HEIGHT / 2.0, Color.web("#ffd700"), 26, 3.5));
                        }
                    }
                } else if (entity instanceof Galon3D) {
                    Galon3D g = (Galon3D) entity;
                    if (laser.checkCollision3D(g, 45.0)) {
                        laser.setDead(true);
                        collectGalon3D(g);
                    }
                }
            }
        }

        // 3. Tabrakan Langsung Pesawat Pemain vs UFO (3x Tabrakan = Mati)
        for (Entity3D entity : worldEntities) {
            if (entity.isDead()) continue;

            if (entity instanceof UfoEnemy3D) {
                UfoEnemy3D ufo = (UfoEnemy3D) entity;
                if (Math.abs(ufo.getZ() - shipZ) < 42.0) {
                    double dx = shipX - ufo.getX();
                    double dy = shipY - ufo.getY();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < 44.0) {
                        ufo.setDead(true);
                        ufo.spawnExplosion(particles, camera);
                        boolean damaged = player.takeDamage();
                        if (damaged) {
                            spaceBackground.triggerScreenShake(16.0);
                            floatingTexts.add(new FloatingText("💥 TERTABRAK UFO! NYAWA: " + player.getHealth() + "/3",
                                    camera.projectX(shipX, shipZ) - 100, camera.projectY(shipY, shipZ) - 30, Color.web("#ff1744"), 20, 1.5));
                        }
                    }
                }
            } else if (entity instanceof Asteroid3D) {
                Asteroid3D ast = (Asteroid3D) entity;
                if (Math.abs(ast.getZ() - shipZ) < 42.0) {
                    double dx = shipX - ast.getX();
                    double dy = shipY - ast.getY();
                    if (Math.sqrt(dx * dx + dy * dy) < 42.0) {
                        ast.setDead(true);
                        boolean damaged = player.takeDamage();
                        if (damaged) {
                            spaceBackground.triggerScreenShake(16.0);
                            floatingTexts.add(new FloatingText("💥 MENABRAK ASTEROID! NYAWA: " + player.getHealth() + "/3",
                                    camera.projectX(shipX, shipZ) - 110, camera.projectY(shipY, shipZ) - 30, Color.web("#ff1744"), 20, 1.5));
                        }
                    }
                }
            }
        }

        // 4. Tabrakan Peluru Plasma Musuh vs Pesawat Pemain
        for (EnemyPlasma3D plasma : enemyPlasmas) {
            if (!plasma.isDead() && Math.abs(plasma.getZ() - shipZ) < 38.0) {
                double dx = shipX - plasma.getX();
                double dy = shipY - plasma.getY();
                if (Math.sqrt(dx * dx + dy * dy) < 36.0) {
                    plasma.setDead(true);
                    boolean damaged = player.takeDamage();
                    if (damaged) {
                        spaceBackground.triggerScreenShake(12.0);
                        floatingTexts.add(new FloatingText("⚠️ TERTEMBAK PLASMA! NYAWA: " + player.getHealth() + "/3",
                                camera.projectX(shipX, shipZ) - 90, camera.projectY(shipY, shipZ) - 30, Color.web("#ff5252"), 18, 1.2));
                    }
                }
            }
        }
    }

    private void collectGalon3D(Galon3D galon) {
        galon.setDead(true);
        int val = galon.getGalonValue();
        totalGalonCount += val;
        levelGalonCount += val;
        score += galon.getScoreValue();
        SoundManager.playGalonPickup();

        double sx = camera.projectX(galon.getX(), galon.getZ());
        double sy = camera.projectY(galon.getY(), galon.getZ());

        if (galon.isGolden()) {
            floatingTexts.add(new FloatingText("✨ +3 GALON EMAS! (+300)", sx - 50, sy, Color.web("#ffd700"), 20, 1.5));
        } else {
            floatingTexts.add(new FloatingText("💧 +1 GALON! (+100)", sx - 35, sy, Color.web("#00e5ff"), 18, 1.2));
        }
    }

    private void checkLevelProgress() {
        if (inLevelTransition) return;

        if (levelManager.canLevelUp(levelGalonCount)) {
            boolean advanced = levelManager.advanceLevel();
            if (advanced) {
                levelGalonCount = 0;
                inLevelTransition = true;
                warpTimer = 3.0;

                SoundManager.playWarp();
                SoundManager.playLevelUp();
                spaceBackground.triggerWarpJump(3.0);

                LevelData nextLvl = levelManager.getCurrentLevelData();
                spaceBackground.setLevelColors(nextLvl.getNebulaColor1(), nextLvl.getNebulaColor2());

                floatingTexts.add(new FloatingText("🚀 HYPERSPACE WARP ACTIVATED!", WIDTH / 2.0 - 200, HEIGHT / 2.0 - 40, Color.web("#00e5ff"), 28, 3.0));
                floatingTexts.add(new FloatingText("MEMASUKI " + nextLvl.getName(), WIDTH / 2.0 - 180, HEIGHT / 2.0 + 10, Color.web("#ffd600"), 22, 3.0));
            }
        }
    }

    private void renderProfessionalHUD() {
        gc.save();

        LevelData currentLvl = levelManager.getCurrentLevelData();
        double hudTime = System.currentTimeMillis() / 1000.0;

        // --- 1. COCKPIT CANOPY CORNER BRACKETS (HUD JET TEMPUR) ---
        gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.35));
        gc.setLineWidth(1.6);
        // Kiri-Atas
        gc.strokeLine(20, 20, 55, 20);
        gc.strokeLine(20, 20, 20, 55);
        // Kanan-Atas
        gc.strokeLine(WIDTH - 20, 20, WIDTH - 55, 20);
        gc.strokeLine(WIDTH - 20, 20, WIDTH - 20, 55);
        // Kiri-Bawah
        gc.strokeLine(20, HEIGHT - 20, 55, HEIGHT - 20);
        gc.strokeLine(20, HEIGHT - 20, 20, HEIGHT - 55);
        // Kanan-Bawah
        gc.strokeLine(WIDTH - 20, HEIGHT - 20, WIDTH - 55, HEIGHT - 20);
        gc.strokeLine(WIDTH - 20, HEIGHT - 20, WIDTH - 20, HEIGHT - 55);

        // --- 2. FLOATING GLASSMORPHIC HUD TOP BAR ---
        double barX = 22;
        double barY = 12;
        double barW = WIDTH - 44;
        double barH = 58;

        // Latar Belakang Kaca Dark Sapphire Glass
        LinearGradient barGrad = new LinearGradient(
                barX, barY, barX + barW, barY, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0.02, 0.07, 0.20, 0.92)),
                new Stop(0.5, Color.color(0.01, 0.04, 0.12, 0.95)),
                new Stop(1.0, Color.color(0.03, 0.08, 0.22, 0.92))
        );
        gc.setFill(barGrad);
        gc.fillRoundRect(barX, barY, barW, barH, 18, 18);

        // Border Neon Luar & Dalam
        gc.setStroke(Color.web("#00e5ff", 0.65));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(barX, barY, barW, barH, 18, 18);

        gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.15));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(barX + 3, barY + 3, barW - 6, barH - 6, 15, 15);

        // --- 3. MODUL KIRI: PILOT & SEKTOR TELEMETRI ---
        double beaconPulse = (Math.sin(hudTime * 4.0) + 1.0) / 2.0;
        gc.setFill(Color.color(0.0, 0.9, 0.4, 0.4 + beaconPulse * 0.6));
        gc.fillOval(38, 26, 8, 8); // Radar blip status dot

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#90caf9"));
        gc.fillText("PILOT // " + currentPlayerName.toUpperCase(), 52, 34);

        gc.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 13));
        gc.setFill(Color.web("#ffd600"));
        gc.fillText(currentLvl.getName().toUpperCase() + " [" + currentLvl.getLevelNumber() + "/5]", 38, 54);

        // Garis Pembatas Vertikal Kiri
        gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.25));
        gc.strokeLine(260, 20, 260, 62);

        // --- 4. MODUL TENGAH: GALON AIR KOSMIK & HYPERSPACE METER ---
        renderAnimatedMiniGalonHUD(gc, 280, 18, hudTime);

        gc.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 22));
        gc.setFill(Color.web("#00e5ff"));
        gc.fillText("TOTAL GALON: " + totalGalonCount, 316, 44);

        // Meter Hyperspace Warp Jump
        if (!levelManager.isFinalLevel()) {
            double progW = 160;
            double progH = 8;
            double progX = 530;
            double progY = 38;
            double ratio = Math.min(1.0, (double) levelGalonCount / currentLvl.getTargetGalon());

            // Frame Background Bar
            gc.setFill(Color.color(0, 0, 0, 0.7));
            gc.fillRoundRect(progX, progY, progW, progH, 4, 4);
            gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.3));
            gc.setLineWidth(1.0);
            gc.strokeRoundRect(progX, progY, progW, progH, 4, 4);

            // Isi Bar Berenergi
            LinearGradient fillGrad = new LinearGradient(
                    progX, 0, progX + progW, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#00b0ff")),
                    new Stop(1.0, Color.web("#00e676"))
            );
            gc.setFill(fillGrad);
            gc.fillRoundRect(progX + 1, progY + 1, (progW - 2) * ratio, progH - 2, 3, 3);

            // Teks Status Warp
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.setFill(Color.web("#b0bec5"));
            int pct = (int) (ratio * 100);
            gc.fillText("WARP TARGET: " + levelGalonCount + "/" + currentLvl.getTargetGalon() + " (" + pct + "%)", progX, progY - 4);
        } else {
            gc.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 13));
            gc.setFill(Color.web("#ff1744"));
            gc.fillText("⚠️ FINAL BOSS SECTOR: MOTHERSHIP ⚠️", 520, 44);
        }

        // Garis Pembatas Vertikal Kanan
        gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.25));
        gc.strokeLine(715, 20, 715, 62);

        // --- 5. MODUL KANAN: NYAWA (SHIELD CELLS) & SPEEDOMETER ---
        int hp = (player != null) ? Math.max(0, player.getHealth()) : 0;
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        gc.setFill(hp == 1 ? Color.web("#ff1744") : Color.web("#90caf9"));
        gc.fillText("SHIELD CELLS:", 732, 33);

        // 3 Cybernetic Shield Cells
        for (int i = 0; i < 3; i++) {
            double cX = 732 + (i * 26);
            double cY = 39;
            double cW = 20;
            double cH = 18;

            if (i < hp) {
                // Sel Aktif Bercahaya Cyan/Hijau
                LinearGradient shieldGrad = new LinearGradient(
                        cX, cY, cX, cY + cH, false, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.WHITE),
                        new Stop(0.3, Color.web("#00e5ff")),
                        new Stop(1.0, Color.web("#0091ea"))
                );
                gc.setFill(shieldGrad);
                gc.fillRoundRect(cX, cY, cW, cH, 6, 6);

                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.2);
                gc.strokeRoundRect(cX, cY, cW, cH, 6, 6);
            } else {
                // Sel Rusak / Kosong
                gc.setFill(Color.color(0.15, 0.15, 0.20, 0.6));
                gc.fillRoundRect(cX, cY, cW, cH, 6, 6);

                gc.setStroke(Color.color(1.0, 0.2, 0.2, 0.5));
                gc.setLineWidth(1.2);
                gc.strokeRoundRect(cX, cY, cW, cH, 6, 6);

                // Tanda Silang Kecil
                gc.setStroke(Color.web("#ff5252"));
                gc.strokeLine(cX + 5, cY + 5, cX + cW - 5, cY + cH - 5);
                gc.strokeLine(cX + cW - 5, cY + 5, cX + 5, cY + cH - 5);
            }
        }

        // Throttle Speedometer
        boolean fwd = (player != null && player.isMoveForward());
        boolean bwd = (player != null && player.isMoveBackward());

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        if (fwd) {
            gc.setFill(Color.web("#00e676"));
            gc.fillText("[W] BOOST 110% ▲", 830, 44);
        } else if (bwd) {
            gc.setFill(Color.web("#ff9100"));
            gc.fillText("[S] BRAKE 85% ▼", 830, 44);
        } else {
            gc.setFill(Color.web("#80d8ff"));
            gc.fillText("[CRUISE] 100% ▶", 830, 44);
        }

        // Kontrol Bantuan
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        gc.setFill(Color.web("#607d8b"));
        gc.fillText("[W/S] Maju/Mundur | [A/D] Belok", WIDTH - 190, 31);
        gc.fillText("[SPACE] Tembak | [ESC] Pause", WIDTH - 190, 43);

        // --- 6. PERINGATAN HULL INTEGRITY JIKA TINGGAL 1 NYAWA ---
        if (hp == 1) {
            double warnPulse = (Math.sin(hudTime * 8.0) + 1.0) / 2.0;
            gc.setFill(Color.color(1.0, 0.1, 0.1, 0.15 * warnPulse));
            gc.fillRect(0, 0, WIDTH, HEIGHT);

            gc.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 14));
            gc.setFill(Color.color(1.0, 0.2, 0.2, 0.7 + warnPulse * 0.3));
            gc.fillText("⚠️ PERINGATAN: INTEGRITAS PERISAI KRITIS (1/3) ⚠️", WIDTH / 2.0 - 180, HEIGHT - 30);
        }

        gc.restore();
    }

    private void renderAnimatedMiniGalonHUD(GraphicsContext gc, double gx, double gy, double time) {
        gc.save();
        // Bodi Polikarbonat Transparan
        gc.setFill(Color.web("#0288d1"));
        gc.fillRoundRect(gx, gy + 5, 20, 24, 6, 6);

        // Air Kosmik Mengombak
        double wave = Math.sin(time * 5.0) * 1.5;
        gc.setFill(Color.web("#4fc3f7"));
        gc.fillRect(gx + 2, gy + 10 + wave, 16, 17 - wave);

        // Tutup Segel Merah Neon
        gc.setFill(Color.web("#ff1744"));
        gc.fillRoundRect(gx + 5, gy + 1, 10, 6, 2, 2);

        // Aura Kapsul Air
        gc.setStroke(Color.web("#e0f7fa"));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(gx, gy + 5, 20, 24, 6, 6);
        gc.restore();
    }

    private void renderPauseOverlay() {
        gc.save();
        gc.setFill(Color.color(0, 0, 0, 0.7));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        gc.setFill(Color.web("#00e5ff"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("⏸️ GAME DIPARIR (PAUSED)", WIDTH / 2.0, HEIGHT / 2.0 - 20);

        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 18));
        gc.setFill(Color.WHITE);
        gc.fillText("Tekan [ESC] untuk melanjutkan permainan", WIDTH / 2.0, HEIGHT / 2.0 + 30);
        gc.restore();
    }

    private void onGameOver() {
        gameRunning = false;
        gameLoop.stop();

        leaderboardManager.addScore(currentPlayerName, score, totalGalonCount);

        VBox rootBox = new VBox(20);
        rootBox.setAlignment(Pos.CENTER);
        rootBox.setStyle("-fx-background-color: linear-gradient(to bottom, #150004, #260914, #0a0014);");

        Text overText = new Text("MISI SELESAI");
        overText.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 52));
        overText.setFill(Color.web("#00e5ff"));
        overText.setEffect(new DropShadow(25, Color.web("#00b0ff")));

        VBox statsBox = new VBox(10);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setMaxWidth(460);
        statsBox.setStyle(
                "-fx-background-color: rgba(20, 10, 30, 0.88);"
                + "-fx-padding: 24;"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: #00e5ff;"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 14;"
        );

        Label pLabel = new Label("PILOT: " + currentPlayerName.toUpperCase() + " (" + selectedShip.getName() + ")");
        pLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        pLabel.setTextFill(Color.WHITE);

        Label lvlLabel = new Label("SEKTOR TERTINGGI: " + levelManager.getCurrentLevelData().getName());
        lvlLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lvlLabel.setTextFill(Color.web("#ffd600"));

        Label gLabel = new Label("💧 TOTAL GALON KOSMIK: " + totalGalonCount + " Galon");
        gLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 26));
        gLabel.setTextFill(Color.web("#00e5ff"));

        Label sLabel = new Label("SKOR MISI: " + score);
        sLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        sLabel.setTextFill(Color.web("#ffea00"));

        Label savedLabel = new Label("✅ Skor & Galon tersimpan di Papan Peringkat!");
        savedLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        savedLabel.setTextFill(Color.web("#81c784"));

        statsBox.getChildren().addAll(pLabel, lvlLabel, gLabel, sLabel, savedLabel);

        Button playAgainBtn = createSciFiButton("🔄 MAIN LAGI", "#00e676", "#00c853", 240);
        playAgainBtn.setOnAction(e -> startNewGame());

        Button leaderBtn = createSciFiButton("🏆 LIHAT LEADERBOARD", "#2979ff", "#1565c0", 240);
        leaderBtn.setOnAction(e -> showLeaderboardScreen());

        Button hangarBtn = createSciFiButton("🚀 KEMBALI KE HANGAR", "#ab47bc", "#7b1fa2", 240);
        hangarBtn.setOnAction(e -> showHangarMenu());

        VBox btnBox = new VBox(12);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.getChildren().addAll(playAgainBtn, leaderBtn, hangarBtn);

        rootBox.getChildren().addAll(overText, statsBox, btnBox);

        Scene overScene = new Scene(rootBox, WIDTH, HEIGHT);
        primaryStage.setScene(overScene);
    }

    private void showLeaderboardScreen() {
        VBox rootBox = new VBox(18);
        rootBox.setAlignment(Pos.CENTER);
        rootBox.setStyle("-fx-background-color: linear-gradient(to bottom, #050b18, #0e1e3e, #130924);");
        rootBox.setPadding(new Insets(20));

        Text titleText = new Text("🏆 PAPAN SKOR TERTINGGI (TOP GALON COLLECTORS)");
        titleText.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 32));
        titleText.setFill(Color.web("#ffea00"));
        titleText.setEffect(new DropShadow(18, Color.web("#ffb300")));

        VBox tableBox = new VBox(8);
        tableBox.setAlignment(Pos.CENTER);
        tableBox.setMaxWidth(720);
        tableBox.setStyle(
                "-fx-background-color: rgba(10, 25, 50, 0.85);"
                + "-fx-padding: 16;"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: #00e5ff;"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 12;"
        );

        HBox headerRow = createLeaderboardRow("NO", "NAMA PILOT", "💧 GALON", "SKOR", "WAKTU", true);
        tableBox.getChildren().add(headerRow);

        List<LeaderboardEntry> topList = leaderboardManager.getTopEntries();
        for (int i = 0; i < topList.size(); i++) {
            LeaderboardEntry entry = topList.get(i);
            String medal = (i == 0) ? "🥇 1" : (i == 1 ? "🥈 2" : (i == 2 ? "🥉 3" : ("  " + (i + 1))));
            HBox row = createLeaderboardRow(medal, entry.getPlayerName(), String.valueOf(entry.getGalonCount()), String.valueOf(entry.getScore()), entry.getTimestamp(), false);
            tableBox.getChildren().add(row);
        }

        Button backBtn = createSciFiButton("⬅️ KEMBALI KE HANGAR", "#2979ff", "#1565c0", 260);
        backBtn.setOnAction(e -> showHangarMenu());

        rootBox.getChildren().addAll(titleText, tableBox, backBtn);

        Scene lbScene = new Scene(rootBox, WIDTH, HEIGHT);
        primaryStage.setScene(lbScene);
    }

    private HBox createLeaderboardRow(String rank, String name, String galon, String score, String time, boolean isHeader) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 12, 6, 12));
        if (isHeader) {
            row.setStyle("-fx-background-color: rgba(0, 229, 255, 0.18); -fx-background-radius: 6;");
        } else {
            row.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-background-radius: 6;");
        }

        Label rankLbl = new Label(rank);
        rankLbl.setPrefWidth(60);
        rankLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, isHeader ? 14 : 15));
        rankLbl.setTextFill(isHeader ? Color.web("#00e5ff") : Color.web("#ffea00"));

        Label nameLbl = new Label(name);
        nameLbl.setPrefWidth(220);
        nameLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, isHeader ? 14 : 15));
        nameLbl.setTextFill(isHeader ? Color.web("#00e5ff") : Color.WHITE);

        Label galonLbl = new Label(galon);
        galonLbl.setPrefWidth(120);
        galonLbl.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, isHeader ? 14 : 16));
        galonLbl.setTextFill(isHeader ? Color.web("#00e5ff") : Color.web("#40c4ff"));

        Label scoreLbl = new Label(score);
        scoreLbl.setPrefWidth(100);
        scoreLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, isHeader ? 14 : 15));
        scoreLbl.setTextFill(isHeader ? Color.web("#00e5ff") : Color.web("#ffea00"));

        Label timeLbl = new Label(time);
        timeLbl.setPrefWidth(150);
        timeLbl.setFont(Font.font("Segoe UI", FontWeight.NORMAL, isHeader ? 14 : 12));
        timeLbl.setTextFill(isHeader ? Color.web("#00e5ff") : Color.web("#b0bec5"));

        row.getChildren().addAll(rankLbl, nameLbl, galonLbl, scoreLbl, timeLbl);
        return row;
    }

    private void showInstructionsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Petunjuk Space Invader 3D");
        alert.setHeaderText("🚀 PANDUAN MISI LUAR ANGKASA 3D (KONTROL ALAMI)");
        alert.setContentText(
                "🎮 KONTROL 3D ALAMI (FORWARD/BACKWARD & STEER):\n"
                + "• [W] atau [Panah Atas]: MAJU KE DEPAN (Akselerasi mengejar Galon)\n"
                + "• [S] atau [Panah Bawah]: MUNDUR / REM (Melambat menghindari tabrakan UFO)\n"
                + "• [A / D] atau [Panah Kiri/Kanan]: BELOK KIRI & KANAN (Steer & Banking Roll)\n"
                + "• [SPASI]: Tembak Laser 3D ke Horizon\n"
                + "• [M]: Menyalakan / Mematikan Musik Latar (Mute/Unmute)\n"
                + "• [ESC]: Jeda Permainan (Pause/Resume)\n\n"
                + "💧 ATURAN MISI:\n"
                + "1. Fokus Utama: Kumpulkan Galon Kosmik 💧 sebanyak mungkin!\n"
                + "2. Anda bebas menembak UFO atau menghindarinya (menekan S untuk rem atau A/D untuk belok).\n"
                + "3. UFO yang melayang lewat ke belakang tidak mengurangi nyawa.\n"
                + "4. Roket memiliki 3 NYAWA. Jika bertabrakan langsung dengan UFO / Asteroid, nyawa hilang 1!\n"
                + "5. Kumpulkan target galon untuk memicu HYPERSPACE WARP JUMP menuju Sektor berikutnya!"
        );
        alert.showAndWait();
    }

    private Button createSciFiButton(String text, String primaryColor, String darkColor, double width) {
        Button btn = new Button(text);
        btn.setPrefWidth(width);
        btn.setPrefHeight(44);
        btn.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 13));
        String normalStyle =
                "-fx-background-color: linear-gradient(to right, " + darkColor + ", " + primaryColor + ");"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 22;"
                + "-fx-border-radius: 22;"
                + "-fx-border-color: rgba(255, 255, 255, 0.45);"
                + "-fx-border-width: 1.5;"
                + "-fx-cursor: hand;";
        String hoverStyle =
                "-fx-background-color: linear-gradient(to right, " + primaryColor + ", #ffffff);"
                + "-fx-text-fill: #000000;"
                + "-fx-background-radius: 22;"
                + "-fx-border-radius: 22;"
                + "-fx-border-color: #ffffff;"
                + "-fx-border-width: 2;"
                + "-fx-cursor: hand;";

        btn.setStyle(normalStyle);
        btn.setEffect(new DropShadow(10, Color.web(primaryColor, 0.45)));

        btn.setOnMouseEntered(e -> {
            btn.setStyle(hoverStyle);
            btn.setScaleX(1.04);
            btn.setScaleY(1.04);
            btn.setEffect(new DropShadow(18, Color.web(primaryColor)));
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(normalStyle);
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
            btn.setEffect(new DropShadow(10, Color.web(primaryColor, 0.45)));
        });

        return btn;
    }

    private Button createMiniButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(48);
        btn.setPrefHeight(38);
        btn.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 17));
        btn.setStyle(
                "-fx-background-color: #06152d;"
                + "-fx-text-fill: #00e5ff;"
                + "-fx-border-color: #00e5ff;"
                + "-fx-border-radius: 10;"
                + "-fx-background-radius: 10;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0, 229, 255, 0.35), 8, 0, 0, 0);"
        );
        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: #00e5ff; -fx-text-fill: black; -fx-border-color: #ffffff; -fx-border-radius: 10; -fx-background-radius: 10;");
            btn.setScaleX(1.06);
            btn.setScaleY(1.06);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: #06152d; -fx-text-fill: #00e5ff; -fx-border-color: #00e5ff; -fx-border-radius: 10; -fx-background-radius: 10;");
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
        return btn;
    }

    private ProgressBar createStatBar(double progress, String colorHex) {
        ProgressBar bar = new ProgressBar(progress);
        bar.setPrefWidth(160);
        bar.setPrefHeight(12);
        bar.setStyle(
                "-fx-accent: " + colorHex + ";"
                + "-fx-control-inner-background: rgba(0, 0, 0, 0.65);"
                + "-fx-border-color: " + colorHex + ";"
                + "-fx-border-width: 1.0;"
                + "-fx-border-radius: 4;"
                + "-fx-background-radius: 4;"
        );
        return bar;
    }

    private HBox createStatRow(String labelText, ProgressBar bar, Label pctLabel) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web("#90caf9"));
        lbl.setPrefWidth(130);
        pctLabel.setPrefWidth(45);
        row.getChildren().addAll(lbl, bar, pctLabel);
        return row;
    }
}
