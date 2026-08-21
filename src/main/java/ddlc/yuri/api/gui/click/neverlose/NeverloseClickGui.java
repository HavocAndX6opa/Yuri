package ddlc.yuri.api.gui.click.neverlose;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.api.config.GithubConfigFetcher;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.DescriptorProperty;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.animations.Direction;
import ddlc.yuri.utils.render.animations.impl.DecelerateAnimation;
import ddlc.yuri.utils.render.imgui.ImGuiManager;
import ddlc.yuri.utils.render.imgui.style.ImGuiStyles;
import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NeverloseClickGui extends GuiScreen {

    private static final float MAIN_WIDTH = 940f;
    private static final float MAIN_HEIGHT = 580f;
    private static final float SIDEBAR_WIDTH = 210f;
    private static final float TOPBAR_HEIGHT = 56f;
    private static final float CARD_WIDTH = 262f;
    private static final float CARD_GAP = 16f;
    private static final float ROW_HEIGHT = 32f;
    private static final float SLIDER_ROW_HEIGHT = 42f;
    private static final float SCROLL_SPEED = 50f;
    private static final float SCROLL_SMOOTHING = 0.2f;

    private static final float GITHUB_WIDTH = 380f;
    private static final float GITHUB_HEIGHT = 440f;

    private static final int COL_BG = ImColor.rgba(9, 10, 15, 255);
    private static final int COL_BG_GLOW = ImColor.rgba(114, 137, 255, 20);
    private static final int COL_SIDEBAR = ImColor.rgba(12, 13, 19, 255);
    private static final int COL_TOPBAR = ImColor.rgba(13, 15, 22, 255);
    private static final int COL_DIVIDER = ImColor.rgba(255, 255, 255, 12);
    private static final int COL_CARD = ImColor.rgba(17, 19, 27, 255);
    private static final int COL_CARD_BORDER = ImColor.rgba(255, 255, 255, 10);
    private static final int COL_SHADOW = ImColor.rgba(0, 0, 0, 130);
    private static final int COL_ACCENT = ImColor.rgba(114, 137, 255, 255);
    private static final int COL_ACCENT_DIM = ImColor.rgba(114, 137, 255, 45);
    private static final int COL_ACCENT_SOFT = ImColor.rgba(114, 137, 255, 90);
    private static final int COL_TEXT = ImColor.rgba(232, 235, 242, 255);
    private static final int COL_TEXT_MUTED = ImColor.rgba(128, 134, 148, 255);
    private static final int COL_TEXT_DIM = ImColor.rgba(88, 93, 106, 255);
    private static final int COL_TRACK_OFF = ImColor.rgba(42, 45, 56, 255);
    private static final int COL_ROW_HOVER = ImColor.rgba(255, 255, 255, 12);
    private static final int COL_ROW_ACTIVE = ImColor.rgba(114, 137, 255, 28);

    private final DecelerateAnimation openAnimation = new DecelerateAnimation(280, 1.0D, Direction.FORWARDS);
    private final Map<Property<?>, ImString> stringBuffers = new HashMap<>();
    private final ImString searchBuffer = new ImString(64);
    private final List<IconRequest> iconRequests = new ArrayList<>();
    private final List<String> remoteConfigs = new ArrayList<>();

    private ModuleCategory selectedCategory = ModuleCategory.values()[0];
    private Property<Integer> listeningKeybind;
    @Getter
    private boolean closing;
    private boolean githubOpen;

    private float contentScrollTarget;
    private float contentScrollCurrent;
    private float lastContentHeight;

    @Override
    public void initGui() {
        ImGuiManager.get().init(ImGuiStyles.regular());
        openAnimation.setDirection(Direction.FORWARDS);
        openAnimation.reset();
        closing = false;
        super.initGui();
    }

    @Override
    public void onGuiClosed() {
        Yuri.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float progress = openAnimation.getOutput().floatValue();

        if (closing && openAnimation.finished(Direction.BACKWARDS)) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }

        iconRequests.clear();
        ImGuiManager.get().newFrame(mc.displayWidth, mc.displayHeight);

        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, progress);
        buildWindow();
        if (githubOpen) {
            buildGithubPanel();
        }
        ImGui.popStyleVar();

        ImGuiManager.get().render();
        drawIconRequests();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void buildWindow() {
        float x0 = (mc.displayWidth - MAIN_WIDTH) / 2f;
        float y0 = (mc.displayHeight - MAIN_HEIGHT) / 2f;

        ImDrawList bg = ImGui.getBackgroundDrawList();
        bg.addRectFilled(x0 - 6f, y0 - 6f, x0 + MAIN_WIDTH + 6f, y0 + MAIN_HEIGHT + 10f, COL_SHADOW, 14f);

        ImGui.setNextWindowPos(x0, y0, ImGuiCond.Always);
        ImGui.setNextWindowSize(MAIN_WIDTH, MAIN_HEIGHT, ImGuiCond.Always);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 10f);
        ImGui.begin("##yuri", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoMove);

        ImVec2 windowPos = ImGui.getWindowPos();
        ImVec2 windowSize = ImGui.getWindowSize();
        ImDrawList drawList = ImGui.getWindowDrawList();

        drawList.addRectFilled(windowPos.x, windowPos.y, windowPos.x + windowSize.x, windowPos.y + windowSize.y, COL_BG, 10f);
        drawList.addRectFilledMultiColor(windowPos.x, windowPos.y, windowPos.x + windowSize.x, windowPos.y + 120f,
                COL_BG_GLOW, COL_BG_GLOW, 0, 0);
        drawList.addRect(windowPos.x, windowPos.y, windowPos.x + windowSize.x, windowPos.y + windowSize.y, COL_CARD_BORDER, 10f);

        buildSidebar(drawList, windowPos, windowSize);
        buildTopBar(drawList, windowPos, windowSize);
        buildContent(windowPos, windowSize);

        ImGui.end();
        ImGui.popStyleVar();
    }

    private void buildSidebar(ImDrawList drawList, ImVec2 windowPos, ImVec2 windowSize) {
        float x0 = windowPos.x;
        float y0 = windowPos.y;
        float x1 = x0 + SIDEBAR_WIDTH;
        float y1 = y0 + windowSize.y;

        drawList.addRectFilled(x0, y0, x1, y1, COL_SIDEBAR);
        drawList.addLine(x1, y0, x1, y1, COL_DIVIDER, 1f);

        drawList.addRectFilled(x0 + 22f, y0 + 24f, x0 + 34f, y0 + 36f, COL_ACCENT, 3f);
        drawList.addText(x0 + 46f, y0 + 22f, COL_TEXT, "Yuri");
        drawList.addText(x0 + 22f, y0 + 46f, COL_TEXT_DIM, "1.8.9 client");

        float rowY = y0 + 78f;
        for (ModuleCategory category : ModuleCategory.values()) {
            boolean selected = category == selectedCategory;

            ImGui.setCursorScreenPos(x0 + 10f, rowY);
            ImGui.invisibleButton("##cat" + category.name(), SIDEBAR_WIDTH - 20f, 36f);
            boolean hovered = ImGui.isItemHovered();
            if (ImGui.isItemClicked()) {
                selectedCategory = category;
                contentScrollTarget = 0f;
                contentScrollCurrent = 0f;
            }

            float rx0 = x0 + 10f;
            float rx1 = x1 - 10f;
            if (selected) {
                drawList.addRectFilled(rx0, rowY, rx1, rowY + 36f, COL_ROW_ACTIVE, 6f);
                drawList.addRectFilled(x0, rowY + 6f, x0 + 3f, rowY + 30f, COL_ACCENT, 2f);
            } else if (hovered) {
                drawList.addRectFilled(rx0, rowY, rx1, rowY + 36f, COL_ROW_HOVER, 6f);
            }

            int iconBg = selected ? COL_ACCENT_DIM : ImColor.rgba(255, 255, 255, 6);
            drawList.addRectFilled(rx0 + 10f, rowY + 8f, rx0 + 30f, rowY + 28f, iconBg, 5f);

            int textColor = selected ? COL_TEXT : COL_TEXT_MUTED;
            iconRequests.add(new IconRequest(rx0 + 15f, rowY + 12f, iconFor(category), textColor));
            drawList.addText(rx0 + 40f, rowY + 11f, textColor, category.getName());

            rowY += 38f;
        }
    }

    private void buildTopBar(ImDrawList drawList, ImVec2 windowPos, ImVec2 windowSize) {
        float x0 = windowPos.x + SIDEBAR_WIDTH;
        float y0 = windowPos.y;
        float x1 = windowPos.x + windowSize.x;
        float y1 = y0 + TOPBAR_HEIGHT;

        drawList.addRectFilled(x0, y0, x1, y1, COL_TOPBAR);
        drawList.addLine(x0, y1, x1, y1, COL_DIVIDER, 1f);

        drawList.addText(x0 + 24f, y0 + 15f, COL_TEXT, selectedCategory.getName());
        int enabledCount = 0;
        List<Module> categoryModules = Yuri.INSTANCE.getModuleManager().getModulesForCategory(selectedCategory);
        for (Module module : categoryModules) {
            if (module.isEnabled()) {
                enabledCount++;
            }
        }
        drawList.addText(x0 + 24f, y0 + 33f, COL_TEXT_DIM, enabledCount + " / " + categoryModules.size() + " enabled");

        float configsWidth = 96f;
        float configsX = x1 - configsWidth - 20f;
        ImGui.setCursorScreenPos(configsX, y0 + 14f);
        ImGui.invisibleButton("##openGithub", configsWidth, 28f);
        boolean configsHovered = ImGui.isItemHovered();
        if (ImGui.isItemClicked()) {
            githubOpen = true;
            remoteConfigs.clear();
            remoteConfigs.addAll(GithubConfigFetcher.listRemoteConfigs());
        }
        int configsBg = configsHovered ? COL_ACCENT_SOFT : COL_ACCENT_DIM;
        drawList.addRectFilled(configsX, y0 + 14f, configsX + configsWidth, y0 + 42f, configsBg, 6f);
        drawList.addRect(configsX, y0 + 14f, configsX + configsWidth, y0 + 42f, COL_ACCENT_SOFT, 6f);
        drawList.addText(configsX + 14f, y0 + 21f, COL_TEXT, "Configs");

        float searchWidth = 200f;
        ImGui.setCursorScreenPos(configsX - searchWidth - 14f, y0 + 14f);
        ImGui.setNextItemWidth(searchWidth);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, 0.09f, 0.10f, 0.14f, 1f);
        ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, 0.11f, 0.12f, 0.17f, 1f);
        ImGui.pushStyleColor(ImGuiCol.FrameBgActive, 0.12f, 0.13f, 0.19f, 1f);
        ImGui.pushStyleColor(ImGuiCol.Text, 0.91f, 0.92f, 0.95f, 1f);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6f);
        if (ImGui.inputTextWithHint("##search", "Search modules", searchBuffer)) {
            contentScrollTarget = 0f;
            contentScrollCurrent = 0f;
        }
        ImGui.popStyleVar();
        ImGui.popStyleColor(4);
    }

    private void buildContent(ImVec2 windowPos, ImVec2 windowSize) {
        float regionX = windowPos.x + SIDEBAR_WIDTH;
        float regionY = windowPos.y + TOPBAR_HEIGHT;
        float regionWidth = windowPos.x + windowSize.x - regionX - 20f;
        float regionHeight = windowPos.y + windowSize.y - regionY - 20f;

        ImGui.setCursorScreenPos(regionX + 4f, regionY + 4f);
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0f, 0f, 0f, 0f);
        ImGui.beginChild("##content", regionWidth, regionHeight, false,
                ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        boolean hovered = ImGui.isWindowHovered();
        float wheel = ImGui.getIO().getMouseWheel();
        if (hovered && wheel != 0f) {
            contentScrollTarget -= wheel * SCROLL_SPEED;
        }

        float maxScroll = Math.max(0f, lastContentHeight - regionHeight);
        contentScrollTarget = Math.max(0f, Math.min(maxScroll, contentScrollTarget));
        contentScrollCurrent += (contentScrollTarget - contentScrollCurrent) * SCROLL_SMOOTHING;
        if (Math.abs(contentScrollTarget - contentScrollCurrent) < 0.1f) {
            contentScrollCurrent = contentScrollTarget;
        }

        float startX = regionX + 20f;
        float startY = regionY + 16f - contentScrollCurrent;

        List<Module> modules = Yuri.INSTANCE.getModuleManager().getModulesForCategory(selectedCategory);
        String query = searchBuffer.get().trim().toLowerCase();

        int columns = Math.max(1, (int) ((regionWidth - 20f) / (CARD_WIDTH + CARD_GAP)));
        float[] columnY = new float[Math.max(columns, 2)];
        for (int i = 0; i < columnY.length; i++) {
            columnY[i] = startY;
        }

        boolean anyVisible = false;
        for (Module module : modules) {
            if (!query.isEmpty() && !module.getLabel().toLowerCase().contains(query)) {
                continue;
            }
            anyVisible = true;

            int column = 0;
            for (int i = 1; i < columns; i++) {
                if (columnY[i] < columnY[column]) {
                    column = i;
                }
            }

            float cardX = startX + column * (CARD_WIDTH + CARD_GAP);
            float cardY = columnY[column];

            float cardHeight = buildCard(module, cardX, cardY);
            columnY[column] = cardY + cardHeight + CARD_GAP;
        }

        if (!anyVisible) {
            ImGui.getWindowDrawList().addText(startX, startY + 4f, COL_TEXT_DIM, "No modules match your search");
        }

        float tallestColumn = startY;
        for (int i = 0; i < columns; i++) {
            tallestColumn = Math.max(tallestColumn, columnY[i]);
        }
        lastContentHeight = tallestColumn - (regionY + 16f - contentScrollCurrent) + contentScrollCurrent;

        ImGui.endChild();
        ImGui.popStyleColor();
    }

    private float buildCard(Module module, float x, float y) {
        ImDrawList drawList = ImGui.getWindowDrawList();

        List<Property<?>> visible = new ArrayList<>();
        for (Property<?> property : module.getElements()) {
            if (property.isAvailable()) {
                visible.add(property);
            }
        }

        float headerHeight = 42f;
        float bodyHeight = ROW_HEIGHT + 10f;
        for (Property<?> property : visible) {
            bodyHeight += rowHeightFor(property);
        }
        float cardHeight = headerHeight + bodyHeight;

        boolean enabled = module.isEnabled();
        drawList.addRectFilled(x, y, x + CARD_WIDTH, y + cardHeight, COL_CARD, 8f);
        drawList.addRect(x, y, x + CARD_WIDTH, y + cardHeight, COL_CARD_BORDER, 8f);
        if (enabled) {
            drawList.addRectFilled(x, y, x + 3f, y + cardHeight, COL_ACCENT, 2f);
        }

        int dotColor = enabled ? COL_ACCENT : COL_TRACK_OFF;
        drawList.addCircleFilled(x + 18f, y + 21f, 4f, dotColor);
        drawList.addText(x + 30f, y + 13f, COL_TEXT, module.getLabel());
        drawList.addLine(x + 14f, y + headerHeight, x + CARD_WIDTH - 14f, y + headerHeight, COL_DIVIDER, 1f);

        float rowY = y + headerHeight + 6f;
        rowY = buildToggleRow(drawList, "Enable module", enabled, x, rowY, module::toggle);

        for (Property<?> property : visible) {
            rowY = buildPropertyRow(drawList, property, x, rowY);
        }

        return cardHeight;
    }

    private float rowHeightFor(Property<?> property) {
        if (property instanceof NumberProperty) {
            return SLIDER_ROW_HEIGHT;
        }
        return ROW_HEIGHT;
    }

    private float buildToggleRow(ImDrawList drawList, String label, boolean enabled, float cardX, float rowY, Runnable onToggle) {
        drawList.addText(cardX + 14f, rowY + 6f, COL_TEXT_MUTED, label);

        float toggleWidth = 34f;
        float toggleHeight = 18f;
        float toggleX = cardX + CARD_WIDTH - 14f - toggleWidth;

        ImGui.setCursorScreenPos(toggleX, rowY);
        ImGui.invisibleButton("##toggle" + label + rowY, toggleWidth, toggleHeight);
        if (ImGui.isItemClicked()) {
            onToggle.run();
        }

        if (enabled) {
            drawList.addCircleFilled(toggleX + toggleWidth - toggleHeight / 2f, rowY + toggleHeight / 2f, toggleHeight, COL_ACCENT_DIM);
        }

        int trackColor = enabled ? COL_ACCENT : COL_TRACK_OFF;
        drawList.addRectFilled(toggleX, rowY, toggleX + toggleWidth, rowY + toggleHeight, trackColor, toggleHeight / 2f);
        float knobX = enabled ? toggleX + toggleWidth - toggleHeight / 2f : toggleX + toggleHeight / 2f;
        drawList.addCircleFilled(knobX, rowY + toggleHeight / 2f, toggleHeight / 2f - 2f, ImColor.rgba(255, 255, 255, 255));

        return rowY + ROW_HEIGHT;
    }

    @SuppressWarnings("unchecked")
    private float buildPropertyRow(ImDrawList drawList, Property<?> property, float cardX, float rowY) {
        if (property instanceof DescriptorProperty) {
            drawList.addText(cardX + 14f, rowY + 6f, COL_TEXT_DIM, property.getLabel().toUpperCase());
            return rowY + ROW_HEIGHT;
        }

        if (property.getValue() instanceof Boolean) {
            Property<Boolean> booleanProperty = (Property<Boolean>) property;
            return buildToggleRow(drawList, property.getLabel(), booleanProperty.getValue(), cardX, rowY,
                    () -> booleanProperty.setValue(!booleanProperty.getValue()));
        }

        if (property instanceof NumberProperty) {
            return buildSliderRow(drawList, (NumberProperty) property, cardX, rowY);
        }

        drawList.addText(cardX + 14f, rowY + 6f, COL_TEXT_MUTED, property.getLabel());

        float controlWidth = 130f;
        float controlX = cardX + CARD_WIDTH - 14f - controlWidth;
        ImGui.setCursorScreenPos(controlX, rowY - 3f);
        ImGui.setNextItemWidth(controlWidth);

        ImGui.pushStyleColor(ImGuiCol.FrameBg, 0.10f, 0.11f, 0.15f, 1f);
        ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, 0.12f, 0.13f, 0.18f, 1f);
        ImGui.pushStyleColor(ImGuiCol.FrameBgActive, 0.14f, 0.15f, 0.21f, 1f);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.10f, 0.11f, 0.15f, 1f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.14f, 0.17f, 0.27f, 1f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.20f, 0.24f, 0.40f, 1f);
        ImGui.pushStyleColor(ImGuiCol.CheckMark, 0.45f, 0.54f, 1f, 1f);
        ImGui.pushStyleColor(ImGuiCol.Text, 0.91f, 0.92f, 0.95f, 1f);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 5f);

        if (property instanceof ModeProperty) {
            ModeProperty<?> modeProperty = (ModeProperty<?>) property;
            Enum<?>[] values = modeProperty.getValues();
            String[] names = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                names[i] = values[i].toString();
            }
            ImInt current = new ImInt(modeProperty.getValue().ordinal());
            if (ImGui.combo("##" + property.getLabel() + rowY, current, names)) {
                modeProperty.setValue(current.get());
            }
        } else if (property instanceof MultiModeProperty) {
            MultiModeProperty<?> multiModeProperty = (MultiModeProperty<?>) property;
            if (ImGui.beginCombo("##" + property.getLabel() + rowY, "Select")) {
                Enum<?>[] values = multiModeProperty.getValues();
                for (int i = 0; i < values.length; i++) {
                    Enum<?> value = values[i];
                    ImBoolean selected = new ImBoolean(multiModeProperty.isSelected(value));
                    if (ImGui.checkbox(value.toString(), selected)) {
                        multiModeProperty.setValue(i);
                    }
                }
                ImGui.endCombo();
            }
        } else if (property.getValue() instanceof String) {
            Property<String> stringProperty = (Property<String>) property;
            ImString buffer = stringBuffers.computeIfAbsent(property, p -> new ImString(stringProperty.getValue(), 256));
            if (ImGui.inputText("##" + property.getLabel() + rowY, buffer)) {
                stringProperty.setValue(buffer.get());
            }
        } else if (property.getValue() instanceof Integer) {
            Property<Integer> keybindProperty = (Property<Integer>) property;
            boolean listening = listeningKeybind == keybindProperty;
            String label = listening ? ".." : Keyboard.getKeyName(keybindProperty.getValue());
            if (ImGui.button(label + "##" + property.getLabel() + rowY, controlWidth, 0f)) {
                listeningKeybind = listening ? null : keybindProperty;
            }
            try {
                if (ImGui.isItemHovered() && ImGui.isMouseClicked(2)) {
                    listeningKeybind = listening ? null : keybindProperty;
                }
            } catch (Throwable ignored) {
            }
        }

        ImGui.popStyleVar();
        ImGui.popStyleColor(8);

        return rowY + ROW_HEIGHT;
    }

    private float buildSliderRow(ImDrawList drawList, NumberProperty property, float cardX, float rowY) {
        drawList.addText(cardX + 14f, rowY + 2f, COL_TEXT_MUTED, property.getLabel());

        double value = property.getValue().doubleValue();
        String valueText = String.format("%.2f", value);
        float valueWidth = ImGui.calcTextSize(valueText).x;
        drawList.addText(cardX + CARD_WIDTH - 14f - valueWidth, rowY + 2f, COL_TEXT, valueText);

        float trackX0 = cardX + 14f;
        float trackX1 = cardX + CARD_WIDTH - 14f;
        float trackY = rowY + 22f;
        float trackHeight = 6f;

        ImGui.setCursorScreenPos(trackX0, trackY - 6f);
        ImGui.invisibleButton("##slider" + property.getLabel() + rowY, trackX1 - trackX0, 18f);
        boolean active = ImGui.isItemActive();
        boolean hovered = ImGui.isItemHovered();

        if (active) {
            float mouseX = ImGui.getMousePosX();
            double ratio = (mouseX - trackX0) / (trackX1 - trackX0);
            ratio = Math.max(0.0, Math.min(1.0, ratio));
            double range = property.getMax() - property.getMin();
            double newValue = property.getMin() + ratio * range;
            double step = property.getIncrement();
            if (step > 0.0D) {
                newValue = Math.round(newValue / step) * step;
            }
            newValue = Math.max(property.getMin(), Math.min(property.getMax(), newValue));
            property.setValue(newValue);
            value = newValue;
        }

        double fillRatio = (value - property.getMin()) / (property.getMax() - property.getMin());
        float fillX = trackX0 + (float) fillRatio * (trackX1 - trackX0);

        drawList.addRectFilled(trackX0, trackY, trackX1, trackY + trackHeight, COL_TRACK_OFF, trackHeight / 2f);
        drawList.addRectFilled(trackX0, trackY, fillX, trackY + trackHeight, COL_ACCENT, trackHeight / 2f);

        int knobColor = active || hovered ? ImColor.rgba(255, 255, 255, 255) : ImColor.rgba(225, 228, 236, 255);
        if (active) {
            drawList.addCircleFilled(fillX, trackY + trackHeight / 2f, 9f, COL_ACCENT_DIM);
        }
        drawList.addCircleFilled(fillX, trackY + trackHeight / 2f, 5.5f, knobColor);

        return rowY + SLIDER_ROW_HEIGHT;
    }

    private void buildGithubPanel() {
        float x0 = (mc.displayWidth - GITHUB_WIDTH) / 2f;
        float y0 = (mc.displayHeight - GITHUB_HEIGHT) / 2f;

        ImDrawList bg = ImGui.getBackgroundDrawList();
        bg.addRectFilled(0, 0, mc.displayWidth, mc.displayHeight, ImColor.rgba(0, 0, 0, 90));
        bg.addRectFilled(x0 - 6f, y0 - 6f, x0 + GITHUB_WIDTH + 6f, y0 + GITHUB_HEIGHT + 10f, COL_SHADOW, 12f);

        ImGui.setNextWindowPos(x0, y0, ImGuiCond.Always);
        ImGui.setNextWindowSize(GITHUB_WIDTH, GITHUB_HEIGHT, ImGuiCond.Always);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 10f);
        ImGui.pushStyleColor(ImGuiCol.ScrollbarBg, 0f, 0f, 0f, 0f);
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrab, 0.24f, 0.27f, 0.36f, 1f);
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrabHovered, 0.32f, 0.36f, 0.48f, 1f);
        ImGui.begin("##githubconfigs", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoMove);

        ImVec2 pos = ImGui.getWindowPos();
        ImVec2 size = ImGui.getWindowSize();
        ImDrawList drawList = ImGui.getWindowDrawList();

        drawList.addRectFilled(pos.x, pos.y, pos.x + size.x, pos.y + size.y, COL_BG, 10f);
        drawList.addRect(pos.x, pos.y, pos.x + size.x, pos.y + size.y, COL_CARD_BORDER, 10f);

        float headerHeight = 52f;
        drawList.addRectFilled(pos.x, pos.y, pos.x + size.x, pos.y + headerHeight, COL_TOPBAR, 10f);
        drawList.addLine(pos.x, pos.y + headerHeight, pos.x + size.x, pos.y + headerHeight, COL_DIVIDER, 1f);
        drawList.addText(pos.x + 20f, pos.y + 18f, COL_TEXT, "Github Configs");

        float closeSize = 26f;
        float closeX = pos.x + size.x - closeSize - 14f;
        float closeY = pos.y + 13f;
        ImGui.setCursorScreenPos(closeX, closeY);
        ImGui.invisibleButton("##closeGithub", closeSize, closeSize);
        boolean closeHovered = ImGui.isItemHovered();
        if (ImGui.isItemClicked()) {
            githubOpen = false;
        }
        drawList.addRectFilled(closeX, closeY, closeX + closeSize, closeY + closeSize,
                closeHovered ? ImColor.rgba(255, 80, 90, 60) : ImColor.rgba(255, 255, 255, 8), 6f);
        drawList.addLine(closeX + 8f, closeY + 8f, closeX + closeSize - 8f, closeY + closeSize - 8f, COL_TEXT, 1.5f);
        drawList.addLine(closeX + closeSize - 8f, closeY + 8f, closeX + 8f, closeY + closeSize - 8f, COL_TEXT, 1.5f);

        float refreshWidth = 74f;
        float refreshX = closeX - refreshWidth - 10f;
        ImGui.setCursorScreenPos(refreshX, closeY);
        ImGui.invisibleButton("##refreshGithub", refreshWidth, closeSize);
        boolean refreshHovered = ImGui.isItemHovered();
        if (ImGui.isItemClicked()) {
            remoteConfigs.clear();
            remoteConfigs.addAll(GithubConfigFetcher.listRemoteConfigs());
        }
        drawList.addRectFilled(refreshX, closeY, refreshX + refreshWidth, closeY + closeSize,
                refreshHovered ? COL_ACCENT_SOFT : COL_ACCENT_DIM, 6f);
        drawList.addText(refreshX + 16f, closeY + 6f, COL_TEXT, "Refresh");

        ImGui.setCursorScreenPos(pos.x + 4f, pos.y + headerHeight + 4f);
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0f, 0f, 0f, 0f);
        ImGui.beginChild("##githubList", size.x - 8f, size.y - headerHeight - 16f, false);

        if (remoteConfigs.isEmpty()) {
            ImVec2 cursor = ImGui.getCursorScreenPos();
            ImGui.getWindowDrawList().addText(cursor.x + 16f, cursor.y + 10f, COL_TEXT_DIM, "No configs found, hit refresh");
        } else {
            for (String path : remoteConfigs) {
                String name = new java.io.File(path).getName();
                ImGui.invisibleButton("##cfg" + path, size.x - 16f, 40f);
                boolean rowHovered = ImGui.isItemHovered();
                boolean rowClicked = ImGui.isItemClicked();

                ImVec2 minR = ImGui.getItemRectMin();
                ImVec2 maxR = ImGui.getItemRectMax();
                ImDrawList childDraw = ImGui.getWindowDrawList();
                if (rowHovered) {
                    childDraw.addRectFilled(minR.x, minR.y, maxR.x, maxR.y, COL_ROW_HOVER, 6f);
                }
                childDraw.addCircleFilled(minR.x + 14f, minR.y + 20f, 3f, COL_ACCENT);
                childDraw.addText(minR.x + 28f, minR.y + 13f, COL_TEXT, name);

                String hint = "Click to install";
                float hintWidth = ImGui.calcTextSize(hint).x;
                childDraw.addText(maxR.x - hintWidth - 14f, minR.y + 13f, COL_TEXT_DIM, hint);

                if (rowClicked) {
                    float cx = mc.displayWidth / 2f;
                    float cy = 30f;
                    ddlc.yuri.utils.render.progress.ProgressBarEntry entry = ddlc.yuri.managers.impl.ProgressBarManager.add(0f, cx, cy);
                    new Thread(() -> {
                        boolean ok = GithubConfigFetcher.downloadRemoteConfigWithProgress(path, entry);
                        if (ok) {
                            ConfigManager.getInstance().loadConfig(name);
                        }
                        ddlc.yuri.managers.impl.ProgressBarManager.remove(entry);
                    }, "yuri-config-download").start();
                }
            }
        }

        ImGui.endChild();
        ImGui.popStyleColor();

        ImGui.end();
        ImGui.popStyleColor(3);
        ImGui.popStyleVar();
    }

    private void drawIconRequests() {
        if (iconRequests.isEmpty()) {
            return;
        }

        CustomFontRenderer icons = FontUtils.getFont("icons", 18);
        ScaledResolution sr = new ScaledResolution(mc);
        float scale = (float) sr.getScaleFactor();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (IconRequest request : iconRequests) {
            icons.drawString(request.glyph, request.x / scale, request.y / scale, request.color);
        }

        GL11.glDisable(GL11.GL_BLEND);
    }

    private String iconFor(ModuleCategory category) {
        if (category.name().equalsIgnoreCase("Combat")) {
            return "D";
        } else if (category.name().equalsIgnoreCase("Movement")) {
            return "A";
        } else if (category.name().equalsIgnoreCase("Player")) {
            return "B";
        } else if (category.name().equalsIgnoreCase("Render")) {
            return "C";
        } else if (category.name().equalsIgnoreCase("Misc")) {
            return "F";
        }
        return "F";
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        ImGuiManager.get().mouseClicked(mouseButton);
        if (!ImGuiManager.get().wantsMouse()) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state >= 0 && state < 5) {
            ImGui.getIO().setMouseDown(state, false);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            ImGuiManager.get().mouseScrolled(Math.signum(wheel));
        }
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        if (Keyboard.getEventKey() != Keyboard.KEY_NONE) {
            ImGuiManager.get().keyEvent(Keyboard.getEventKey(), Keyboard.getEventKeyState());
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listeningKeybind != null) {
            listeningKeybind.setValue(keyCode);
            listeningKeybind = null;
            return;
        }

        ImGuiManager.get().charTyped(typedChar);

        if (keyCode == Keyboard.KEY_ESCAPE && !ImGuiManager.get().wantsKeyboard()) {
            if (githubOpen) {
                githubOpen = false;
            } else {
                beginClose();
            }
        }
    }

    public void beginClose() {
        if (closing) {
            return;
        }
        closing = true;
        openAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.reset();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static final class IconRequest {
        private final float x;
        private final float y;
        private final String glyph;
        private final int color;

        private IconRequest(float x, float y, String glyph, int color) {
            this.x = x;
            this.y = y;
            this.glyph = glyph;
            this.color = color;
        }
    }
}