package ddlc.yuri.api.gui.click.exhibition.components;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;

public class Checkbox {
    public CategoryPanel panel;
    public boolean enabled;
    public float x, y;
    public String name;
    public Property<Boolean> setting;
    public Module module;

    public Checkbox(CategoryPanel panel, float x, float y, Property<Boolean> setting) {
        this.panel = panel;
        this.name = setting.getLabel();
        this.x = x;
        this.y = y;
        this.setting = setting;
        this.enabled = setting.getValue();
    }

    public Checkbox(CategoryPanel panel, String name, float x, float y, Property<Boolean> setting) {
        this.panel = panel;
        this.name = name;
        this.x = x;
        this.y = y;
        this.setting = setting;
        this.enabled = setting.getValue();
    }

    public Checkbox(CategoryPanel panel, float x, float y, Module module, Property<Boolean> setting) {
        this.panel = panel;
        this.name = setting.getLabel();
        this.x = x;
        this.y = y;
        this.setting = setting;
        this.module = module;
        this.enabled = setting.getValue();
    }

    public void draw(float x, float y) {
        if (panel.visible) {
            panel.categoryButton.panel.theme.checkBoxDraw(this, x, y, this.panel);
        }
    }

    public void mouseClicked(int x, int y, int button) {
        panel.categoryButton.panel.theme.checkBoxMouseClicked(this, x, y, button, this.panel);
    }
}
