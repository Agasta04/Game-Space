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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.*;

/**
 * Game Space Shooter Modern (UFO vs Rocket - Galon Collector).
 * Fokus Misi: Mengumpulkan Galon Kosmik sebanyak-banyaknya.
 * Pemain bebas menghancurkan atau menghindari UFO alien tanpa penalti jika UFO lolos.
 */
public class SpaceShooter extends Application {

    // Resolusi Widescreen untuk Laptop & Tablet
    public static final int WIDTH = 1050;
    public static final int HEIGHT = 700;

    private Stage primaryStage;
    private Scene gameScene;
    private Pane gameRoot;
    private Canvas canvas;
    private GraphicsContext gc;

    // Entitas Game
    private final List<GameObject> gameObjects = new ArrayList<>();
    private final List<GameObject> newObjects = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private Player player;
    private SpaceBackground spaceBackground;

    // Leaderboard
    private final LeaderboardManager leaderboardManager = new LeaderboardManager();
    private String currentPlayerName = "Astronaut";

    // Game State
    private int score = 0;
    private int galonCount = 0;
    private boolean gameRunning = false;
    private boolean isPaused = false;
    private boolean bossExists = false;
    private final Random random = new Random();

    // Game Loop
    private AnimationTimer gameLoop;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Space Shooter 3D - Misi Pemburu Galon Kosmik");
        stage.setResizable(false);

        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/player.png"))));
        } catch (Exception ignored) {}

        spaceBackground = new SpaceBackground(WIDTH, HEIGHT);
        initGameScene();

        // Mulai pemutaran musik latar retro synthwave
        SoundManager.startBGM();

        // Tampilkan Menu Utama di awal
        showMainMenu();
        stage.show();
    }

    /**
     * Inisialisasi Canvas dan Event Handler permainan.
     */
    private void initGameScene() {
        gameRoot = new Pane();
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        gameRoot.getChildren().add(canvas);
        gameScene = new Scene(gameRoot, WIDTH, HEIGHT, Color.BLACK);

        // Key Listeners
        gameScene.setOnKeyPressed(event -> {
            if (player == null) return;
            KeyCode code = event.getCode();
            if (code == KeyCode.A || code == KeyCode.LEFT) player.setMoveLeft(true);
            if (code == KeyCode.D || code == KeyCode.RIGHT) player.setMoveRight(true);
            if (code == KeyCode.W || code == KeyCode.UP) player.setMoveForward(true);
            if (code == KeyCode.S || code == KeyCode.DOWN) player.setMoveBackward(true);
            if (code == KeyCode.SPACE && gameRunning && !isPaused) {
                player.shoot(newObjects);
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

        // Game Loop
        gameLoop = new AnimationTimer() {
            private long lastEnemySpawn = 0;
            private long lastGalonSpawn = 0;

            @Override
            public void handle(long now) {
                if (!gameRunning) return;

                if (isPaused) {
                    renderPauseOverlay();
                    return;
                }

                // Update Background & Camera Shake
                spaceBackground.update();

                // Spawn Musuh UFO (Musuh untuk dihindari atau ditembak jika menghalangi)
                if (now - lastEnemySpawn > 1_200_000_000L) {
                    spawnEnemyUfo();
                    lastEnemySpawn = now;
                }

                // Spawn Galon Kosmik lebih sering agar banyak galon untuk dikumpulkan!
                if (now - lastGalonSpawn > 1_600_000_000L) {
                    spawnGalon();
                    lastGalonSpawn = now;
                }

                // Kemunculan Boss UFO Mothership setiap 15 galon
                if (galonCount >= 15 && !bossExists && (galonCount % 15 == 0)) {
                    spawnBossMothership();
                }

                // Update & Render
                gc.save();
                // Terapkan efek getaran layar (Screen Shake)
                gc.translate(spaceBackground.getShakeX(), spaceBackground.getShakeY());

                spaceBackground.render(gc);

                // Partikel roket
                if (player != null && !player.isDead()) {
                    player.emitThrusterParticles(particles);
                }

                // Update Partikel
                Iterator<Particle> pIt = particles.iterator();
                while (pIt.hasNext()) {
                    Particle p = pIt.next();
                    p.update();
                    p.render(gc);
                    if (p.isDead()) pIt.remove();
                }

                // Update & Render GameObjects
                checkCollisions();
                checkEnemiesBottom();

                gameObjects.addAll(newObjects);
                newObjects.clear();

                for (GameObject obj : gameObjects) {
                    obj.update();
                    obj.render(gc);

                    if (obj instanceof BossEnemy) {
                        ((BossEnemy) obj).shoot(newObjects);
                    }
                }

                // Hapus objek mati
                Iterator<GameObject> objIt = gameObjects.iterator();
                while (objIt.hasNext()) {
                    GameObject obj = objIt.next();
                    if (obj.isDead()) {
                        if (obj instanceof BossEnemy) {
                            bossExists = false;
                        }
                        objIt.remove();
                    }
                }

                // Update & Render Floating Texts
                Iterator<FloatingText> tIt = floatingTexts.iterator();
                while (tIt.hasNext()) {
                    FloatingText ft = tIt.next();
                    ft.update();
                    ft.render(gc);
                    if (ft.isDead()) tIt.remove();
                }

                gc.restore();

                // Render HUD Sci-Fi di atas layar
                renderHUD();

                // Cek Game Over
                if (player != null && player.isDead()) {
                    onGameOver();
                }
            }
        };
    }

    /**
     * Tampilan Layar Utama (Main Menu) dengan input nama pemain.
     */
    private void showMainMenu() {
        gameRunning = false;

        VBox rootBox = new VBox(20);
        rootBox.setAlignment(Pos.CENTER);
        rootBox.setStyle("-fx-background-color: linear-gradient(to bottom, #050b18, #0b1a3a, #15092a);");

        // Judul Utama dengan efek Glow Kosmik
        Text titleText = new Text("SPACE INVADER 3D");
        titleText.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 48));
        titleText.setFill(Color.web("#00e5ff"));
        titleText.setEffect(new DropShadow(20, Color.web("#00b0ff")));

        Text subtitleText = new Text("💧 MISI UTAMA: PENGUMPUL GALON KOSMIK 💧");
        subtitleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        subtitleText.setFill(Color.web("#ffea00"));
        subtitleText.setEffect(new DropShadow(10, Color.web("#ff9100")));

        Text hintText = new Text("💡 Anda boleh menghabisi atau menghindari UFO. Poin utama adalah Galon!");
        hintText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        hintText.setFill(Color.web("#80d8ff"));

        // Box Input Nama Pemain
        VBox inputContainer = new VBox(8);
        inputContainer.setAlignment(Pos.CENTER);
        inputContainer.setMaxWidth(380);
        inputContainer.setStyle(
                "-fx-background-color: rgba(16, 32, 64, 0.7);"
                + "-fx-padding: 16;"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: #00e5ff;"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 14;"
        );

        Label nameLabel = new Label("NAMA PILOT ASTRONOT:");
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#b0bec5"));

        TextField nameInput = new TextField(currentPlayerName);
        nameInput.setMaxWidth(320);
        nameInput.setAlignment(Pos.CENTER);
        nameInput.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        nameInput.setStyle(
                "-fx-background-color: #0a192f;"
                + "-fx-text-fill: #ffffff;"
                + "-fx-border-color: #29b6f6;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 8;"
        );

        inputContainer.getChildren().addAll(nameLabel, nameInput);

        // Tombol-tombol Menu
        Button startBtn = createSciFiButton("🚀 MULAI MISI", "#00e676", "#00c853");
        startBtn.setOnAction(e -> {
            String enteredName = nameInput.getText().trim();
            if (!enteredName.isEmpty()) {
                currentPlayerName = enteredName;
            }
            startNewGame();
        });

        Button leaderboardBtn = createSciFiButton("🏆 PAPAN SKOR (LEADERBOARD)", "#2979ff", "#1565c0");
        leaderboardBtn.setOnAction(e -> showLeaderboardScreen());

        Button instructionsBtn = createSciFiButton("📜 PETUNJUK & KONTROL", "#ab47bc", "#7b1fa2");
        instructionsBtn.setOnAction(e -> showInstructionsDialog());

        Button musicBtn = createSciFiButton("🎵 MUSIK: " + (SoundManager.isMusicEnabled() ? "ON" : "OFF"), "#ff9100", "#e65100");
        musicBtn.setOnAction(e -> {
            SoundManager.toggleMusic();
            musicBtn.setText("🎵 MUSIK: " + (SoundManager.isMusicEnabled() ? "ON" : "OFF"));
        });

        Button exitBtn = createSciFiButton("❌ KELUAR", "#ef5350", "#c62828");
        exitBtn.setOnAction(e -> System.exit(0));

        VBox buttonsBox = new VBox(12);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.getChildren().addAll(startBtn, leaderboardBtn, instructionsBtn, musicBtn, exitBtn);

        rootBox.getChildren().addAll(titleText, subtitleText, hintText, inputContainer, buttonsBox);

        Scene menuScene = new Scene(rootBox, WIDTH, HEIGHT);
        primaryStage.setScene(menuScene);
    }

    /**
     * Memulai sesi permainan baru.
     */
    private void startNewGame() {
        gameObjects.clear();
        newObjects.clear();
        particles.clear();
        floatingTexts.clear();

        score = 0;
        galonCount = 0;
        bossExists = false;
        isPaused = false;
        Enemy.SPEED = 1.6;

        player = new Player(WIDTH / 2.0, HEIGHT - 80);
        gameObjects.add(player);

        gameRunning = true;
        gameLoop.start();
        primaryStage.setScene(gameScene);

        floatingTexts.add(new FloatingText("💧 AMBIL GALON SEBANYAK MUNGKIN! HINDARI TABRAKAN UFO!", WIDTH / 2.0 - 280, HEIGHT / 2.0, Color.web("#00e5ff"), 20, 2.5));
    }

    private void spawnEnemyUfo() {
        int x = random.nextInt(WIDTH - 120) + 60;
        gameObjects.add(new Enemy(x, -40));
    }

    private void spawnGalon() {
        int x = random.nextInt(WIDTH - 100) + 50;
        // 20% kemungkinan muncul Galon Emas bernilai 3 Galon!
        boolean isGolden = (random.nextDouble() < 0.20);
        gameObjects.add(new GalonItem(x, -50, isGolden));
    }

    private void spawnBossMothership() {
        if (!bossExists) {
            bossExists = true;
            BossEnemy boss = new BossEnemy(WIDTH / 2.0, -80);
            gameObjects.add(boss);
            SoundManager.playBossAlert();
            spaceBackground.triggerScreenShake(12.0);
            floatingTexts.add(new FloatingText("⚠️ BOSS UFO MOTHERSHIP MUNCUL!", WIDTH / 2.0 - 180, HEIGHT / 2.0 - 50, Color.web("#ff1744"), 22, 3.0));
        }
    }

    /**
     * Pengecekan Tabrakan (Collision Detection).
     */
    private void checkCollisions() {
        List<Bullet> bullets = new ArrayList<>();
        List<Enemy> enemies = new ArrayList<>();
        List<EnemyBullet> enemyBullets = new ArrayList<>();
        List<GalonItem> galons = new ArrayList<>();

        for (GameObject obj : gameObjects) {
            if (obj instanceof Bullet) bullets.add((Bullet) obj);
            else if (obj instanceof Enemy) enemies.add((Enemy) obj);
            else if (obj instanceof EnemyBullet) enemyBullets.add((EnemyBullet) obj);
            else if (obj instanceof GalonItem) galons.add((GalonItem) obj);
        }

        // 1. Peluru Pemain vs UFO Musuh (Opsional untuk membuka jalur)
        for (Bullet bullet : bullets) {
            for (Enemy enemy : enemies) {
                if (!bullet.isDead() && !enemy.isDead() && bullet.getBounds().intersects(enemy.getBounds())) {
                    bullet.setDead(true);
                    if (enemy instanceof BossEnemy) {
                        BossEnemy boss = (BossEnemy) enemy;
                        boss.takeDamage();
                        score += 50;
                        floatingTexts.add(new FloatingText("+50", boss.getX(), boss.getY() - 10, Color.web("#ffea00"), 14, 0.8));
                        SoundManager.playHit();
                        if (boss.isDead()) {
                            score += 500;
                            galonCount += 5; // Mengalahkan Boss memberi 5 Galon bonus!
                            spaceBackground.triggerScreenShake(16.0);
                            boss.spawnExplosionParticles(particles);
                            SoundManager.playExplosion();
                            floatingTexts.add(new FloatingText("💥 BOSS HANCUR! +5 GALON BONUS!", WIDTH / 2.0 - 180, HEIGHT / 2.0 - 40, Color.web("#00e5ff"), 24, 2.5));
                        }
                    } else {
                        enemy.setDead(true);
                        score += 20;
                        enemy.spawnExplosionParticles(particles);
                        SoundManager.playExplosion();
                        spaceBackground.triggerScreenShake(4.0);
                        floatingTexts.add(new FloatingText("UFO HANCUR", enemy.getX() - 20, enemy.getY(), Color.web("#69f0ae"), 13, 0.7));
                    }
                }
            }

            // 2. Peluru Pemain Mengambil Galon Jarak Jauh
            for (GalonItem galon : galons) {
                if (!bullet.isDead() && !galon.isDead() && bullet.getBounds().intersects(galon.getBounds())) {
                    bullet.setDead(true);
                    collectGalon(galon);
                }
            }
        }

        // 3. Roket Pemain Mengambil Galon Langsung
        if (player != null && !player.isDead()) {
            for (GalonItem galon : galons) {
                if (!galon.isDead() && player.getBounds().intersects(galon.getBounds())) {
                    collectGalon(galon);
                }
            }

            // 4. Peluru Musuh vs Roket Pemain
            for (EnemyBullet eb : enemyBullets) {
                if (!eb.isDead() && player.getBounds().intersects(eb.getBounds())) {
                    eb.setDead(true);
                    boolean damaged = player.takeDamage();
                    if (damaged) {
                        spaceBackground.triggerScreenShake(12.0);
                        spawnSparks(player.getX(), player.getY(), Color.web("#ff1744"));
                        floatingTexts.add(new FloatingText("⚠️ TERTEMBAK! NYAWA: " + player.getHealth() + "/3",
                                player.getX() - 80, player.getY() - 25, Color.web("#ff5252"), 16, 1.2));
                    }
                }
            }

            // 5. Tabrakan Langsung UFO vs Roket Pemain (Nyawa hilang 1, 3x tabrakan = mati)
            for (Enemy enemy : enemies) {
                if (!enemy.isDead() && player.getBounds().intersects(enemy.getBounds())) {
                    if (!(enemy instanceof BossEnemy)) {
                        enemy.setDead(true);
                        enemy.spawnExplosionParticles(particles);
                    }
                    boolean damaged = player.takeDamage();
                    if (damaged) {
                        spaceBackground.triggerScreenShake(16.0);
                        spawnSparks(player.getX(), player.getY(), Color.web("#ff1744"));
                        floatingTexts.add(new FloatingText("💥 TERTABRAK UFO! NYAWA: " + player.getHealth() + "/3",
                                player.getX() - 95, player.getY() - 25, Color.web("#ff1744"), 18, 1.5));
                    }
                }
            }
        }
    }

    private void collectGalon(GalonItem galon) {
        galon.setDead(true);
        int val = galon.getGalonValue();
        galonCount += val;
        score += galon.getScoreValue();
        SoundManager.playGalonPickup();

        if (galon.isGolden()) {
            spawnSparks(galon.getX(), galon.getY(), Color.web("#ffd700"));
            floatingTexts.add(new FloatingText("✨ +3 GALON EMAS! (+300)", galon.getX() - 40, galon.getY(), Color.web("#ffd700"), 20, 1.5));
        } else {
            spawnSparks(galon.getX(), galon.getY(), Color.web("#40c4ff"));
            floatingTexts.add(new FloatingText("💧 +1 GALON! (+100)", galon.getX() - 30, galon.getY(), Color.web("#00e5ff"), 18, 1.2));
        }
    }

    private void spawnSparks(double x, double y, Color color) {
        for (int i = 0; i < 15; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double spd = 2.0 + random.nextDouble() * 4.0;
            particles.add(new Particle(x, y, Math.cos(angle) * spd, Math.sin(angle) * spd, 4, 0.4, color));
        }
    }

    /**
     * Pengecekan objek yang mencapai bawah layar.
     * Sesuai konsep baru: UFO lolos TIDAK MENGURANGI NYAWA! Pemain bebas menghindarinya.
     */
    private void checkEnemiesBottom() {
        for (GameObject obj : gameObjects) {
            if (obj instanceof Enemy && !(obj instanceof BossEnemy)) {
                Enemy enemy = (Enemy) obj;
                if (enemy.getY() >= HEIGHT + 30) {
                    // UFO hanya dihapus secara damai, tidak ada penalti nyawa / skor!
                    enemy.setDead(true);
                }
            } else if (obj instanceof GalonItem) {
                GalonItem g = (GalonItem) obj;
                if (g.getY() >= HEIGHT + 40) {
                    g.setDead(true);
                }
            }
        }
    }

    /**
     * Render HUD (Heads-Up Display) bergaya futuristik di bagian atas.
     * Galon yang dikumpulkan menjadi sorotan utama di tengah.
     */
    private void renderHUD() {
        gc.save();

        // Baris Kontainer Atas Semi-Transparan
        gc.setFill(Color.color(0.02, 0.05, 0.12, 0.85));
        gc.fillRoundRect(15, 12, WIDTH - 30, 52, 14, 14);
        gc.setStroke(Color.web("#00b0ff", 0.5));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(15, 12, WIDTH - 30, 52, 14, 14);

        // Pilot Name
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        gc.setFill(Color.web("#90caf9"));
        gc.fillText("PILOT: " + currentPlayerName.toUpperCase(), 35, 43);

        // UTAMA: Total Galon Dikumpulkan (Disorot Paling Menonjol di Tengah)
        renderMiniGalon(gc, 340, 22);
        gc.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 22));
        gc.setFill(Color.web("#00e5ff"));
        gc.fillText("TOTAL GALON: " + galonCount, 368, 45);

        // Skor Sekunder
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        gc.setFill(Color.web("#ffea00"));
        gc.fillText("SKOR: " + score, 620, 43);

        // Nyawa Pemain (3 Nyawa: Tertabrak 3x = Mati)
        int hp = (player != null) ? Math.max(0, player.getHealth()) : 0;
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        gc.setFill(hp == 1 ? Color.web("#ff1744") : Color.web("#cfd8dc"));
        gc.fillText("NYAWA (" + hp + "/3): ", 735, 43);

        for (int i = 0; i < 3; i++) {
            double hX = 840 + (i * 24);
            double hY = 28;
            if (i < hp) {
                // Hati / Nyawa aktif (Merah Terang)
                gc.setFill(Color.web("#ff1744"));
                gc.fillRoundRect(hX, hY, 18, 18, 6, 6);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.2);
                gc.strokeRoundRect(hX, hY, 18, 18, 6, 6);
            } else {
                // Hati / Nyawa hilang (Gelap)
                gc.setFill(Color.color(0.3, 0.3, 0.3, 0.4));
                gc.fillRoundRect(hX, hY, 18, 18, 6, 6);
                gc.setStroke(Color.color(0.5, 0.5, 0.5, 0.3));
                gc.strokeRoundRect(hX, hY, 18, 18, 6, 6);
            }
        }

        // Bantuan tombol
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        gc.setFill(Color.web("#78909c"));
        gc.fillText("[ESC] Pause | [M] Audio", WIDTH - 165, 43);

        gc.restore();
    }

    private void renderMiniGalon(GraphicsContext gc, double gx, double gy) {
        gc.setFill(Color.web("#0288d1"));
        gc.fillRoundRect(gx, gy + 4, 16, 20, 4, 4);
        gc.setFill(Color.web("#4fc3f7"));
        gc.fillRect(gx + 2, gy + 7, 12, 14);
        gc.setFill(Color.web("#d32f2f"));
        gc.fillRect(gx + 4, gy, 8, 5);
    }

    private void renderPauseOverlay() {
        gc.save();
        gc.setFill(Color.color(0, 0, 0, 0.65));
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

    /**
     * Ditampilkan ketika nyawa pemain habis.
     */
    private void onGameOver() {
        gameRunning = false;
        gameLoop.stop();

        // Simpan ke Leaderboard (dengan urutan galon terbanyak)
        leaderboardManager.addScore(currentPlayerName, score, galonCount);

        VBox rootBox = new VBox(20);
        rootBox.setAlignment(Pos.CENTER);
        rootBox.setStyle("-fx-background-color: linear-gradient(to bottom, #1a0005, #2b0b14, #0d0014);");

        Text overText = new Text("MISI SELESAI");
        overText.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 52));
        overText.setFill(Color.web("#00e5ff"));
        overText.setEffect(new DropShadow(25, Color.web("#00b0ff")));

        // Panel Statistik Misi
        VBox statsBox = new VBox(10);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setMaxWidth(440);
        statsBox.setStyle(
                "-fx-background-color: rgba(20, 10, 25, 0.85);"
                + "-fx-padding: 22;"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: #00e5ff;"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 14;"
        );

        Label pLabel = new Label("PILOT: " + currentPlayerName.toUpperCase());
        pLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        pLabel.setTextFill(Color.WHITE);

        // Sorotan Utama: Galon yang Dikumpulkan
        Label gLabel = new Label("💧 TOTAL GALON: " + galonCount + " Galon");
        gLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 26));
        gLabel.setTextFill(Color.web("#00e5ff"));

        Label sLabel = new Label("TOTAL SKOR: " + score);
        sLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        sLabel.setTextFill(Color.web("#ffea00"));

        Label savedLabel = new Label("✅ Skor & Galon tersimpan di Papan Peringkat!");
        savedLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        savedLabel.setTextFill(Color.web("#81c784"));

        statsBox.getChildren().addAll(pLabel, gLabel, sLabel, savedLabel);

        Button playAgainBtn = createSciFiButton("🔄 MAIN LAGI", "#00e676", "#00c853");
        playAgainBtn.setOnAction(e -> startNewGame());

        Button leaderBtn = createSciFiButton("🏆 LIHAT LEADERBOARD", "#2979ff", "#1565c0");
        leaderBtn.setOnAction(e -> showLeaderboardScreen());

        Button menuBtn = createSciFiButton("🏠 MENU UTAMA", "#ab47bc", "#7b1fa2");
        menuBtn.setOnAction(e -> showMainMenu());

        VBox btnBox = new VBox(12);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.getChildren().addAll(playAgainBtn, leaderBtn, menuBtn);

        rootBox.getChildren().addAll(overText, statsBox, btnBox);

        Scene overScene = new Scene(rootBox, WIDTH, HEIGHT);
        primaryStage.setScene(overScene);
    }

    /**
     * Tampilan Layar Leaderboard (Papan Skor Tertinggi).
     * Urutan utama berdasarkan Galon Terbanyak.
     */
    private void showLeaderboardScreen() {
        VBox rootBox = new VBox(18);
        rootBox.setAlignment(Pos.CENTER);
        rootBox.setStyle("-fx-background-color: linear-gradient(to bottom, #050b18, #0e1e3e, #130924);");
        rootBox.setPadding(new Insets(20));

        Text titleText = new Text("🏆 PAPAN SKOR TERTINGGI (TOP GALON COLLECTORS)");
        titleText.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 32));
        titleText.setFill(Color.web("#ffea00"));
        titleText.setEffect(new DropShadow(18, Color.web("#ffb300")));

        // List Leaderboard
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

        // Header Row: Kolom Galon ditaruh sebelum Skor
        HBox headerRow = createLeaderboardRow("NO", "NAMA PILOT", "💧 GALON", "SKOR", "WAKTU", true);
        tableBox.getChildren().add(headerRow);

        List<LeaderboardEntry> topList = leaderboardManager.getTopEntries();
        for (int i = 0; i < topList.size(); i++) {
            LeaderboardEntry entry = topList.get(i);
            String medal = (i == 0) ? "🥇 1" : (i == 1 ? "🥈 2" : (i == 2 ? "🥉 3" : ("  " + (i + 1))));
            HBox row = createLeaderboardRow(medal, entry.getPlayerName(), String.valueOf(entry.getGalonCount()), String.valueOf(entry.getScore()), entry.getTimestamp(), false);
            tableBox.getChildren().add(row);
        }

        Button backBtn = createSciFiButton("⬅️ KEMBALI KE MENU", "#2979ff", "#1565c0");
        backBtn.setOnAction(e -> showMainMenu());

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

        // Galon menjadi kolom sorotan utama
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

    /**
     * Dialog Petunjuk Permainan.
     */
    private void showInstructionsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Petunjuk Space Shooter 3D");
        alert.setHeaderText("🚀 PANDUAN MISI PENGUMPUL GALON");
        alert.setContentText(
                "🎮 KONTROL PERMAINAN:\n"
                + "• [W, A, S, D] atau [Panah]: Menggerakkan Roket 3D\n"
                + "• [SPASI]: Menembak Laser (Bisa juga untuk mengambil galon jarak jauh)\n"
                + "• [M]: Menyalakan / Mematikan Musik Latar (Mute/Unmute)\n"
                + "• [ESC]: Jeda Permainan (Pause/Resume)\n\n"
                + "💧 ATURAN MISI:\n"
                + "1. Fokus utama Anda adalah mengumpulkan GALON KOSMIK sebanyak mungkin!\n"
                + "2. Anda TIDAK HARUS membunuh semua UFO. Boleh menembaknya atau menghindarinya!\n"
                + "3. UFO yang lolos ke bawah layar TIDAK MENGURANGI NYAWA.\n"
                + "4. Roket memiliki 3 NYAWA. Setiap kali bertabrakan langsung dengan UFO, nyawa hilang 1!\n"
                + "5. Jika 3 KALI TERTABRAK UFO, roket akan hancur dan misi berakhir (Game Over)."
        );
        alert.showAndWait();
    }

    /**
     * Helper untuk membuat tombol bernuansa Sci-Fi futuristik dengan transisi glow saat hover.
     */
    private Button createSciFiButton(String text, String primaryColor, String darkColor) {
        Button btn = new Button(text);
        btn.setPrefWidth(350);
        btn.setPrefHeight(42);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
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
}
