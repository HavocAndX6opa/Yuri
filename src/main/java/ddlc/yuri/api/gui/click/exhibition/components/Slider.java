package ddlc.yuri.api.gui.click.exhibition.components;

import ddlc.yuri.api.properties.impl.NumberProperty;

public class Slider {
    public float x, y;
    public String name;
    public NumberProperty setting;
    public CategoryPanel panel;
    public boolean dragging;
    public double dragX, lastDragX;

    public Slider(CategoryPanel panel, float x, float y, NumberProperty setting) {
        this.panel = panel;
        this.x = x;
        this.y = y;
        this.setting = setting;
        panel.categoryButton.panel.theme.SliderContructor(this, panel);
    }

    public void draw(float x, float y) {
        panel.categoryButton.panel.theme.SliderDraw(this, x, y, this.panel);
    }

    public void mouseClicked(int x, int y, int button) {
        panel.categoryButton.panel.theme.SliderMouseClicked(this, x, y, button, this.panel);
    }

    public void mouseReleased(int x, int y, int button) {
        panel.categoryButton.panel.theme.SliderMouseMovedOrUp(this, x, y, button, this.panel);
    }
}
