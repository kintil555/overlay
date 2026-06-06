package com.kintil.overlaymod.client;

import com.kintil.overlaymod.client.gui.OverlayScreen;
import com.kintil.overlaymod.config.OverlayConfig;
import com.kintil.overlaymod.config.OverlayEntry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point client-side untuk Overlay Mod.
 * Mendaftarkan:
 * - Keybinding backtick (`) untuk buka/tutup GUI
 * - HUD render callback untuk menampilkan overlay
 * - Load config saat startup
 */
public class OverlayModClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("overlaymod");

    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[OverlayMod] Initializing Overlay Mod...");

        // Load config
        OverlayConfig config = OverlayConfig.load();
        OverlayRenderer.setConfig(config);

        // Pre-load semua texture yang tersimpan di config
        preloadTextures(config);

        // Daftarkan keybinding: backtick `
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.overlaymod.open_gui",           // translation key
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,          // tombol backtick `
                "key.category.overlaymod"            // kategori
        ));

        // Tick event: cek apakah tombol ditekan
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                // Toggle GUI
                if (client.currentScreen instanceof OverlayScreen) {
                    client.setScreen(null);
                } else if (client.currentScreen == null) {
                    client.setScreen(new OverlayScreen());
                }
            }
        });

        // HUD render: tampilkan overlay setiap frame
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            // Hanya render jika tidak di screen lain (atau jika ingin selalu render)
            OverlayRenderer.render(drawContext, renderTickCounter.getTickDelta(false));
        });

        LOGGER.info("[OverlayMod] Overlay Mod initialized! Tekan ` untuk buka GUI.");
    }

    /**
     * Pre-load semua texture dari config saat startup.
     */
    private void preloadTextures(OverlayConfig config) {
        if (config.overlays == null) return;
        for (OverlayEntry entry : config.overlays) {
            if (entry.path != null && !entry.path.isBlank()) {
                // Load dilakukan secara async di render thread
                // OverlayTextureManager akan menangani ini saat render pertama
                LOGGER.info("[OverlayMod] Akan memuat texture: {}", entry.path);
            }
        }
    }
}
