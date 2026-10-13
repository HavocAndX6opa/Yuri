package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.TargetManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@ModuleInfo(label = "Armor Ratio", category = ModuleCategory.RENDER, description = "Shows a dot above your target colored by armor comparison")
public class ArmorRatioModule extends Module {

    public final NumberProperty size = new NumberProperty("Size", 0.12, 0.05, 0.5, 0.01);
    public final NumberProperty height = new NumberProperty("Height", 0.3, 0.0, 1.5, 0.05);
    public final Property<Boolean> includeWeapon = new Property<>("Include Weapon", true);
    public final Property<Boolean> outline = new Property<>("Outline", true);
    public final Property<Boolean> throughWalls = new Property<>("Through Walls", true);

    private static final Color BETTER = new Color(255, 60, 60);
    private static final Color EQUAL = new Color(255, 220, 40);
    private static final Color WORSE = new Color(60, 255, 90);

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Entity entity = TargetManager.getTarget();
        if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer || !entity.isEntityAlive()) return;

        EntityLivingBase target = (EntityLivingBase) entity;

        float pt = mc.timer.renderPartialTicks;
        double x = target.lastTickPosX + (target.posX - target.lastTickPosX) * pt;
        double y = target.lastTickPosY + (target.posY - target.lastTickPosY) * pt + target.height + height.getValue();
        double z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * pt;

        double diff = score(target) - score(mc.thePlayer);
        Color color = Math.abs(diff) < 0.01 ? EQUAL : diff < 0 ? WORSE : BETTER;

        float distance = mc.thePlayer.getDistanceToEntity(target);
        float radius = size.getValue().floatValue() * Math.max(1.0f, distance / 6.0f);

        RenderUtils.setupOrientationMatrix(x, y, z);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        if (throughWalls.getValue()) GlStateManager.disableDepth();

        if (outline.getValue()) {
            drawDot(radius * 1.3f, Color.BLACK);
        }
        drawDot(radius, color);

        GlStateManager.popMatrix();

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawDot(float radius, Color color) {
        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1.0f);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(0.0f, 0.0f);
        for (int i = 0; i <= 40; i++) {
            double angle = i * Math.PI * 2.0 / 40.0;
            GL11.glVertex2f((float) (Math.cos(angle) * radius), (float) (Math.sin(angle) * radius));
        }
        GL11.glEnd();
    }

    private double score(EntityLivingBase entity) {
        double total = 0.0;

        for (int slot = 1; slot <= 4; slot++) {
            ItemStack stack = entity.getEquipmentInSlot(slot);
            if (stack == null || !(stack.getItem() instanceof ItemArmor)) continue;
            total += ((ItemArmor) stack.getItem()).damageReduceAmount;
            total += EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
        }

        if (includeWeapon.getValue()) {
            ItemStack held = entity.getHeldItem();
            if (held != null && held.getItem() instanceof ItemSword) {
                total += ((ItemSword) held.getItem()).getDamageVsEntity();
                total += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, held) * 1.25;
            }
        }

        return total;
    }
}