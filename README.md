# 🚀 Space Invader 3D: Misi Pemburu UFO & Galon Kosmik

Game bertema luar angkasa modern berbasis **JavaFX** yang dioptimalkan untuk tampilan **Laptop & Tablet (Widescreen 1050x700)** dengan visual bergaya 3D, animasi dinamis, efek audio prosedural, dan sistem papan skor (*Leaderboard*).

---

## 🌟 Fitur Utama

- **Tampilan Widescreen (1050 × 700):** Didesain khusus agar nyaman dimainkan di laptop, monitor desktop, maupun layar tablet.
- **Visual Luar Angkasa Dinamis:**
  - *Multi-layer Parallax Starfield* dengan bintang-bintang kosmik berkilau.
  - Nebula gas kosmik ungu & cyan serta planet bercincin di latar belakang.
  - *Screen Shake Effect* saat terjadi ledakan dramatis.
- **Animasi Roket Pemain 3D:**
  - *3D Banking Roll:* Sayap dan badan pesawat miring secara perspektif saat bergerak ke kiri atau kanan.
  - *Twin Plasma Thrusters:* Pendorong mesin roket dengan partikel api plasma menyala.
  - *Twin Laser Cannons:* Meriam laser ganda di kedua sayap.
- **Konsep Misi Baru (Pengumpul Galon):**
  - **Fokus Utama:** Mengumpulkan Galon Kosmik 💧 sebanyak mungkin sebagai penentu skor utama di Leaderboard.
  - **UFO Alien:** Anda bebas membunuh atau menghindari armada UFO. UFO yang melayang melewati batas bawah layar **tidak mengurangi nyawa**.
  - **Sistem 3 Nyawa:** Nyawa roket berkurang 1 setiap kali bertabrakan langsung dengan UFO. Tertabrak 3 kali = Game Over.
  - **Galon Emas Langka (Golden):** Bernilai +3 Galon bonus!
  - **Boss UFO Mothership:** Hadir dengan meriam plasma ganda dan bonus +5 Galon saat dikalahkan.
- **Fitur Leaderboard Persisten:**
  - Input nama pilot di menu utama.
  - Menyimpan Top 10 pilot pengumpul galon terbanyak ke file lokal `leaderboard.txt`.
- **Audio Prosedural Tanpa File Eksternal:**
  - Menggunakan synthesizer bawaan Java Sound API (`javax.sound.sampled`).
  - Musik retro synthwave luar angkasa dengan tombol mute/unmute `[M]`.

---

## 🎮 Kontrol Permainan

| Tombol | Fungsi |
|---|---|
| **W, A, S, D** / **Tombol Panah** | Menggerakkan Roket (*3D Banking Roll*) |
| **SPASI (Spacebar)** | Menembakkan Laser Ganda (Bisa juga untuk mengambil galon jarak jauh) |
| **M** | Menyalakan / Mematikan Musik Latar (*Mute/Unmute*) |
| **ESC** | Jeda Permainan (*Pause / Resume*) |

---

## 🛠️ Prasyarat Sistem

- **JDK 17** atau yang lebih baru
- Sistem Operasi: Windows / macOS / Linux

---

## 🚀 Cara Menjalankan Game

### Opsi 1: Paling Mudah (1 Klik)
Cukup **klik 2x (Double-click)** file:
```text
jalankan_game.bat
```

### Opsi 2: Menggunakan Terminal / Command Prompt
Jalankan perintah berikut di direktori proyek:
```bash
./mvnw javafx:run
```
*(Di Windows PowerShell/CMD gunakan: `.\mvnw.cmd javafx:run`)*

---

## 📜 Lisensi
Proyek ini dilisensikan di bawah lisensi terbuka untuk keperluan edukasi dan pengembangan game.
