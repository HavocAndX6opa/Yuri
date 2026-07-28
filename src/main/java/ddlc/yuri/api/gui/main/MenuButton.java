package ddlc.yuri.api.gui.main;

import ddlc.yuri.api.gui.click.GuiTheme;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;

import java.awt.*;
/**
 * Self-contained menu button: owns its hitbox, hover animation, click dispatch,
 * and box rendering. Text drawing is left to the caller since font handling
 * (FontUtils scaled fonts) varies slightly per screen.
 */
public class MenuButton {

    public static final float DEFAULT_WIDTH = 70f;

    public final String label;
    public final Runnable action;
    private Float widthOverride;
    private Float heightOverride;
    private Color textColor = GuiTheme.TEXT;

    public float x, y, width, height;
    public float hoverAnim;

    public MenuButton(String label, Runnable action) {
        this.label = label;
        this.action = action;
        this.widthOverride = DEFAULT_WIDTH;
    }

    public MenuButton(String label, Runnable action, float width) {
        this(label, action);
        this.widthOverride = width;
    }

    public MenuButton(String label, Runnable action, float width, float height) {
        this(label, action, width);
        this.heightOverride = height;
        this.width = width;
        this.height = height;
    }

    /** Uses a custom width while retaining the normal button height. */
    public MenuButton withWidth(float width) {
        this.widthOverride = width;
        return this;
    }

    /** Restores text-sized layout for this button. */
    public MenuButton autoWidth() {
        this.widthOverride = null;
        return this;
    }

    /** Uses the muted text tone while retaining the normal hover animation. */
    public MenuButton mutedText() {
        this.textColor = GuiTheme.TEXT_MUTE;
        return this;
    }

    public float getLayoutWidth(float textWidth) {
        return widthOverride == null ? textWidth + 16f : widthOverride;
    }

    public float getLayoutHeight(float textHeight) {
        return heightOverride == null ? textHeight + 10f : heightOverride;
    }

    /** Positions/sizes the button so its text box sits at (textX, textY) with the given metrics. */
    public void layout(float textX, float textY, float textWidth, float textHeight) {
        this.x = textX - 8f;
        this.y = textY - 5f;
        this.width = widthOverride == null ? textWidth + 16f : widthOverride;
        this.height = getLayoutHeight(textHeight);
    }

    /** Positions an icon-only or placeholder button with an explicit box. */
    public void layoutBox(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void updateHover(float mouseX, float mouseY) {
        float target = this.isHovered(mouseX, mouseY) ? 1.00f : 0.00f;
        float speed = 12f/1000;
        hoverAnim += (target - hoverAnim) * (1f - (float)Math.exp(-speed * RenderUtils.delta));
    }

    public int getTextColor() {
        return RenderUtils.interpolateColor(textColor, ColorManager.getColor(), hoverAnim);
    }

    /** Draws the box + animated hover accent line. Call font.drawString(label, x, y, getTextColor()) after this. */
    public void renderBox() {

        Gui.drawRectOutline(x, y, width, height, 0.5F, GuiTheme.BUTTON.getRGB(), GuiTheme.BUTTON_OUTLINE.getRGB());

        if (this.hoverAnim > 0.01f) {

            float ease = 1f - (1f - this.hoverAnim) * (1f - this.hoverAnim);
            float half = (this.width / 2f) * ease;
            float mid = this.x + this.width / 2f;

            RenderUtils.drawCenteredGradientRect(mid - half, this.y, mid + half, this.y + 0.5F, RenderUtils.withAlpha(ColorManager.getColor(), 50), ColorManager.getColor().getRGB());
        }
    }

    public void mouseClicked() {
        if (action != null) {
            action.run();
        }
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x &&
                mouseX <= x + width &&
                mouseY >= y &&
                mouseY <= y + height;
    }
}