package com.kintil.overlaymod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Mengelola loading gambar eksternal (dari path file) ke TextureManager Minecraft.
 * Semua operasi registerTexture HARUS di render thread — gunakan isOnRenderThread() check.
 */
public class OverlayTextureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("overlaymod");

    private static final Map<String, Identifier> loadedTextures = new HashMap<>();
    private static final Map<String, int[]> textureSizes = new HashMap<>();
    private static final Map<String, NativeImageBackedTexture> textureObjects = new HashMap<>();

    private static int textureCounter = 0;

    /**
     * Cek apakah file ada dan bisa dibaca.
     */
    public static boolean fileExists(String path) {
        if (path == null || path.isBlank()) return false;
        File f = new File(path);
        return f.exists() && f.isFile() && f.canRead();
    }

    /**
     * Load gambar dari path file. HARUS dipanggil dari render thread.
     * Kalau belum di render thread, gunakan scheduleLoad().
     * @return Identifier texture, atau null jika gagal.
     */
    public static Identifier loadFromPath(String path) {
        if (path == null || path.isBlank()) return null;

        if (loadedTextures.containsKey(path)) {
            return loadedTextures.get(path);
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            LOGGER.warn("[OverlayMod] File tidak ditemukan: {}", path);
            return null;
        }

        try {
            // Baca NativeImage — jangan pakai try-with-resources karena
            // NativeImageBackedTexture mengambil ownership dari image
            InputStream is = new FileInputStream(file);
            NativeImage image = NativeImage.read(is);
            is.close();

            int w = image.getWidth();
            int h = image.getHeight();

            // NativeImageBackedTexture mengambil ownership NativeImage
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);

            Identifier id = Identifier.of("overlaymod", "overlay_" + textureCounter++);

            // Register langsung — harus di render thread
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.isOnThread()) {
                mc.getTextureManager().registerTexture(id, texture);
            } else {
                // Jadwalkan ke render thread
                mc.execute(() -> mc.getTextureManager().registerTexture(id, texture));
            }

            loadedTextures.put(path, id);
            textureSizes.put(path, new int[]{w, h});
            textureObjects.put(path, texture);

            LOGGER.info("[OverlayMod] Load texture OK: {} ({}x{})", path, w, h);
            return id;

        } catch (Exception e) {
            LOGGER.error("[OverlayMod] Gagal load '{}': {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * Hapus texture dari TextureManager.
     */
    public static void unloadTexture(String path) {
        Identifier id = loadedTextures.remove(path);
        textureSizes.remove(path);
        textureObjects.remove(path);
        if (id != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.isOnThread()) {
                mc.getTextureManager().destroyTexture(id);
            } else {
                mc.execute(() -> mc.getTextureManager().destroyTexture(id));
            }
        }
    }

    public static void unloadAll() {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (Identifier id : loadedTextures.values()) {
            mc.execute(() -> mc.getTextureManager().destroyTexture(id));
        }
        loadedTextures.clear();
        textureSizes.clear();
        textureObjects.clear();
    }

    public static Identifier getIdentifier(String path) {
        return loadedTextures.get(path);
    }

    public static boolean isLoaded(String path) {
        return loadedTextures.containsKey(path);
    }

    public static int[] getSize(String path) {
        return textureSizes.getOrDefault(path, new int[]{1, 1});
    }
}
