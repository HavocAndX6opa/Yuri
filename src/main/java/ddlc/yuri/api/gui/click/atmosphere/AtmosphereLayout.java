package ddlc.yuri.api.gui.click.atmosphere;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.utils.render.FontUtils;

public final class AtmosphereLayout {

    public static final float BASE_PANEL_WIDTH = 154.0F;
    public static final float BASE_HEADER_HEIGHT = 20.0F;
    public static final float BASE_ROW_HEIGHT = 18.0F;
    public static final int BASE_FONT_SIZE = 20;
    public static final float BASE_GAP = 2.0F;

    private static final float MIN_SCALE = 0.1F;
    private static final float WIDTH_MARGIN = 0.96F;
    private static final float SCALE_STEPS = 64.0F;

    public final float scale;
    public final float panelWidth;
    public final float headerHeight;
    public final float rowHeight;
    public final float fontSize;
    public final float gap;
    public final float padX;
    public final float indent;
    public final float groupGap;
    public final float trackRoom;
    public final float indicatorWidth;
    public final float indicatorHeight;
    public final float indicatorInset;
    public final float railWidth;
    public final float washWidth;
    public final float headerLineHeight;

    public AtmosphereLayout(float scale) {
        this.scale = scale;

        this.panelWidth = BASE_PANEL_WIDTH * scale;
        this.headerHeight = BASE_HEADER_HEIGHT * scale;
        this.rowHeight = BASE_ROW_HEIGHT * scale;
        this.fontSize = BASE_FONT_SIZE * scale;
        this.gap = BASE_GAP * scale;

        this.padX = 7.0F * scale;
        this.indent = 0.0F * scale;
        this.groupGap = 1.0F * scale;
        this.trackRoom = 4.0F * scale;

        this.indicatorWidth = 7.0F * scale;
        this.indicatorHeight = 3.0F * scale;
        this.indicatorInset = 7.0F * scale;

        this.railWidth = 2.0F * scale;
        this.washWidth = BASE_PANEL_WIDTH * 2 * scale;
        this.headerLineHeight = 1.0F * scale;
    }

    public CustomFontRenderer font() {
        return FontUtils.getScaledFont("sf", BASE_FONT_SIZE, scale);
    }

    public static float fitScale(float screenWidth, int panelCount) {
        float available = screenWidth * WIDTH_MARGIN;
        float fit = available / stripWidth(panelCount, 1.0F);

        float clamped = Math.max(MIN_SCALE, Math.min(1.0F, fit));
        float stepped = (float) (Math.floor(clamped * SCALE_STEPS) / SCALE_STEPS);

        return stepped < MIN_SCALE ? MIN_SCALE : stepped;
    }

    public static float stripWidth(int panelCount, float scale) {
        return panelCount * BASE_PANEL_WIDTH * scale + Math.max(0, panelCount - 1) * BASE_GAP * scale;
    }
}
