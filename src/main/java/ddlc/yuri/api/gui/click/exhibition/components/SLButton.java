package ddlc.yuri.api.gui.click.exhibition.components;

public class SLButton {
    public float x, y;
    public String name;
    public MainPanel panel;
    public boolean load;

    public SLButton(MainPanel panel, String name, float x, float y, boolean load) {
        this.panel = panel;
        this.name = name;
        this.x = x;
        this.y = y;
        this.load = load;
    }

    public void draw(float x, float y) {
        panel.theme.slButtonDraw(this, x, y, this.panel);
    }

    public void mouseClicked(int x, int y, int button) {
        panel.theme.slButtonMouseClicked(this, x, y, button, this.panel);
    }
}
