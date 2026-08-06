package ddlc.yuri.api.gui.click.csgo;

import com.github.koxx12dev.fuckyou.ImGuiGL3;
import com.github.koxx12dev.fuckyou.ImGuiLwjgl2;
import ddlc.yuri.api.gui.click.novoline.GuiTheme;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.InputStream;

public final class ImGuiManager {

    private static ImGuiManager instance;

    private boolean initialized;
    private final ImGuiGL3 imGuiGl = new ImGuiGL3();
    private final ImGuiLwjgl2 imGuiLwjgl = new ImGuiLwjgl2();

    private ImGuiManager() {
    }

    public static ImGuiManager get() {
        if (instance == null) {
            instance = new ImGuiManager();
        }
        return instance;
    }

    public void init() {
        if (initialized) {
            return;
        }
        ImGui.createContext();
        imGuiLwjgl.init();

        setupStyle();
        buildFontAtlas();

        // GL3 must be initialized AFTER fonts are loaded so the texture atlas builds correctly
        imGuiGl.init("#version 120");

        initialized = true;
    }

    private void setupStyle() {
        ImGuiStyle style = ImGui.getStyle();
        style.setWindowRounding(4f);
        style.setFrameRounding(2f);
        style.setTabRounding(2f);
        style.setWindowPadding(10f, 10f);
        style.setFramePadding(6f, 4f);
        style.setItemSpacing(8f, 6f);

        setColor(ImGuiCol.WindowBg, GuiTheme.PANEL, 245);
        setColor(ImGuiCol.TitleBgActive, GuiTheme.MODULE_BG, 255);
        setColor(ImGuiCol.TitleBg, GuiTheme.MODULE_BG, 255);
        setColor(ImGuiCol.FrameBg, GuiTheme.MODULE_BG, 255);
        setColor(ImGuiCol.FrameBgHovered, GuiTheme.MODULE_HOVER, 255);
        setColor(ImGuiCol.FrameBgActive, GuiTheme.MODULE_HOVER, 255);
        setColor(ImGuiCol.CheckMark, GuiTheme.ACCENT, 255);
        setColor(ImGuiCol.SliderGrab, GuiTheme.ACCENT, 255);
        setColor(ImGuiCol.SliderGrabActive, GuiTheme.ACCENT, 255);
        setColor(ImGuiCol.Header, GuiTheme.ACCENT, 120);
        setColor(ImGuiCol.HeaderHovered, GuiTheme.ACCENT, 160);
        setColor(ImGuiCol.HeaderActive, GuiTheme.ACCENT, 200);
        setColor(ImGuiCol.Tab, GuiTheme.MODULE_BG, 255);
        setColor(ImGuiCol.TabHovered, GuiTheme.ACCENT, 160);
        setColor(ImGuiCol.TabActive, GuiTheme.ACCENT, 220);
        setColor(ImGuiCol.Text, GuiTheme.TEXT, 255);
        setColor(ImGuiCol.Button, GuiTheme.BUTTON, 180);
        setColor(ImGuiCol.ButtonHovered, GuiTheme.ACCENT, 160);
        setColor(ImGuiCol.ButtonActive, GuiTheme.ACCENT, 220);
        setColor(ImGuiCol.Border, GuiTheme.BUTTON_OUTLINE, 180);
    }

    private void setColor(int slot, java.awt.Color color, int overrideAlpha) {
        ImGui.getStyle().setColor(
                slot,
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                overrideAlpha / 255f
        );
    }

    private void buildFontAtlas() {
        ImGuiIO io = ImGui.getIO();
        ResourceLocation fontLocation = new ResourceLocation("yuri/fonts/sf.ttf");

        try (InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(fontLocation).getInputStream()) {
            byte[] bytes = readAllBytes(stream);
            ImFontConfig config = new ImFontConfig();
            config.setOversampleH(2);
            config.setOversampleV(2);
            io.getFonts().addFontFromMemoryTTF(bytes, 18f, config);
            config.destroy();
        } catch (Exception e) {
            io.getFonts().addFontDefault();
        }

        imGuiGl.updateFontsTexture();
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    public void mouseClicked(int button) {
        if (button >= 0 && button < 5) {
            ImGui.getIO().setMouseDown(button, true);
        }
    }

    public void mouseScrolled(float delta) {
        imGuiLwjgl.scrollCallback(delta);
    }

    public void charTyped(char character) {
        if (character >= 32 && character != 127) {
            imGuiLwjgl.charCallback((int) character);
        }
    }

    public void keyEvent(int lwjglKeyCode, boolean down) {
        if (lwjglKeyCode >= 0 && lwjglKeyCode < 512) {
            ImGui.getIO().setKeysDown(lwjglKeyCode, down);
        }
    }

    public boolean wantsMouse() {
        return ImGui.getIO().getWantCaptureMouse();
    }

    public boolean wantsKeyboard() {
        return ImGui.getIO().getWantCaptureKeyboard();
    }

    public void newFrame(float width, float height) {
        float delta = 1.0f / Math.max(Minecraft.getDebugFPS(), 1);
        imGuiLwjgl.newFrame(width, height, delta);
        ImGui.newFrame();
    }

    public void render() {
        ImGui.render();

        // Save Minecraft's OpenGL State
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GL20.glUseProgram(0); // Unbind Minecraft shaders

        // Render ImGui
        imGuiGl.renderDrawData(ImGui.getDrawData());

        // Restore OpenGL State for Minecraft
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }
}