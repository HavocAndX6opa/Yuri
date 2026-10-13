package ddlc.yuri.api.gui.click.atmosphere.config;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.click.atmosphere.AtmosphereDraw;
import ddlc.yuri.api.gui.click.atmosphere.AtmospherePalette;
import ddlc.yuri.utils.misc.Timer;
import org.lwjgl.input.Keyboard;

public class ConfigTextField extends ConfigEntry {

    private final Timer backspace = new Timer();

    private String value = "";
    private boolean focused;

    public ConfigTextField(String name, ConfigTab parent) {
        super(name, parent);
    }

    public String getValue() {
        return value == null ? "" : value;
    }

    @Override
    public boolean isActive() {
        return this.focused;
    }

    @Override
    public boolean cancelInteraction() {
        if (!this.focused) return false;

        this.focused = false;

        return true;
    }

    @Override
    protected String label() {
        return this.focused ? this.getName() : this.getName() + ":";
    }

    @Override
    public void render(float panelX, float rowY, float width, boolean hovered, int accent, float alpha) {
        super.render(panelX, rowY, width, hovered, accent, alpha);

        if (!this.focused) return;

        if (Keyboard.isKeyDown(Keyboard.KEY_BACK) && this.backspace.hasTimeElapsed(100, true)
                && !this.value.isEmpty()) {
            this.value = this.value.substring(0, this.value.length() - 1);
        }

        CustomFontRenderer font = this.layout.font();

        String text = this.value + (((System.currentTimeMillis() / 400) % 2 == 0) ? "|" : "");

        AtmosphereDraw.value(font, text, panelX + width - this.layout.padX,
                AtmosphereDraw.labelY(font, rowY, this.height()),
                AtmospherePalette.fade(AtmospherePalette.argb(AtmospherePalette.TEXT_VALUE), alpha));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        this.focused = !this.focused;

        this.parent.setSelectedConfig(this.focused ? this : null);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!this.focused) return;

        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
            this.focused = false;
            this.parent.setSelectedConfig(null);
        } else if (keyCode != Keyboard.KEY_BACK && !isIgnoredKey(keyCode)) {
            this.value += typedChar;
        }
    }

    private static boolean isIgnoredKey(int keyCode) {
        return keyCode == Keyboard.KEY_RCONTROL
                || keyCode == Keyboard.KEY_LCONTROL
                || keyCode == Keyboard.KEY_RSHIFT
                || keyCode == Keyboard.KEY_LSHIFT
                || keyCode == Keyboard.KEY_TAB;
    }
}
