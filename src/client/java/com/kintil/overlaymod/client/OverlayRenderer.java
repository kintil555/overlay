package com.kintil.overlaymod.client;

import com.kintil.overlaymod.config.OverlayConfig;
import com.kintil.overlaymod.config.OverlayEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Merender semua overlay aktif + rule of thirds ke layar game.
 * Dipanggil via HudRenderCallback setiap frame.
 */
public class OverlayRenderer {

    private static OverlayConfig config;

    public static void setConfig(OverlayConfig cfg) {
        config = cfg;
    }

    public static OverlayConfig getConfig() {
        return config;
    }

    /**
     * Render semua overlay yang aktif.
     */
    public static void render(DrawContext context, float tickDelta) {
        if (config == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        // Render setiap overlay
        for (OverlayEntry entry : config.overlays) {
            if (!entry.enabled || entry.path == null || entry.path.isBlank()) continue;

            Identifier texId = OverlayTextureManager.getIdentifier(entry.path);
            if (texId == null) {
                // Coba load jika belum
                texId = OverlayTextureManager.loadFromPath(entry.path);
                if (texId == null) continue;
            }

            renderTextureWithOpacity(context, texId, 0, 0, screenW, screenH, entry.opacity);
        }

        // Render rule of thirds
        if (config.showRuleOfThirds) {
            renderRuleOfThirds(context, screenW, screenH);
        }
    }

    /**
     * Render texture dengan opasitas tertentu menggunakan RenderSystem.
     * Menggunakan drawTexture yang mendukung alpha color.
     */
    private static void renderTextureWithOpacity(DrawContext context, Identifier texture,
                                                  int x, int y, int width, int height, float opacity) {
        if (opacity <= 0f) return;

        int alpha = (int) (Math.min(Math.max(opacity, 0f), 1f) * 255);
        // ARGB: alpha di bits 24-31
        int color = (alpha << 24) | 0x00FFFFFF;

        // drawTexture(Identifier, x, y, u, v, width, height, texWidth, texHeight)
        // Gunakan color-tinted version via matrices + RenderSystem color
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, opacity);

        // drawTexture: texture, x, y, u, v, width, height, texWidth, texHeight
        context.drawTexture(texture, x, y, 0, 0, width, height, width, height);

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    /**
     * Render garis Rule of Thirds (2 horizontal + 2 vertical) di atas layar.
     */
    public static void renderRuleOfThirds(DrawContext context, int screenW, int screenH) {
        // Warna: putih semi-transparan
        int lineColor = 0x88FFFFFF; // ARGB
        int lineThickness = 1;

        int x1 = screenW / 3;
        int x2 = (screenW * 2) / 3;
        int y1 = screenH / 3;
        int y2 = (screenH * 2) / 3;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        // Garis vertikal kiri
        context.fill(x1, 0, x1 + lineThickness, screenH, lineColor);
        // Garis vertikal kanan
        context.fill(x2, 0, x2 + lineThickness, screenH, lineColor);
        // Garis horizontal atas
        context.fill(0, y1, screenW, y1 + lineThickness, lineColor);
        // Garis horizontal bawah
        context.fill(0, y2, screenW, y2 + lineThickness, lineColor);

        // Titik-titik intersection (rule of thirds points)
        int dotSize = 6;
        int dotColor = 0xCCFFCC00; // kuning semi-transparan
        renderDot(context, x1, y1, dotSize, dotColor);
        renderDot(context, x2, y1, dotSize, dotColor);
        renderDot(context, x1, y2, dotSize, dotColor);
        renderDot(context, x2, y2, dotSize, dotColor);

        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private static void renderDot(DrawContext context, int cx, int cy, int size, int color) {
        int half = size / 2;
        context.fill(cx - half, cy - half, cx + half, cy + half, color);
    }
}
