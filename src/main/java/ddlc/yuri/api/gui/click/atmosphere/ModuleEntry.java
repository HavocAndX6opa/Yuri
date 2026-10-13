package ddlc.yuri.api.gui.click.atmosphere;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.client.KeyUtil;
import org.lwjgl.input.Keyboard;

public class ModuleEntry implements ColumnRow {

    private final CategoryTab tab;
    private final Module module;

    private boolean open;
    private boolean listeningForKey;
    private float layoutY;
    private AtmosphereLayout layout;

    public ModuleEntry(CategoryTab tab, Module module) {
        this.tab = tab;
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    public boolean hasSettings() {
        return !this.module.getElements().isEmpty();
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isListeningForKey() {
        return listeningForKey;
    }

    public void setListeningForKey(boolean listeningForKey) {
        this.listeningForKey = listeningForKey;
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
    public void setLayout(AtmosphereLayout layout) {
        this.layout = layout;
    }

    @Override
    public float height() {
        return this.layout.rowHeight;
    }

    @Override
    public boolean isActive() {
        return this.module.isEnabled();
    }

    @Override
    public String description() {
        String description = this.module.getDescription();

        return description == null || description.isEmpty() ? null : description;
    }

    @Override
    public boolean cancelInteraction() {
        if (!this.listeningForKey) return false;

        this.listeningForKey = false;

        return true;
    }

    @Override
    public void render(float panelX, float rowY, float width, boolean hovered, int accent, float alpha) {
        AtmosphereLayout l = this.layout;

        CustomFontRenderer font = l.font();

        String name = this.listeningForKey ? "..." : this.module.getLabel();
        String bind = this.bindText();

        float right = panelX + width - l.indicatorInset - l.indicatorWidth;

        boolean showBind = bind != null && ClickGUIModule.keybinds.getValue();

        if (showBind) {
            right -= Math.min(font.getStringWidth(bind), (width - 2.0F * l.padX) / 3.0F);
        }

        float textY = AtmosphereDraw.labelY(font, rowY, l.rowHeight);

        int nameColor = this.module.isEnabled() ? accent : AtmospherePalette.argb(AtmospherePalette.TEXT);

        font.drawString(AtmosphereDraw.truncate(font, name, Math.max(0.0F, right - panelX - l.padX)),
                panelX + l.padX, textY, AtmospherePalette.fade(nameColor, alpha));

        if (showBind) {
            AtmosphereDraw.value(font, bind, right, textY,
                    AtmospherePalette.fade(AtmospherePalette.argb(AtmospherePalette.TEXT_VALUE), alpha));
        }

        if (this.hasSettings()) {
            AtmosphereDraw.triangleUp(panelX + width - l.indicatorInset - l.indicatorWidth / 2.0F,
                    rowY + (l.rowHeight - l.indicatorHeight) / 2.0F,
                    l.indicatorWidth, l.indicatorHeight,
                    AtmospherePalette.fade(AtmospherePalette.argb(AtmospherePalette.INDICATOR), alpha));
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            this.module.toggle();
        } else if (mouseButton == 1 && this.hasSettings()) {
            this.tab.openModule(this);
        } else if (mouseButton == 2 && this.hasSettings()) {
            this.listeningForKey = true;
        }
    }

    private String bindText() {
        if (this.listeningForKey) return null;

        int key = this.module.getKey();

        if (key == Keyboard.KEY_NONE) return null;

        return KeyUtil.getKeyName(key);
    }
}
