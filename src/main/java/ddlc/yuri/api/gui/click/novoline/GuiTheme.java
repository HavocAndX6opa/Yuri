package ddlc.yuri.api.gui.click.novoline;

import java.awt.*;

public final class GuiTheme {

    public static final Color ACCENT = new Color(161, 82, 230);
    public static final Color PANEL = new Color(29, 29, 29);
    public static final Color MODULE_BG = new Color(40, 40, 40);
    public static final Color MODULE_HOVER = new Color(29, 29, 29);
    public static final Color OVERLAY = new Color(0, 0, 0, 120);
    public static final Color TEXT = new Color(143, 144, 155);
    public static final Color TEXT_MUTE = new Color(84, 85, 94);
    public static final Color BUTTON = new Color(24, 24, 26, 180);
    public static final Color BUTTON_OUTLINE = new Color(36, 36, 42, 180);

    private GuiTheme() {
    }

    public static Color getAccent() {
        return ACCENT;
    }
}
