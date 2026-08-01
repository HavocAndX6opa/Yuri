package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.ESPUtils;
import ddlc.yuri.utils.render.FontUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;

@ModuleInfo(label = "Name Tags", description = "Renders name tags through walls", category = ModuleCategory.RENDER)
public class NameTagsModule extends Module {

    public final Property<Boolean> renderSelf = new Property<>("Render Self", true);
    public final Property<Boolean> background = new Property<>("Background", true);
    public final NumberProperty bgOpacity = new NumberProperty("Background Opacity", 0.4, 0.0, 1.0, 0.05, background::getValue);
    public final Property<Boolean> customFont = new Property<>("Custom Font", false);
    public final NumberProperty scale = new NumberProperty("Scale", 1.0, 0.5, 2.0, 0.1);

    private static final String CLIENT_NAME = "Yuri";
    private static final String CLIENT_TAG_PLACEHOLDER = "unlegit";
    private static final int FONT_SIZE = 16;
    private static final int BADGE_PADDING = 2;
    private static final int BADGE_GAP = 4;

    @EventHook
    public void onRender2D(Render2DEvent event) {
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) {
                continue;
            }

            double[] pos = ESPUtils.getInterpolatedPos(entity);
            if (!entity.equals(mc.thePlayer) || (mc.gameSettings.thirdPersonView != 0 && renderSelf.getValue())) {
                renderNameTag((EntityPlayer) entity, pos[0], pos[1] + entity.height + 0.2D, pos[2]);
            }
        }
    }

    private void renderNameTag(EntityPlayer player, double x, double y, double z) {
        ScaledResolution sr = new ScaledResolution(mc);

        ESPUtils.windPos.clear();
        if (!GLU.gluProject((float) x, (float) y, (float) z,
                ActiveRenderInfo.MODELVIEW,
                ActiveRenderInfo.PROJECTION,
                ActiveRenderInfo.VIEWPORT,
                ESPUtils.windPos)) {
            return;
        }

        if (ESPUtils.windPos.get(2) > 1) {
            return;
        }

        double screenX = ESPUtils.windPos.get(0) / sr.getScaleFactor();
        double screenY = sr.getScaledHeight() - (ESPUtils.windPos.get(1) / sr.getScaleFactor());

        if (screenX < 0 || screenX > sr.getScaledWidth() || screenY < 0 || screenY > sr.getScaledHeight()) {
            return;
        }

        double distance = mc.thePlayer.getDistanceToEntity(player);
        String distanceText = String.format("%.1f", distance);
        String heart = "\u2764";
        String tagText = CLIENT_TAG_PLACEHOLDER;

        int fontHeight = getFontHeight();
        int clientWidth = mc.gameSettings.thirdPersonView != 0 && renderSelf.getValue() && player.equals(mc.thePlayer) ? getStringWidth(CLIENT_NAME) : 0;
        int nameWidth = getStringWidth(player.getName());
        int distanceWidth = getStringWidth(distanceText);
        int heartWidth = mc.fontRendererObj.getStringWidth(heart);
        int gap = 4;

        double mainTextWidth = clientWidth + gap + nameWidth + gap + distanceWidth + gap + heartWidth;
        double badgeWidth = mc.gameSettings.thirdPersonView != 0 && renderSelf.getValue() && player.equals(mc.thePlayer) ? getStringWidth(tagText) + BADGE_PADDING * 2 : 0;
        double totalWidth = mainTextWidth + BADGE_GAP + badgeWidth;

        double startX = screenX - totalWidth / 2.0;
        double topY = screenY - fontHeight - 4;

        GL11.glPushMatrix();
        GL11.glTranslated(screenX, screenY, 0);
        GL11.glScaled(scale.getValue(), scale.getValue(), 1.0);
        GL11.glTranslated(-screenX, -screenY, 0);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        if (background.getValue()) {
            Gui.drawRect(startX - 2, topY - 1, startX + mainTextWidth + 2, topY + fontHeight + 1,
                    new Color(0, 0, 0, (int) (bgOpacity.getValue() * 255)).getRGB());
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        double cursorX = startX;
        if (mc.gameSettings.thirdPersonView != 0 && renderSelf.getValue() && player.equals(mc.thePlayer)) {
            drawString(CLIENT_NAME, cursorX, topY, ColorManager.getColor().getRGB());
            cursorX += clientWidth + gap;
        }

        drawString(player.getName(), cursorX, topY, Color.white.getRGB());
        cursorX += nameWidth + gap;

        drawString(distanceText, cursorX, topY, Color.white.getRGB());
        cursorX += distanceWidth + gap;

        mc.fontRendererObj.drawStringWithShadow(heart, (float) cursorX, customFont.getValue() ? (float) topY - 1f : (float) topY, Color.red.getRGB());

        if (mc.gameSettings.thirdPersonView != 0 && renderSelf.getValue() && player.equals(mc.thePlayer)) {
            double badgeX = startX + mainTextWidth + BADGE_GAP;
            drawBadge(tagText, badgeX, topY, fontHeight, badgeWidth);
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GlStateManager.resetColor();
    }

    private void drawBadge(String text, double x, double y, int fontHeight, double width) {
        int height = fontHeight + 4;

        if (background.getValue()) {
            Gui.drawRect(x, y - 1, x + width, y + height - 3,
                    new Color(0, 0, 0, (int) (bgOpacity.getValue() * 255)).getRGB());
        }

        drawString(text, x + BADGE_PADDING, y, ColorManager.getColor().getRGB());
    }

    private void drawString(String text, double x, double y, int color) {
        if (customFont.getValue()) {
            CustomFontRenderer font = FontUtils.getFont("sf", FONT_SIZE);
            font.drawString(text, (float) x, (float) y, color);
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, (float) x, (float) y, color);
        }
    }

    private int getStringWidth(String text) {
        if (customFont.getValue()) {
            return FontUtils.getFont("sf", FONT_SIZE).getStringWidth(text);
        }
        return mc.fontRendererObj.getStringWidth(text);
    }

    private int getFontHeight() {
        if (customFont.getValue()) {
            return FontUtils.getFont("sf", FONT_SIZE).getHeight();
        }
        return mc.fontRendererObj.FONT_HEIGHT;
    }
}