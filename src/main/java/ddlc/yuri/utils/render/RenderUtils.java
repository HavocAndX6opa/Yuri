package ddlc.yuri.utils.render;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static ddlc.yuri.utils.misc.IMinecraft.mc;
import static org.lwjgl.opengl.GL11.*;

public final class RenderUtils {
    public static float delta = 0f;

    private static float scissorTransformScale = 1.0f;
    private static float scissorTransformOriginX;
    private static float scissorTransformOriginY;

    // 2D Rendering

    public static void drawGradientRect(double left, double top, double right, double bottom,
                                        boolean sideways,
                                        int startColor, int endColor) {

        float sa = (startColor >> 24 & 255) / 255F;
        float sr = (startColor >> 16 & 255) / 255F;
        float sg = (startColor >> 8 & 255) / 255F;
        float sb = (startColor & 255) / 255F;

        float ea = (endColor >> 24 & 255) / 255F;
        float er = (endColor >> 16 & 255) / 255F;
        float eg = (endColor >> 8 & 255) / 255F;
        float eb = (endColor & 255) / 255F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);

        if (sideways) {
            GlStateManager.color(sr, sg, sb, sa);
            GL11.glVertex2d(left, top);
            GL11.glVertex2d(left, bottom);

            GlStateManager.color(er, eg, eb, ea);
            GL11.glVertex2d(right, bottom);
            GL11.glVertex2d(right, top);
        } else {
            GlStateManager.color(sr, sg, sb, sa);
            GL11.glVertex2d(left, top);
            GL11.glVertex2d(right, top);

            GlStateManager.color(er, eg, eb, ea);
            GL11.glVertex2d(right, bottom);
            GL11.glVertex2d(left, bottom);
        }

        GL11.glEnd();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();

        // reset color so fonts don't inherit gradient color
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void drawCenteredGradientRect(double left, double top, double right, double bottom,
                                                int edgeColor, int centerColor) {
        // 1. Extract RGBA components for the Edge Color (Left & Right)
        float ea = (edgeColor >> 24 & 255) / 255F;
        float er = (edgeColor >> 16 & 255) / 255F;
        float eg = (edgeColor >> 8 & 255) / 255F;
        float eb = (edgeColor & 255) / 255F;

        // 2. Extract RGBA components for the Center Color (The Glow)
        float ca = (centerColor >> 24 & 255) / 255F;
        float cr = (centerColor >> 16 & 255) / 255F;
        float cg = (centerColor >> 8 & 255) / 255F;
        float cb = (centerColor & 255) / 255F;

        // Setup OpenGL states matching your OG method
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);

        // Find the exact horizontal midpoint of the notification box
        double midX = left + (right - left) / 2.0;

        // --- LEFT HALF (Fades from Edge Color to Center Color) ---
        GlStateManager.color(er, eg, eb, ea);
        GL11.glVertex2d(left, top);
        GL11.glVertex2d(left, bottom);

        GlStateManager.color(cr, cg, cb, ca);
        GL11.glVertex2d(midX, bottom);
        GL11.glVertex2d(midX, top);

        // --- RIGHT HALF (Fades from Center Color back to Edge Color) ---
        GlStateManager.color(cr, cg, cb, ca);
        GL11.glVertex2d(midX, top);
        GL11.glVertex2d(midX, bottom);

        GlStateManager.color(er, eg, eb, ea);
        GL11.glVertex2d(right, bottom);
        GL11.glVertex2d(right, top);

        GL11.glEnd();

