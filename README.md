# ★ STAR INVADER 3D: Misi Pemburu Galon Kosmik ★

[![Play Live Game](https://img.shields.io/badge/🎮%20MAIN%20ONLINE-Klik%20Disini-00e5ff?style=for-the-badge&logo=googlechrome&logoColor=white)](https://agasta04.github.io/Game-Space/)
[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Platform](https://img.shields.io/badge/Platform-Web%20%7C%20Windows%20%7C%20Mac%20%7C%20Linux-00c853?style=for-the-badge)](https://agasta04.github.io/Game-Space/)

Game pertempuran luar angkasa 3D modern dengan **perspektif kamera 3D di belakang pesawat (*Chase View*)**, model pesawat tempur *polyhedral* 3D presisi, kontrol akselerasi maju-mundur alami, dan misi utama mengumpulkan **Galon Air Kosmik 💧** di 5 sektor galaksi!

Tersedia dalam **2 versi**:
1. **🌐 Versi Web (HTML5/Canvas)**: Langsung dimainkan di browser tanpa instalasi apa pun via [**GitHub Pages**](https://agasta04.github.io/Game-Space/).
2. **💻 Versi Desktop (JavaFX)**: Aplikasi desktop dengan performa tinggi dan synthesizer audio prosedural.

---

## 🌟 Fitur Utama

- **🚀 Model Pesawat Tempur 3D Nyata (*True 3D Polyhedral Mesh*):**
  - Sayap *inverted-gull* 3D aerodinamis dengan rel meriam laser di ujung kedua sayap.
  - Tiga corong mesin knalpot silinder 3D (*Triple Plasma Thrusters*) dengan lidah petir biru dan api plasma menyala.
  - Kubah kanopi kokpit kaca cyan berefleksi specular.
  - Matriks rotasi 3D penuh: *Banking Roll* saat belok kiri/kanan (`A/D`) dan *Pitch* menukik saat akselerasi maju/rem (`W/S`).
- **🌌 Latar Belakang Luar Angkasa Deep Space:**
  - Ruang hampa kosmik dengan kabut nebula dinamis dan lantai kisi-kisi 3D (*perspective cyber grid*).
  - Bintang *Hyperspace Warp Streaks* saat melompat antar sektor.
- **💧 Mekanik Skor Berbasis Galon Air Kosmik:**
  - **Fokus Utama:** Mengumpulkan Galon Air Mineral Kosmik sebanyak mungkin untuk memuncaki Leaderboard.
  - Tarikan magnet gravitasi menyedot galon yang melintas dekat pesawat.
  - **Galon Emas:** Bernilai bonus +3 Galon (+300 Poin)!
- **🛸 UFO Alien:**
  - Piring terbang alien dengan cincin lampu LED berputar dan kubah kokpit ungu.
  - Anda bebas menembak atau menghindarinya. UFO yang lewat di belakang pesawat **tidak mengurangi nyawa**.
  - **Sistem 3 Nyawa / Shield Cells:** Bertabrakan langsung dengan UFO akan mengurangi 1 nyawa. Tertabrak 3 kali = Game Over.
- **🛰️ 5 Sektor Progresif:**
  - Sektor 1: Orbit Bumi
  - Sektor 2: Sabuk Asteroid
  - Sektor 3: Cyber Nebula
  - Sektor 4: Badai Surya
  - Sektor 5: Inti Alien Boss (Mothership)
- **🎛️ Antarmuka Kokpit Tempur (*Cockpit Canopy HUD*):**
  - Tampilan *floating glassmorphic bar* dengan status pilot, meteran galon beranimasi cairan mengombak, persentase target *Hyperspace Warp*, indikator 3 sel perisai energi, dan speedometer kecepatan *throttle*.
- **🏆 Papan Skor Persisten (Leaderboard):**
  - Menyimpan Top 10 pilot pengumpul galon terbanyak.
- **🎵 Audio Synthesizer Prosedural:**
  - Efek laser, denting galon kristal, ledakan, dan musik synthwave retro tanpa ketergantungan file eksternal.

---

## 🎮 Kontrol Permainan

| Tombol | Fungsi |
|---|---|
| **W** atau **Panah Atas** | **Akselerasi Maju (Boost Throttle)** – Pesawat meluncur lebih cepat ke dalam lintasan $Z$ |
| **S** atau **Panah Bawah** | **Mundur / Mengerem (Brake)** – Pesawat melambat untuk bermanuver taktis |
| **A** atau **Panah Kiri** | **Belok Kiri (Banking Roll 3D)** |
| **D** atau **Panah Kanan** | **Belok Kanan (Banking Roll 3D)** |
| **SPASI (Spacebar)** | **Tembak Laser Dual** ke horizon target |
| **M** | Menyalakan / Mematikan Musik (*Mute/Unmute*) |
| **ESC** | Jeda Permainan (*Pause / Resume*) |

*Catatan: Pada perangkat layar sentuh (Tablet/HP), tombol navigasi virtual otomatis tersedia di layar.*

---

## 🕹️ Cara Menjalankan

### Cara 1: Buka Langsung di Web Browser
Klik link berikut untuk bermain langsung di Google Chrome / Edge / Safari / Firefox:
👉 [**https://agasta04.github.io/Game-Space/**](https://agasta04.github.io/Game-Space/)

*(Atau buka file `index.html` langsung dari komputer Anda).*

### Cara 2: Jalankan Versi Desktop (JavaFX)
1. Pastikan Anda telah menginstal **JDK 17** atau yang lebih baru.
2. Klik dua kali file:
   ```cmd
   jalankan_game.bat
   ```
   Atau jalankan melalui terminal:
   ```bash
   ./mvnw javafx:run
   ```

---

Dibuat dengan ❤️ untuk pengalaman game 3D luar angkasa terbaik!
