package ddlc.yuri.api.gui.click.exhibition.components;

public class GroupBox {
    public float x, y, width, height;
    public String label;
    public boolean renderLabel;
    public CategoryPanel categoryPanel;

    public GroupBox(String label, CategoryPanel categoryPanel, float x, float y, float height) {
        this(label, categoryPanel, x, y, 90, height, false);
    }

    public GroupBox(String label, CategoryPanel categoryPanel, float x, float y, float width, float height, boolean renderLabel) {
        this.x = x;
        this.y = y;
        this.label = label;
        this.width = width;
        this.height = height;
        this.renderLabel = renderLabel;
        this.categoryPanel = categoryPanel;
        categoryPanel.categoryButton.panel.theme.groupBoxConstructor(this, x, y);
    }

    public void draw(float x, float y) {
        categoryPanel.categoryButton.panel.theme.groupBoxDraw(this, x, y);
    }

    public void mouseClicked(int x, int y, int button) {
        categoryPanel.categoryButton.panel.theme.groupBoxMouseClicked(this, x, y, button);
    }

    public void mouseReleased(int x, int y, int button) {
        categoryPanel.categoryButton.panel.theme.groupBoxMouseMovedOrUp(this, x, y, button);
    }
}
