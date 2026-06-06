package com.kintil.overlaymod.config;

/**
 * Merepresentasikan satu entri overlay gambar.
 */
public class OverlayEntry {
    public String path;     // path absolut atau relatif ke file gambar
    public float opacity;   // 0.0 - 1.0
    public boolean enabled;
    public String name;     // nama display (opsional, default dari file name)

    public OverlayEntry() {
        this.path = "";
        this.opacity = 0.5f;
        this.enabled = true;
        this.name = "";
    }

    public OverlayEntry(String path, float opacity) {
        this.path = path;
        this.opacity = opacity;
        this.enabled = true;
        // nama = bagian terakhir path
        this.name = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1)
                    : (path.contains("\\") ? path.substring(path.lastIndexOf('\\') + 1) : path);
    }
}
