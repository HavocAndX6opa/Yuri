package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.Pair;
import javafx.beans.property.BooleanProperty;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@ModuleInfo(label = "China Hat", description = "Renders a china hat on your head", category = ddlc.yuri.modules.ModuleCategory.RENDER)
public final class ChinaHatModule extends Module {

    private enum Quality {
        UMBRELLA("Umbrella", 16),
        VERY_LOW("Very Low", 32),
        LOW("Low", 64),
        NORMAL("Normal", 128),
        HIGH("High", 256),
        VERY_HIGH("Very High", 512),
        SMOOTH("Smooth", 1024);

        public final String name;
        public final int segments;

        Quality(String name, int segments) {
            this.name = name;
            this.segments = segments;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum ColorMode {
        NORMAL("Normal"),
        RAINBOW("Rainbow"),
        GRADIENT("Gradient");

        public final String name;

        ColorMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Quality> quality = new ModeProperty<>("Quality", Quality.NORMAL);
    private final ModeProperty<ColorMode> colorMode = new ModeProperty<>("Color Mode", ColorMode.RAINBOW);
    public final Property<Boolean>  showInFirstPerson = new Property<>("Show In First Person", true);
    public final Property<Boolean> rotate = new Property<>("Rotate", true);

    private static final long RAINBOW_PERIOD_MS = 3000L;

    public static long lastFrame = 0;

    @EventHook
    public void onRender3DEvent(Render3DEvent event) {
        if (mc.gameSettings.thirdPersonView == 0 && !showInFirstPerson.getValue()) {
            return;
        }

        lastFrame = System.currentTimeMillis();

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableCull();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        final double x = mc.thePlayer.lastTickPosX +
                (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * mc.timer.renderPartialTicks -
                mc.getRenderManager().viewerPosX;
        final double y = (mc.thePlayer.lastTickPosY +
                (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * mc.timer.renderPartialTicks -
                mc.getRenderManager().viewerPosY
        ) + mc.thePlayer.getEyeHeight() + 0.5 + (mc.thePlayer.isSneaking() ? -0.2 : 0);
        final double z = mc.thePlayer.lastTickPosZ +
                (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * mc.timer.renderPartialTicks -
                mc.getRenderManager().viewerPosZ;

        final double rad = 0.65f;

        int q = 64;

        boolean increaseCount = false;

        switch (quality.getValue()) {
            case UMBRELLA:
                q = 16;
                break;
            case VERY_LOW:
                q = 32;
                increaseCount = true;
                break;
            case LOW:
                increaseCount = true;
                break;
            case NORMAL:
                q = 128;
                break;
            case HIGH:
                q = 256;
                increaseCount = true;
                break;
            case VERY_HIGH:
                q = 512;
                increaseCount = true;
                break;
            case SMOOTH:
                q = 1024;
                increaseCount = true;
                break;
        }

        final double rotations = rotate.getValue() ? ((mc.thePlayer.prevRenderYawOffset +
                                                       (mc.thePlayer.renderYawOffset - mc.thePlayer.prevRenderYawOffset
                                                       ) * mc.timer.renderPartialTicks
        ) / 60
        ) + 20 : 0;

        final float timeOffset = (System.currentTimeMillis() % RAINBOW_PERIOD_MS) / (float) RAINBOW_PERIOD_MS;
        final Pair<Color, Color> gradientColors = colorMode.getValue() == ColorMode.GRADIENT ? ColorManager.getColors() : null;

        for (float i = 0; i < Math.PI * 2 + (increaseCount ? 0.01 : 0); i += (float) (Math.PI * 4 / q)) {
            final double vecX = x + rad * Math.cos(i + rotations);
            final double vecZ = z + rad * Math.sin(i + rotations);

            final float progress = (float) (i / (Math.PI * 2));
            final Color c = getColorForAngle(progress, timeOffset, gradientColors);

            GL11.glColor4f(c.getRed() / 255.F,
                    c.getGreen() / 255.F,
                    c.getBlue() / 255.F,
                    0.8f
            );

            GL11.glVertex3d(vecX, y - 0.25, vecZ);

            GL11.glColor4f(c.getRed() / 255.F,
                    c.getGreen() / 255.F,
                    c.getBlue() / 255.F,
                    0.8f
            );

            GL11.glVertex3d(x, y, z);

        }

        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GlStateManager.enableCull();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();

        GL11.glColor3f(255, 255, 255);
    }

    private Color getColorForAngle(float progress, float timeOffset, Pair<Color, Color> gradientColors) {
        switch (colorMode.getValue()) {
            case RAINBOW:
                final float hue = (progress + timeOffset) % 1.0f;
                return Color.getHSBColor(hue, 0.8f, 0.8f);
            case GRADIENT:
                return lerpColor(gradientColors.getFirst(), gradientColors.getSecond(), progress);
            default:
                return ColorManager.getColor();
        }
    }

    private static Color lerpColor(Color a, Color b, float t) {
        final float clamped = Math.max(0f, Math.min(1f, t));
        final int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * clamped);
        final int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * clamped);
        final int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * clamped);
        final int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * clamped);
        return new Color(r, g, bl, al);
    }

}