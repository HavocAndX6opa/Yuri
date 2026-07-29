package ddlc.yuri.api.gui.click.exhibition;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.gui.click.exhibition.components.MainPanel;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.ScaleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExhibitionClickGui extends GuiScreen implements IMinecraft {

    private MainPanel mainPanel;
    private final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void initGui() {
        if (mainPanel == null) {
            ScaledResolution sr = new ScaledResolution(mc);
            UI theme = new ExhibitionUI();
            theme.mainConstructor(this);
            mainPanel = new MainPanel("Skeet", sr.getScaledWidth() / 2f - 170, sr.getScaledHeight() / 2f - 170, theme, this);
            mainPanel.isOpen = true;
        }

        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GL11.glPushMatrix();
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int[] scaledMouse = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        int scaledMouseX = scaledMouse[0];
        int scaledMouseY = scaledMouse[1];
        ScaleUtils.scale(mc);

        mainPanel.draw(scaledMouseX, scaledMouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
        GL11.glPopMatrix();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int[] scaledMouse = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        mainPanel.mouseClicked(scaledMouse[0], scaledMouse[1], mouseButton);

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        int[] scaledMouse = ScaleUtils.getScaledMouseCoordinates(mc, mouseX, mouseY);
        mainPanel.mouseMovedOrUp(scaledMouse[0], scaledMouse[1], state);

        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }

        mainPanel.keyPressed(keyCode);
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

    public List<Module> getModulesForCategory(String name) {
        for (ModuleCategory category : ModuleCategory.values()) {
            if (category.getName().equalsIgnoreCase(name)) {
                return Yuri.INSTANCE.getModuleManager().getModulesForCategory(category);
            }
        }
        return new ArrayList<>();
    }

    public MainPanel getMainPanel() {
        return mainPanel;
    }
}