        // Reset OpenGL states
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
    }


    public static void drawImage(ResourceLocation resourceLocation, float x, float y, float imgWidth, float imgHeight) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.getTextureManager().bindTexture(resourceLocation);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, imgWidth, imgHeight, imgWidth, imgHeight);
        GlStateManager.disableBlend();
    }

    public static void drawArrow(float x, float y, float size, int color, double rotation) {
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float alpha = (color >> 24 & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.rotate((float) rotation, 0.0F, 0.0F, 1.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(-size, -size / 2.0F);
        GL11.glVertex2f(size, 0.0F);
        GL11.glVertex2f(-size, size / 2.0F);
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public static void drawCheck(float x, float y, float size, int color) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >> 24 & 255) / 255.0F);
        GL11.glLineWidth(1.5F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + size, y + size);
        GL11.glVertex2f(x + size * 2.5F, y - size);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
    }

    public static void drawBorderedRect(float x, float y, float width, float height, final float outlineThickness, int rectColor, int outlineColor, boolean top, boolean right, boolean bottom, boolean left) {
        Gui.drawRect2(x, y, width, height, rectColor);
        glEnable(GL_LINE_SMOOTH);
        RenderUtils.color(outlineColor);

        GLUtils.setup2DRendering();

        glLineWidth(outlineThickness);
        float cornerValue = (float) (outlineThickness * .19);

        glBegin(GL_LINES);
        // left start
        glVertex2d(x, y);
        // left end
        glVertex2d(x, left ? y + height + cornerValue : y);
        // right start
        glVertex2d(x + width, y + height + cornerValue);
        // right end
        glVertex2d(x + width, right ? y - cornerValue : y + height + cornerValue);
        // top start
        glVertex2d(x, y);
        // top end
        glVertex2d(top ? x + width : x, y);
        // bottom start
        glVertex2d(x, y + height);
        // bottom end
        glVertex2d(bottom ? x + width : x, y + height);
        glEnd();

        GLUtils.end2DRendering();

        glDisable(GL_LINE_SMOOTH);
    }

    // Color Utilities

    public static void color(int color, float alpha) {
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        GlStateManager.color(r, g, b, alpha);
    }

    public static void color(int color) {
        color(color, (float) (color >> 24 & 255) / 255.0F);
    }

    public static Color astolfoColors(int yOffset, int yTotal) {
        float speed = 2900F;
        float hue = (float) (System.currentTimeMillis() % (int) speed) + ((yTotal - yOffset) * 9);
        while (hue > speed) {
            hue -= speed;
        }
        hue /= speed;
        if (hue > 0.5) {
            hue = 0.5F - (hue - 0.5f);
        }
        hue += 0.5F;
        return new Color(Color.HSBtoRGB(hue, 0.5f, 1F));
    }

    public static float interpolate(float old,
                                    float now,
                                    float partialTicks) {

        return old + (now - old) * partialTicks;
    }


    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static int applyOpacity(int color, float opacity) {
        Color old = new Color(color);
        return applyOpacity(old, opacity).getRGB();
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount),
                interpolateInt(color1.getGreen(), color2.getGreen(), amount),
                interpolateInt(color1.getBlue(), color2.getBlue(), amount),
                interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static Color interpolateColorHue(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);

        Color resultColor = Color.getHSBColor(interpolateFloat(color1HSB[0], color2HSB[0], amount),
                interpolateFloat(color1HSB[1], color2HSB[1], amount), interpolateFloat(color1HSB[2], color2HSB[2], amount));

        return applyOpacity(resultColor, interpolateInt(color1.getAlpha(), color2.getAlpha(), amount) / 255f);
    }

    public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return trueColor ? interpolateColorHue(start, end, angle / 360f) : interpolateColorC(start, end, angle / 360f);
    }

    public static int interpolateColor(int from, int to, float fraction) {
        return interpolateColor(new Color(from, true), new Color(to, true), fraction);
    }

    public static int interpolateColor(Color from, Color to, float fraction) {
        fraction = clamp(fraction, 0.0F, 1.0F);
        int red = (int) (from.getRed() + (to.getRed() - from.getRed()) * fraction);
        int green = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * fraction);
        int blue = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * fraction);
        int alpha = (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * fraction);
        return new Color(red, green, blue, alpha).getRGB();
    }

    public static int withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha).getRGB();
    }

    public static double incValue(double value, double increment) {
        if (increment <= 0.0D) {
            return value;
        }
        return Math.round(value / increment) * increment;
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    // Random GL
    public static void setAlphaLimit(float limit) {
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (limit * .01));
    }
    public static void setScissorTransform(float scale, float originX, float originY) {
        scissorTransformScale = scale;
        scissorTransformOriginX = originX;
        scissorTransformOriginY = originY;
    }

    public static void clearScissorTransform() {
        scissorTransformScale = 1.0f;
        scissorTransformOriginX = 0.0f;
        scissorTransformOriginY = 0.0f;
    }

    public static float getScissorTransformScale() {
        return scissorTransformScale;
    }

}
