package com.kintil.overlaymod.client;

import com.kintil.overlaymod.client.gui.OverlayScreen;
import com.kintil.overlaymod.config.OverlayConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlayModClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("overlaymod");

    private static KeyBinding openGuiKey;
    private static KeyBinding compareKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[OverlayMod] Initializing...");

        // Load config
        OverlayConfig config = OverlayConfig.load();
        OverlayRenderer.setConfig(config);

        // Keybinding: ` untuk buka GUI
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.overlaymod.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "key.category.overlaymod"
        ));

        // Keybinding: Z untuk compare/peek (tahan = overlay hilang, lepas = muncul lagi)
        compareKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.overlaymod.compare",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "key.category.overlaymod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Toggle GUI
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen instanceof OverlayScreen) {
                    client.setScreen(null);
                } else if (client.currentScreen == null) {
                    client.setScreen(new OverlayScreen());
                }
            }

            // Compare / peek mode — cek apakah tombol sedang DITAHAN
            // Pakai InputUtil.isKeyPressed agar responsif setiap frame, bukan hanya saat pressed event
            long windowHandle = client.getWindow().getHandle();
            int compareGlfw   = InputUtil.fromTranslationKey(compareKey.getTranslationKey()) == null
                                 ? GLFW.GLFW_KEY_Z
                                 : compareKey.getDefaultKey().getCode();

            boolean holding = InputUtil.isKeyPressed(windowHandle, compareGlfw);

            // Update flag di renderer
            // Jangan aktifkan compare kalau GUI overlay sedang terbuka
            if (client.currentScreen instanceof OverlayScreen) {
                OverlayRenderer.setHideForCompare(false);
            } else {
                OverlayRenderer.setHideForCompare(holding);
            }
        });

        // HUD render tiap frame
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) ->
                OverlayRenderer.render(drawContext, renderTickCounter.getTickDelta(false)));

        LOGGER.info("[OverlayMod] Ready! ` = buka GUI, tahan Z = compare mode");
    }
}
