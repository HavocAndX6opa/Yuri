package ddlc.yuri.api.gui.click.exhibition.components;

import ddlc.yuri.api.gui.click.exhibition.ExhibitionClickGui;
import ddlc.yuri.api.gui.click.exhibition.UI;

import java.util.ArrayList;

public class MainPanel {
    public boolean isOpen;
    public float x, y;
    public String headerString;
    public float dragX, dragY;
    public float lastDragX, lastDragY;
    public boolean dragging;
    public UI theme;
    public ExhibitionClickGui gui;
    public ArrayList<CategoryButton> typeButton;
    public ArrayList<SLButton> slButtons;

    public MainPanel(String header, float x, float y, UI theme, ExhibitionClickGui gui) {
        this.headerString = header;
        this.x = x;
        this.y = y;
        this.theme = theme;
        this.gui = gui;
        typeButton = new ArrayList<>();
        slButtons = new ArrayList<>();
        theme.panelConstructor(this, x, y);
    }

    public void mouseClicked(int x, int y, int state) {
        theme.panelMouseClicked(this, x, y, state);
    }

    public void mouseMovedOrUp(int x, int y, int state) {
        theme.panelMouseMovedOrUp(this, x, y, state);
    }

    public void draw(int mouseX, int mouseY) {
        theme.mainPanelDraw(this, mouseX, mouseY);
    }

    public void keyPressed(int key) {
        theme.mainPanelKeyPress(this, key);
    }

    public void handleMouseInput() {
        theme.handleMouseInput(this);
    }
}
