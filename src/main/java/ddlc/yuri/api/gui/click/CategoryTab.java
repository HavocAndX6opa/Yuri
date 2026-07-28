package ddlc.yuri.api.gui.click;

import ddlc.yuri.Yuri;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CategoryTab {

    private final ModuleCategory category;
    private float posX;
    private float posY;
    public boolean dragging;
    public boolean opened = true;
    public final List<ModuleEntry> modules = new CopyOnWriteArrayList<>();

    private int dragX;
    private int dragY;
    private float lastMouseX;
    private float lastMouseY;
    private float angle;
    private float angularVelocity;

    public CategoryTab(ModuleCategory category, float posX, float posY) {
        this.category = category;
        this.posX = posX;
        this.posY = posY;
        if (category != null) {
            for (Module module : Yuri.INSTANCE.getModuleManager().getModulesForCategory(category)) {
                modules.add(new ModuleEntry(module, this));
            }
        }
    }

    public String drawScreen(int mouseX, int mouseY, float animationProgress) {
        float offsetY = (1.0F - animationProgress) * -18.0F;
        float drawX = posX;
        float drawY = posY + offsetY;
        int headerAlpha = (int) (255 * animationProgress);

        float pivotX = drawX + 50;
        float pivotY = drawY + 7;

        GlStateManager.pushMatrix();
        GlStateManager.translate(pivotX, pivotY, 0.0F);
        GlStateManager.rotate(angle, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(-pivotX, -pivotY, 0.0F);

        Gui.drawRect(drawX - 1, drawY, drawX + 101, drawY + 15,
                RenderUtils.withAlpha(GuiTheme.PANEL, headerAlpha));

        FontUtils.getFont("sf", 21).drawStringWithShadow(
                category.getName(),
                drawX + 4,
                drawY + 4,
                new Color(255, 255, 255, headerAlpha).getRGB()
        );

        String tooltip = null;
        if (opened) {
            Gui.drawRect(drawX - 1, drawY + 15, drawX + 101, drawY + 15 + getTabHeight() + 1,
                    RenderUtils.withAlpha(GuiTheme.PANEL, headerAlpha));
            for (ModuleEntry module : modules) {
                String moduleTooltip = module.drawScreen(mouseX, mouseY, drawX, drawY, animationProgress);
                if (moduleTooltip != null) {
                    tooltip = moduleTooltip;
                }
            }
        } else {
            modules.forEach(module -> module.yPerModule = 0);
        }

        GlStateManager.popMatrix();
        return tooltip;
    }

    public void startDragging(int mouseX, int mouseY) {
        dragging = true;
        dragX = mouseX - (int) posX;
        dragY = mouseY - (int) posY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public void updatePhysics(int mouseX, int mouseY) {
        if (dragging) {
            posX = mouseX - dragX;
            posY = mouseY - dragY;
            float mouseDX = mouseX - lastMouseX;
            float targetAngle = Math.max(-14f, Math.min(14f, mouseDX * 1.25f));
            angle = MathUtils.lerp(angle, targetAngle, 0.25f);
        } else {
            float springConstant = 0.18f;
            float damping = 0.85f;
            float displacement = 0f - angle;
            angularVelocity += displacement * springConstant;
            angularVelocity *= damping;
            angle += angularVelocity;
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public int getTabHeight() {
        int height = 0;
        for (ModuleEntry module : modules) {
            height += module.yPerModule;
        }
        return height;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= posX && mouseY >= posY && mouseX <= posX + 101 && mouseY <= posY + 15;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (opened) {
            modules.forEach(module -> module.mouseReleased(mouseX, mouseY, state));
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        modules.forEach(module -> module.keyTyped(typedChar, keyCode));
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isHovered(mouseX, mouseY) && mouseButton == 1) {
            opened = !opened;
            if (opened) {
                for (ModuleEntry module : modules) {
                    module.fraction = 0;
                }
            }
        }

        if (opened) {
            for (ModuleEntry module : modules) {
                module.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public float getPosY() {
        return posY;
    }

    public float getPosX() {
        return posX;
    }

    public ModuleCategory getCategory() {
        return category;
    }
}