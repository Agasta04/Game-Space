package com.example.spaceshootergamejavafx;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.Random;

/**
 * SoundManager menghasilkan efek suara dan musik latar menggunakan synthesizer
 * bawaan Java Sound (javax.sound.sampled), sehingga 100% mandiri tanpa perlu file audio luar.
 */
public class SoundManager {
    private static final float SAMPLE_RATE = 22050f;
    private static boolean soundEnabled = true;
    private static boolean musicEnabled = true;

    private static Thread musicThread;
    private static volatile boolean runningMusic = false;

    // Cache sound clips untuk latensi minimal
    private static byte[] laserAudio;
    private static byte[] explosionAudio;
    private static byte[] galonAudio;
    private static byte[] hitAudio;
    private static byte[] bossAlertAudio;

    static {
        try {
            laserAudio = generateLaserSample();
            explosionAudio = generateExplosionSample();
            galonAudio = generateGalonSample();
            hitAudio = generateHitSample();
            bossAlertAudio = generateBossAlertSample();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static boolean isMusicEnabled() {
        return musicEnabled;
    }

    public static void toggleMusic() {
        musicEnabled = !musicEnabled;
        if (!musicEnabled) {
            stopBGM();
        } else {
            startBGM();
        }
    }

    public static void playLaser() {
        if (!soundEnabled) return;
        playPcmData(laserAudio);
    }

    public static void playExplosion() {
        if (!soundEnabled) return;
        playPcmData(explosionAudio);
    }

    public static void playGalonPickup() {
        if (!soundEnabled) return;
        playPcmData(galonAudio);
    }

    public static void playHit() {
        if (!soundEnabled) return;
        playPcmData(hitAudio);
    }

    public static void playBossAlert() {
        if (!soundEnabled) return;
        playPcmData(bossAlertAudio);
    }

    private static void playPcmData(byte[] pcmData) {
        if (pcmData == null) return;
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, false, false);
                Clip clip = AudioSystem.getClip();
                clip.open(format, pcmData, 0, pcmData.length);
                clip.start();
                // Tunggu sampai selesai lalu close
                Thread.sleep(Math.max(100, (long) (pcmData.length * 1000.0 / SAMPLE_RATE)));
                clip.close();
            } catch (Exception ignored) {
            }
        }).start();
    }

    // Suara tembakan laser sci-fi
    private static byte[] generateLaserSample() {
        int length = (int) (SAMPLE_RATE * 0.12);
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / length;
            double freq = 900.0 * (1.0 - progress * 0.7);
            double sample = Math.sin(2 * Math.PI * freq * t) * (1.0 - progress);
            buffer[i] = (byte) (sample * 110 + 128);
        }
        return buffer;
    }

    // Suara ledakan UFO bergemuruh
    private static byte[] generateExplosionSample() {
        int length = (int) (SAMPLE_RATE * 0.35);
        byte[] buffer = new byte[length];
        Random rand = new Random(42);
        double lowPass = 0;
        for (int i = 0; i < length; i++) {
            double progress = (double) i / length;
            double envelope = Math.pow(1.0 - progress, 2.2);
            double noise = (rand.nextDouble() * 2.0 - 1.0);
            lowPass = lowPass * 0.8 + noise * 0.2; // Filter low-pass
            double sample = lowPass * envelope;
            buffer[i] = (byte) (sample * 120 + 128);
        }
        return buffer;
    }

    // Suara ambil galon (chord kristal menyejukkan)
    private static byte[] generateGalonSample() {
        int length = (int) (SAMPLE_RATE * 0.3);
        byte[] buffer = new byte[length];
        double[] freqs = {523.25, 659.25, 783.99, 1046.50}; // C5, E5, G5, C6
        for (int i = 0; i < length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / length;
            double envelope = Math.exp(-progress * 4.5);
            double sample = 0;
            for (double f : freqs) {
                sample += Math.sin(2 * Math.PI * f * t);
            }
            sample = (sample / freqs.length) * envelope;
            buffer[i] = (byte) (sample * 115 + 128);
        }
        return buffer;
    }

    // Suara tabrakan / damage
    private static byte[] generateHitSample() {
        int length = (int) (SAMPLE_RATE * 0.15);
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / length;
            double freq = 120.0 * (1.0 - progress * 0.4);
            double square = (Math.sin(2 * Math.PI * freq * t) > 0) ? 0.7 : -0.7;
            double sample = square * (1.0 - progress);
            buffer[i] = (byte) (sample * 110 + 128);
        }
        return buffer;
    }

    // Suara peringatan kemunculan Boss UFO
    private static byte[] generateBossAlertSample() {
        int length = (int) (SAMPLE_RATE * 0.6);
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = (t < 0.3) ? 220.0 : 180.0;
            double sample = Math.sin(2 * Math.PI * freq * t) * (0.8 - (t % 0.3));
            buffer[i] = (byte) (sample * 110 + 128);
        }
        return buffer;
    }

    /**
     * Memulai pemutaran musik latar luar angkasa retro synthwave secara looping.
     */
    public static synchronized void startBGM() {
        if (!musicEnabled || runningMusic) return;
        runningMusic = true;

        musicThread = new Thread(() -> {
            // Urutan nada arpeggio sci-fi (Frekuensi dalam Hz)
            double[] bassNotes = {130.81, 146.83, 164.81, 174.61, 196.00, 220.00}; // C3 - A3
            double[] leadNotes = {
                261.63, 329.63, 392.00, 523.25, // C - E - G - C
                293.66, 369.99, 440.00, 587.33, // D - F# - A - D
                246.94, 329.63, 392.00, 493.88, // B - E - G - B
                261.63, 329.63, 392.00, 659.25  // C - E - G - E
            };

            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, false, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format, 4096);
                line.start();

                int noteIdx = 0;
                int bassIdx = 0;
                int stepDurationSamples = (int) (SAMPLE_RATE * 0.16); // 160ms per beat
                byte[] chunk = new byte[stepDurationSamples];

                while (runningMusic) {
                    double leadFreq = leadNotes[noteIdx % leadNotes.length];
                    double bassFreq = bassNotes[(noteIdx / 4) % bassNotes.length];

                    for (int i = 0; i < stepDurationSamples; i++) {
                        double t = (double) i / SAMPLE_RATE;
                        double envelope = Math.exp(-((double) i / stepDurationSamples) * 2.2);

                        // Synthesize lead synth (sin + tri) & sub-bass
                        double lead = Math.sin(2 * Math.PI * leadFreq * t) * 0.5
                                    + Math.sin(4 * Math.PI * leadFreq * t) * 0.2;
                        double bass = Math.sin(2 * Math.PI * bassFreq * t) * 0.45;

                        double mix = (lead * envelope + bass * 0.7) * 0.4;
                        chunk[i] = (byte) (mix * 100 + 128);
                    }

                    line.write(chunk, 0, chunk.length);
                    noteIdx++;
                }

                line.drain();
                line.stop();
            } catch (Exception ignored) {
            }
        });
        musicThread.setDaemon(true);
        musicThread.start();
    }

    /**
     * Menghentikan musik latar.
     */
    public static synchronized void stopBGM() {
        runningMusic = false;
        if (musicThread != null) {
            musicThread.interrupt();
            musicThread = null;
        }
    }
}

