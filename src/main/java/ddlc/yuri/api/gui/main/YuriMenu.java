package ddlc.yuri.api.gui.main;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.alt.EuphoriaAltMenu;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.impl.client.ClickGUIModule;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
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

    private String[] lines = {
            "natsuki is gonna steal you again?!"
    };

    private String[] errorLines = {
           "monika hacked into your client"
    };

    @Override
    public void initGui() {

        // lines

        Random rng = new Random();

        showError = rng.nextFloat() < 0.02f;

        currentLine = showError
                ? errorLines[rng.nextInt(errorLines.length)]
                : lines[rng.nextInt(lines.length)];

        // buttons

        buttons.clear();

        buttons.add(new MenuButton("Singleplayer", () -> mc.displayGuiScreen(new GuiSelectWorld(this))));
        buttons.add(new MenuButton("Multiplayer", () -> mc.displayGuiScreen(new GuiMultiplayer(this))));
        buttons.add(new MenuButton("Account Manager", () -> mc.displayGuiScreen(new EuphoriaAltMenu())));
        buttons.add(new MenuButton("Game Settings", () -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings))));
        buttons.add(new MenuButton("Quit Playing", () -> mc.shutdown()));
        buttons.add(new MenuButton("", null));

        if (sr == null) sr = new ScaledResolution(mc);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        sr = new ScaledResolution(mc);

        // background + the global corner toggle button now both come from
        // GuiScreen#drawDefaultBackground (patched), same as every other screen
        Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, 255).getRGB());

        float w = sr.getScaledWidth();
        float h = sr.getScaledHeight();

        float mx = (mouseX - w / 2f) * 0.01f;
        float my = (mouseY - h / 2f) * 0.01f;

        animX += (mx - animX) * 0.05f;
        animY += (my - animY) * 0.05f;

        float center = w / 2;

        drawContent(w, h, mouseX, mouseY, center);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawContent(float w, float h, float mouseX, float mouseY, float center) {

        CustomFontRenderer font = FontUtils.getScaledFont("sf", 18, (float) sr.getScaleFactor() / 2);
        CustomFontRenderer font2 = FontUtils.getScaledFont("sf", 18, (float) sr.getScaleFactor() / 2);

        if (font == null || font2 == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(animX, animY, 0);

        RenderUtils.drawImage(
                new ResourceLocation("yuri/gui/logo.png"),
                center - 128 / 2,
                h / 2 - 128 - 40,
                128,
                128
        );

        GlStateManager.popMatrix();

        drawButtons(center, h / 2 - 30, mouseX, mouseY);

        font2.drawString("Yuri " + Yuri.VERSION, 4, h - font2.getHeight() - 4, new Color(196, 199, 199, 70).getRGB());

        String text = "Broughtto you by: unlegit!";

        font2.drawString(text, (float) width - font2.getStringWidth(text) - 4, h - font2.getHeight() - 4, new Color(196, 199, 199, 70).getRGB());

        int lineColor = showError
                ? new Color(ERROR_COLOR.getRed(), ERROR_COLOR.getGreen(), ERROR_COLOR.getBlue(), 160).getRGB()
                : RenderUtils.withAlpha(ColorManager.getColor(), 70);

        font2.drawCenteredString(currentLine, center, h / 2 + font2.getHeight() + 8, lineColor);
    }

    private void drawButtons(float center, float y, float mx, float my) {
        int spacing = 5;
        CustomFontRenderer font = FontUtils.getScaledFont("sf", 14, (float) sr.getScaleFactor() / 2);
        if (font == null) return;

        float buttonHeight = font.getHeight() + 10f;
        drawRow(new MenuButton[]{buttons.get(0), buttons.get(1), buttons.get(2)}, center, y, spacing, mx, my);
        drawRow(new MenuButton[]{buttons.get(3), buttons.get(4), buttons.get(5)}, center,
                y + buttonHeight + spacing, spacing, mx, my);
    }

    private void drawRow(MenuButton[] row, float center, float y, float gap, float mx, float my) {

        CustomFontRenderer font = FontUtils.getScaledFont("sf", 14, (float) sr.getScaleFactor() / 2);

        if (font == null) return;

        float total = 0;

        for (int i = 0; i < row.length; i++) {
            float itemWidth = row[i].label.isEmpty()
                    ? font.getHeight() + 10f
                    : row[i].getLayoutWidth(font.getStringWidth(row[i].label));
            total += itemWidth;
            if (i != row.length - 1) total += gap;
        }

        float x = center - total / 2;

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