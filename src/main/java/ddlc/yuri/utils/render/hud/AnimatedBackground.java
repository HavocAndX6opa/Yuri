package ddlc.yuri.utils.render.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class AnimatedBackground {

    private static final float TAU = (float) (Math.PI * 2.0);
    private static final int SEGMENTS = 16;

    private final ResourceLocation image;
    private final float aspect;

    private float speed = 20.0f;
    private float parallaxStrength = 0.007f;
    private boolean glow = true;
    private boolean vignette = true;

    private boolean checked;
    private boolean available;

    private float time;
    private float parallaxX = 0f;
    private float parallaxY = 0f;
    private long lastFrame = -1L;

    public AnimatedBackground(ResourceLocation image) {
        this(image, 16f / 9f);
    }

    public AnimatedBackground(ResourceLocation image, float aspect) {
        this.image = image;
        this.aspect = aspect;
    }

    public AnimatedBackground setSpeed(float speed) {
        this.speed = speed;
        return this;
    }

    public AnimatedBackground setParallaxStrength(float strength) {
        this.parallaxStrength = strength;
        return this;
    }

    public AnimatedBackground setGlow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public AnimatedBackground setVignette(boolean vignette) {
        this.vignette = vignette;
        return this;
    }

    public boolean isAvailable() {
        if (!checked) {
            checked = true;
            try {
                Minecraft.getMinecraft().getResourceManager().getResource(image);
                available = true;
            } catch (Exception e) {
                available = false;
            }
        }
        return available;
    }

    public void reset() {
        checked = false;
        time = 0f;
        parallaxX = 0.5f;
        parallaxY = 0.5f;
        lastFrame = -1L;
    }

    public void render(float w, float h, float mouseX, float mouseY, float alpha) {
        if (alpha <= 0f || !isAvailable()) return;

        long now = System.nanoTime();
        float dt = lastFrame < 0L ? 0f : Math.min(0.1f, (now - lastFrame) / 1.0E9f);
        lastFrame = now;

        time += dt * speed;

        float follow = Math.min(1f, dt * 6f);
        parallaxX += (mouseX / w - parallaxX) * follow;
        parallaxY += (mouseY / h - parallaxY) * follow;

        float px = (parallaxX - 0.5f) * -2f * w * parallaxStrength;
        float py = (parallaxY - 0.5f) * -2f * h * parallaxStrength;

        float coverW;
        float coverH;
        if (w / h > aspect) {
            coverW = w;
            coverH = w / aspect;
        } else {
            coverH = h;
            coverW = h * aspect;
        }

        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(image);

        float w1 = TAU / 140f;
        float w2 = TAU / 180f;

        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        drawLayer(w, h, coverW, coverH,
                (float) Math.sin(time * w1) * 0.015f * w + px,
                (float) Math.cos(time * w1 * 0.7f) * 0.015f * h + py,
                1.03f + 0.015f * (float) Math.sin(time * w1 * 0.5f),
                0f,
                false, 1f, 1f, 1f, alpha);

        GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO);
        drawLayer(w, h, coverW, coverH,
                (float) Math.cos(time * w2) * 0.02f * w + px * 1.2f,
                (float) Math.sin(time * w2 * 0.8f) * 0.02f * h + py * 1.2f,
                1.06f + 0.02f * (float) Math.cos(time * w2 * 0.6f),
                0f,
                false, 0.45f * alpha, 0.2f * alpha, 0.55f * alpha, 1f);

        if (glow) drawGlow(w, h, alpha);
        if (vignette) drawVignette(w, h, alpha);

        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        resetColor();
    }

    private void drawLayer(float w, float h, float coverW, float coverH, float dx, float dy,
                           float scale, float rotation, boolean mirror,
                           float r, float g, float b, float a) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(w / 2f + dx, h / 2f + dy, 0f);
        GlStateManager.rotate(rotation, 0f, 0f, 1f);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.color(r, g, b, a);

        float hw = coverW / 2f;
        float hh = coverH / 2f;
        float u0 = mirror ? 1f : 0f;
        float u1 = mirror ? 0f : 1f;

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(u0, 0f);
        GL11.glVertex2f(-hw, -hh);
        GL11.glTexCoord2f(u0, 1f);
        GL11.glVertex2f(-hw, hh);
        GL11.glTexCoord2f(u1, 1f);
        GL11.glVertex2f(hw, hh);
        GL11.glTexCoord2f(u1, 0f);
        GL11.glVertex2f(hw, -hh);
        GL11.glEnd();

        GlStateManager.popMatrix();
    }

    private void drawGlow(float w, float h, float alpha) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(time / 10 * TAU / 9f);
        float a = (0.12f + 0.28f * pulse) * alpha;
        float grow = 1f + 0.15f * pulse;
        float cx = w * 0.55f;
        float cy = h * 0.45f;
        float rx = w * 0.6f * grow;
        float ry = h * 0.5f * grow;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(0.66f, 0.33f, 0.97f, a);
        GL11.glVertex2f(cx, cy);
        GL11.glColor4f(0.66f, 0.33f, 0.97f, 0f);
        for (int i = 0; i <= SEGMENTS; i++) {
            float ang = TAU * i / SEGMENTS;
            GL11.glVertex2f(cx + (float) Math.cos(ang) * rx, cy + (float) Math.sin(ang) * ry);
        }
        GL11.glEnd();

        resetColor();
    }

    private void drawVignette(float w, float h, float alpha) {
        float cx = w / 2f;
        float cy = h / 2f;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= SEGMENTS; i++) {
            float ang = TAU * i / SEGMENTS;
            float cos = (float) Math.cos(ang);
            float sin = (float) Math.sin(ang);
            GL11.glColor4f(0f, 0f, 0f, 0f);
            GL11.glVertex2f(cx + cos * w * 0.42f, cy + sin * h * 0.42f);
            GL11.glColor4f(0f, 0f, 0f, 0.7f * alpha);
            GL11.glVertex2f(cx + cos * w * 0.8f, cy + sin * h * 0.8f);
        }
        GL11.glEnd();

        resetColor();
    }

    private void resetColor() {
        GlStateManager.color(0f, 0f, 0f, 0f);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}