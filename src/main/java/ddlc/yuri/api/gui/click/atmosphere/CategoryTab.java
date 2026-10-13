package ddlc.yuri.api.gui.click.atmosphere;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.DescriptorProperty;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.client.KeyUtil;
import ddlc.yuri.utils.misc.Timer;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.ScaleUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class CategoryTab {

    private static final float SCROLL_EASE = 0.35F;
    private static final float SETTINGS_EASE = 0.3F;

    private final ModuleCategory category;

    protected final List<ColumnRow> rows = new ArrayList<>();

    private final Set<Property<?>> openModes = new HashSet<>();
    private final List<PropertyEntry> settingRows = new ArrayList<>();

    private Property<?> editing;
    private Property<?> listening;

    private final Timer backSpace = new Timer();

    private AtmosphereLayout layout;
    private float x;
    private float y;
    private float width;

    private float boxHeight;
    private float contentHeight;

    private ModuleEntry openModule;
    private float settingsProgress;

    private float scroll;
    private float scrollTarget;

    private NumberProperty dragging;
    private float settingsTop;
    private float contentTop;
    private float viewBottom;

    public CategoryTab(ModuleCategory category) {
        this.category = category;
        this.rows.addAll(createRows());
    }

    protected List<ColumnRow> createRows() {
        if (category == null) {
            return Collections.emptyList();
        }

        List<ColumnRow> created = new ArrayList<>();

        for (Module module : Yuri.INSTANCE.getModuleManager().getModulesForCategory(category)) {
            created.add(new ModuleEntry(this, module));
        }

        return created;
    }

    public String getTitle() {
        return category == null ? "Configs" : category.getName();
    }

    public ModuleCategory getCategory() {
        return category;
    }

    protected AtmosphereLayout getLayout() {
        return layout;
    }

    public void applyLayout(AtmosphereLayout layout, float x, float y) {
        this.layout = layout;
        this.x = x;
        this.y = y;
        this.width = layout.panelWidth;

        for (ColumnRow row : rows) {
            row.setLayout(layout);
        }
    }

    private void buildSettingRows() {
        this.settingRows.clear();

        if (this.openModule == null) {
            this.openModes.clear();
            return;
        }

        List<Property<?>> settings = this.openModule.getModule().getElements();

        int previousDepth = 0;

        for (Property<?> property : settings) {
            if (!property.isAvailable()) continue;

            PropertyEntry row = new PropertyEntry(property);
            row.setLayout(this.layout);
            row.setGapBefore(previousDepth > 0);
            row.setModeOpen(this.openModes.contains(property));
            row.setFocused(this.editing == property);
            row.setListening(this.listening == property);
            this.settingRows.add(row);

            previousDepth = 0;

            if (!isMode(property) || !this.openModes.contains(property)) continue;

            previousDepth = 1;

            for (Enum<?> option : options(property)) {
                PropertyEntry optionRow = new PropertyEntry(property, option, 1);
                optionRow.setLayout(this.layout);
                this.settingRows.add(optionRow);
            }
        }

        this.openModes.retainAll(settings);

        if (this.editing != null && !settings.contains(this.editing)) this.editing = null;
        if (this.listening != null && !settings.contains(this.listening)) this.listening = null;
    }

    private float settingsHeight() {
        float height = 0.0F;

        for (PropertyEntry row : this.settingRows) {
            height += (row.isGapBefore() ? this.layout.groupGap : 0.0F) + row.height();
        }

        return height;
    }

    private float sectionHeight(float rowsHeight, float progress) {
        float gap = this.layout.groupGap;

        return gap + 1.0F + gap + rowsHeight + gap * progress;
    }

    private float contentHeight(float progress) {
        float height = this.layout.headerHeight + this.rows.size() * this.layout.rowHeight;

        if (this.openModule != null && !this.settingRows.isEmpty()) {
            height += this.sectionHeight(this.settingsHeight() * progress, progress);
        }

        return height;
    }

    private void measure(float screenHeight, float progress) {
        AtmosphereLayout l = this.layout;

        this.contentHeight = this.contentHeight(progress);

        float room = screenHeight - this.y - l.rowHeight;

        float viewport = Math.max(l.rowHeight,
                (float) Math.floor((room - l.headerHeight) / l.rowHeight) * l.rowHeight);

        this.boxHeight = Math.min(this.contentHeight, l.headerHeight + viewport);
    }

    public void render(int mouseX, int mouseY, float screenHeight, float progress, float alpha) {
        this.buildSettingRows();
        this.measure(screenHeight, this.settingsProgress);

        AtmosphereLayout l = this.layout;

        float height = this.boxHeight * progress;

        this.viewBottom = this.y + height;

        float header = Math.min(l.headerHeight, height);

        this.contentTop = this.y + header;

        if (height <= 0.0F) return;

        int accent = ColorManager.getColor().getRGB();

        AtmosphereDraw.fill(this.x, this.y, this.x + this.width, this.viewBottom,
                AtmospherePalette.argb(AtmospherePalette.PANEL, alpha));

        AtmosphereDraw.fill(this.x, this.y, this.x + this.width, this.y + header,
                AtmospherePalette.argb(AtmospherePalette.HEADER, alpha));

        l.font().drawCenteredString(getTitle(), this.x + this.width / 2.0F,
                AtmosphereDraw.labelY(l.font(), this.y, header),
                AtmospherePalette.argb(AtmospherePalette.TEXT_TITLE, alpha));

        float lineY = this.y + header - l.headerLineHeight;

        AtmosphereDraw.fill(this.x + 1.0F, lineY, this.x + this.width - 1.0F, lineY + l.headerLineHeight,
                AtmospherePalette.fade(accent, alpha));

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaleUtils.applyScissor(Minecraft.getMinecraft(), this.x, this.y + header, this.x + this.width,
                this.viewBottom);

        float cursor = this.y + header - this.scroll;

        for (ColumnRow row : this.rows) {
            boolean hovered = this.hovered(mouseX, mouseY, cursor, row.height());

            row.setLayoutY(cursor);

            this.renderRow(row, cursor, hovered, accent, alpha);

            if (hovered && tooltips() && row.description() != null) {
                AtmosphereClickGui.toolTip = row.description();
            }

            cursor += row.height();

            if (row == this.openModule && !this.settingRows.isEmpty()) {
                this.settingsTop = cursor;

                cursor = this.renderSettings(mouseX, mouseY, cursor, accent, alpha);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        this.renderScrollbar(alpha);

        AtmosphereDraw.outline(this.x, this.y, this.x + this.width, this.viewBottom,
                AtmospherePalette.argb(AtmospherePalette.BORDER, alpha));
    }

    private void renderRow(ColumnRow row, float rowY, boolean hovered, int accent, float alpha) {
        AtmosphereLayout l = this.layout;

        float rowHeight = row.height();

        boolean active = row.isActive();

        if (active) {
            AtmosphereDraw.fadeRight(this.x, rowY, rowHeight, l.washWidth,
                    AtmospherePalette.fade(AtmospherePalette.fade(accent, 0.22F), alpha),
                    Math.max(2, Math.round(l.washWidth)));
        }

        if (hovered) {
            AtmosphereDraw.fill(this.x, rowY, this.x + this.width, rowY + rowHeight,
                    AtmospherePalette.argb(AtmospherePalette.ROW_HOVER, alpha));
        }

        if (row == this.openModule) {
            AtmosphereDraw.fill(this.x, rowY, this.x + this.width, rowY + rowHeight,
                    AtmospherePalette.argb(AtmospherePalette.ROW_OPEN, alpha));

            AtmosphereDraw.outline(this.x, rowY, this.x + this.width, rowY + rowHeight,
                    AtmospherePalette.fade(accent, alpha));
        }

        AtmosphereDraw.fill(this.x, rowY, this.x + l.railWidth, rowY + rowHeight,
                active ? AtmospherePalette.fade(accent, alpha)
                        : AtmospherePalette.argb(AtmospherePalette.ROW_RAIL_OFF, alpha));

        row.render(this.x, rowY, this.width, hovered, accent, alpha);
    }

    private float renderSettings(int mouseX, int mouseY, float top, int accent, float alpha) {
        AtmosphereLayout l = this.layout;

        float shown = this.settingsHeight() * this.settingsProgress;

        float dividerTop = top + l.groupGap;
        float rowsTop = dividerTop + 1.0F + l.groupGap;

        float end = top + this.sectionHeight(shown, this.settingsProgress);

        if (shown <= 0.0F) return end;

        AtmosphereDraw.fill(this.x + 1.0F, dividerTop, this.x + this.width - 1.0F, dividerTop + 1.0F,
                AtmospherePalette.argb(AtmospherePalette.DIVIDER, alpha));

        float cursor = rowsTop;

        for (PropertyEntry row : this.settingRows) {
            if (row.isGapBefore()) cursor += l.groupGap;

            if (cursor >= rowsTop + shown) return end;

            row.render(this.x, cursor, this.width, this.hovered(mouseX, mouseY, cursor, row.height()),
                    accent, alpha);

            cursor += row.height();
        }

        return end;
    }

    private void renderScrollbar(float alpha) {
        AtmosphereLayout l = this.layout;

        float top = this.y + Math.min(l.headerHeight, this.boxHeight);
        float viewport = this.viewBottom - top;

        if (viewport <= 2.0F) return;

        float x = this.x + this.width - 2.0F;

        AtmosphereDraw.fill(x, top + 1.0F, x + 1.0F, this.viewBottom - 1.0F,
                AtmospherePalette.argb(AtmospherePalette.TRACK, alpha));

        float max = this.maxScroll();

        if (max <= 0.0F) return;

        float content = Math.max(viewport, this.contentHeight - l.headerHeight);

        float thumb = Math.max(10.0F * l.scale, viewport * (viewport / content));

        float travel = viewport - thumb - 2.0F;

        if (travel <= 0.0F) return;

        float position = (this.scroll / max) * travel;

        AtmosphereDraw.fill(x, top + 1.0F + position, x + 1.0F, top + 1.0F + position + thumb,
                AtmospherePalette.argb(AtmospherePalette.SCROLLBAR, alpha));
    }

    public void update(int mouseX, int mouseY) {
        if (this.dragging != null) {
            this.applyDrag(mouseX);
        }

        float target = this.openModule == null || this.settingRows.isEmpty() ? 0.0F : 1.0F;

        this.settingsProgress += (target - this.settingsProgress) * SETTINGS_EASE;

        if (Math.abs(target - this.settingsProgress) < 0.01F) this.settingsProgress = target;

        this.scrollTarget = Math.max(0.0F, Math.min(this.maxScroll(), this.scrollTarget));

        this.scroll += (this.scrollTarget - this.scroll) * SCROLL_EASE;

        if (Math.abs(this.scrollTarget - this.scroll) < 0.1F) this.scroll = this.scrollTarget;

        this.editBackSpace();
    }

    @SuppressWarnings("unchecked")
    private void editBackSpace() {
        if (this.editing == null || !(this.editing.getValue() instanceof String)) return;

        String value = (String) this.editing.getValue();

        if (!Keyboard.isKeyDown(Keyboard.KEY_BACK) || value.isEmpty()) return;

        if (this.backSpace.hasTimeElapsed(100, true)) {
            ((Property<String>) this.editing).setValue(value.substring(0, value.length() - 1));
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.dragging != null) {
            this.applyDrag(mouseX);
            return;
        }

        if (this.cancelBind()) return;

        if (!this.contains(mouseX, mouseY)) return;

        this.clickRow(mouseX, mouseY, mouseButton);
    }

    private void clickRow(int mouseX, int mouseY, int mouseButton) {
        if (this.openModule != null && !this.settingRows.isEmpty()) {
            for (PropertyEntry row : this.settingRows) {
                if (!this.hovered(mouseX, mouseY, this.rowY(row), row.height())) continue;

                if (this.editing != row.getProperty()) this.editing = null;
                if (this.listening != row.getProperty()) this.listening = null;

                this.clickSetting(row, mouseX, mouseButton);

                return;
            }
        }

        for (ColumnRow row : this.rows) {
            if (!this.hovered(mouseX, mouseY, row.layoutY(), row.height())) continue;

            this.editing = null;
            this.listening = null;

            row.mouseClicked(mouseX, mouseY, mouseButton);

            return;
        }

        this.editing = null;
        this.listening = null;
    }

    private void clickSetting(PropertyEntry row, int mouseX, int mouseButton) {
        Property<?> property = row.getProperty();

        if (row.isOption()) {
            if (mouseButton != 0) return;

            int index = indexOfOption(property, row.getOption());

            if (index < 0) return;

            if (property instanceof ModeProperty) {
                ((ModeProperty<?>) property).setValue(index);
                this.openModes.remove(property);
            } else if (property instanceof MultiModeProperty) {
                ((MultiModeProperty<?>) property).setValue(index);
            }

            return;
        }

        if (property instanceof DescriptorProperty) return;

        Object value = property.getValue();

        if (value instanceof Boolean) {
            if (mouseButton == 0) {
                ((Property<Boolean>) property).setValue(!(Boolean) value);
            }

            return;
        }

        if (isMode(property)) {
            if (mouseButton == 0) {
                if (!this.openModes.add(property)) this.openModes.remove(property);
            }

            return;
        }

        if (property instanceof NumberProperty) {
            if (mouseButton != 0) return;

            this.dragging = (NumberProperty) property;

            this.applyDrag(mouseX);

            return;
        }

        if (value instanceof Integer) {
            if (this.listening == property) {
                ((Property<Integer>) property).setValue(KeyUtil.mouseButtonToKeyCode(mouseButton));
                this.listening = null;
            } else if (mouseButton == 0 || mouseButton == 2) {
                this.listening = property;
            }

            return;
        }

        if (value instanceof String) {
            this.editing = this.editing == property ? null : property;
        }
    }

    private void applyDrag(int mouseX) {
        if (this.dragging == null) return;

        AtmosphereLayout l = this.layout;

        float left = this.x + l.padX;
        float right = this.x + this.width - l.padX;

        float percent = Math.max(0.0F, Math.min(1.0F, (mouseX - left) / Math.max(1.0F, right - left)));

        double span = this.dragging.getMax() - this.dragging.getMin();
        double raw = this.dragging.getMin() + span * percent;

        this.dragging.setValue(RenderUtils.incValue(raw, this.dragging.getIncrement()));
    }

    public void mouseReleased(int state) {
        if (state == 0) {
            this.dragging = null;
        }
    }

    public void scrollBy(float amount) {
        this.scrollTarget = Math.max(0.0F, Math.min(this.maxScroll(), this.scrollTarget + amount));

        this.scroll = this.scrollTarget;
    }

    private float maxScroll() {
        return Math.max(0.0F, this.contentHeight - this.boxHeight);
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return this.contains(mouseX, mouseY);
    }

    public boolean cancelInteraction() {
        if (this.cancelBind()) return true;

        if (this.editing != null) {
            this.editing = null;
            return true;
        }

        if (this.listening != null) {
            this.listening = null;
            return true;
        }

        return false;
    }

    private boolean cancelBind() {
        for (ColumnRow row : this.rows) {
            if (!row.cancelInteraction()) continue;

            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public void keyTyped(char typedChar, int keyCode) {
        if (this.editing != null && this.editing.getValue() instanceof String) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                this.editing = null;
            } else if (!isIgnoredKey(keyCode)) {
                ((Property<String>) this.editing).setValue((String) this.editing.getValue() + typedChar);
            }

            return;
        }

        if (this.listening != null && this.listening.getValue() instanceof Integer) {
            ((Property<Integer>) this.listening).setValue(bindableKey(keyCode));
            this.listening = null;

            return;
        }

        for (ColumnRow row : this.rows) {
            if (!(row instanceof ModuleEntry)) continue;

            ModuleEntry moduleRow = (ModuleEntry) row;

            if (!moduleRow.isListeningForKey()) continue;

            moduleRow.setListeningForKey(false);
            moduleRow.getModule().setKey(bindableKey(keyCode));

            return;
        }

        for (ColumnRow row : this.rows) {
            row.keyTyped(typedChar, keyCode);
        }
    }

    public void openModule(ModuleEntry row) {
        ModuleEntry next = this.openModule == row ? null : row;

        this.openModule = next;
        this.openModes.clear();
        this.editing = null;
        this.listening = null;

        this.settingsProgress = 0.0F;

        for (ColumnRow other : this.rows) {
            if (other instanceof ModuleEntry) {
                ((ModuleEntry) other).setOpen(other == next);
            }
        }
    }

    private float rowY(PropertyEntry row) {
        AtmosphereLayout l = this.layout;

        float cursor = this.settingsTop + l.groupGap + 1.0F + l.groupGap;

        for (PropertyEntry other : this.settingRows) {
            if (other == row) return cursor;

            if (other.isGapBefore()) cursor += l.groupGap;

            cursor += other.height();
        }

        return cursor;
    }

    private boolean hovered(int mouseX, int mouseY, float rowY, float height) {
        int top = (int) Math.round(rowY);
        int bottom = (int) Math.round(rowY + height);
        int visibleTop = (int) Math.round(this.contentTop);

        return mouseX > this.x && mouseX < this.x + this.width
                && mouseY >= top && mouseY < bottom
                && mouseY >= visibleTop && mouseY < this.viewBottom;
    }

    private boolean contains(float mouseX, float mouseY) {
        return mouseX > this.x && mouseX < this.x + this.width && mouseY > this.y && mouseY < this.viewBottom;
    }

    private static boolean tooltips() {
        return ClickGUIModule.tooltips.getValue();
    }

    private static boolean isMode(Property<?> property) {
        return property instanceof ModeProperty || property instanceof MultiModeProperty;
    }

    private static Enum<?>[] options(Property<?> property) {
        return property instanceof ModeProperty
                ? ((ModeProperty<?>) property).getValues()
                : ((MultiModeProperty<?>) property).getValues();
    }

    private static int indexOfOption(Property<?> property, Enum<?> option) {
        Enum<?>[] values = options(property);

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(option)) return i;
        }

        return -1;
    }

    private static int bindableKey(int keyCode) {
        return KeyUtil.getBindableKey(keyCode);
    }

    private static boolean isIgnoredKey(int keyCode) {
        return keyCode == Keyboard.KEY_BACK
                || keyCode == Keyboard.KEY_RCONTROL
                || keyCode == Keyboard.KEY_LCONTROL
                || keyCode == Keyboard.KEY_RSHIFT
                || keyCode == Keyboard.KEY_LSHIFT
                || keyCode == Keyboard.KEY_TAB
                || keyCode == Keyboard.KEY_CAPITAL
                || keyCode == Keyboard.KEY_DELETE
                || keyCode == Keyboard.KEY_HOME
                || keyCode == Keyboard.KEY_INSERT
                || keyCode == Keyboard.KEY_UP
                || keyCode == Keyboard.KEY_DOWN
                || keyCode == Keyboard.KEY_RIGHT
                || keyCode == Keyboard.KEY_LEFT
                || keyCode == Keyboard.KEY_LMENU
                || keyCode == Keyboard.KEY_RMENU;
    }
}
