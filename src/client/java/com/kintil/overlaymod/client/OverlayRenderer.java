package com.kintil.overlaymod.client;

import com.kintil.overlaymod.config.OverlayConfig;
import com.kintil.overlaymod.config.OverlayEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Render semua overlay aktif + rule of thirds ke layar.
 * Dipanggil setiap frame via HudRenderCallback.
 */
public class OverlayRenderer {

    private static OverlayConfig config;

    public static void setConfig(OverlayConfig cfg) { config = cfg; }
    public static OverlayConfig getConfig()          { return config; }

    public static void render(DrawContext context, float tickDelta) {
        if (config == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        // Jangan render di screen selain in-game (mis. inventory, pause) — opsional,
        // bisa dikomentari jika mau tetap tampil di semua screen
        // if (mc.currentScreen != null && !(mc.currentScreen instanceof OverlayScreen)) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        for (OverlayEntry entry : config.overlays) {
            if (!entry.enabled) continue;
            if (entry.path == null || entry.path.isBlank()) continue;

            // Lazy-load: load texture kalau belum ada (kita sudah di render thread sini)
            Identifier texId = OverlayTextureManager.getIdentifier(entry.path);
            if (texId == null) {
                texId = OverlayTextureManager.loadFromPath(entry.path);
                if (texId == null) continue; // file tidak ada / gagal
            }

            renderWithOpacity(context, texId, 0, 0, sw, sh, entry.opacity);
        }

        if (config.showRuleOfThirds) {
            renderRuleOfThirds(context, sw, sh);
        }
    }

    private static void renderWithOpacity(DrawContext context, Identifier tex,
                                          int x, int y, int w, int h, float opacity) {
        if (opacity <= 0f) return;
        float alpha = Math.min(Math.max(opacity, 0f), 1f);

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        // drawTexture(id, x, y, u, v, regionW, regionH, texW, texH)
        context.drawTexture(tex, x, y, 0, 0, w, h, w, h);

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    public static void renderRuleOfThirds(DrawContext context, int sw, int sh) {
        int lc = 0x88FFFFFF; // putih semi-transparan
        int dc = 0xCCFFCC00; // kuning untuk titik intersection

        int x1 = sw / 3, x2 = sw * 2 / 3;
        int y1 = sh / 3, y2 = sh * 2 / 3;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        // 2 garis vertikal
        context.fill(x1, 0, x1 + 1, sh, lc);
        context.fill(x2, 0, x2 + 1, sh, lc);
        // 2 garis horizontal
        context.fill(0, y1, sw, y1 + 1, lc);
        context.fill(0, y2, sw, y2 + 1, lc);

        // 4 titik intersection
        dot(context, x1, y1, 6, dc);
        dot(context, x2, y1, 6, dc);
        dot(context, x1, y2, 6, dc);
        dot(context, x2, y2, 6, dc);

        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private static void dot(DrawContext ctx, int cx, int cy, int size, int color) {
        int h = size / 2;
        ctx.fill(cx - h, cy - h, cx + h, cy + h, color);
    }
}
