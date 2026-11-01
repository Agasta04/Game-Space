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
        Text titleText = new Text("SPACE INVADER 3D");
        titleText.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 44));
        titleText.setFill(Color.web("#00e5ff"));
        titleText.setEffect(new DropShadow(22, Color.web("#00b0ff")));

        Text subText = new Text("HANGAR PEMILIHAN PESAWAT & PERSIAPAN MISI");
        subText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        subText.setFill(Color.web("#ffd600"));

        HBox mainHangarBox = new HBox(24);
        mainHangarBox.setAlignment(Pos.CENTER);
        mainHangarBox.setMaxWidth(880);

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

        VBox leftBox = new VBox(8);
        leftBox.setAlignment(Pos.CENTER);
        leftBox.setStyle(
                "-fx-background-color: rgba(10, 25, 55, 0.75);"
                + "-fx-padding: 14;"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: #00e5ff;"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 14;"
        );

        Label previewTitle = new Label("3D SHIP HOLOGRAM");
        previewTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        previewTitle.setTextFill(Color.web("#80d8ff"));

        // Tombol Ganti Pesawat < dan >
        HBox shipSelectorBox = new HBox(12);
        shipSelectorBox.setAlignment(Pos.CENTER);

        Button prevBtn = createMiniButton("◀");
        Button nextBtn = createMiniButton("▶");

        Label shipNameLabel = new Label(selectedShip.getName());
        shipNameLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 18));
        shipNameLabel.setTextFill(selectedShip.getPrimaryColor());
        shipNameLabel.setPrefWidth(170);
        shipNameLabel.setAlignment(Pos.CENTER);

        // Right Info Box
        VBox rightBox = new VBox(10);
        rightBox.setAlignment(Pos.CENTER_LEFT);
        rightBox.setPrefWidth(420);
        rightBox.setStyle(
                "-fx-background-color: rgba(10, 25, 55, 0.75);"
                + "-fx-padding: 16;"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: #00e5ff;"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 14;"
        );

        Label descLabel = new Label(selectedShip.getDescription());
        descLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        descLabel.setTextFill(Color.web("#cfd8dc"));
        descLabel.setWrapText(true);

        // Bar Statistik Pesawat
        ProgressBar speedBar = createStatBar(selectedShip.getSpeed() / 10.0, "#00e5ff");
        ProgressBar handleBar = createStatBar(selectedShip.getHandling() / 0.22, "#ffea00");
        ProgressBar magnetBar = createStatBar(selectedShip.getMagnetRange() / 250.0, "#d500f9");

        VBox statsBox = new VBox(6);
        statsBox.getChildren().addAll(
                createStatRow("KECEPATAN (SPEED)", speedBar),
                createStatRow("MANUVER (HANDLING)", handleBar),
                createStatRow("MAGNET GALON", magnetBar)
        );

        // Pilihan Drone Pendukung
        Label droneLabel = new Label("PILIH DRONE PENDUKUNG:");
        droneLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        droneLabel.setTextFill(Color.web("#80d8ff"));

        HBox droneChoices = new HBox(8);
        ToggleGroup droneGroup = new ToggleGroup();

        for (DroneType dt : DroneType.values()) {
            RadioButton rb = new RadioButton(dt.getTitle());
            rb.setToggleGroup(droneGroup);
            rb.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            rb.setTextFill(Color.WHITE);
            if (dt == selectedDrone) rb.setSelected(true);
            rb.setOnAction(e -> {
                selectedDrone = dt;
                SoundManager.playClick();
            });
            droneChoices.getChildren().add(rb);
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
            SoundManager.playClick();
        });

        shipSelectorBox.getChildren().addAll(prevBtn, shipNameLabel, nextBtn);
        leftBox.getChildren().addAll(previewTitle, previewCanvas, shipSelectorBox);

        // Input Nama Pemain
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label pNameLbl = new Label("PILOT:");
        pNameLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        pNameLbl.setTextFill(Color.web("#80d8ff"));

        TextField nameInput = new TextField(currentPlayerName);
        nameInput.setPrefWidth(260);
        nameInput.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nameInput.setStyle("-fx-background-color: #0b1d3a; -fx-text-fill: white; -fx-border-color: #00e5ff; -fx-border-radius: 6;");

        nameBox.getChildren().addAll(pNameLbl, nameInput);

        rightBox.getChildren().addAll(nameBox, descLabel, statsBox, droneLabel, droneChoices);
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

        Button instructionsBtn = createSciFiButton("📜 PANDUAN", "#ab47bc", "#7b1fa2", 180);
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

        // Platform Hologram Hangar
        double cx = 170;
        double cy = 175;
        double platW = 190;
        double platH = 44;

        pgc.setStroke(Color.web("#00e5ff", 0.65));
        pgc.setLineWidth(2.0);
        pgc.strokeOval(cx - platW / 2.0, cy - platH / 2.0, platW, platH);

        double pulseW = platW + Math.sin(angle * 3.0) * 8.0;
        double pulseH = platH + Math.sin(angle * 3.0) * 3.0;
        pgc.setStroke(Color.web("#00e5ff", 0.25));
        pgc.strokeOval(cx - pulseW / 2.0, cy - pulseH / 2.0, pulseW, pulseH);

        LinearGradient beamGrad = new LinearGradient(
                0, cy, 0, 40, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0.0, 0.9, 1.0, 0.22)),
                new Stop(1.0, Color.color(0, 0, 0, 0))
        );
        pgc.setFill(beamGrad);
        double[] bx = {cx - platW / 2.0, cx + platW / 2.0, cx + 55, cx - 55};
        double[] by = {cy, cy, 35, 35};
        pgc.fillPolygon(bx, by, 4);

        double shipY = 115 + Math.sin(angle * 2.2) * 7.0;
        double gentleRoll = Math.sin(angle * 0.8) * 0.14;

        // Turntable 3D Penuh 360 Derajat
        ShipRenderer.renderShipFull(pgc, ship, cx, shipY, 1.35, gentleRoll, -0.22, angle, angle, false);
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

        // 1. Bar Panel Atas Glassmorphism
        gc.setFill(Color.color(0.02, 0.05, 0.12, 0.88));
        gc.fillRoundRect(16, 12, WIDTH - 32, 54, 16, 16);
        gc.setStroke(Color.web("#00e5ff", 0.55));
        gc.setLineWidth(1.4);
        gc.strokeRoundRect(16, 12, WIDTH - 32, 54, 16, 16);

        // Pilot & Sektor Level (Kiri)
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        gc.setFill(Color.web("#90caf9"));
        gc.fillText("PILOT: " + currentPlayerName.toUpperCase(), 35, 34);

        gc.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 14));
        gc.setFill(Color.web("#ffd600"));
        gc.fillText(currentLvl.getName() + " (" + currentLvl.getLevelNumber() + "/5)", 35, 52);

        // UTAMA: Total Galon & Level Target Meter (Tengah)
        renderMiniGalonHUD(gc, 340, 20);
        gc.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 22));
        gc.setFill(Color.web("#00e5ff"));
        gc.fillText("TOTAL GALON: " + totalGalonCount, 368, 44);

        // Progress Bar Menuju Level Berikutnya
        if (!levelManager.isFinalLevel()) {
            double progW = 120;
            double progH = 6;
            double progX = 570;
            double progY = 36;
            double ratio = Math.min(1.0, (double) levelGalonCount / currentLvl.getTargetGalon());

            gc.setFill(Color.color(0, 0, 0, 0.6));
            gc.fillRoundRect(progX, progY, progW, progH, 4, 4);
            gc.setFill(Color.web("#00e676"));
            gc.fillRoundRect(progX, progY, progW * ratio, progH, 4, 4);

            gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
            gc.setFill(Color.web("#b0bec5"));
            gc.fillText("WARP TARGET: " + levelGalonCount + "/" + currentLvl.getTargetGalon(), progX, progY - 4);
        } else {
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            gc.setFill(Color.web("#ff1744"));
            gc.fillText("🔥 FINAL BOSS SECTOR 🔥", 570, 42);
        }

        // Nyawa Pemain (3 Nyawa: Kanan)
        int hp = (player != null) ? Math.max(0, player.getHealth()) : 0;
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        gc.setFill(hp == 1 ? Color.web("#ff1744") : Color.web("#cfd8dc"));
        gc.fillText("NYAWA (" + hp + "/3):", 735, 44);

        for (int i = 0; i < 3; i++) {
            double hX = 835 + (i * 24);
            double hY = 28;
            if (i < hp) {
                gc.setFill(Color.web("#ff1744"));
                gc.fillRoundRect(hX, hY, 18, 18, 6, 6);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.2);
                gc.strokeRoundRect(hX, hY, 18, 18, 6, 6);
            } else {
                gc.setFill(Color.color(0.3, 0.3, 0.3, 0.4));
                gc.fillRoundRect(hX, hY, 18, 18, 6, 6);
                gc.setStroke(Color.color(0.5, 0.5, 0.5, 0.3));
                gc.strokeRoundRect(hX, hY, 18, 18, 6, 6);
            }
        }

        // Bantuan Tombol Navigasi Maju-Mundur
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        gc.setFill(Color.web("#78909c"));
        gc.fillText("[W/S] Maju-Mundur | [A/D] Belok", WIDTH - 180, 40);
        gc.fillText("[SPACE] Tembak | [ESC] Pause", WIDTH - 180, 53);

        gc.restore();
    }

    private void renderMiniGalonHUD(GraphicsContext gc, double gx, double gy) {
        gc.setFill(Color.web("#0288d1"));
        gc.fillRoundRect(gx, gy + 4, 16, 20, 4, 4);
        gc.setFill(Color.web("#4fc3f7"));
        gc.fillRect(gx + 2, gy + 7, 12, 14);
        gc.setFill(Color.web("#d32f2f"));
        gc.fillRect(gx + 4, gy, 8, 5);
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
        btn.setPrefHeight(42);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        String normalStyle =
                "-fx-background-color: linear-gradient(to right, " + darkColor + ", " + primaryColor + ");"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-color: rgba(255, 255, 255, 0.4);"
                + "-fx-border-width: 1.5;"
                + "-fx-cursor: hand;";
        String hoverStyle =
                "-fx-background-color: linear-gradient(to right, " + primaryColor + ", #ffffff);"
                + "-fx-text-fill: #000000;"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-color: #ffffff;"
                + "-fx-border-width: 2;"
                + "-fx-cursor: hand;";

        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(hoverStyle);
            btn.setEffect(new Glow(0.6));
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(normalStyle);
            btn.setEffect(null);
        });

        return btn;
    }

    private Button createMiniButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(44);
        btn.setPrefHeight(36);
        btn.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 16));
        btn.setStyle(
                "-fx-background-color: #0b1d3a;"
                + "-fx-text-fill: #00e5ff;"
                + "-fx-border-color: #00e5ff;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #00e5ff; -fx-text-fill: black; -fx-border-radius: 8; -fx-background-radius: 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #0b1d3a; -fx-text-fill: #00e5ff; -fx-border-color: #00e5ff; -fx-border-radius: 8; -fx-background-radius: 8;"));
        return btn;
    }

    private ProgressBar createStatBar(double progress, String colorHex) {
        ProgressBar bar = new ProgressBar(progress);
        bar.setPrefWidth(160);
        bar.setPrefHeight(12);
        bar.setStyle("-fx-accent: " + colorHex + ";");
        return bar;
    }

    private HBox createStatRow(String labelText, ProgressBar bar) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web("#90caf9"));
        lbl.setPrefWidth(140);
        row.getChildren().addAll(lbl, bar);
        return row;
    }
}
