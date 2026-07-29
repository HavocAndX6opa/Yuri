package ddlc.yuri.api.gui.click.exhibition.components;

public class DropdownButton {
    public String name;
    public float x, y;
    public DropdownBox box;

    public DropdownButton(String name, float x, float y, DropdownBox box) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.box = box;
    }

    public void draw(float x, float y) {
        box.panel.categoryButton.panel.theme.dropDownButtonDraw(this, box, x, y);
    }

    public void mouseClicked(int x, int y, int button) {
        box.panel.categoryButton.panel.theme.dropDownButtonMouseClicked(this, box, x, y, button);
    }
}
