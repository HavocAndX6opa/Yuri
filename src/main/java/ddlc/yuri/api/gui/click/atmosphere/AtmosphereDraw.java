package ddlc.yuri.api.gui.click.atmosphere;

import ddlc.yuri.api.font.CustomFontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public final class AtmosphereDraw {

    public static void fill(float x, float y, float x1, float y1, int argb) {
        if (argb >>> 24 == 0 || x1 <= x || y1 <= y) return;

        Gui.drawRect(Math.round(x), Math.round(y), Math.round(x1), Math.round(y1), argb);
    }

    public static void outline(float x, float y, float x1, float y1, int argb) {
        int left = Math.round(x);
        int top = Math.round(y);
        int right = Math.round(x1);
        int bottom = Math.round(y1);

        fill(left, top, right, top + 1, argb);
        fill(left, bottom - 1, right, bottom, argb);
        fill(left, top + 1, left + 1, bottom - 1, argb);
        fill(right - 1, top + 1, right, bottom - 1, argb);
    }

    public static void triangleUp(float centreX, float top, float width, float height, int argb) {
        if (width <= 0.0F || height <= 0.0F || (argb >>> 24) == 0) return;

        float half = width / 2.0F;
        drawSmoothTriangle(centreX - half, top + height, centreX + half, top + height, centreX, top, argb);
    }

    public static void triangleDown(float centreX, float bottom, float width, float height, int argb) {
        if (width <= 0.0F || height <= 0.0F || (argb >>> 24) == 0) return;

        float half = width / 2.0F;
        drawSmoothTriangle(centreX - half, bottom - height, centreX + half, bottom - height, centreX, bottom, argb);
    }

    private static void drawSmoothTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        float a = (argb >> 24 & 0xFF) / 255.0F;
        float r = (argb >> 16 & 0xFF) / 255.0F;
        float g = (argb >> 8 & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x1, y1, 0.0D).color(r, g, b, a).endVertex();
        worldrenderer.pos(x2, y2, 0.0D).color(r, g, b, a).endVertex();
        worldrenderer.pos(x3, y3, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        worldrenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x1, y1, 0.0D).color(r, g, b, a).endVertex();
        worldrenderer.pos(x2, y2, 0.0D).color(r, g, b, a).endVertex();
        worldrenderer.pos(x3, y3, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void fadeRight(float x, float y, float height, float width, int from, int steps) {
        if (width <= 0.0F || height <= 0.0F) return;

        int slices = Math.max(1, steps);
        int alpha = from >>> 24;

        if (alpha == 0) return;

        for (int i = 0; i < slices; i++) {
            int step = Math.round(alpha * (1.0F - (float) i / (float) slices));

            if (step <= 0) break;

            int sliceX = Math.round(x + width * i / slices);
            int sliceX1 = Math.round(x + width * (i + 1) / slices);

            if (sliceX1 <= sliceX) sliceX1 = sliceX + 1;

            fill(sliceX, y, sliceX1, y + height, (step << 24) | (from & 0x00FFFFFF));
        }
    }

    public static float labelY(CustomFontRenderer font, float rowY, float height) {
        return rowY + (height - font.getHeight()) / 2.0F;
    }

    public static void value(CustomFontRenderer font, String text, float right, float y, int color) {
        String shortened = truncate(font, text, Math.max(0.0F, right));

        font.drawString(shortened, right - font.getStringWidth(shortened), y, color);
    }

    public static String truncate(CustomFontRenderer font, String text, float maxWidth) {
        if (text == null) return "";

        if (font.getStringWidth(text) <= maxWidth) return text;

        String ellipsis = "...";

        if (font.getStringWidth(ellipsis) > maxWidth) return "";

        StringBuilder truncated = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            if (font.getStringWidth(truncated.toString()) + font.getStringWidth(ellipsis) >= maxWidth) {
                break;
            }

            truncated.append(text.charAt(i));
        }

        return truncated.append(ellipsis).toString();
    }

    private AtmosphereDraw() {
    }
}