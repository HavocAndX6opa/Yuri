package ddlc.yuri.api.gui.click.exhibition.components;

import ddlc.yuri.modules.Module;

public class Button {
    public float x, y;
    public String name;
    public CategoryPanel panel;
    public boolean enabled;
    public Module module;
    public boolean isBinding;

    public Button(CategoryPanel panel, String name, float x, float y, Module module) {
        this.panel = panel;
        this.name = name;
        this.x = x;
        this.y = y;
        this.module = module;
        this.enabled = module.isEnabled();
        panel.categoryButton.panel.theme.buttonContructor(this, this.panel);
    }

    public void draw(float x, float y) {
        if (panel.visible) {
            panel.categoryButton.panel.theme.buttonDraw(this, x, y, this.panel);
        }
    }

    public void mouseClicked(int x, int y, int button) {
        panel.categoryButton.panel.theme.buttonMouseClicked(this, x, y, button, this.panel);
    }

    public void keyPressed(int key) {
        panel.categoryButton.panel.theme.buttonKeyPressed(this, key);
    }
}
