package com.kintil.overlaymod.client.gui;

import com.kintil.overlaymod.client.OverlayRenderer;
import com.kintil.overlaymod.client.OverlayTextureManager;
import com.kintil.overlaymod.config.OverlayConfig;
import com.kintil.overlaymod.config.OverlayEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.File;
import java.util.ArrayList;

/**
 * GUI utama Overlay Mod — dibuka dengan tombol backtick (`).
 */
public class OverlayScreen extends Screen {

    // Warna
    private static final int BG_COLOR    = 0xCC1A1A2E;
    private static final int ACCENT_COLOR= 0xFF0F3460;
    private static final int HIGHLIGHT   = 0xFFE94560;
    private static final int TEXT_WHITE  = 0xFFFFFFFF;
    private static final int TEXT_GRAY   = 0xFFAAAAAA;
    private static final int TEXT_GREEN  = 0xFF44FF88;
    private static final int TEXT_RED    = 0xFFFF5555;
    private static final int TEXT_YELLOW = 0xFFFFDD44;

    // Panel
    private int panelX, panelY, panelW, panelH;

    // List scroll
    private int scrollOffset = 0;
    private static final int ITEM_HEIGHT = 56;
    private static final int LIST_MARGIN_TOP = 72;
    private static final int LIST_MARGIN_BOT = 95;
    private int listAreaH;
    private int maxScroll;

    // Widgets
    private TextFieldWidget pathInput;
    private ButtonWidget addButton;
    private ButtonWidget rotButton;
    private ButtonWidget closeButton;

    private final java.util.List<ButtonWidget> enableBtns  = new ArrayList<>();
    private final java.util.List<ButtonWidget> removeBtns  = new ArrayList<>();
    private final java.util.List<OpacitySlider> opSliders  = new ArrayList<>();

    // Pesan status
    private String statusMsg  = null;
    private boolean statusOk  = true;
    private int statusTimer   = 0;

    private OverlayConfig config;

    public OverlayScreen() {
        super(Text.literal("Overlay Mod"));
        this.config = OverlayRenderer.getConfig();
        if (this.config == null) {
            this.config = new OverlayConfig();
            OverlayRenderer.setConfig(this.config);
        }
    }

    @Override
    protected void init() {
        panelW = Math.min(580, (int)(width * 0.87f));
        panelH = Math.min(480, (int)(height * 0.87f));
        panelX = (width  - panelW) / 2;
        panelY = (height - panelH) / 2;
        listAreaH = panelH - LIST_MARGIN_TOP - LIST_MARGIN_BOT;
        buildWidgets();
    }

    // ── Widget builder ─────────────────────────────────────────────────────────

    private void buildWidgets() {
        clearChildren();
        enableBtns.clear();
        removeBtns.clear();
        opSliders.clear();

        // ── Input path ─────────────────────────────────────────────────────────
        int inputY = panelY + panelH - 78;
        int inputW = panelW - 110;
        pathInput = new TextFieldWidget(textRenderer,
                panelX + 10, inputY, inputW, 20,
                Text.literal(""));
        pathInput.setMaxLength(512);
        pathInput.setPlaceholder(Text.literal("Contoh: C:/gambar/overlay.png atau /home/user/frame.png"));
        addSelectableChild(pathInput);

        // ── Tombol Tambah ──────────────────────────────────────────────────────
        addButton = ButtonWidget.builder(Text.literal("+ Tambah"), btn -> doAddOverlay())
                .dimensions(panelX + inputW + 18, inputY, 70, 20)
                .build();
        addDrawableChild(addButton);

        // ── Rule of Thirds ─────────────────────────────────────────────────────
        rotButton = ButtonWidget.builder(
                rotText(),
                btn -> {
                    config.showRuleOfThirds = !config.showRuleOfThirds;
                    config.save();
                    btn.setMessage(rotText());
                }
        ).dimensions(panelX + 10, panelY + panelH - 48, 180, 20).build();
        addDrawableChild(rotButton);

        // ── Tutup ──────────────────────────────────────────────────────────────
        closeButton = ButtonWidget.builder(Text.literal("✕ Tutup"), btn -> close())
                .dimensions(panelX + panelW - 85, panelY + panelH - 48, 75, 20)
                .build();
        addDrawableChild(closeButton);

        rebuildItemWidgets();
    }

