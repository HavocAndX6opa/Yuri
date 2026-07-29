package ddlc.yuri.api.gui.click.exhibition.components;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.MultiModeProperty;

import java.util.ArrayList;
import java.util.List;

public class MultiDropdownBox {
    public String name;
    public MultiModeProperty<?> multiMode;
    public Property<?> setting;
    public float x, y;
    public List<MultiDropdownButton> buttons = new ArrayList<>();
    public CategoryPanel panel;
    public boolean active;

    public MultiDropdownBox(MultiModeProperty<?> multiMode, Property<?> setting, float x, float y, CategoryPanel panel) {
        this.name = multiMode.getLabel();
        this.multiMode = multiMode;
        this.setting = setting;
        this.panel = panel;
        this.x = x;
        this.y = y;
        panel.categoryButton.panel.theme.multiDropDownContructor(this, x, y, this.panel);
    }

    public void draw(float x, float y) {
        if (panel.visible) {
            panel.categoryButton.panel.theme.multiDropDownDraw(this, x, y, this.panel);
        }
    }

    public void mouseClicked(int x, int y, int button) {
        panel.categoryButton.panel.theme.multiDropDownMouseClicked(this, x, y, button, this.panel);
    }
}
