package com.kintil.overlaymod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Menyimpan dan memuat konfigurasi overlay (daftar overlay + opasitas).
 */
public class OverlayConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("overlaymod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("overlaymod_config.json");

    public List<OverlayEntry> overlays = new ArrayList<>();
    public boolean showRuleOfThirds = false;
    public boolean ruleOfThirdsVisible = false; // ditampilkan saat GUI terbuka atau selalu

    public static OverlayConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new OverlayConfig();
        }
        try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
            OverlayConfig cfg = GSON.fromJson(reader, OverlayConfig.class);
            if (cfg == null) return new OverlayConfig();
            if (cfg.overlays == null) cfg.overlays = new ArrayList<>();
            return cfg;
        } catch (Exception e) {
            LOGGER.error("[OverlayMod] Gagal membaca config: {}", e.getMessage());
            return new OverlayConfig();
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            LOGGER.error("[OverlayMod] Gagal menyimpan config: {}", e.getMessage());
        }
    }
}
