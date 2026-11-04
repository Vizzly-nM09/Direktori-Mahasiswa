# Direktori CRUD Mahasiswa

Aplikasi mobile Android untuk manajemen data mahasiswa yang terintegrasi dengan REST API berbasis Node.js dan database MySQL (Podman).

Aplikasi ini mengimplementasikan operasi lengkap CRUD (Create, Read, Update, Delete) menggunakan arsitektur 3 Activity, library Retrofit2, serta koneksi USB via ADB Reverse untuk pengujian langsung pada perangkat fisik Android tanpa bergantung pada konfigurasi IP Wi-Fi.

---

## Daftar Isi
- [Teknologi yang Digunakan](#teknologi-yang-digunakan)
- [Fitur Utama](#fitur-utama)
- [Arsitektur dan Alur Komunikasi](#arsitektur-dan-alur-komunikasi)
- [Prasyarat Sistem](#prasyarat-sistem)
- [Panduan Instalasi dan Menjalankan](#panduan-instalasi-dan-menjalankan)
  - [1. Konfigurasi Database MySQL (Podman)](#1-konfigurasi-database-mysql-podman)
  - [2. Menjalankan Backend Node.js](#2-menjalankan-backend-nodejs)
  - [3. Menghubungkan Perangkat Fisik Android (USB)](#3-menghubungkan-perangkat-fisik-android-usb)
  - [4. Menjalankan Aplikasi dari Android Studio](#4-menjalankan-aplikasi-dari-android-studio)
- [Spesifikasi REST API](#spesifikasi-rest-api)
- [Struktur Direktori Proyek](#struktur-direktori-proyek)

---

## Teknologi yang Digunakan

### Frontend (Android)
- Bahasa: Java
- Minimum SDK: Android 7.0 (API Level 24)
- Target / Compile SDK: Android 14+ (API Level 37)
- HTTP Client: Retrofit 2.9.0
- JSON Parser: Gson Converter 2.9.0
- Logging: OkHttp Logging Interceptor 4.11.0
- Komponen UI: Material Components 3, RecyclerView, CardView, SwipeRefreshLayout

### Backend & Database
- Runtime: Node.js (v20+)
- Framework: Express 4
- Database Driver: `mysql2/promise` (dengan prepared statements)
- Database Engine: MySQL 8 running on Podman container
- Query Management: Beekeeper Studio

---

## Fitur Utama

1. **Daftar Mahasiswa Interaktif (`MainActivity`)**
   - Menampilkan daftar kartu mahasiswa secara scannable.
   - Dilengkapi fitur *pull-to-refresh* menggunakan `SwipeRefreshLayout`.
   - Layout responsif: 1 kolom di layar ponsel (<600dp), 2 kolom di tablet (600-899dp), dan 3 kolom di layar lebar (>=900dp).
   - Tiga status tampilan eksplisit: Loading state (indikator proses), Empty state (pesan jika belum ada data), dan Error state (keterangan kendala jaringan beserta tombol Muat Ulang).

2. **Form Tambah dan Edit Mahasiswa (`AddEditActivity`)**
   - Formulir tunggal yang menangani pembuatan data baru maupun pembaruan data yang sudah ada.
   - Validasi input di sisi klien untuk memastikan tidak ada kolom kosong serta verifikasi format email valid (`Patterns.EMAIL_ADDRESS`).
   - Penanganan error respons dari server (misalnya penolakan saat NIM sudah terdaftar).

3. **Detail Rekaman Mahasiswa (`DetailActivity`)**
   - Halaman dossier yang menampilkan informasi lengkap tiap mahasiswa termasuk ID database.
   - Tombol aksi langsung untuk mengirim email ke mahasiswa melalui intent eksternal (`Intent.ACTION_SENDTO` dengan skema URI `mailto:`).
   - Akses cepat untuk mengubah data atau menghapus rekaman dengan dialog konfirmasi.

---

## Arsitektur dan Alur Komunikasi

```
[ Ponsel Android ]
        │
   (Kabel USB)
        │
  [ adb reverse: port 3000 -> port 3000 ]
        │
        ▼
[ Backend Node.js / Express ] (localhost:3000)
        │
   (TCP Socket)
        │
        ▼
[ MySQL Database di Podman ] (localhost:3306)
```

Dengan menjalankan `adb reverse tcp:3000 tcp:3000`, aplikasi Android dapat menghubungi server backend pada alamat loopback `http://127.0.0.1:3000/`. Pendekatan ini membuat koneksi tetap stabil tanpa perlu mencari alamat IP laptop di jaringan Wi-Fi lokal.

---

## Prasyarat Sistem

Pastikan perangkat kerja Anda memiliki:
1. Android Studio Ladybug atau versi lebih baru.
2. Node.js v18 atau v20 ke atas.
3. Podman (atau Docker) dengan container MySQL aktif.
4. Beekeeper Studio (opsional, untuk inspeksi GUI database).
5. Ponsel Android dengan kabel data USB dan opsi Pengembang (Developer Options) aktif.

---

## Panduan Instalasi dan Menjalankan

### 1. Konfigurasi Database MySQL (Podman)

Pastikan container MySQL di Podman berjalan pada port host `3306`.

Jika ingin membuat database dan tabel secara manual di Beekeeper Studio, jalankan script SQL berikut:

```sql
CREATE DATABASE IF NOT EXISTS your_database;
USE your_database;

CREATE TABLE IF NOT EXISTS mahasiswa (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nim VARCHAR(20) NOT NULL UNIQUE,
    nama VARCHAR(100) NOT NULL,
    jurusan VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL
);
```

*(Catatan: Saat backend dijalankan, fungsi bootstrap pada `backend/server.js` juga akan otomatis membuat database dan tabel jika belum tersedia).*

### 2. Menjalankan Backend Node.js

1. Masuk ke folder backend:
   ```bash
   cd backend
   ```
2. Buat atau sesuaikan file `.env` berdasarkan kredensial Podman MySQL Anda:
   ```env
   DB_HOST=your-ip-address or localhost
   DB_PORT=your db port
   DB_USER=your db user
   DB_PASSWORD=your db password
   DB_NAME=your db name
   PORT=your server port
   ```
3. Pasang pustaka dan jalankan server:
   ```bash
   npm install
   npm start
   ```
   Server akan aktif di `http://127.0.0.1:3000`.

### 3. Menghubungkan Perangkat Fisik Android (USB)

1. Sambungkan ponsel ke laptop menggunakan kabel USB.
2. Pada ponsel, pilih opsi **Transfer files / Android Auto** pada dialog USB (hindari opsi *Charging only*).
3. Di menu Pengembang ponsel (Developer Options), pastikan **USB Debugging** aktif.
4. Di terminal laptop, pastikan ponsel terdeteksi dan aktifkan bridging port 3000:
   ```bash
   adb devices
   adb reverse tcp:3000 tcp:3000
   ```

### 4. Menjalankan Aplikasi dari Android Studio

1. Buka folder proyek ini di Android Studio.
2. Lakukan sinkronisasi Gradle (**Sync Project with Gradle Files**).
3. Pilih perangkat fisik Anda pada daftar perangkat di toolbar atas.
4. Klik tombol **Run ('app')** (atau tekan `Shift + F10`).

---

## Spesifikasi REST API

Base URL server: `http://127.0.0.1:3000/`

| HTTP Method | Endpoint | Fungsi | Payload Request | Status Sukses |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/mahasiswa` | Mendapatkan semua data mahasiswa | Tidak ada | `200 OK` (Array JSON) |
| `GET` | `/mahasiswa/:id` | Mengambil detail 1 mahasiswa | Tidak ada | `200 OK` (Objek JSON) |
| `POST` | `/mahasiswa` | Menambahkan mahasiswa baru | Objek JSON (`nim`, `nama`, `jurusan`, `email`) | `201 Created` |
| `PUT` | `/mahasiswa/:id` | Memperbarui data mahasiswa | Objek JSON (`nim`, `nama`, `jurusan`, `email`) | `200 OK` |
| `DELETE` | `/mahasiswa/:id` | Menghapus data mahasiswa | Tidak ada | `200 OK` |

### Format Contoh Respons JSON

**GET /mahasiswa**
```json
[
  {
    "id": 1,
    "nim": "2024001",
    "nama": "Budi Santoso",
    "jurusan": "Teknik Informatika",
    "email": "budi.santoso@kampus.ac.id"
  }
]
```

**POST /mahasiswa (Gagal / Duplikat NIM)**
```json
{
  "error": "NIM sudah terdaftar"
}
```

---

## Struktur Direktori Proyek

```text
CRUDmahasiswa/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/crudmahasiswa/
│       │   ├── MainActivity.java
│       │   ├── AddEditActivity.java
│       │   ├── DetailActivity.java
│       │   ├── adapter/
│       │   │   └── MahasiswaAdapter.java
│       │   ├── api/
│       │   │   ├── ApiService.java
│       │   │   └── RetrofitClient.java
│       │   └── model/
│       │       └── Mahasiswa.java
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_add_edit.xml
│           │   ├── activity_detail.xml
│           │   └── item_mahasiswa.xml
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── values-night/
│               ├── colors.xml
│               └── themes.xml
├── backend/
│   ├── package.json
│   ├── server.js
│   ├── .env.example
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

