package ddlc.yuri.api.gui.click.atmosphere;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.DescriptorProperty;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.utils.client.KeyUtil;

import java.util.List;

public final class PropertyEntry {

    private final Property<?> property;
    private final Enum<?> option;
    private final int depth;

    private boolean gapBefore;
    private boolean modeOpen;
    private boolean focused;
    private boolean listening;

    private AtmosphereLayout layout;

    public PropertyEntry(Property<?> property) {
        this(property, null, 0);
    }

    public PropertyEntry(Property<?> property, Enum<?> option, int depth) {
        this.property = property;
        this.option = option;
        this.depth = depth;
    }

    public Property<?> getProperty() {
        return property;
    }

    public Enum<?> getOption() {
        return option;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isOption() {
        return option != null;
    }

    public boolean isMode() {
        return option == null && (property instanceof ModeProperty || property instanceof MultiModeProperty);
    }

    public boolean isGapBefore() {
        return gapBefore;
    }

    public void setGapBefore(boolean gapBefore) {
        this.gapBefore = gapBefore;
    }

    public void setLayout(AtmosphereLayout layout) {
        this.layout = layout;
    }

    public void setModeOpen(boolean modeOpen) {
        this.modeOpen = modeOpen;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public float height() {
        return this.layout.rowHeight;
    }

    public String value() {
        if (this.option != null || property instanceof DescriptorProperty) return null;

        Object value = property.getValue();

        if (value instanceof Boolean) return null;

        if (property instanceof NumberProperty) {
            return formatNumber((NumberProperty) property, ((NumberProperty) property).getValue());
        }

        if (property instanceof ModeProperty) {
            return String.valueOf(property.getValue());
        }

        if (property instanceof MultiModeProperty) {
            List<?> selected = (List<?>) property.getValue();

            StringBuilder joined = new StringBuilder();

            for (Object entry : selected) {
                if (joined.length() > 0) joined.append(", ");

                joined.append(entry);
            }

            return joined.toString();
        }

        if (value instanceof Integer) {
            return "[" + (this.listening ? ".." : KeyUtil.getKeyName((Integer) value)) + "]";
        }

        if (value instanceof String) return (String) value;

        return null;
    }

    public boolean hasTrack() {
        return this.option == null && property instanceof NumberProperty;
    }

    public float trackProgress() {
        if (!this.hasTrack()) return -1.0F;

        NumberProperty number = (NumberProperty) this.property;

        double span = number.getMax() - number.getMin();

        return span == 0.0D ? 0.0F : (float) ((number.getValue() - number.getMin()) / span);
    }

    public boolean isOn() {
        return this.option == null && property.getValue() instanceof Boolean && (Boolean) property.getValue();
    }

    public boolean isSelected() {
        if (this.option == null) return false;

        if (property instanceof ModeProperty) {
            return property.getValue() == this.option;
        }

        if (property instanceof MultiModeProperty) {
            return ((MultiModeProperty<?>) property).isSelected(this.option);
        }

        return false;
    }

    public void render(float panelX, float rowY, float width, boolean hovered, int accent, float alpha) {
        AtmosphereLayout l = this.layout;

        float rowHeight = this.height();

        CustomFontRenderer font = l.font();

        boolean selected = this.isSelected();

        if (selected) {
            AtmosphereDraw.fill(panelX, rowY, panelX + width, rowY + rowHeight,
                    AtmospherePalette.fade(AtmospherePalette.fade(accent, 0.22F), alpha));
        } else if (hovered) {
            AtmosphereDraw.fill(panelX, rowY, panelX + width, rowY + rowHeight,
                    AtmospherePalette.argb(AtmospherePalette.ROW_HOVER, alpha));
        }

        float left = panelX + l.padX + l.indent * this.depth;
        float right = panelX + width - l.padX - (this.isMode() ? l.indicatorInset + l.indicatorWidth : 0.0F);

        boolean track = this.hasTrack();

        float textY = AtmosphereDraw.labelY(font, rowY, track ? rowHeight - l.trackRoom : rowHeight);

        String label = this.label();
        String value = this.value();

        if (property instanceof DescriptorProperty) {
            this.renderDescriptor(font, left, right, rowY, rowHeight, label, accent, alpha);

            return;
        }

        int labelColor = selected || this.isOn() ? accent
                : hovered ? AtmospherePalette.argb(AtmospherePalette.TEXT_TITLE)
                        : AtmospherePalette.argb(AtmospherePalette.TEXT);

        float labelRoom = value == null ? right - left : right - left - l.indent - font.getStringWidth(value);

        font.drawString(AtmosphereDraw.truncate(font, label, Math.max(0.0F, labelRoom)), left, textY,
                AtmospherePalette.fade(labelColor, alpha));

        if (value != null) {
            AtmosphereDraw.value(font, value, right, textY,
                    AtmospherePalette.fade(AtmospherePalette.argb(AtmospherePalette.TEXT_VALUE), alpha));
        }

        if (this.isMode()) {
            float centreX = panelX + width - l.indicatorInset - l.indicatorWidth / 2.0F;

            int indicator = AtmospherePalette.fade(AtmospherePalette.argb(AtmospherePalette.INDICATOR), alpha);

            if (this.modeOpen) {
                AtmosphereDraw.triangleDown(centreX, rowY + (rowHeight + l.indicatorHeight) / 2.0F,
                        l.indicatorWidth, l.indicatorHeight, indicator);
            } else {
                AtmosphereDraw.triangleUp(centreX, rowY + (rowHeight - l.indicatorHeight) / 2.0F,
                        l.indicatorWidth, l.indicatorHeight, indicator);
            }
        }

        if (track) {
            this.renderTrack(left, right, rowY, rowHeight, accent, alpha);
        }
    }

    private void renderDescriptor(CustomFontRenderer font, float left, float right, float rowY,
                                  float rowHeight, String label, int accent, float alpha) {
        int color = this.focused ? accent : AtmospherePalette.argb(AtmospherePalette.TEXT_VALUE);

        String caption = AtmosphereDraw.truncate(font, label, Math.max(0.0F, right - left));

        font.drawString(caption, left, AtmosphereDraw.labelY(font, rowY, rowHeight),
                AtmospherePalette.fade(color, alpha));

        AtmosphereDraw.fill(left, rowY + rowHeight - 1.5F, left + font.getStringWidth(caption),
                rowY + rowHeight - 0.5F, AtmospherePalette.fade(accent, alpha));
    }

    private void renderTrack(float left, float right, float rowY, float rowHeight, int accent, float alpha) {
        float y = rowY + rowHeight - 2.0F;

        AtmosphereDraw.fill(left, y, right, y + 1.0F,
                AtmospherePalette.fade(AtmospherePalette.argb(AtmospherePalette.TRACK), alpha));

        float progress = Math.max(0.0F, Math.min(1.0F, this.trackProgress()));

        if (progress > 0.0F) {
            AtmosphereDraw.fill(left, y, left + (right - left) * progress, y + 1.0F,
                    AtmospherePalette.fade(accent, alpha));
        }

        float knob = left + (right - left) * progress;

        AtmosphereDraw.fill(knob, rowY + rowHeight - 3.0F, knob + 1.0F, rowY + rowHeight,
                AtmospherePalette.fade(AtmospherePalette.argb(AtmospherePalette.TEXT_TITLE), alpha));
    }

    private String label() {
        return this.option != null ? this.option.toString() : property.getLabel();
    }

    private static String formatNumber(NumberProperty property, double value) {
        double rounded = Math.round(value * 100.0D) / 100.0D;

        switch (property.getRepresentation()) {
            case INT:
                return String.valueOf((int) rounded);
            case PERCENTAGE:
                return (int) (rounded * 100) + "%";
            case MILLISECONDS:
                return (int) rounded + "ms";
            case DISTANCE:
                return rounded + "m";
            case DOUBLE:
            default:
                return String.valueOf(rounded);
        }
    }
}
