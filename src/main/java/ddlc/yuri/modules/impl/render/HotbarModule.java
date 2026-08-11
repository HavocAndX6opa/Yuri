package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

@ModuleInfo(label = "Hotbar", description = "Renders a custom DDLC themed hotbar", category = ModuleCategory.RENDER)
public class HotbarModule extends Module {

    private static final Color BODY_COLOR = new Color(0, 0, 0, 80);
    private final TimerUtils stopwatch = new TimerUtils();
    private float rPosX;

    @EventHook
    public void onRender2D(Render2DEvent event) {
        renderHotbar();
    }

    public void renderHotbar() {
        if (!(mc.getRenderViewEntity() instanceof EntityPlayer)) {
            return;
        }

        final ScaledResolution sr = new ScaledResolution(mc);
        final EntityPlayer entityplayer = (EntityPlayer) mc.getRenderViewEntity();

        final int posX = (int) (sr.getScaledWidth() / 2.0F - 95);
        final int posY = (int) (sr.getScaledHeight() - 21 - 2f - 18);
        final int scaleX = 95 * 2;
        final int scaleY = 22 + 18;

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        RenderUtils.drawImage(new ResourceLocation("yuri/gui/textbox.png"), posX + 1, posY + 18, scaleX, scaleY - 18);

        for (int j = 0; j < 9; ++j) {
            final int k = sr.getScaledWidth() / 2 - 90 + j * 21 - 2;
            final int l = sr.getScaledHeight() - 16 - 3;
            renderHotBarItem(j, k, l - 1, mc.timer.renderPartialTicks, entityplayer);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
    }

    private void renderHotBarItem(final int index, final int xPos, final int yPos, final float partialTicks, final EntityPlayer entityPlayer) {
        final ItemStack itemstack = entityPlayer.inventory.mainInventory[index];
        final RenderItem itemRenderer = mc.getRenderItem();

        if (itemstack == null) {
            return;
        }

        final float f = (float) itemstack.animationsToGo - partialTicks;

        if (f > 0.0F) {
            GlStateManager.pushMatrix();
            final float f1 = 1.0F + f / 5.0F;
            GlStateManager.translate((float) (xPos + 8), (float) (yPos + 12), 0.0F);
            GlStateManager.scale(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
            GlStateManager.translate((float) (-(xPos + 8)), (float) (-(yPos + 12)), 0.0F);
        }

        if (mc.thePlayer.inventory.currentItem != index) {
            RenderHelper.enableGUIStandardItemLighting();
        }
        itemRenderer.renderItemAndEffectIntoGUI(itemstack, xPos, yPos);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (f > 0.0F) {
            GlStateManager.popMatrix();
        }

        itemRenderer.renderItemOverlays(mc.fontRendererObj, itemstack, xPos, yPos);
        if (mc.thePlayer.inventory.currentItem != index) {
            RenderHelper.disableStandardItemLighting();
        }
    }
}