    private void doAddOverlay() {
        String path = pathInput.getText().trim();
        if (path.isEmpty()) {
            showStatus("Masukkan path file gambar!", false);
            return;
        }

        // Cek file ada
        File f = new File(path);
        if (!f.exists() || !f.isFile()) {
            showStatus("File tidak ditemukan: " + shortenPath(path, 40), false);
            return;
        }

        // Cek ekstensi
        String lower = path.toLowerCase();
        if (!lower.endsWith(".png") && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg")) {
            showStatus("Format tidak didukung! Gunakan PNG atau JPG.", false);
            return;
        }

        // Tambah ke list (texture akan di-load saat render pertama)
        OverlayEntry entry = new OverlayEntry(path, 0.5f);
        config.overlays.add(entry);
        config.save();
        pathInput.setText("");
        showStatus("Overlay '" + entry.name + "' berhasil ditambah!", true);
        rebuildItemWidgets();
    }

    private void rebuildItemWidgets() {
        enableBtns.forEach(this::remove);
        removeBtns.forEach(this::remove);
        opSliders.forEach(this::remove);
        enableBtns.clear();
        removeBtns.clear();
        opSliders.clear();

        int total  = config.overlays.size();
        int totalH = total * ITEM_HEIGHT;
        maxScroll  = Math.max(0, totalH - listAreaH);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        int listTop = panelY + LIST_MARGIN_TOP;

        for (int i = 0; i < total; i++) {
            final int idx = i;
            OverlayEntry e = config.overlays.get(i);
            int iy = listTop + i * ITEM_HEIGHT - scrollOffset;
            boolean vis = iy + ITEM_HEIGHT > listTop && iy < listTop + listAreaH;

            // Toggle enable
            ButtonWidget enBtn = ButtonWidget.builder(
                    e.enabled ? Text.literal("👁 ON") : Text.literal("▌ OFF"),
                    btn -> {
                        config.overlays.get(idx).enabled = !config.overlays.get(idx).enabled;
                        btn.setMessage(config.overlays.get(idx).enabled
                                ? Text.literal("👁 ON") : Text.literal("▌ OFF"));
                        config.save();
                    }
            ).dimensions(panelX + panelW - 140, iy + 4, 60, 18).build();
            enBtn.visible = vis;
            addDrawableChild(enBtn);
            enableBtns.add(enBtn);

            // Hapus
            ButtonWidget rmBtn = ButtonWidget.builder(
                    Text.literal("✕"),
                    btn -> {
                        OverlayTextureManager.unloadTexture(config.overlays.get(idx).path);
                        config.overlays.remove(idx);
                        config.save();
                        rebuildItemWidgets();
                    }
            ).dimensions(panelX + panelW - 75, iy + 4, 24, 18).build();
            rmBtn.visible = vis;
            addDrawableChild(rmBtn);
            removeBtns.add(rmBtn);

            // Opacity slider
            OpacitySlider sl = new OpacitySlider(
                    panelX + 10, iy + 34,
                    panelW - 90, 14,
                    e.opacity,
                    val -> {
                        config.overlays.get(idx).opacity = val;
                        config.save();
                    }
            );
            sl.visible = vis;
            addDrawableChild(sl);
            opSliders.add(sl);
        }
    }

    // ── Render ─────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);

