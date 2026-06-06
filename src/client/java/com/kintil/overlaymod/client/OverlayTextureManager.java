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
 * Menggunakan NativeImageBackedTexture agar bisa render via DrawContext.
 */
public class OverlayTextureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("overlaymod");

    // path -> Identifier texture yang sudah terdaftar
    private static final Map<String, Identifier> loadedTextures = new HashMap<>();
    // path -> ukuran gambar asli [width, height]
    private static final Map<String, int[]> textureSizes = new HashMap<>();
    // path -> texture object agar bisa di-close
    private static final Map<String, NativeImageBackedTexture> textureObjects = new HashMap<>();

    private static int textureCounter = 0;

    /**
     * Load gambar dari path file dan daftarkan ke TextureManager.
     * @return Identifier texture, atau null jika gagal.
     */
    public static Identifier loadFromPath(String path) {
        if (path == null || path.isBlank()) return null;

        // Jika sudah pernah di-load, kembalikan identifier yang ada
        if (loadedTextures.containsKey(path)) {
            return loadedTextures.get(path);
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            LOGGER.warn("[OverlayMod] File tidak ditemukan: {}", path);
            return null;
        }

        try (InputStream is = new FileInputStream(file)) {
            NativeImage image = NativeImage.read(is);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);

            // Buat identifier unik untuk texture ini
            Identifier id = Identifier.of("overlaymod", "dynamic_overlay_" + textureCounter++);

            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
            });

            loadedTextures.put(path, id);
            textureSizes.put(path, new int[]{image.getWidth(), image.getHeight()});
            textureObjects.put(path, texture);

            LOGGER.info("[OverlayMod] Berhasil load texture: {} -> {}", path, id);
            return id;

        } catch (Exception e) {
            LOGGER.error("[OverlayMod] Gagal load gambar dari '{}': {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * Hapus texture yang sudah di-load dari path tertentu.
     */
    public static void unloadTexture(String path) {
        Identifier id = loadedTextures.remove(path);
        textureSizes.remove(path);
        NativeImageBackedTexture tex = textureObjects.remove(path);
        if (id != null) {
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
            });
        }
        if (tex != null) {
            tex.close();
        }
    }

    /**
     * Hapus semua texture yang di-load.
     */
    public static void unloadAll() {
        for (String path : loadedTextures.keySet()) {
            Identifier id = loadedTextures.get(path);
            MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        }
        textureObjects.values().forEach(NativeImageBackedTexture::close);
        loadedTextures.clear();
        textureSizes.clear();
        textureObjects.clear();
    }

    public static Identifier getIdentifier(String path) {
        return loadedTextures.get(path);
    }

    public static int[] getSize(String path) {
        return textureSizes.getOrDefault(path, new int[]{1, 1});
    }
}
