# 🎨 Overlay Mod - Fabric 1.21.1

Mod Minecraft Fabric yang memungkinkan kamu menambahkan **gambar overlay** ke layar game, lengkap dengan:
- 🖼️ **Multiple overlay gambar** (PNG/JPG dari path lokal)
- 🔆 **Kontrol opasitas** per overlay (0–100%)
- 📐 **Rule of Thirds** visual guides
- 🎛️ **GUI custom** yang intuitif

Cocok untuk membuat **thumbnail**, **compositing scene**, atau sekedar nge-pas frame foto.

---

## 🎮 Cara Pakai

1. Tekan tombol **`` ` ``** (backtick/grave) saat dalam game
2. GUI Overlay Mod akan terbuka
3. Masukkan **path file gambar** di kotak input bawah, lalu tekan **+ Tambah** atau Enter
4. Atur **opasitas** dengan slider, toggle **enable/disable** per overlay
5. Aktifkan **Rule of Thirds** untuk panduan komposisi
6. Tekan **✕ Tutup** atau `` ` `` lagi untuk menutup GUI

### Contoh Path

```
C:/Users/kamu/Pictures/overlay.png
/home/kamu/Pictures/frame.png
./overlays/thumbnail_frame.jpg
```

---

## ✨ Fitur

| Fitur | Keterangan |
|-------|------------|
| Multi-overlay | Bisa tambah banyak overlay sekaligus |
| Opasitas | Slider 0-100% per overlay |
| Toggle | Enable/disable tiap overlay tanpa hapus |
| Rule of Thirds | 2 garis horizontal + 2 vertikal + titik intersection |
| Persistent config | Tersimpan otomatis ke `config/overlaymod_config.json` |
| GUI non-pause | Game tetap jalan saat GUI overlay terbuka |

---

## 📦 Install

1. Install **Fabric Loader** untuk Minecraft 1.21.1
2. Install **Fabric API**
3. Taruh file `.jar` mod ini ke folder `mods/`
4. Jalankan game

---

## 🏗️ Build

```bash
./gradlew build
```

File JAR akan ada di `build/libs/overlay-mod-*.jar`

---

## 🔧 Konfigurasi Manual

Config disimpan di: `<minecraft>/config/overlaymod_config.json`

```json
{
  "overlays": [
    {
      "path": "C:/gambar/overlay.png",
      "opacity": 0.5,
      "enabled": true,
      "name": "overlay.png"
    }
  ],
  "showRuleOfThirds": false
}
```

---

## 📋 Persyaratan

- Minecraft 1.21.1
- Fabric Loader ≥ 0.15.0
- Fabric API
- Java 21
