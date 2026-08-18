package ddlc.yuri.api.gui.click.neverlose;

import ddlc.yuri.Yuri;
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

    private static final float SIDEBAR_WIDTH = 200f;
    private static final float TOPBAR_HEIGHT = 50f;
    private static final float CARD_WIDTH = 260f;
    private static final float CARD_GAP = 16f;
    private static final float ROW_HEIGHT = 30f;
    private static final float SCROLL_SPEED = 50f;
    private static final float SCROLL_SMOOTHING = 0.2f;

    private final DecelerateAnimation openAnimation = new DecelerateAnimation(280, 1.0D, Direction.FORWARDS);
    private final Map<Property<?>, ImString> stringBuffers = new HashMap<>();
    private final ImString searchBuffer = new ImString(64);
    private final List<IconRequest> iconRequests = new ArrayList<>();

    private ModuleCategory selectedCategory = ModuleCategory.values()[0];
    private Property<Integer> listeningKeybind;
    @Getter
    private boolean closing;

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
        ImGui.popStyleVar();

        ImGuiManager.get().render();
        drawIconRequests();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void buildWindow() {
        ImGui.setNextWindowSize(940f, 580f, ImGuiCond.Once);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 8f);
        ImGui.begin("##yuri", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoScrollbar);

        ImVec2 windowPos = ImGui.getWindowPos();
        ImVec2 windowSize = ImGui.getWindowSize();
        ImDrawList drawList = ImGui.getWindowDrawList();

        drawList.addRectFilled(windowPos.x, windowPos.y, windowPos.x + windowSize.x, windowPos.y + windowSize.y, ImColor.rgba(13, 16, 24, 255), 8f);

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

        drawList.addRectFilled(x0, y0, x1, y1, ImColor.rgba(8, 10, 16, 255));

        ImGui.setCursorScreenPos(x0 + 22, y0 + 24);
        ImGui.pushStyleColor(ImGuiCol.Text, 1f, 1f, 1f, 1f);
        ImGui.text("Yuri");
        ImGui.popStyleColor();

        float rowY = y0 + 68;
        for (ModuleCategory category : ModuleCategory.values()) {
            boolean selected = category == selectedCategory;

            ImGui.setCursorScreenPos(x0, rowY);
            ImGui.invisibleButton("##cat" + category.name(), SIDEBAR_WIDTH, 34f);
            boolean hovered = ImGui.isItemHovered();
            if (ImGui.isItemClicked()) {
                selectedCategory = category;
                contentScrollTarget = 0f;
                contentScrollCurrent = 0f;
            }

            if (selected) {
                drawList.addRectFilled(x0, rowY, x1, rowY + 34f, ImColor.rgba(30, 40, 58, 255));
                drawList.addRectFilled(x0, rowY, x0 + 3f, rowY + 34f, ImColor.rgba(90, 160, 255, 255));
            } else if (hovered) {
                drawList.addRectFilled(x0, rowY, x1, rowY + 34f, ImColor.rgba(20, 24, 34, 255));
            }

            int textColor = selected ? ImColor.rgba(230, 240, 255, 255) : ImColor.rgba(150, 158, 175, 255);
            iconRequests.add(new IconRequest(x0 + 20f, rowY + 13f, iconFor(category), textColor));
            drawList.addText(x0 + 46f, rowY + 9f, textColor, category.getName());

            rowY += 34f;
        }
    }

    private void buildTopBar(ImDrawList drawList, ImVec2 windowPos, ImVec2 windowSize) {
        float x0 = windowPos.x + SIDEBAR_WIDTH;
        float y0 = windowPos.y;
        float x1 = windowPos.x + windowSize.x;
        float y1 = y0 + TOPBAR_HEIGHT;

        drawList.addRectFilled(x0, y0, x1, y1, ImColor.rgba(16, 19, 28, 255));
        drawList.addLine(x0, y1, x1, y1, ImColor.rgba(30, 34, 46, 255), 1f);
        drawList.addText(x0 + 24f, y0 + 16f, ImColor.rgba(230, 235, 245, 255), selectedCategory.getName());

        float searchWidth = 200f;
        ImGui.setCursorScreenPos(x1 - searchWidth - 20f, y0 + 12f);
        ImGui.setNextItemWidth(searchWidth);
        if (ImGui.inputTextWithHint("##search", "Search modules", searchBuffer)) {
            contentScrollTarget = 0f;
            contentScrollCurrent = 0f;
        }
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

        for (Module module : modules) {
            if (!query.isEmpty() && !module.getLabel().toLowerCase().contains(query)) {
                continue;
            }

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

        float headerHeight = 40f;
        float bodyHeight = ROW_HEIGHT + visible.size() * ROW_HEIGHT + 12f;
        float cardHeight = headerHeight + bodyHeight;

        drawList.addRectFilled(x, y, x + CARD_WIDTH, y + cardHeight, ImColor.rgba(18, 21, 30, 255), 6f);
        drawList.addText(x + 14f, y + 12f, ImColor.rgba(220, 226, 236, 255), module.getLabel());
        drawList.addLine(x + 14f, y + headerHeight, x + CARD_WIDTH - 14f, y + headerHeight, ImColor.rgba(32, 36, 48, 255), 1f);

        float rowY = y + headerHeight + 6f;
        rowY = buildToggleRow(drawList, "Enable " + module.getLabel(), module.isEnabled(), x, rowY, module::toggle);

        for (Property<?> property : visible) {
            rowY = buildPropertyRow(drawList, property, x, rowY);
        }

        return cardHeight;
    }

    private float buildToggleRow(ImDrawList drawList, String label, boolean enabled, float cardX, float rowY, Runnable onToggle) {
        drawList.addText(cardX + 14f, rowY + 6f, ImColor.rgba(200, 206, 218, 255), label);

        float toggleWidth = 34f;
        float toggleHeight = 18f;
        float toggleX = cardX + CARD_WIDTH - 14f - toggleWidth;

        ImGui.setCursorScreenPos(toggleX, rowY);
        ImGui.invisibleButton("##toggle" + label + rowY, toggleWidth, toggleHeight);
        if (ImGui.isItemClicked()) {
            onToggle.run();
        }

        int trackColor = enabled ? ImColor.rgba(90, 160, 255, 255) : ImColor.rgba(50, 54, 66, 255);
        drawList.addRectFilled(toggleX, rowY, toggleX + toggleWidth, rowY + toggleHeight, trackColor, toggleHeight / 2f);
        float knobX = enabled ? toggleX + toggleWidth - toggleHeight / 2f : toggleX + toggleHeight / 2f;
        drawList.addCircleFilled(knobX, rowY + toggleHeight / 2f, toggleHeight / 2f - 2f, ImColor.rgba(255, 255, 255, 255));

        return rowY + ROW_HEIGHT;
    }

    @SuppressWarnings("unchecked")
    private float buildPropertyRow(ImDrawList drawList, Property<?> property, float cardX, float rowY) {
        if (property instanceof DescriptorProperty) {
            drawList.addText(cardX + 14f, rowY + 6f, ImColor.rgba(140, 146, 158, 255), property.getLabel().toUpperCase());
            return rowY + ROW_HEIGHT;
        }

        if (property.getValue() instanceof Boolean) {
            Property<Boolean> booleanProperty = (Property<Boolean>) property;
            return buildToggleRow(drawList, property.getLabel(), booleanProperty.getValue(), cardX, rowY,
                    () -> booleanProperty.setValue(!booleanProperty.getValue()));
        }

        drawList.addText(cardX + 14f, rowY + 6f, ImColor.rgba(200, 206, 218, 255), property.getLabel());

        float controlWidth = 130f;
        float controlX = cardX + CARD_WIDTH - 14f - controlWidth;
        ImGui.setCursorScreenPos(controlX, rowY - 3f);
        ImGui.setNextItemWidth(controlWidth);

        if (property instanceof NumberProperty) {
            NumberProperty numberProperty = (NumberProperty) property;
            float[] holder = {numberProperty.getValue().floatValue()};
            if (ImGui.sliderFloat("##" + property.getLabel() + rowY, holder, (float) numberProperty.getMin(), (float) numberProperty.getMax(), "%.2f")) {
                double step = numberProperty.getIncrement();
                double val = holder[0];
                if (step > 0.0D) {
                    val = Math.round(val / step) * step;
                }
                numberProperty.setValue(Math.max(numberProperty.getMin(), Math.min(numberProperty.getMax(), val)));
            }
        } else if (property instanceof ModeProperty) {
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
            // support middle-click to start listening
            try {
                if (ImGui.isItemHovered() && ImGui.isMouseClicked(2)) {
                    listeningKeybind = listening ? null : keybindProperty;
                }
            } catch (Throwable ignored) {
            }
        }

        return rowY + ROW_HEIGHT;
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
            beginClose();
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