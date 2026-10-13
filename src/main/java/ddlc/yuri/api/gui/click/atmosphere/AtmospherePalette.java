package ddlc.yuri.api.gui.click.atmosphere;

import java.awt.*;

public final class AtmospherePalette {

    public static final Color PANEL = new Color(0, 0, 0, 205);
    public static final Color HEADER = new Color(255, 255, 255, 30);
    public static final Color BORDER = new Color(255, 255, 255, 40);
    public static final Color DIVIDER = new Color(255, 255, 255, 30);
    public static final Color ROW_HOVER = new Color(255, 255, 255, 16);
    public static final Color ROW_OPEN = new Color(255, 255, 255, 30);
    public static final Color TRACK = new Color(255, 255, 255, 34);
    public static final Color SCROLLBAR = new Color(255, 255, 255, 90);
    public static final Color TEXT = new Color(255, 255, 255, 216);
    public static final Color TEXT_TITLE = new Color(255, 255, 255);
    public static final Color TEXT_VALUE = new Color(255, 255, 255, 172);
    public static final Color INDICATOR = new Color(255, 255, 255, 165);
    public static final Color ROW_RAIL_OFF = new Color(255, 255, 255, 58);
    public static final Color TOOLTIP = new Color(0, 0, 0, 210);

    public static int argb(Color color) {
        return color.getRGB();
    }

    public static int argb(Color color, float alpha) {
        return fade(argb(color), alpha);
    }

    public static int fade(int argb, float alpha) {
        if (alpha >= 1.0F) return argb;

        int scaled = Math.round((argb >>> 24) * Math.max(0.0F, Math.min(1.0F, alpha)));

        return (scaled << 24) | (argb & 0x00FFFFFF);
    }

    private AtmospherePalette() {
    }
}
