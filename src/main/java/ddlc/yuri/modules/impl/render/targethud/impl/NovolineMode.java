package ddlc.yuri.modules.impl.render.targethud.impl;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.impl.render.TargetHudModule;
import ddlc.yuri.modules.impl.render.targethud.TargetHudMode;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;

import java.awt.*;

public final class NovolineMode extends TargetHudMode {

    private final TargetHudModule parentModule;

    public NovolineMode(TargetHudModule parentModule) {
        super("Novoline");
        this.parentModule = parentModule;
    }

    @Override
    public int getMinWidth() { return 130; }

    @Override
    public int getHudHeight() { return 24; }

    @Override
    public int getLabelHeight() { return 0; }

    @Override
    public void draw(EntityLivingBase targetEntity, TargetHudModule.TargetState state,
                     double x, double y, long now, float delta) {

        float health = targetEntity.isEntityAlive() ? targetEntity.getHealth() : 0f;
        float maxHealth = targetEntity.getMaxHealth();

        if (state.displayHealth < 0f) {
            state.displayHealth = health;
        }
        state.displayHealth += (health - state.displayHealth) * Math.min(1f, delta * 10f);
        float healthPercentage = Math.max(0f, Math.min(1f, state.displayHealth / maxHealth));

        float currentAlpha = state.alpha;
        int borderAlpha = (int) (255 * currentAlpha);
        int textAlpha = (int) (255 * currentAlpha);

        Color baseColor = ColorManager.getColor();
        Color fadedColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int) (baseColor.getAlpha() * currentAlpha));

        GlStateManager.pushMatrix();
        // Translate to the destination position so all drawing can happen relative to (0, 0)
        GlStateManager.translate(x, y, 0);

        int headSize = 24;
        int maxL = 100;

        String name = targetEntity.getName();

        Color backgroundColor = new Color(45, 45, 45, borderAlpha);
        Color backgroundColor2 = new Color(21, 21, 21, borderAlpha);

        // Outer border (Relative to 0, 0)
        Gui.drawRect(-1, -1, 2 + headSize + maxL + 1, 2 + headSize + 2 + 1, backgroundColor2.getRGB());

        // Main background (Relative to 0, 0)
        Gui.drawRect(0, 0, 2 + headSize + maxL, 2 + headSize + 2, backgroundColor.getRGB());

        // Draw player head safely
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, borderAlpha / 255f);

        if (targetEntity instanceof AbstractClientPlayer) {
            mc.getTextureManager().bindTexture(((AbstractClientPlayer) targetEntity).getLocationSkin());
            Gui.drawScaledCustomSizeModalRect(2, 2, 8, 8, 8, 8, headSize, headSize, 64, 64);

            if (((EntityPlayer) targetEntity).func_175148_a(EnumPlayerModelParts.HAT)) {
                Gui.drawScaledCustomSizeModalRect(2, 2, 40, 8, 8, 8, headSize, headSize, 64, 64);
            }
        } else {
            // Fallback safely to mc.thePlayer if target is a Mob/NPC
            mc.getTextureManager().bindTexture(mc.thePlayer.getLocationSkin());
            Gui.drawScaledCustomSizeModalRect(2, 2, 8, 8, 8, 8, headSize, headSize, 64, 64);

            if (mc.thePlayer.func_175148_a(EnumPlayerModelParts.HAT)) {
                Gui.drawScaledCustomSizeModalRect(2, 2, 40, 8, 8, 8, headSize, headSize, 64, 64);
            }
        }
        GlStateManager.popMatrix();

        // Draw name
        int whiteWithAlpha = (textAlpha << 24) | 0xFFFFFF;
        mc.fontRendererObj.drawString(name, 2 + headSize + 2, 4, whiteWithAlpha, true);

        // Health bar coordinates (Relative to 0,0)
        int healthBarStartX = 2 + headSize + 2;
        int healthBarStartY = 4 + mc.fontRendererObj.FONT_HEIGHT + 2;
        int healthBarEndX = 2 + headSize + maxL - 2;
        int healthBarEndY = healthBarStartY + mc.fontRendererObj.FONT_HEIGHT + 2;

        String healthPercentageText = String.format("%.1f%%", healthPercentage * 100);

        // Draw health bar background
        Gui.drawRect(healthBarStartX, healthBarStartY, healthBarEndX, healthBarEndY, backgroundColor.darker().getRGB());

        // Draw filled health bar
        int filledHealthBarEndX = (int) (healthBarStartX + (healthBarEndX - healthBarStartX) * healthPercentage);
        Gui.drawRect(healthBarStartX, healthBarStartY, filledHealthBarEndX, healthBarEndY, fadedColor.getRGB());

        // Draw health percentage text centered inside the health bar
        int textWidth = mc.fontRendererObj.getStringWidth(healthPercentageText);
        int textHeight = mc.fontRendererObj.FONT_HEIGHT;

        double rectCenterX = (healthBarStartX + healthBarEndX) / 2.0;
        double rectCenterY = (healthBarStartY + healthBarEndY) / 2.0;

        double textX = rectCenterX - textWidth / 2.0;
        double textY = rectCenterY - textHeight / 2.0;

        mc.fontRendererObj.drawString(healthPercentageText, (int) textX, (int) textY + 1, new Color(255, 255, 255, textAlpha).getRGB(), true);

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }
}