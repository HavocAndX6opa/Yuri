package ddlc.yuri.api.gui.click.exhibition.components;

import ddlc.yuri.api.gui.click.exhibition.UI;

import java.util.ArrayList;
import java.util.List;

public class CategoryPanel {
    public float x, y;
    public float scrollY;
    public boolean visible;
    public CategoryButton categoryButton;
    public String headerString;
    public List<Button> buttons = new ArrayList<>();
    public List<Slider> sliders = new ArrayList<>();
    public List<DropdownBox> dropdownBoxes = new ArrayList<>();
    public List<MultiDropdownBox> multiDropdownBoxes = new ArrayList<>();
    public List<Checkbox> checkboxes = new ArrayList<>();
    public List<GroupBox> groupBoxes = new ArrayList<>();
    public List<TextBox> textBoxes = new ArrayList<>();

    public CategoryPanel(String name, CategoryButton categoryButton, float x, float y, float scrollY) {
        this.headerString = name;
        this.x = x;
        this.y = y;
        this.scrollY = y;
        this.categoryButton = categoryButton;
        this.visible = false;
        categoryButton.panel.theme.categoryPanelConstructor(this, categoryButton, x, y);
    }

    public void draw(float x, float y) {
        categoryButton.panel.theme.categoryPanelDraw(this, x, y);
    }

    public void mouseClicked(int x, int y, int button) {
        categoryButton.panel.theme.categoryPanelMouseClicked(this, x, y, button);
    }

    public void mouseReleased(int x, int y, int button) {
        categoryButton.panel.theme.categoryPanelMouseMovedOrUp(this, x, y, button);
    }
}
