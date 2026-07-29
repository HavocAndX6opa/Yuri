package ddlc.yuri.api.gui.click.exhibition.components;

import ddlc.yuri.api.properties.Property;

public class TextBox {
    public String textString;
    public float x, y;
    public Property<String> setting;
    public CategoryPanel panel;
    public boolean isFocused, isTyping;
    public float cursorAlpha = 255;
    public boolean backwards;
    public int cursorPos;
    public float offset;

    public TextBox(Property<String> setting, float x, float y, CategoryPanel panel) {
        this.x = x;
        this.y = y;
        this.panel = panel;
        this.setting = setting;
        this.textString = setting.getValue();
        this.cursorPos = textString.length();
    }

    public void draw(float x, float y) {
        if (panel.visible) {
            panel.categoryButton.panel.theme.textBoxDraw(this, x, y);
        }
    }

    public void mouseClicked(int x, int y, int button) {
        if (panel.visible) {
            panel.categoryButton.panel.theme.textBoxMouseClicked(this, x, y, button);
        }
    }

    public void keyPressed(int key) {
        if (panel.visible) {
            panel.categoryButton.panel.theme.textBoxKeyPressed(this, key);
        }
    }
}