        // Border + panel
        ctx.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0xFF0F3460);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_COLOR);

        // Header bar
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 38, ACCENT_COLOR);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("✦ Overlay Mod ✦"), panelX + panelW / 2, panelY + 8, HIGHLIGHT);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Tekan ` untuk toggle  |  Scroll untuk navigasi daftar"),
                panelX + panelW / 2, panelY + 22, TEXT_GRAY);

        // Sub-header
        ctx.drawTextWithShadow(textRenderer, Text.literal("DAFTAR OVERLAY"),
                panelX + 10, panelY + 44, HIGHLIGHT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Enable"),
                panelX + panelW - 140, panelY + 44, TEXT_GRAY);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Hapus"),
                panelX + panelW - 75, panelY + 44, TEXT_GRAY);

        // List area background
        int listTop = panelY + LIST_MARGIN_TOP;
        int listBot = listTop + listAreaH;
        ctx.fill(panelX + 4, listTop, panelX + panelW - 4, listBot, 0x44000000);

        // Render items
        updateItemPositions(listTop, listBot);

        for (int i = 0; i < config.overlays.size(); i++) {
            OverlayEntry e = config.overlays.get(i);
            int iy = listTop + i * ITEM_HEIGHT - scrollOffset;
            if (iy + ITEM_HEIGHT < listTop || iy > listBot) continue;

            // Row bg
            ctx.fill(panelX + 5, iy + 1, panelX + panelW - 5, iy + ITEM_HEIGHT - 1,
                    i % 2 == 0 ? 0x22FFFFFF : 0x11FFFFFF);

            // Nama file
            String name = e.name.isEmpty() ? e.path : e.name;
            if (name.length() > 48) name = "…" + name.substring(name.length() - 46);
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal((e.enabled ? "▶ " : "▌ ") + name),
                    panelX + 10, iy + 6,
                    e.enabled ? TEXT_WHITE : TEXT_GRAY);

            // Status texture
            boolean loaded = OverlayTextureManager.isLoaded(e.path);
            boolean exists = new File(e.path).exists();
            String statusStr = loaded ? "✔ Loaded" : (exists ? "⏳ Belum di-load" : "✘ File tidak ada");
            int statusColor  = loaded ? TEXT_GREEN : (exists ? TEXT_YELLOW : TEXT_RED);
            ctx.drawTextWithShadow(textRenderer, Text.literal(statusStr),
                    panelX + 10, iy + 18, statusColor);

            // Opacity label
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal("Opasitas: " + (int)(e.opacity * 100) + "%"),
                    panelX + 10, iy + 32, TEXT_GRAY);
        }

        // Kosong
        if (config.overlays.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Belum ada overlay. Ketik path gambar di bawah lalu klik + Tambah."),
                    panelX + panelW / 2, listTop + listAreaH / 2 - 5, TEXT_GRAY);
        }

        // Scrollbar
        if (maxScroll > 0) {
            int sbX = panelX + panelW - 6;
            int sbH = listAreaH;
            int total = config.overlays.size() * ITEM_HEIGHT;
            int thumbH = Math.max(20, sbH * sbH / total);
            int thumbY = listTop + (int)((long)scrollOffset * (sbH - thumbH) / maxScroll);
            ctx.fill(sbX, listTop, sbX + 4, listBot, 0x33FFFFFF);
            ctx.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xAAFFFFFF);
        }

        // Separator atas input
        ctx.fill(panelX + 8, panelY + panelH - LIST_MARGIN_BOT + 4,
                panelX + panelW - 8, panelY + panelH - LIST_MARGIN_BOT + 5, 0x44FFFFFF);

        // Label input
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("Path gambar (PNG/JPG):"),
                panelX + 10, panelY + panelH - 95, TEXT_GREEN);

        // Status pesan
        if (statusTimer > 0) {
            statusTimer--;
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(statusMsg),
                    panelX + panelW / 2, panelY + panelH - LIST_MARGIN_BOT - 12,
                    statusOk ? TEXT_GREEN : TEXT_RED);
        }

        super.render(ctx, mx, my, delta);
    }

    private void updateItemPositions(int listTop, int listBot) {
        for (int i = 0; i < config.overlays.size(); i++) {
            int iy = listTop + i * ITEM_HEIGHT - scrollOffset;
            boolean vis = iy + ITEM_HEIGHT > listTop && iy < listBot;

            if (i < enableBtns.size()) {
                enableBtns.get(i).setY(iy + 4);
                enableBtns.get(i).visible = vis;
            }
            if (i < removeBtns.size()) {
                removeBtns.get(i).setY(iy + 4);
                removeBtns.get(i).visible = vis;
            }
            if (i < opSliders.size()) {
                opSliders.get(i).setY(iy + 34);
                opSliders.get(i).visible = vis;
            }
        }
    }

    // ── Input events ───────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int listTop = panelY + LIST_MARGIN_TOP;
        int listBot = listTop + listAreaH;
        if (mx >= panelX && mx <= panelX + panelW && my >= listTop && my <= listBot) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(v * 14)));
            return true;
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 && pathInput != null && pathInput.isFocused()) { // ENTER
            doAddOverlay();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (config != null) config.save();
        super.close();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void showStatus(String msg, boolean ok) {
        statusMsg   = msg;
        statusOk    = ok;
        statusTimer = 100;
    }

    private Text rotText() {
        return config.showRuleOfThirds
                ? Text.literal("⊞ Rule of Thirds: ON")
                : Text.literal("⊟ Rule of Thirds: OFF");
    }

    private static String shortenPath(String p, int max) {
        if (p.length() <= max) return p;
        return "…" + p.substring(p.length() - (max - 1));
    }

    // ── Opacity Slider ─────────────────────────────────────────────────────────

    public static class OpacitySlider extends SliderWidget {
        private final java.util.function.Consumer<Float> onChange;

        public OpacitySlider(int x, int y, int w, int h, float init,
                             java.util.function.Consumer<Float> onChange) {
            super(x, y, w, h, Text.empty(), init);
            this.onChange = onChange;
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Text.literal((int)(value * 100) + "%"));
        }

        @Override protected void applyValue() {
            if (onChange != null) onChange.accept((float) value);
        }
    }
}
