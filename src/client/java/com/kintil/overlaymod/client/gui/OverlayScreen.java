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

import java.util.ArrayList;

/**
 * GUI utama Overlay Mod.
 * Dibuka dengan tombol backtick (`) saat dalam game.
 *
 * Layout:
 * - Header: judul mod
 * - Daftar overlay (scrollable list): nama, toggle enabled, slider opasitas, tombol hapus
 * - Input field: tambah overlay baru (ketik path)
 * - Tombol: Tambah, Rule of Thirds toggle, Tutup
 */
public class OverlayScreen extends Screen {

    // Warna panel
    private static final int BG_COLOR       = 0xCC1A1A2E;  // biru gelap transparan
    private static final int PANEL_COLOR    = 0xCC16213E;
    private static final int ACCENT_COLOR   = 0xFF0F3460;
    private static final int HIGHLIGHT      = 0xFFE94560;
    private static final int TEXT_WHITE     = 0xFFFFFFFF;
    private static final int TEXT_GRAY      = 0xFFAAAAAA;
    private static final int TEXT_GREEN     = 0xFF44FF88;
    private static final int TEXT_RED       = 0xFFFF4444;

    // Panel geometry
    private int panelX, panelY, panelW, panelH;

    // Komponen scroll list
    private int scrollOffset = 0;
    private static final int ITEM_HEIGHT = 52;
    private static final int LIST_MARGIN_TOP = 70;
    private static final int LIST_MARGIN_BOTTOM = 90;
    private int listHeight;
    private int maxScroll;

    // Komponen input
    private TextFieldWidget pathInput;
    private ButtonWidget addButton;
    private ButtonWidget rotButton;
    private ButtonWidget closeButton;

    // Per-item widgets
    private final java.util.List<ButtonWidget> enableButtons = new ArrayList<>();
    private final java.util.List<ButtonWidget> removeButtons = new ArrayList<>();
    private final java.util.List<OpacitySlider> opacitySliders = new ArrayList<>();

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
        // Hitung ukuran panel (80% layar)
        panelW = Math.min(560, (int)(width * 0.85f));
        panelH = Math.min(460, (int)(height * 0.85f));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        listHeight = panelH - LIST_MARGIN_TOP - LIST_MARGIN_BOTTOM;

