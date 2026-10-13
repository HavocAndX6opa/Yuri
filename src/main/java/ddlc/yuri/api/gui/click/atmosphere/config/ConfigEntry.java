package ddlc.yuri.api.gui.click.atmosphere.config;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.click.atmosphere.AtmosphereDraw;
import ddlc.yuri.api.gui.click.atmosphere.AtmosphereLayout;
import ddlc.yuri.api.gui.click.atmosphere.AtmospherePalette;
import ddlc.yuri.api.gui.click.atmosphere.ColumnRow;

public abstract class ConfigEntry implements ColumnRow {

    protected final String name;
    protected final ConfigTab parent;

    protected AtmosphereLayout layout;

    private float layoutY;

    protected ConfigEntry(String name, ConfigTab parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public ConfigTab getParent() {
        return parent;
    }

    @Override
    public void setLayout(AtmosphereLayout layout) {
        this.layout = layout;
    }

    @Override
    public float layoutY() {
        return layoutY;
    }

    @Override
    public void setLayoutY(float layoutY) {
        this.layoutY = layoutY;
    }

    @Override
    public float height() {
        return this.layout.rowHeight;
    }

    @Override
    public void render(float panelX, float rowY, float width, boolean hovered, int accent, float alpha) {
        CustomFontRenderer font = this.layout.font();

        float left = panelX + this.layout.padX;
        float right = panelX + width - this.layout.padX;

        int color = this.isActive() ? accent
                : hovered ? AtmospherePalette.argb(AtmospherePalette.TEXT_TITLE)
                        : AtmospherePalette.argb(AtmospherePalette.TEXT);

        font.drawString(AtmosphereDraw.truncate(font, this.label(), right - left), left,
                AtmosphereDraw.labelY(font, rowY, this.height()), AtmospherePalette.fade(color, alpha));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    protected String label() {
        return this.name;
    }
}
