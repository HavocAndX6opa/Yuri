package ddlc.yuri.api.gui.main;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.alt.YuriAltMenu;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.hud.AnimatedBackground;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class YuriMenu extends GuiScreen {

    public static final Color BASE_BG = new Color(10, 9, 13, 255);
    private static final Color ACCENT = new Color(222, 64, 255);
    private static final Color ACCENT_SOFT = new Color(170, 80, 255);
    private static final Color BUTTON_FILL = new Color(52, 50, 56);
    private static final Color FOOTER = new Color(226, 170, 255);

    private static final ResourceLocation LOGO = new ResourceLocation("yuri/gui/menu/text_logo.png");
    private static final ResourceLocation YURI = new ResourceLocation("yuri/gui/logo.png");
    private static final ResourceLocation LINES = new ResourceLocation("yuri/gui/menu/splashes.txt");
    private static final ResourceLocation BACKGROUND = new ResourceLocation("yuri/gui/menu/background.jpg");

    private static final String[] LABELS = {"Singleplayer", "Multiplayer", "Account Manager", "Settings", "Exit"};
    private static final String[] ICONS = {"singleplayer.png", "multiplayer.png", "alt.png", "options.png", "shop.png"};
    private static final String[] FALLBACK = {"S", "M", "A", "O", "X"};

    private static final float RADIUS = 27f;
    private static final float GAP = 16f;
    private static final float LOGO_SIZE = 170f;


    private final AnimatedBackground animatedBackground = new AnimatedBackground(BACKGROUND);

    private long lastFrame;

    private final float[] hover = new float[5];
    private final boolean[] hasIcon = new boolean[5];
    private final ResourceLocation[] iconLocations = new ResourceLocation[5];

    private ScaledResolution sr;

    private float animX;
    private float animY;
    private long openTime;

    private String currentLine;

    @Override
    public void initGui() {
        sr = new ScaledResolution(mc);
        openTime = System.currentTimeMillis();

        Random rng = new Random();

        currentLine = pickLine(rng);

        lastFrame = System.nanoTime();

        for (int i = 0; i < ICONS.length; i++) {
            iconLocations[i] = new ResourceLocation("yuri/gui/menu/" + ICONS[i]);
            hasIcon[i] = resourceExists(iconLocations[i]);
            hover[i] = 0f;
        }
    }

    private String pickLine(Random rng) {
        List<String> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(mc.getResourceManager().getResource(LINES).getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) entries.add(line);
            }
        } catch (Exception ignored) {
        }
        if (entries.isEmpty()) return null;
        return entries.get(rng.nextInt(entries.size()));
    }

    private boolean resourceExists(ResourceLocation location) {
        try {
            mc.getResourceManager().getResource(location);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void runAction(int index) {
        switch (index) {
            case 0:
                mc.displayGuiScreen(new GuiSelectWorld(this));
                break;
            case 1:
                mc.displayGuiScreen(new GuiMultiplayer(this));
                break;
            case 2:
                mc.displayGuiScreen(new YuriAltMenu());
                break;
            case 3:
                mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                break;
            case 4:
                mc.shutdown();
                break;
        }
    }

    private float buttonCenterX(float w, int index) {
        float total = LABELS.length * RADIUS * 2f + (LABELS.length - 1) * GAP;
        return w / 2f - total / 2f + RADIUS + index * (RADIUS * 2f + GAP);
    }

    private float buttonCenterY(float h) {
        return h / 2f + 52f;
    }

    private int argb(Color c, float alpha) {
        int a = (int) Math.max(0, Math.min(255, alpha * 255f));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a).getRGB();
    }

    private void setColor(int argb) {
        float a = (argb >> 24 & 255) / 255f;
        float r = (argb >> 16 & 255) / 255f;
        float g = (argb >> 8 & 255) / 255f;
        float b = (argb & 255) / 255f;
        GlStateManager.color(r, g, b, a);
    }

    private void fillCircle(float cx, float cy, float r, int argb) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        setColor(argb);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= 64; i++) {
            double ang = Math.PI * 2.0 * i / 64.0;
            GL11.glVertex2f(cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r);
        }
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void ringCircle(float cx, float cy, float r, float width, int argb) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(width);
        setColor(argb);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 64; i++) {
            double ang = Math.PI * 2.0 * i / 64.0;
            GL11.glVertex2f(cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        sr = new ScaledResolution(mc);

        float w = sr.getScaledWidth();
        float h = sr.getScaledHeight();

        Gui.drawRect(0, 0, (int) w, (int) h, BASE_BG.getRGB());

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1.0E9f);
        lastFrame = now;

        float mx = (w / 2f) * 0.00001f;
        float my = (h / 2f) * 0.01f;
        animX += (mx - animX) * 0.05f;
        animY += (my - animY) * 0.05f;

        float t = Math.min(1f, (System.currentTimeMillis() - openTime) / 700f);
        float fade = 1f - (float) Math.pow(1f - t, 3);

        drawBackground(w, h, mouseX, mouseY, dt, fade);
        drawContent(w, h, mouseX, mouseY, fade);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBackground(float w, float h, float mouseX, float mouseY, float dt, float fade) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        animatedBackground.render(w, h, mouseX, mouseY, fade);

        GlStateManager.color(1f, 1f, 1f, 1f);

        Gui.drawRect(0, 0, (int) w, (int) h, argb(new Color(20, 6, 36), 0.22f * fade));
    }

    private void drawContent(float w, float h, int mouseX, int mouseY, float fade) {
        float scale = (float) sr.getScaleFactor() / 2f;
        CustomFontRenderer small = FontUtils.getScaledFont("sf", 14, scale);
        CustomFontRenderer medium = FontUtils.getScaledFont("sf", 18, scale);
        if (small == null || medium == null) return;

        float cx = w / 2f;
        float logoY = h / 2f - LOGO_SIZE / 1.5f + (1f - fade);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, fade);
        RenderUtils.drawImage(LOGO, cx - LOGO_SIZE / 2f, logoY, LOGO_SIZE, LOGO_SIZE);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();

        float by = buttonCenterY(h) + (1f - fade) * 14f;
        int hovered = -1;

        for (int i = 0; i < LABELS.length; i++) {
            float bx = buttonCenterX(w, i);
            float dx = mouseX - bx;
            float dy = mouseY - buttonCenterY(h);
            boolean over = dx * dx + dy * dy <= RADIUS * RADIUS;
            if (over) hovered = i;

            hover[i] += ((over ? 1f : 0f) - hover[i]) * 0.2f;
            float hv = hover[i];
            float r = RADIUS + hv * 2.5f;

            if (hasIcon[i]) {
                float size = r * 2f / 0.75f;
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.color(1f, 1f, 1f, (0.8f + 0.12f * hv) * fade);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                mc.getTextureManager().bindTexture(iconLocations[i]);
                Gui.drawModalRectWithCustomSizedTexture(bx - size / 2f, by - size / 2f, 0, 0, size, size, size, size);
                GlStateManager.disableBlend();
                GlStateManager.color(1f, 1f, 1f, 1f);
            } else {
                fillCircle(bx, by, r, argb(BUTTON_FILL, (0.78f + 0.12f * hv) * fade));
                ringCircle(bx, by, r, 1.4f, argb(ACCENT_SOFT, (0.35f + 0.55f * hv) * fade));
                float gh = medium.getHeight();
                medium.drawCenteredString(FALLBACK[i], bx, by - gh / 2f, argb(ACCENT, (0.8f + 0.2f * hv) * fade));
            }
        }

        if (hovered != -1) {
            small.drawCenteredString(LABELS[hovered], buttonCenterX(w, hovered), by + RADIUS + 10f,
                    argb(Color.WHITE, 0.9f * fade));
        }

        if (currentLine != null) {
            medium.drawCenteredString(currentLine, cx, by + RADIUS + 34f, argb(Color.WHITE, 0.6f * fade));
        }

        float footerY = h - small.getHeight() - 3f;
        small.drawString("Yuri " + Yuri.VERSION, 4f, footerY, argb(FOOTER, 0.8f * fade));

        String credits = "Brought to you by: unlegit!";
        small.drawString(credits, w - small.getStringWidth(credits) - 4f, footerY, argb(FOOTER, 0.8f * fade));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            float w = sr.getScaledWidth();
            float h = sr.getScaledHeight();
            for (int i = 0; i < LABELS.length; i++) {
                float dx = mouseX - buttonCenterX(w, i);
                float dy = mouseY - buttonCenterY(h);
                if (dx * dx + dy * dy <= RADIUS * RADIUS) {
                    runAction(i);
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}