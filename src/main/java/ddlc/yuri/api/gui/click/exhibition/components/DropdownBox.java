package ddlc.yuri.api.gui.click.exhibition.components;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;

import java.util.ArrayList;
import java.util.List;

public class DropdownBox {
    public ModeProperty<?> option;
    public Property<?> setting;
    public float x, y;
    public List<DropdownButton> buttons = new ArrayList<>();
    public CategoryPanel panel;
    public boolean active;

    public DropdownBox(Property<?> setting, float x, float y, CategoryPanel panel) {
        this.setting = setting;
        this.option = (ModeProperty<?>) setting;
        this.panel = panel;
        this.x = x;
        this.y = y;
        panel.categoryButton.panel.theme.dropDownContructor(this, x, y, this.panel);
    }

    public void draw(float x, float y) {
        if (panel.visible) {
            panel.categoryButton.panel.theme.dropDownDraw(this, x, y, this.panel);
        }
    }

    public void mouseClicked(int x, int y, int button) {
        panel.categoryButton.panel.theme.dropDownMouseClicked(this, x, y, button, this.panel);
    }
}
