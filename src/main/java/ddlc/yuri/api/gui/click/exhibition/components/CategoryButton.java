package ddlc.yuri.api.gui.click.exhibition.components;

import ddlc.yuri.api.gui.click.exhibition.UI;

public class CategoryButton {
    public float x, y;
    public String name;
    public MainPanel panel;
    public boolean enabled;
    public CategoryPanel categoryPanel;
    public float fade;

    public CategoryButton(MainPanel panel, String name, float x, float y) {
        this.fade = 0;
        this.panel = panel;
        this.name = name;
        this.x = x;
        this.y = y;
        panel.theme.categoryButtonConstructor(this, this.panel);
    }

    public void draw(float x, float y) {
        panel.theme.categoryButtonDraw(this, x, y);
    }

    public void mouseClicked(int x, int y, int button) {
        panel.theme.categoryButtonMouseClicked(this, this.panel, x, y, button);
    }

    public void mouseReleased(int x, int y, int button) {
        panel.theme.categoryButtonMouseReleased(this, x, y, button);
    }
}
