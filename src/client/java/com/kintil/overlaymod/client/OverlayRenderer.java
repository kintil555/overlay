package com.kintil.overlaymod.client;

import com.kintil.overlaymod.config.OverlayConfig;
import com.kintil.overlaymod.config.OverlayEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class OverlayRenderer {

    private static OverlayConfig config;

    /** True = semua overlay disembunyikan sementara (compare/peek mode) */
    private static boolean hideForCompare = false;

    public static void setConfig(OverlayConfig cfg)     { config = cfg; }
    public static OverlayConfig getConfig()             { return config; }
    public static void setHideForCompare(boolean hide)  { hideForCompare = hide; }
    public static boolean isHiddenForCompare()          { return hideForCompare; }

    public static void render(DrawContext context, float tickDelta) {
        if (config == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        if (hideForCompare) {
            // Tampilkan badge kecil "COMPARE" di pojok kiri atas
            renderCompareBadge(context, mc);
            return;
        }

        for (OverlayEntry entry : config.overlays) {
            if (!entry.enabled) continue;
            if (entry.path == null || entry.path.isBlank()) continue;

            Identifier texId = OverlayTextureManager.getIdentifier(entry.path);
            if (texId == null) {
                texId = OverlayTextureManager.loadFromPath(entry.path);
                if (texId == null) continue;
            }

            renderWithOpacity(context, texId, 0, 0, sw, sh, entry.opacity);
        }

        if (config.showRuleOfThirds) {
            renderRuleOfThirds(context, sw, sh);
        }
    }

    /**
     * Badge kecil di pojok kiri atas saat compare mode aktif.
     * Biar player tahu mereka lagi "peek" bukan bug.
     */
    private static void renderCompareBadge(DrawContext context, MinecraftClient mc) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        // Background badge
        context.fill(4, 4, 110, 18, 0xBB000000);
        context.fill(4, 4, 6, 18, 0xFFE94560);  // garis merah di kiri

        // Teks
        context.drawTextWithShadow(mc.textRenderer,
                net.minecraft.text.Text.literal("◎ COMPARE MODE"),
                10, 7, 0xFFE94560);

        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private static void renderWithOpacity(DrawContext context, Identifier tex,
                                          int x, int y, int w, int h, float opacity) {
        if (opacity <= 0f) return;
        float alpha = Math.min(Math.max(opacity, 0f), 1f);

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        context.drawTexture(tex, x, y, 0, 0, w, h, w, h);

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    public static void renderRuleOfThirds(DrawContext context, int sw, int sh) {
        int lc = 0x88FFFFFF;
        int dc = 0xCCFFCC00;

        int x1 = sw / 3, x2 = sw * 2 / 3;
        int y1 = sh / 3, y2 = sh * 2 / 3;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        context.fill(x1, 0, x1 + 1, sh, lc);
        context.fill(x2, 0, x2 + 1, sh, lc);
        context.fill(0, y1, sw, y1 + 1, lc);
        context.fill(0, y2, sw, y2 + 1, lc);

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
