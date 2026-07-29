package ddlc.yuri.api.gui.click.exhibition.components;

public class MultiDropdownButton {
    public String name;
    public float x, y;
    public MultiDropdownBox box;
    public boolean selected;

    public MultiDropdownButton(String name, float x, float y, MultiDropdownBox box, boolean selected) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.box = box;
        this.selected = selected;
    }

    public void draw(float x, float y) {
        box.panel.categoryButton.panel.theme.multiDropDownButtonDraw(this, box, x, y);
    }

    public void mouseClicked(int x, int y, int button) {
        box.panel.categoryButton.panel.theme.multiDropDownButtonMouseClicked(this, box, x, y, button);
    }
}
