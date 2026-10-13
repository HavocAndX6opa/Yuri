package ddlc.yuri.api.gui.click.atmosphere;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.click.atmosphere.config.ConfigTab;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.ScaleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AtmosphereClickGui extends GuiScreen implements IMinecraft {

    private static final float OPEN_FRAMES = 12.0F;
    private static final float CLOSE_FRAMES = 5.0F;
    private static final float STAGGER = 0.06F;
    private static final float SCROLL_STEP = 12.0F * AtmosphereLayout.BASE_ROW_HEIGHT;

    public static String toolTip;

    private final List<CategoryTab> tabs = new CopyOnWriteArrayList<>();

    private AtmosphereLayout layout;

    private float progress;
    private boolean closing;

    private float laidOutWidth = -1.0F;
    private float laidOutHeight = -1.0F;

    private final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void initGui() {
        super.initGui();

        this.progress = 0.0F;
        this.closing = false;

        toolTip = null;

        this.build();

        this.laidOutWidth = -1.0F;
    }

    private void build() {
        this.tabs.clear();

        for (ModuleCategory category : ModuleCategory.values()) {
            if (Yuri.INSTANCE.getModuleManager().getModulesForCategory(category).isEmpty()) continue;

            this.tabs.add(new CategoryTab(category));
        }

        this.tabs.add(new ConfigTab());
    }

    private void layout() {
        float width = viewWidth();
        float height = viewHeight();

        if (this.layout != null && Math.abs(width - this.laidOutWidth) < 0.5F
                && Math.abs(height - this.laidOutHeight) < 0.5F) {
            return;
        }

        this.laidOutWidth = width;
        this.laidOutHeight = height;

        this.layout = new AtmosphereLayout(AtmosphereLayout.fitScale(width, this.tabs.size()));

        float x = (width - AtmosphereLayout.stripWidth(this.tabs.size(), this.layout.scale)) / 2.0F;
        float y = Math.max(3.0F, height * 0.02F);

        for (CategoryTab tab : this.tabs) {
            tab.applyLayout(this.layout, x, y);

            x += this.layout.panelWidth + this.layout.gap;
        }
    }

    private float viewWidth() {
        return new ScaledResolution(mc).getScaledWidth() / ScaleUtils.getScale(mc);
    }

    private float viewHeight() {
        return new ScaledResolution(mc).getScaledHeight() / ScaleUtils.getScale(mc);
    }

    private void advance() {
        if (this.closing) {
            this.progress -= 1.0F / CLOSE_FRAMES;

            if (this.progress <= 0.0F) {
                this.progress = 0.0F;

                mc.displayGuiScreen(null);
            }

            return;
        }

        this.progress = Math.min(1.0F, this.progress + 1.0F / OPEN_FRAMES);
    }

    private float slice(int index) {
        int last = this.tabs.size() - 1;

        float t = this.progress * (1.0F + STAGGER * last) - STAGGER * index;

        return easeOutCubic(Math.max(0.0F, Math.min(1.0F, t)));
    }

    private float alpha() {
        return Math.min(1.0F, this.progress * 2.5F);
    }

    private static float easeOutCubic(float t) {
        float inverse = 1.0F - t;

        return 1.0F - inverse * inverse * inverse;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.advance();

        if (mc.currentScreen != this) return;

        this.layout();

        int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        int scaledMouseX = scaled[0];
        int scaledMouseY = scaled[1];

        GL11.glPushMatrix();
        ScaleUtils.scale(mc);

        toolTip = null;

        float height = this.laidOutHeight;
        float alpha = this.alpha();

        for (CategoryTab tab : this.tabs) {
            tab.update(scaledMouseX, scaledMouseY);
        }

        for (int i = 0; i < this.tabs.size(); i++) {
            this.tabs.get(i).render(scaledMouseX, scaledMouseY, height, this.slice(i), alpha);
        }

        if (toolTip != null) {
            this.renderToolTip(scaledMouseX, scaledMouseY);
        }

        GL11.glPopMatrix();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderToolTip(int mouseX, int mouseY) {
        CustomFontRenderer font = this.layout.font();

        int ink = font.getHeight();

        String[] lines = toolTip.split("\n");

        float lineHeight = ink + 2.0F * this.layout.scale;

        for (int i = 0; i < lines.length; i++) {
            int width = font.getStringWidth(lines[i]);

            AtmosphereDraw.fill(mouseX + 3.0F, mouseY - 6.0F + i * lineHeight, mouseX + 6.0F + width,
                    mouseY + ink - 3.0F + i * lineHeight, AtmospherePalette.argb(AtmospherePalette.TOOLTIP));

            font.drawString(lines[i], mouseX + 5.0F, mouseY - 5.0F + i * lineHeight, 0xFFFFFFFF);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!this.closing) {
            int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);

            for (CategoryTab tab : this.tabs) {
                tab.mouseClicked(scaled[0], scaled[1], mouseButton);
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();

        if (wheel == 0 || this.closing || this.layout == null) return;

        int guiMouseX = Mouse.getEventX() * this.width / mc.displayWidth;
        int guiMouseY = this.height - Mouse.getEventY() * this.height / mc.displayHeight - 1;

        int[] scaled = ScaleUtils.getScaledMouseCoordinates(mc, guiMouseX, guiMouseY);

        float amount = (wheel > 0 ? -1.0F : 1.0F) * SCROLL_STEP * this.layout.scale;

        for (CategoryTab tab : this.tabs) {
            if (!tab.isHovered(scaled[0], scaled[1])) continue;

            tab.scrollBy(amount);

            return;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (CategoryTab tab : this.tabs) {
            tab.mouseReleased(state);
        }

        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (this.cancelInteractions()) return;

            this.beginClose();

            return;
        }

        if (this.closing || this.layout == null) return;

        float step = SCROLL_STEP * this.layout.scale;

        for (CategoryTab tab : this.tabs) {
            if (keyCode == Keyboard.KEY_UP) {
                tab.scrollBy(-step);
            } else if (keyCode == Keyboard.KEY_DOWN) {
                tab.scrollBy(step);
            } else {
                tab.keyTyped(typedChar, keyCode);
            }
        }
    }

    private boolean cancelInteractions() {
        boolean cancelled = false;

        for (CategoryTab tab : this.tabs) {
            if (tab.cancelInteraction()) cancelled = true;
        }

        return cancelled;
    }

    @Override
    public void onGuiClosed() {
        Yuri.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);

        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public void beginClose() {
        if (this.closing) return;

        this.closing = true;
    }

    public boolean isClosing() {
        return this.closing;
    }

    public List<CategoryTab> getTabs() {
        return new ArrayList<>(this.tabs);
    }
}
