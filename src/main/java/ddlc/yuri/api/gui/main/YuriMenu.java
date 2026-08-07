package ddlc.yuri.api.gui.main;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.alt.YuriAltMenu;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class YuriMenu extends GuiScreen {

    private final List<MenuButton> buttons = new ArrayList<>();

    private static final Color ERROR_COLOR = new Color(214, 64, 69);

    private float animX;
    private float animY;

    private ScaledResolution sr;

    private String currentLine;
    private boolean showError;

    private final String[] lines = {
            "natsuki is gonna steal you again?!"
    };

    private final String[] errorLines = {
            "monika hacked into your client"
    };

    @Override
    public void initGui() {

        // lines selection
        Random rng = new Random();

        showError = rng.nextFloat() < 0.02f;

        currentLine = showError
                ? errorLines[rng.nextInt(errorLines.length)]
                : lines[rng.nextInt(lines.length)];

        // buttons configuration
        buttons.clear();

        buttons.add(new MenuButton("Singleplayer", () -> mc.displayGuiScreen(new GuiSelectWorld(this))));
        buttons.add(new MenuButton("Multiplayer", () -> mc.displayGuiScreen(new GuiMultiplayer(this))));
        buttons.add(new MenuButton("Account Manager", () -> mc.displayGuiScreen(new YuriAltMenu())));
        buttons.add(new MenuButton("Game Settings", () -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings))));
        buttons.add(new MenuButton("Quit Playing", () -> mc.shutdown()));

        if (sr == null) sr = new ScaledResolution(mc);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        sr = new ScaledResolution(mc);

        // draw background
        Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, 255).getRGB());

        float w = sr.getScaledWidth();
        float h = sr.getScaledHeight();

        float mx = (mouseX - w / 2f) * 0.01f;
        float my = (mouseY - h / 2f) * 0.01f;

        animX += (mx - animX) * 0.05f;
        animY += (my - animY) * 0.05f;

        float center = w / 2f;

        drawContent(w, h, mouseX, mouseY, center);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawContent(float w, float h, float mouseX, float mouseY, float center) {

        CustomFontRenderer font = FontUtils.getScaledFont("sf", 18, (float) sr.getScaleFactor() / 2f);
        if (font == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(animX, animY, 0);

        RenderUtils.drawImage(
                new ResourceLocation("yuri/gui/logo.png"),
                center - 128 / 2f,
                h / 2f - 128 - 10,
                128,
                128
        );

        GlStateManager.popMatrix();

        drawButtons(center, h / 2f, mouseX, mouseY);

        font.drawString("Yuri " + Yuri.VERSION, 4, h - font.getHeight() - 4, new Color(196, 199, 199, 70).getRGB());

        String credits = "Brought to you by: unlegit!";
        font.drawString(credits, (float) width - font.getStringWidth(credits) - 4, h - font.getHeight() - 4, new Color(196, 199, 199, 70).getRGB());

        int lineColor = showError
                ? new Color(ERROR_COLOR.getRed(), ERROR_COLOR.getGreen(), ERROR_COLOR.getBlue(), 160).getRGB()
                : RenderUtils.withAlpha(ColorManager.getColor(), 70);

        font.drawCenteredString(currentLine, center, h / 2f + font.getHeight() + 28, lineColor);
    }

    private void drawButtons(float center, float y, float mx, float my) {
        int spacing = 6;
        CustomFontRenderer font = FontUtils.getScaledFont("sf", 14, (float) sr.getScaleFactor() / 2f);
        if (font == null || buttons.size() < 5) return;

        float buttonHeight = font.getHeight() + 10f;

        // Row 1: Singleplayer, Multiplayer, Account Manager
        drawRow(new MenuButton[]{buttons.get(0), buttons.get(1), buttons.get(2)}, center, y, spacing, mx, my);

        // Row 2: Game Settings, Quit
        drawRow(new MenuButton[]{buttons.get(3), buttons.get(4)}, center, y + buttonHeight + spacing, spacing, mx, my);
    }

    private void drawRow(MenuButton[] row, float center, float y, float gap, float mx, float my) {

        CustomFontRenderer font = FontUtils.getScaledFont("sf", 14, (float) sr.getScaleFactor() / 2f);
        if (font == null) return;

        float totalWidth = 0;

        for (int i = 0; i < row.length; i++) {
            float itemWidth = row[i].label.isEmpty()
                    ? font.getHeight() + 10f
                    : row[i].getLayoutWidth(font.getStringWidth(row[i].label));
            totalWidth += itemWidth;
            if (i != row.length - 1) totalWidth += gap;
        }

        float x = center - totalWidth / 2f;

        for (MenuButton b : row) {

            float textWidth = font.getStringWidth(b.label);
            float textHeight = font.getHeight();

            float itemHeight = b.getLayoutHeight(textHeight);
            float itemWidth = b.label.isEmpty() ? itemHeight : b.getLayoutWidth(textWidth);

            if (b.label.isEmpty()) {
                b.layoutBox(x, y - 5f, itemWidth, itemHeight);
            } else {
                b.layout(x + 8f, y, textWidth, textHeight);
            }

            b.updateHover(mx, my);
            b.renderBox();

            if (!b.label.isEmpty()) {
                font.drawString(b.label, b.x + (b.width - textWidth) / 2f, y, b.getTextColor());
            }

            x += b.width + gap;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            for (MenuButton button : buttons) {
                if (button.isHovered(mouseX, mouseY)) {
                    button.mouseClicked();
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}