        buildWidgets();
    }

    private void buildWidgets() {
        clearChildren();
        enableButtons.clear();
        removeButtons.clear();
        opacitySliders.clear();

        // Input field untuk path gambar baru
        int inputY = panelY + panelH - 80;
        int inputW = panelW - 180;
        pathInput = new TextFieldWidget(textRenderer,
                panelX + 10, inputY, inputW, 20,
                Text.literal("Path gambar..."));
        pathInput.setMaxLength(512);
        pathInput.setPlaceholder(Text.literal("Contoh: C:/images/overlay.png"));
        addSelectableChild(pathInput);

        // Tombol Tambah
        addButton = ButtonWidget.builder(Text.literal("+ Tambah"), btn -> {
            String path = pathInput.getText().trim();
            if (!path.isEmpty()) {
                // Coba load dulu
                var texId = OverlayTextureManager.loadFromPath(path);
                if (texId != null) {
                    config.overlays.add(new OverlayEntry(path, 0.5f));
                    config.save();
                    pathInput.setText("");
                    rebuildList();
                } else {
                    // Tampilkan error - tetap tambah tapi dengan flag gagal
                    // (user bisa cek path-nya)
                    showError("File tidak ditemukan atau format tidak didukung!");
                }
            }
        }).dimensions(panelX + inputW + 20, inputY, 80, 20).build();
        addDrawableChild(addButton);

        // Tombol Rule of Thirds
        updateRotButtonText();
        rotButton = ButtonWidget.builder(
                getRuleOfThirdsText(),
                btn -> {
                    config.showRuleOfThirds = !config.showRuleOfThirds;
                    config.save();
                    btn.setMessage(getRuleOfThirdsText());
                }
        ).dimensions(panelX + 10, panelY + panelH - 50, 160, 20).build();
        addDrawableChild(rotButton);

        // Tombol Tutup
        closeButton = ButtonWidget.builder(Text.literal("✕ Tutup"), btn -> close())
                .dimensions(panelX + panelW - 90, panelY + panelH - 50, 80, 20).build();
        addDrawableChild(closeButton);

        // Rebuild item list
        rebuildList();
    }

    private String errorMsg = null;
    private int errorTimer = 0;

    private void showError(String msg) {
        errorMsg = msg;
        errorTimer = 80; // ~4 detik
    }

    private Text getRuleOfThirdsText() {
        return config.showRuleOfThirds
                ? Text.literal("⊞ Rule of Thirds: ON")
                : Text.literal("⊟ Rule of Thirds: OFF");
    }

    private void updateRotButtonText() {
        if (rotButton != null) {
            rotButton.setMessage(getRuleOfThirdsText());
        }
    }

    private void rebuildList() {
        // Hapus widget lama dari item list
        enableButtons.forEach(this::remove);
        removeButtons.forEach(this::remove);
        opacitySliders.forEach(this::remove);
        enableButtons.clear();
        removeButtons.clear();
        opacitySliders.clear();

        // Recalculate scroll
        int totalItems = config.overlays.size();
        int totalHeight = totalItems * ITEM_HEIGHT;
        maxScroll = Math.max(0, totalHeight - listHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        // Buat widget untuk setiap overlay yang visible
        for (int i = 0; i < config.overlays.size(); i++) {
            final int idx = i;
            OverlayEntry entry = config.overlays.get(i);
            int itemY = panelY + LIST_MARGIN_TOP + (i * ITEM_HEIGHT) - scrollOffset;

            // Cek apakah item ini visible dalam list area
            boolean visible = itemY + ITEM_HEIGHT > panelY + LIST_MARGIN_TOP
                    && itemY < panelY + LIST_MARGIN_TOP + listHeight;

            // Toggle enabled
            ButtonWidget enableBtn = ButtonWidget.builder(
                    entry.enabled ? Text.literal("👁 ON") : Text.literal("👁 OFF"),
                    btn -> {
                        config.overlays.get(idx).enabled = !config.overlays.get(idx).enabled;
                        btn.setMessage(config.overlays.get(idx).enabled
                                ? Text.literal("👁 ON") : Text.literal("👁 OFF"));
                        config.save();
                    }
            ).dimensions(panelX + panelW - 130, itemY + 4, 55, 18).build();
            enableBtn.visible = visible;
            addDrawableChild(enableBtn);
            enableButtons.add(enableBtn);

            // Hapus overlay
            ButtonWidget removeBtn = ButtonWidget.builder(
                    Text.literal("✕"),
                    btn -> {
                        OverlayTextureManager.unloadTexture(config.overlays.get(idx).path);
                        config.overlays.remove(idx);
                        config.save();
                        rebuildList();
                    }
            ).dimensions(panelX + panelW - 70, itemY + 4, 24, 18).build();
            removeBtn.visible = visible;
            addDrawableChild(removeBtn);
            removeButtons.add(removeBtn);

            // Slider opasitas
            OpacitySlider slider = new OpacitySlider(
                    panelX + 10, itemY + 26,
                    panelW - 50, 16,
                    entry.opacity,
                    (val) -> {
                        config.overlays.get(idx).opacity = val;
                        config.save();
                    }
            );
            slider.visible = visible;
            addDrawableChild(slider);
            opacitySliders.add(slider);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render background gelap
        renderBackground(context, mouseX, mouseY, delta);

        // Panel utama
        context.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0xFF0F3460);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_COLOR);

        // Header
        context.fill(panelX, panelY, panelX + panelW, panelY + 36, ACCENT_COLOR);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("✦ Overlay Mod ✦"),
                panelX + panelW / 2, panelY + 8, HIGHLIGHT);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Tekan ` untuk toggle | Scroll untuk navigasi daftar"),
                panelX + panelW / 2, panelY + 22, TEXT_GRAY);

        // Judul kolom
        context.drawTextWithShadow(textRenderer, Text.literal("DAFTAR OVERLAY"), panelX + 10, panelY + 42, HIGHLIGHT);
        context.drawTextWithShadow(textRenderer, Text.literal("Enable"), panelX + panelW - 130, panelY + 42, TEXT_GRAY);
        context.drawTextWithShadow(textRenderer, Text.literal("Hapus"), panelX + panelW - 70, panelY + 42, TEXT_GRAY);

        // Area list - clipping zone
        int listTop = panelY + LIST_MARGIN_TOP;
        int listBottom = listTop + listHeight;

        // Background list
        context.fill(panelX + 4, listTop, panelX + panelW - 4, listBottom, 0x44000000);

        // Render item list (teks dan progress bar opasitas)
        enableScrolling(context, listTop, listBottom, mouseX, mouseY);

        for (int i = 0; i < config.overlays.size(); i++) {
            OverlayEntry entry = config.overlays.get(i);
            int itemY = listTop + (i * ITEM_HEIGHT) - scrollOffset;

            // Skip jika tidak terlihat
            if (itemY + ITEM_HEIGHT < listTop || itemY > listBottom) continue;

            // Background item
            int itemBg = i % 2 == 0 ? 0x22FFFFFF : 0x11FFFFFF;
            context.fill(panelX + 5, itemY + 1, panelX + panelW - 5, itemY + ITEM_HEIGHT - 1, itemBg);

            // Nama overlay
            String displayName = entry.name.isEmpty() ? entry.path : entry.name;
            if (displayName.length() > 45) displayName = "..." + displayName.substring(displayName.length() - 42);
            context.drawTextWithShadow(textRenderer,
                    Text.literal((entry.enabled ? "▶ " : "▌ ") + displayName),
                    panelX + 10, itemY + 6,
                    entry.enabled ? TEXT_WHITE : TEXT_GRAY);

            // Path (kecil)
            String shortPath = entry.path.length() > 50
                    ? entry.path.substring(0, 20) + "..." + entry.path.substring(entry.path.length() - 27)
                    : entry.path;
            context.drawTextWithShadow(textRenderer,
                    Text.literal(shortPath),
                    panelX + 10, itemY + 17, 0xFF666699);

            // Label opasitas di atas slider
            int opacityPct = (int)(entry.opacity * 100);
            context.drawTextWithShadow(textRenderer,
                    Text.literal("Opasitas: " + opacityPct + "%"),
                    panelX + 10, itemY + 30, TEXT_GRAY);
        }

        // Kosong jika tidak ada overlay
        if (config.overlays.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Belum ada overlay. Tambah dengan memasukkan path gambar di bawah."),
                    panelX + panelW / 2, listTop + listHeight / 2 - 5, TEXT_GRAY);
        }

        // Scrollbar (jika perlu)
        if (maxScroll > 0) {
            int sbX = panelX + panelW - 8;
            int sbH = listHeight;
            int thumbH = Math.max(20, sbH * listHeight / (config.overlays.size() * ITEM_HEIGHT));
            int thumbY = listTop + (int)((long)scrollOffset * (sbH - thumbH) / maxScroll);
            context.fill(sbX, listTop, sbX + 4, listBottom, 0x44FFFFFF);
            context.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xAAFFFFFF);
        }

        // Separator sebelum input area
        context.fill(panelX + 10, panelY + panelH - 90, panelX + panelW - 10, panelY + panelH - 89, 0x44FFFFFF);

        // Label input
        context.drawTextWithShadow(textRenderer,
                Text.literal("Path gambar (PNG/JPG):"),
                panelX + 10, panelY + panelH - 100, TEXT_GREEN);

        // Error message
        if (errorTimer > 0) {
            errorTimer--;
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("⚠ " + errorMsg),
                    panelX + panelW / 2, panelY + panelH - 110, TEXT_RED);
        }

        // Render widgets
        super.render(context, mouseX, mouseY, delta);
    }

    private void enableScrolling(DrawContext context, int listTop, int listBottom, int mx, int my) {
        // Update visibility widget berdasarkan scroll
        for (int i = 0; i < config.overlays.size(); i++) {
            int itemY = listTop + (i * ITEM_HEIGHT) - scrollOffset;
            boolean vis = itemY + ITEM_HEIGHT > listTop && itemY < listBottom;

            if (i < enableButtons.size()) {
                enableButtons.get(i).setY(itemY + 4);
                enableButtons.get(i).visible = vis;
            }
            if (i < removeButtons.size()) {
                removeButtons.get(i).setY(itemY + 4);
                removeButtons.get(i).visible = vis;
            }
            if (i < opacitySliders.size()) {
                opacitySliders.get(i).setY(itemY + 30);
                opacitySliders.get(i).visible = vis;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listTop = panelY + LIST_MARGIN_TOP;
        int listBottom = listTop + listHeight;

        if (mouseX >= panelX && mouseX <= panelX + panelW
                && mouseY >= listTop && mouseY <= listBottom) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(verticalAmount * 12)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter di textfield → tambah
        if (keyCode == 257 && pathInput.isFocused()) { // ENTER
            addButton.onPress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false; // GUI terbuka tapi game tetap jalan
    }

    @Override
    public void close() {
        if (config != null) config.save();
        super.close();
    }

    /**
     * Slider opasitas custom.
     */
    public static class OpacitySlider extends SliderWidget {
        private final java.util.function.Consumer<Float> onChange;

        public OpacitySlider(int x, int y, int width, int height, float initialValue,
                             java.util.function.Consumer<Float> onChange) {
            super(x, y, width, height, Text.empty(), initialValue);
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int pct = (int)(value * 100);
            setMessage(Text.literal("Opasitas: " + pct + "%"));
        }

        @Override
        protected void applyValue() {
            if (onChange != null) onChange.accept((float) value);
        }

        public float getOpacity() {
            return (float) value;
        }
    }
}
