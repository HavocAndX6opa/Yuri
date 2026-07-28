package ddlc.yuri.api.gui.alt.comp;

import ddlc.yuri.api.gui.click.GuiTheme;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

public class CustomTextBox extends Gui {

    private static final Color FIELD_BACKGROUND = new Color(15, 15, 18);
    private final int maxStringLength = 32;

    public int xPosition;
    public int yPosition;

    private int width, height;
    private String text = "";
    private String placeholder = "";
    private boolean focused;
    private boolean selectedAll;

    public CustomTextBox(int x, int y, int w, int h) {
        this.xPosition = x;
        this.yPosition = y;
        this.width = w;
        this.height = h;
    }

    public void drawTextBox() {
        int borderColor = focused ? GuiTheme.getAccent().getRGB() : RenderUtils.withAlpha(GuiTheme.PANEL, 255);
        drawRect(xPosition, yPosition, xPosition + width, yPosition + height, borderColor);
        drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + height - 1, RenderUtils.withAlpha(FIELD_BACKGROUND, 255));

        boolean empty = text.isEmpty();
        String renderText = empty ? placeholder : text;
        int color = empty ? 0x777777 : 0xFFFFFF;
        boolean showCursor = focused && !empty && (System.currentTimeMillis() / 500) % 2 == 0;

        FontUtils.getFont("sf", 18).drawString(
                renderText + (showCursor ? "|" : ""),
                xPosition + 6,
                yPosition + (height / 2f) - (FontUtils.getFont("sf", 18).getHeight() / 2f),
                color
        );
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        focused = mouseButton == 0 &&
                mouseX >= xPosition &&
                mouseX <= xPosition + width &&
                mouseY >= yPosition &&
                mouseY <= yPosition + height;

        if (!focused) selectedAll = false;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!focused) return;

        if (keyCode == Keyboard.KEY_A && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
            selectedAll = true;
            return;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (selectedAll) {
                text = "";
                selectedAll = false;
                return;
            }
            if (!text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
            }
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
            focused = false;
            selectedAll = false;
            return;
        }

        if (typedChar >= 32 && typedChar != 127) {
            if (selectedAll) {
                text = "";
                selectedAll = false;
            }
            if (text.length() < maxStringLength) {
                text += typedChar;
            }
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text.length() > maxStringLength ? text.substring(0, maxStringLength) : text;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) selectedAll = false;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
