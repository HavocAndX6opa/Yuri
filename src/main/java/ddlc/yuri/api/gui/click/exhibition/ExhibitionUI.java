package ddlc.yuri.api.gui.click.exhibition;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.click.exhibition.components.*;
import ddlc.yuri.api.gui.click.exhibition.components.Button;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.api.properties.impl.Representation;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class ExhibitionUI extends UI {

    private final Translate bar = new Translate(0, 0);

    private float progress = 0.0F;
    private boolean closing = false;

    private ResourceLocation tex = new ResourceLocation("yuri/gui/skeet/tex.png");
    private ResourceLocation texture = new ResourceLocation("yuri/gui/skeet/skeetchainmail.png");

    private ResourceLocation getIconForCategory(String name) {
        switch (name) {
            case "Combat": return new ResourceLocation("yuri/gui/skeet/combat.png");
            case "Movement": return new ResourceLocation("yuri/gui/skeet/movement.png");
            case "Player": return new ResourceLocation("yuri/gui/skeet/player.png");
            case "Render": return new ResourceLocation("yuri/gui/skeet/render.png");
            case "Misc": return new ResourceLocation("yuri/gui/skeet/misc.png");
            default: return new ResourceLocation("yuri/gui/skeet/render.png");
        }
    }

    private CustomFontRenderer getFont() { return FontUtils.getFont("tahoma", 15); }
    private CustomFontRenderer getSmallFont() { return FontUtils.getFont("tahoma", 13); }
    private CustomFontRenderer getBoldFont() { return FontUtils.getFont("tahoma-bold", 10); }

    @Override
    public void mainConstructor(ExhibitionClickGui p0) {}

    @Override
    public void mainPanelDraw(MainPanel panel, int p0, int p1) {
        float target = closing ? 0.0F : 1.0F;
        progress += (target - progress) * 0.2F;

        if (closing && progress < 0.08F) {
            progress = 0.0F;
            closing = false;
            if (mc.currentScreen instanceof ExhibitionClickGui) {
                mc.displayGuiScreen(null);
            }
            return;
        }

        if (progress < 0.08F) {
            return;
        }

        mc.mcProfiler.startSection("exhibition_background");

        int alpha = (int) (255 * progress);

        RenderingUtil.rectangleBordered(panel.x + panel.dragX - 0.3, panel.y + panel.dragY - 0.3, panel.x + 340 + panel.dragX + 0.5, panel.y + 340 + panel.dragY + 0.3, 0.5, Colors.getColor(0, 0), Colors.getColor(10, alpha));
        RenderingUtil.rectangleBordered(panel.x + panel.dragX, panel.y + panel.dragY, panel.x + 340 + panel.dragX, panel.y + 340 + panel.dragY, 0.5, Colors.getColor(0, 0), Colors.getColor(60, alpha));
        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2, panel.y + panel.dragY + 2, panel.x + 340 + panel.dragX - 2, panel.y + 340 + panel.dragY - 2, 0.5, Colors.getColor(0, 0), Colors.getColor(60, alpha));
        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 0.6, panel.y + panel.dragY + 0.6, panel.x + 340 + panel.dragX - 0.5, panel.y + 340 + panel.dragY - 0.6, 1.3, Colors.getColor(0, 0), Colors.getColor(40, alpha));

        float y = 15;
        for (int i = 0; i <= panel.typeButton.size(); i++) {
            if (i <= panel.typeButton.size() - 1 && panel.typeButton.get(i).categoryPanel.visible && i > 0) {
                y = 15 + ((i) * 40);
            }
        }
        bar.interpolate(0, y, 0.6F);
        y = (float) bar.getY();

        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + 4F, panel.x + panel.dragX + 40, panel.y + panel.dragY + y, -1);
        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + y + 40, panel.x + panel.dragX + 40, panel.y + panel.dragY + 307.5 + 30, -1);

        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2.5, panel.y + panel.dragY + 2.5, panel.x + 340 + panel.dragX - 2.5, panel.y + 340 + panel.dragY - 2.5, 0.5, Colors.getColor(22, alpha), Colors.getColor(22, alpha));

        RenderingUtil.drawGradientSideways(panel.x + panel.dragX + 3, panel.y + panel.dragY + 3, panel.x + 178 + panel.dragX - 3, panel.dragY + panel.y + 4, Colors.getColor(55, 177, 218, alpha), Colors.getColor(204, 77, 198, alpha));
        RenderingUtil.drawGradientSideways(panel.x + panel.dragX + 175, panel.y + panel.dragY + 3, panel.x + 340 + panel.dragX - 3, panel.dragY + panel.y + 4, Colors.getColor(204, 77, 198, alpha), Colors.getColor(204, 227, 53, alpha));

        int i11 = alpha - 145;
        if (i11 < 0) i11 = 0;
        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + 3.3, panel.x + 340 + panel.dragX - 3, panel.dragY + panel.y + 4, Colors.getColor(0, i11));

        GlStateManager.enableBlend();
        mc.mcProfiler.startSection("texture_bg");
        mc.getTextureManager().bindTexture(tex);
        GlStateManager.pushMatrix();
        GlStateManager.translate(panel.x + panel.dragX + 40, panel.dragY + panel.y + 3f, 0);
        RenderingUtil.drawIcon(0, 0, 0, .5F, 340 - 3 - 40, 310 - 6 + 30, 812 / 2F, 688 / 2F);
        RenderingUtil.drawIcon(-40 + 2.5, y - 3, .5F, .5F + y, 40 - 2.5F, 40, 812 / 2F, 688 / 2F);
        GlStateManager.popMatrix();
        mc.mcProfiler.endSection();
        GlStateManager.disableBlend();

        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + 4F, panel.x + panel.dragX + 40, panel.y + panel.dragY + y + 1, -1);
        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + y + 40, panel.x + panel.dragX + 40, panel.y + panel.dragY + 307.5 + 30, -1);
        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2, panel.y + panel.dragY + 3, panel.x + panel.dragX + 39.5, panel.y + panel.dragY + y - 0.5, 0.5, Colors.getColor(0, 0), Colors.getColor(0, alpha));
        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2, panel.y + panel.dragY + 3, panel.x + panel.dragX + 40, panel.y + panel.dragY + y, 0.5, Colors.getColor(0, 0), Colors.getColor(48, alpha));
        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + 4, panel.x + panel.dragX + 39, panel.y + panel.dragY + y - 1, Colors.getColor(12, alpha));
        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2, panel.y + panel.dragY + y + 40.5, panel.x + panel.dragX + 39.5, panel.y + panel.dragY + 308 + 30, 0.5, Colors.getColor(0, 0), Colors.getColor(0, alpha));
        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2, panel.y + panel.dragY + y + 40, panel.x + panel.dragX + 40, panel.y + panel.dragY + 308 + 30, 0.5, Colors.getColor(0, 0), Colors.getColor(48, alpha));
        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + y + 41, panel.x + panel.dragX + 39, panel.y + panel.dragY + 307.5 + 30, Colors.getColor(12, alpha));

        for (SLButton button : panel.slButtons) {
            button.draw(p0, p1);
        }
        for (CategoryButton button : panel.typeButton) {
            button.draw(p0, p1);
        }

        ScaledResolution rs = new ScaledResolution(mc);
        double twoDscale = (rs.getScaleFactor() / Math.pow(rs.getScaleFactor(), 2.0D)) * 2;
        if (panel.dragging) {
            panel.dragX = p0 - panel.lastDragX;
            panel.dragY = p1 - panel.lastDragY;
        }
        double xBorder = (rs.getScaledWidth() / twoDscale - 392);
        if (panel.dragX > xBorder) panel.dragX = (float) xBorder;
        if (panel.dragX < 2 - 50) panel.dragX = 2 - 50;
        double yBorder = (rs.getScaledHeight() / twoDscale - 392);
        if (panel.dragY > yBorder) panel.dragY = (float) yBorder;
        if (panel.dragY < 2 - 50) panel.dragY = 2 - 50;
        mc.mcProfiler.endSection();
    }

    @Override
    public void mainPanelKeyPress(MainPanel panel, int key) {
        boolean bad = false;
        if (key == 1) {
            for (CategoryButton buttonb : panel.typeButton) {
                for (Button button : buttonb.categoryPanel.buttons) {
                    if (button.isBinding) bad = true;
                }
            }
        }
        if (!bad && (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_INSERT || key == Keyboard.KEY_DELETE || key == Keyboard.KEY_RSHIFT)) {
            panel.typeButton.forEach(o -> o.categoryPanel.buttons.forEach(b -> b.isBinding = false));
            panel.typeButton.forEach(o -> o.categoryPanel.textBoxes.forEach(b -> { b.isTyping = false; b.isFocused = false; }));
            closing = true;
            return;
        }
        panel.typeButton.forEach(o -> o.categoryPanel.buttons.forEach(b -> b.keyPressed(key)));
        panel.typeButton.forEach(o -> o.categoryPanel.textBoxes.forEach(t -> t.keyPressed(key)));
    }

    @Override
    public void panelConstructor(MainPanel mainPanel, float x, float y) {
        progress = 0.0F;
        closing = false;
        int y1 = 15;
        mainPanel.typeButton.add(new CategoryButton(mainPanel, "Combat", x + 3, y + y1));
        y1 += 40;
        mainPanel.typeButton.add(new CategoryButton(mainPanel, "Movement", x + 3, y + y1));
        y1 += 40;
        mainPanel.typeButton.add(new CategoryButton(mainPanel, "Player", x + 3, y + y1));
        y1 += 40;
        mainPanel.typeButton.add(new CategoryButton(mainPanel, "Render", x + 3, y + y1));
        y1 += 40;
        mainPanel.typeButton.add(new CategoryButton(mainPanel, "Misc", x + 3, y + y1));
        mainPanel.typeButton.get(0).enabled = true;
        mainPanel.typeButton.get(0).categoryPanel.visible = true;
    }

    @Override
    public void panelMouseClicked(MainPanel mainPanel, int x, int y, int z) {
        if (closing) return;
        if (x >= mainPanel.x + mainPanel.dragX && y >= mainPanel.dragY + mainPanel.y && x <= mainPanel.dragX + mainPanel.x + 400 && y <= mainPanel.dragY + mainPanel.y + 12.0f && z == 0) {
            mainPanel.dragging = true;
            mainPanel.lastDragX = x - mainPanel.dragX;
            mainPanel.lastDragY = y - mainPanel.dragY;
        }
        mainPanel.typeButton.forEach(c -> {
            c.mouseClicked(x, y, z);
            c.categoryPanel.mouseClicked(x, y, z);
        });
        mainPanel.slButtons.forEach(slButton -> slButton.mouseClicked(x, y, z));
    }

    @Override
    public void panelMouseMovedOrUp(MainPanel mainPanel, int x, int y, int z) {
        if (z == 0) mainPanel.dragging = false;
        for (CategoryButton button : mainPanel.typeButton) {
            button.mouseReleased(x, y, z);
        }
    }

    @Override
    public void categoryButtonConstructor(CategoryButton p0, MainPanel p1) {
        p0.categoryPanel = new CategoryPanel(p0.name, p0, 0, 0, 0);
    }

    @Override
    public void categoryButtonMouseClicked(CategoryButton p0, MainPanel p1, int p2, int p3, int p4) {
        if (p2 >= p0.x + p1.dragX && p3 >= p1.dragY + p0.y && p2 <= p1.dragX + p0.x + 40 && p3 <= p1.dragY + p0.y + 40 && p4 == 0) {
            for (CategoryButton button : p1.typeButton) {
                if (button == p0) {
                    p0.enabled = true;
                    p0.categoryPanel.visible = true;
                } else {
                    button.enabled = false;
                    button.categoryPanel.visible = false;
                }
            }
        }
    }

    @Override
    public void categoryButtonDraw(CategoryButton p0, float p2, float p3) {
        int alpha = (int) (255 * progress);
        int brightness = p0.enabled ? 210 : 91;
        boolean hovering = p2 >= p0.x + p0.panel.dragX && p3 >= p0.panel.dragY + p0.y && p2 <= p0.panel.dragX + p0.x + 40 && p3 < p0.panel.dragY + p0.y + 40;
        if (hovering && !p0.enabled) brightness = 165;

        if (hovering) {
            getSmallFont().drawStringWithShadow(p0.name, (p0.panel.x + 2 + p0.panel.dragX) + 55, (p0.panel.y + 9 + p0.panel.dragY), Colors.getColor(220, alpha));
        }

        p0.fade += (brightness - p0.fade) * 0.1F;

        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, progress);
        GlStateManager.enableBlend();
        ResourceLocation icon = getIconForCategory(p0.name);
        RenderUtils.drawImage(icon, (int) (p0.x + 6 + p0.panel.dragX), (int) (p0.y + 6 + p0.panel.dragY), 28, 28);
        GlStateManager.popMatrix();

        if (p0.enabled) {
            p0.categoryPanel.draw(p2, p3);
        }
    }

    @Override
    public void categoryPanelConstructor(CategoryPanel categoryPanel, CategoryButton categoryButton, float x, float y) {
        float xOff = 50 + categoryButton.panel.x;
        float yOff = 15 + categoryButton.panel.y;
        float panelRight = categoryButton.panel.x + 340;

        ExhibitionClickGui gui = categoryButton.panel.gui;

        float biggestY = 18 + 16;
        float defYOff = yOff;

        for (ddlc.yuri.modules.Module module : gui.getModulesForCategory(categoryButton.name)) {
            y = 20;
            List<Property<?>> settings = module.getElements();
            List<Property<?>> availableSettings = new ArrayList<>();
            for (Property<?> s : settings) {
                if (s.isAvailable()) availableSettings.add(s);
            }

            if (!availableSettings.isEmpty()) {
                categoryPanel.buttons.add(new Button(categoryPanel, module.getLabel(), xOff + 0.5f, yOff + 10, module));
                float x1 = 0.5f;
                int tY = 0;

                for (Property<?> setting : availableSettings) {
                    if (setting.getValue() instanceof Boolean) {
                        categoryPanel.checkboxes.add(new Checkbox(categoryPanel, setting.getLabel(), xOff + x1, yOff + y, (Property<Boolean>) setting));
                        x1 += 44;
                        tY = 10;
                        if (x1 > 44) { x1 = 0.5f; y += 10; tY = 0; }
                    }
                }
                if (x1 > 44) { y += 10; }

                x1 = 0.5f;
                List<NumberProperty> sliders = new ArrayList<>();
                for (Property<?> setting : availableSettings) {
                    if (setting instanceof NumberProperty) sliders.add((NumberProperty) setting);
                }
                sliders.sort(Comparator.comparing(Property::getLabel));
                for (NumberProperty setting : sliders) {
                    categoryPanel.sliders.add(new Slider(categoryPanel, xOff + x1 + 1, yOff + y + 4, setting));
                    x1 += 44;
                    tY = 10;
                    if (x1 > 44) { tY = 0; x1 = 0.5f; y += 12; }
                }

                List<Property<?>> inverted = new ArrayList<>(availableSettings);
                Collections.reverse(inverted);
                for (Property<?> setting : inverted) {
                    if (setting instanceof ModeProperty || setting instanceof MultiModeProperty) {
                        if (x1 > 44) { y += 14; }
                        x1 = 0.5f;
                    }
                }
                for (Property<?> setting : inverted) {
                    if (setting instanceof ModeProperty) {
                        categoryPanel.dropdownBoxes.add(new DropdownBox(setting, xOff + x1, yOff + y + 4, categoryPanel));
                        tY = 17;
                        x1 += 44;
                        if (x1 > 44) { y += 17; tY = 0; x1 = 0.5f; }
                    }
                    if (setting instanceof MultiModeProperty) {
                        categoryPanel.multiDropdownBoxes.add(new MultiDropdownBox((MultiModeProperty<?>) setting, setting, xOff + x1, yOff + y + 4, categoryPanel));
                        tY = 17;
                        x1 += 44;
                        if (x1 > 44) { y += 17; tY = 0; x1 = 0.5f; }
                    }
                }

                for (Property<?> setting : availableSettings) {
                    if (setting.getValue() instanceof String) {
                        if (x1 > 44) { y += 11; }
                        x1 = 0.5f;
                    }
                }
                for (Property<?> setting : availableSettings) {
                    if (setting.getValue() instanceof String) {
                        categoryPanel.textBoxes.add(new TextBox((Property<String>) setting, xOff + x1, yOff + y + 4, categoryPanel));
                        tY = 16;
                        x1 += 88;
                        if (x1 > 88) { y += 15.5f; tY = 0; x1 = 0.5f; }
                    }
                }

                y += tY;
                float groupHeight = y < 30 ? 40 : y - 11;
                categoryPanel.groupBoxes.add(new GroupBox(module.getLabel(), categoryPanel, xOff, yOff, groupHeight));
                xOff += 95;
                if (y >= biggestY) biggestY = y;
            } else {
                categoryPanel.buttons.add(new Button(categoryPanel, module.getLabel(), xOff + 0.5f, yOff + 15, module));
                xOff += 95;
            }

            if (xOff + 95 > panelRight) {
                xOff = 50 + categoryButton.panel.x;
                yOff += (biggestY < 25 ? 30 : biggestY);
                biggestY = 18 + 16;
            }
        }
    }

    @Override
    public void categoryPanelMouseClicked(CategoryPanel categoryPanel, int p1, int p2, int p3) {
        boolean active = false;
        for (TextBox tb : categoryPanel.textBoxes) {
            if (tb.isFocused || tb.isTyping) {
                tb.mouseClicked(p1, p2, p3);
                active = true;
                break;
            }
        }
        for (DropdownBox db : categoryPanel.dropdownBoxes) {
            if (db.active) { db.mouseClicked(p1, p2, p3); active = true; break; }
        }
        for (MultiDropdownBox db : categoryPanel.multiDropdownBoxes) {
            if (db.active) { db.mouseClicked(p1, p2, p3); active = true; break; }
        }
        if (!active) {
            categoryPanel.textBoxes.forEach(o -> o.mouseClicked(p1, p2, p3));
            categoryPanel.dropdownBoxes.forEach(o -> o.mouseClicked(p1, p2, p3));
            for (MultiDropdownBox db : categoryPanel.multiDropdownBoxes) db.mouseClicked(p1, p2, p3);
            for (Button button : categoryPanel.buttons) button.mouseClicked(p1, p2, p3);
            for (Checkbox checkbox : categoryPanel.checkboxes) checkbox.mouseClicked(p1, p2, p3);
            for (Slider slider : categoryPanel.sliders) slider.mouseClicked(p1, p2, p3);
        }
    }

    @Override
    public void categoryPanelDraw(CategoryPanel categoryPanel, float x, float y) {
        int alpha = (int) (255 * progress);
        float panelX = categoryPanel.categoryButton.panel.x + 45 + categoryPanel.categoryButton.panel.dragX;
        float panelY = categoryPanel.categoryButton.panel.y + 10 + categoryPanel.categoryButton.panel.dragY;
        float panelWidth = 290;
        float panelHeight = 320;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        prepareScissor(panelX, panelY, panelWidth, panelHeight);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, categoryPanel.scrollY, 0);

        for (GroupBox groupBox : categoryPanel.groupBoxes) {
            groupBox.draw(x, y - categoryPanel.scrollY);
        }
        for (TextBox tb : categoryPanel.textBoxes) {
            if (categoryPanel.visible) tb.draw(x, y - categoryPanel.scrollY);
        }
        for (Button button : categoryPanel.buttons) {
            button.draw(x, y - categoryPanel.scrollY);
        }
        for (Checkbox checkbox : categoryPanel.checkboxes) {
            checkbox.draw(x, y - categoryPanel.scrollY);
        }
        for (Slider slider : categoryPanel.sliders) {
            slider.draw(x, y - categoryPanel.scrollY);
        }

        List<MultiDropdownBox> multiList = new ArrayList<>(categoryPanel.multiDropdownBoxes);
        Collections.reverse(multiList);
        for (MultiDropdownBox db : multiList) db.draw(x, y - categoryPanel.scrollY);

        List<DropdownBox> list = new ArrayList<>(categoryPanel.dropdownBoxes);
        Collections.reverse(list);
        for (DropdownBox db : list) db.draw(x, y - categoryPanel.scrollY);

        GlStateManager.popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void categoryPanelMouseMovedOrUp(CategoryPanel categoryPanel, int x, int y, int button) {
        for (Slider slider : categoryPanel.sliders) slider.mouseReleased(x, y, button);
    }

    @Override
    public void groupBoxConstructor(GroupBox groupBox, float x, float y) {}

    @Override
    public void groupBoxMouseClicked(GroupBox groupBox, int p1, int p2, int p3) {}

    @Override
    public void groupBoxDraw(GroupBox groupBox, float x, float y) {
        int alpha = (int) (255 * progress);
        float xOff = groupBox.x + groupBox.categoryPanel.categoryButton.panel.dragX - 2.5F;
        float yOff = groupBox.y + groupBox.categoryPanel.categoryButton.panel.dragY + 10;

        RenderingUtil.rectangleBordered(xOff, yOff - 6, xOff + groupBox.width, yOff + groupBox.height, 0.5, Colors.getColor(0, 0), Colors.getColor(10, alpha));
        RenderingUtil.rectangleBordered(xOff + 0.5, yOff - 5.5, xOff + groupBox.width - 0.5, yOff + groupBox.height - 0.5, 0.5, Colors.getColor(17, alpha), Colors.getColor(48, alpha));

        if (groupBox.renderLabel) {
            getFont().drawStringWithShadow(groupBox.label, xOff + 6, yOff - 6.5F, Colors.getColor(220, alpha));
        }
    }

    @Override
    public void groupBoxMouseMovedOrUp(GroupBox groupBox, int x, int y, int button) {}

    @Override
    public void handleMouseInput(MainPanel panel) {
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            for (CategoryButton button : panel.typeButton) {
                if (button.enabled && button.categoryPanel != null) {
                    if (dWheel > 0) {
                        button.categoryPanel.scrollY = Math.min(0, button.categoryPanel.scrollY + 15);
                    } else {
                        button.categoryPanel.scrollY -= 15;
                    }
                }
            }
        }
    }

    @Override
    public void slButtonDraw(SLButton slButton, float x, float y, MainPanel panel) {}

    @Override
    public void slButtonMouseClicked(SLButton slButton, float x, float y, int button, MainPanel panel) {}

    @Override
    public void buttonContructor(Button p0, CategoryPanel panel) {}

    @Override
    public void buttonMouseClicked(Button p0, int p2, int p3, int p4, CategoryPanel panel) {
        if (panel.categoryButton.enabled) {
            float xOff = panel.categoryButton.panel.dragX;
            float yOff = panel.categoryButton.panel.dragY;
            boolean hovering = p2 >= p0.x + xOff && p3 >= p0.y + yOff && p2 <= p0.x + 35 + xOff && p3 <= p0.y + 6 + yOff;
            if (hovering) {
                if (p4 == 0) {
                    if (!p0.isBinding) {
                        p0.module.toggle();
                        p0.enabled = p0.module.isEnabled();
                    } else {
                        p0.isBinding = false;
                    }
                } else if (p4 == 1) {
                    if (p0.isBinding) {
                        p0.module.setKey(Keyboard.getKeyIndex("NONE"));
                        p0.isBinding = false;
                    } else {
                        p0.isBinding = true;
                    }
                }
            } else if (p0.isBinding) {
                p0.isBinding = false;
            }
        }
    }

    @Override
    public void buttonDraw(Button p0, float p2, float p3, CategoryPanel panel) {
        if (panel.categoryButton.enabled) {
            int alpha = (int) (255 * progress);
            float xOff = panel.categoryButton.panel.dragX;
            float yOff = panel.categoryButton.panel.dragY;

            GlStateManager.pushMatrix();
            RenderingUtil.rectangleBordered(p0.x + xOff + 0.6, p0.y + yOff + 0.6, p0.x + 6 + xOff + -0.6, p0.y + 6 + yOff + -0.6, 0.5, Colors.getColor(0, 0), Colors.getColor(10, alpha));
            RenderingUtil.drawGradient(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + 6 + xOff + -1, p0.y + 6 + yOff + -1, Colors.getColor(76, alpha), Colors.getColor(51, alpha));

            p0.enabled = p0.module.isEnabled();
            boolean hovering = p2 >= p0.x + xOff && p3 >= p0.y + yOff && p2 <= p0.x + 35 + xOff && p3 <= p0.y + 6 + yOff;

            getFont().drawStringWithShadow(p0.module.getLabel(), (p0.x + xOff + 3), (p0.y + 0.5f + yOff - 7), Colors.getColor(220, alpha));
            getSmallFont().drawStringWithShadow("Enable", (p0.x + 7.6f + xOff), (p0.y + 1 + yOff), Colors.getColor(185, alpha));

            String keyName = Keyboard.getKeyName(p0.module.getKey());
            String meme = (keyName != null && !keyName.equalsIgnoreCase("NONE")) ? "[" + keyName + "]" : "[-]";
            GlStateManager.pushMatrix();
            GlStateManager.translate((p0.x + xOff + 29), (p0.y + 1f + yOff), 0);
            GlStateManager.enableBlend();
            GlStateManager.scale(0.5, 0.5, 0.5);
            mc.fontRendererObj.drawStringWithShadow(meme, 0, 0, p0.isBinding ? Colors.getColor(216, 56, 56, alpha) : Colors.getColor(75, alpha));
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();

            GlStateManager.popMatrix();

            if (p0.enabled) {
                RenderingUtil.drawGradient(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + xOff + 5, p0.y + yOff + 5, Colors.getColor(161, 82, 230, alpha), Colors.getColor(161, 82, 230, (int) (120 * progress)));
            }
            if (hovering && !p0.enabled) {
                RenderingUtil.rectangle(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + xOff + 5, p0.y + yOff + 5, Colors.getColor(255, (int) (40 * progress)));
            }

            if (hovering) {
                String desc = p0.module.getDescription();
                if (desc != null && !desc.isEmpty()) {
                    getSmallFont().drawStringWithShadow(desc, (panel.categoryButton.panel.x + 2 + panel.categoryButton.panel.dragX) + 55, (panel.categoryButton.panel.y + 9 + panel.categoryButton.panel.dragY), Colors.getColor(220, alpha));
                }
            }
        }
    }

    @Override
    public void buttonKeyPressed(Button button, int key) {
        if (button.isBinding && key != 0) {
            int keyToBind = key;
            if (key == 1 || key == Keyboard.KEY_BACK) {
                keyToBind = Keyboard.getKeyIndex("NONE");
            }
            button.module.setKey(keyToBind);
            button.isBinding = false;
        }
    }

    @Override
    public void checkBoxMouseClicked(Checkbox p0, int p2, int p3, int p4, CategoryPanel panel) {
        if (panel.categoryButton.enabled) {
            float xOff = panel.categoryButton.panel.dragX;
            float yOff = panel.categoryButton.panel.dragY;
            boolean hovering = p2 >= p0.x + xOff && p3 >= p0.y + yOff && p2 <= p0.x + 35 + xOff && p3 <= p0.y + 6 + yOff;
            if (hovering && p4 == 0) {
                p0.setting.setValue(!p0.setting.getValue());
            }
        }
    }

    @Override
    public void checkBoxDraw(Checkbox p0, float p2, float p3, CategoryPanel panel) {
        if (panel.categoryButton.enabled) {
            int alpha = (int) (255 * progress);
            float xOff = panel.categoryButton.panel.dragX;
            float yOff = panel.categoryButton.panel.dragY;

            GlStateManager.pushMatrix();
            String xd = p0.name;
            if (xd.length() > 0) xd = xd.charAt(0) + xd.substring(1).toLowerCase();
            getSmallFont().drawStringWithShadow(xd, (p0.x + 7.5f + xOff), (p0.y + 1 + yOff), Colors.getColor(185, alpha));

            RenderingUtil.rectangleBordered(p0.x + xOff + 0.6, p0.y + yOff + 0.6, p0.x + 6 + xOff + -0.6, p0.y + 6 + yOff + -0.6, 0.5, Colors.getColor(0, 0), Colors.getColor(10, alpha));
            RenderingUtil.drawGradient(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + 6 + xOff + -1, p0.y + 6 + yOff + -1, Colors.getColor(76, alpha), Colors.getColor(51, alpha));

            p0.enabled = p0.setting.getValue();
            boolean hovering = p2 >= p0.x + xOff && p3 >= p0.y + yOff && p2 <= p0.x + 35 + xOff && p3 <= p0.y + 6 + yOff;

            if (p0.enabled) {
                RenderingUtil.drawGradient(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + xOff + 5, p0.y + yOff + 5, Colors.getColor(161, 82, 230, alpha), Colors.getColor(161, 82, 230, (int) (120 * progress)));
            }
            if (hovering && !p0.enabled) {
                RenderingUtil.rectangle(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + xOff + 5, p0.y + yOff + 5, Colors.getColor(255, (int) (40 * progress)));
            }

            GlStateManager.popMatrix();
        }
    }

    @Override
    public void dropDownContructor(DropdownBox p0, float x, float u, CategoryPanel panel) {
        int y = 10;
        for (Object value : p0.option.getValues()) {
            p0.buttons.add(new DropdownButton(value.toString(), x, u + y, p0));
            y += 9;
        }
    }

    @Override
    public void dropDownMouseClicked(DropdownBox dropDown, int mouseX, int mouseY, int mouse, CategoryPanel panel) {
        for (DropdownButton db : dropDown.buttons) {
            if (dropDown.active && dropDown.panel.visible) db.mouseClicked(mouseX, mouseY, mouse);
        }
        if ((mouseX >= panel.categoryButton.panel.dragX + dropDown.x) && (mouseY >= panel.categoryButton.panel.dragY + dropDown.y) && (mouseX <= panel.categoryButton.panel.dragX + dropDown.x + 40) && (mouseY <= panel.categoryButton.panel.dragY + dropDown.y + 8) && (mouse == 0) && dropDown.panel.visible) {
            dropDown.active = (!dropDown.active);
        } else {
            dropDown.active = false;
        }
    }

    @Override
    public void dropDownDraw(DropdownBox p0, float p2, float p3, CategoryPanel panel) {
        int alpha = (int) (255 * progress);
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        boolean hovering = (p2 >= panel.categoryButton.panel.dragX + p0.x) && (p3 >= panel.categoryButton.panel.dragY + p0.y) && (p2 <= panel.categoryButton.panel.dragX + p0.x + 40) && (p3 <= panel.categoryButton.panel.dragY + p0.y + 9);

        RenderingUtil.rectangle(p0.x + xOff - 0.3, p0.y + yOff - 0.3, p0.x + xOff + 40 + 0.3, p0.y + yOff + 9 + 0.3, Colors.getColor(10, alpha));
        RenderingUtil.drawGradient(p0.x + xOff, p0.y + yOff, p0.x + xOff + 40, p0.y + yOff + 9, Colors.getColor(31, alpha), Colors.getColor(36, alpha));
        if (hovering) {
            RenderingUtil.rectangleBordered(p0.x + xOff, p0.y + yOff, p0.x + xOff + 40, p0.y + yOff + 9, 0.3, Colors.getColor(0, 0), Colors.getColor(90, alpha));
        }
        getSmallFont().drawStringWithShadow(p0.option.getLabel(), (p0.x + xOff + 1), (p0.y - 6 + yOff), Colors.getColor(185, alpha));

        GlStateManager.pushMatrix();
        GlStateManager.translate((p0.x + xOff + 38 - (p0.active ? 2.5 : 0)), (p0.y + 4.5 + yOff), 0);
        GlStateManager.rotate(p0.active ? 270 : 90, 0, 0, 90);
        RenderingUtil.rectangle(0 - 1, 0, 0.5 - 1, 2.5, Colors.getColor(0, alpha));
        RenderingUtil.rectangle(0.5 - 1, 0, 0, 2.5, Colors.getColor(151, alpha));
        RenderingUtil.rectangle(0, 0.5, 1.5 - 1, 2, Colors.getColor(151, alpha));
        RenderingUtil.rectangle(1.5 - 1, 1, 2 - 1, 1.5, Colors.getColor(151, alpha));
        GlStateManager.popMatrix();

        getSmallFont().drawString(p0.option.getValue().toString(), (p0.x + 4 + xOff) - 1, (p0.y + 3f + yOff), Colors.getColor(255, alpha));
    }

    @Override
    public void dropDownButtonMouseClicked(DropdownButton p0, DropdownBox p1, int x, int y, int mouse) {
        if ((x >= p1.panel.categoryButton.panel.dragX + p0.x) && (y >= p1.panel.categoryButton.panel.dragY + p0.y) && (x <= p1.panel.categoryButton.panel.dragX + p0.x + 40) && (y <= p1.panel.categoryButton.panel.dragY + p0.y + 8.5) && (mouse == 0)) {
            for (Object val : p1.option.getValues()) {
                if (val.toString().equalsIgnoreCase(p0.name)) {
                    p1.option.setValueObj(val);
                    break;
                }
            }
            p1.active = false;
        }
    }

    @Override
    public void dropDownButtonDraw(DropdownButton p0, DropdownBox p1, float x, float y) {
        float xOff = p1.panel.categoryButton.panel.dragX;
        float yOff = p1.panel.categoryButton.panel.dragY;
        boolean hovering = (x >= xOff + p0.x) && (y >= yOff + p0.y) && (x <= xOff + p0.x + 40) && (y <= yOff + p0.y + 8.5);
        int alpha = (int) (255 * progress);
        boolean active = p1.option.getValue().toString().equalsIgnoreCase(p0.name);
        CustomFontRenderer font = getSmallFont();
        font.drawStringWithShadow((hovering || active ? "\247l" : "") + p0.name, (p0.x + 3 + xOff), (p0.y + 2f + yOff), active && !hovering ? Colors.getColor(161, 82, 230, alpha) : Colors.getColor(255, alpha));
    }

    @Override
    public void multiDropDownContructor(MultiDropdownBox p0, float x, float u, CategoryPanel panel) {
        int y = 10;
        for (Enum<?> value : p0.multiMode.getValues()) {
            p0.buttons.add(new MultiDropdownButton(value.toString(), x, u + y, p0, p0.multiMode.isSelected(value)));
            y += 9;
        }
    }

    @Override
    public void multiDropDownMouseClicked(MultiDropdownBox p0, int x, int u, int mouse, CategoryPanel panel) {
        for (MultiDropdownButton db : p0.buttons) {
            if (p0.active && p0.panel.visible) db.mouseClicked(x, u, mouse);
        }
        if (mouse == 0) {
            if ((x >= panel.categoryButton.panel.dragX + p0.x) && (u >= panel.categoryButton.panel.dragY + p0.y) && (x <= panel.categoryButton.panel.dragX + p0.x + 40) && (u <= panel.categoryButton.panel.dragY + p0.y + 8) && p0.panel.visible) {
                p0.active = (!p0.active);
            } else if (!((x >= panel.categoryButton.panel.dragX + p0.x) && (u >= panel.categoryButton.panel.dragY + p0.y + 8) && (x <= panel.categoryButton.panel.dragX + p0.x + 40) && (u <= panel.categoryButton.panel.dragY + p0.y + 8 + p0.buttons.size() * 9))) {
                p0.active = false;
            }
        }
    }

    @Override
    public void multiDropDownDraw(MultiDropdownBox p0, float x, float y, CategoryPanel panel) {
        int alpha = (int) (255 * progress);
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        boolean hovering = (x >= panel.categoryButton.panel.dragX + p0.x) && (y >= panel.categoryButton.panel.dragY + p0.y) && (x <= panel.categoryButton.panel.dragX + p0.x + 40) && (y <= panel.categoryButton.panel.dragY + p0.y + 9);

        RenderingUtil.rectangle(p0.x + xOff - 0.3, p0.y + yOff - 0.3, p0.x + xOff + 40 + 0.3, p0.y + yOff + 9 + 0.3, Colors.getColor(10, alpha));
        RenderingUtil.drawGradient(p0.x + xOff, p0.y + yOff, p0.x + xOff + 40, p0.y + yOff + 9, Colors.getColor(31, alpha), Colors.getColor(36, alpha));
        if (hovering) {
            RenderingUtil.rectangleBordered(p0.x + xOff, p0.y + yOff, p0.x + xOff + 40, p0.y + yOff + 9, 0.3, Colors.getColor(0, 0), Colors.getColor(90, alpha));
        }
        getSmallFont().drawStringWithShadow(p0.name, (p0.x + xOff + 1), (p0.y - 6 + yOff), Colors.getColor(185, alpha));

        GlStateManager.pushMatrix();
        GlStateManager.translate((p0.x + xOff + 38 - (p0.active ? 2.5 : 0)), (p0.y + 4.5 + yOff), 0);
        GlStateManager.rotate(p0.active ? 270 : 90, 0, 0, 90);
        RenderingUtil.rectangle(0 - 1, 0, 0.5 - 1, 2.5, Colors.getColor(0, alpha));
        RenderingUtil.rectangle(0.5 - 1, 0, 0, 2.5, Colors.getColor(151, alpha));
        RenderingUtil.rectangle(0, 0.5, 1.5 - 1, 2, Colors.getColor(151, alpha));
        RenderingUtil.rectangle(1.5 - 1, 1, 2 - 1, 1.5, Colors.getColor(151, alpha));
        GlStateManager.popMatrix();

        List<String> enabled = new ArrayList<>();
        for (Enum<?> val : p0.multiMode.getValues()) {
            if (p0.multiMode.isSelected(val)) {
                String s = val.toString();
                if (s.length() > 0) s = s.charAt(0) + s.substring(1).toLowerCase();
                enabled.add(s);
            }
        }
        String str = enabled.isEmpty() ? "None" : enabled.toString().replace("[", "").replace("]", "");
        getSmallFont().drawString(str, (p0.x + 4 + xOff) - 1, (p0.y + 3f + yOff), Colors.getColor(255, alpha));

        if (p0.active) {
            int i = p0.buttons.size();
            RenderingUtil.rectangle(p0.x + xOff - 0.3, p0.y + 10 + yOff - 0.3, p0.x + xOff + 40 + 0.3, p0.y + yOff + 9 + (9 * i) + 0.3, Colors.getColor(10, alpha));
            RenderingUtil.drawGradient(p0.x + xOff, p0.y + yOff + 10, p0.x + xOff + 40, p0.y + yOff + 9 + (9 * i), Colors.getColor(31, alpha), Colors.getColor(36, alpha));
        }
    }

    @Override
    public void multiDropDownButtonMouseClicked(MultiDropdownButton p0, MultiDropdownBox p1, int x, int y, int mouse) {
        if ((x >= p1.panel.categoryButton.panel.dragX + p0.x) && (y >= p1.panel.categoryButton.panel.dragY + p0.y) && (x <= p1.panel.categoryButton.panel.dragX + p0.x + 40) && (y <= p1.panel.categoryButton.panel.dragY + p0.y + 8.5) && (mouse == 0)) {
            p0.selected = !p0.selected;
            for (int i = 0; i < p1.multiMode.getValues().length; i++) {
                if (p1.multiMode.getValues()[i].toString().equalsIgnoreCase(p0.name)) {
                    p1.multiMode.setValue(i);
                    break;
                }
            }
        }
    }

    @Override
    public void multiDropDownButtonDraw(MultiDropdownButton p0, MultiDropdownBox p1, float x, float y) {
        float xOff = p1.panel.categoryButton.panel.dragX;
        float yOff = p1.panel.categoryButton.panel.dragY;
        int alpha = (int) (255 * progress);
        boolean hovering = (x >= xOff + p0.x) && (y >= yOff + p0.y) && (x <= xOff + p0.x + 40) && (y <= yOff + p0.y + 8.5);
        String label = p0.name.length() > 0 ? p0.name.charAt(0) + p0.name.substring(1).toLowerCase() : p0.name;
        CustomFontRenderer font = getSmallFont();
        font.drawStringWithShadow((hovering || p0.selected ? "\247l" : "") + label, (p0.x + 3 + xOff), (p0.y + 2f + yOff), p0.selected && !hovering ? Colors.getColor(161, 82, 230, alpha) : Colors.getColor(255, alpha));
    }

    @Override
    public void categoryButtonMouseReleased(CategoryButton categoryButton, int x, int y, int button) {
        categoryButton.categoryPanel.mouseReleased(x, y, button);
    }

    @Override
    public void SliderContructor(Slider p0, CategoryPanel panel) {
        double percent = (p0.setting.getValue() - p0.setting.getMin()) / (p0.setting.getMax() - p0.setting.getMin());
        p0.dragX = 40 * percent;
    }

    @Override
    public void SliderMouseClicked(Slider slider, int mouseX, int mouseY, int mouse, CategoryPanel panel) {
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        if (panel.visible && mouseX >= panel.x + xOff + slider.x && mouseY >= yOff + panel.y + slider.y - 6 && mouseX <= xOff + panel.x + slider.x + 38.0f && mouseY <= yOff + panel.y + slider.y + 3.5F && mouse == 0) {
            slider.dragging = true;
            slider.lastDragX = mouseX;
            slider.dragX = (mouseX - (slider.x + xOff));
        }
        if (panel.visible && mouseX >= panel.x + xOff + slider.x - 3 && mouseY >= yOff + panel.y + slider.y && mouseX <= xOff + panel.x + slider.x - 0.5 && mouseY <= yOff + panel.y + slider.y + 2 && mouse == 0) {
            double value = slider.setting.getValue();
            if (value - slider.setting.getIncrement() >= slider.setting.getMin()) {
                slider.setting.setValue(MathUtils.roundToDecimalPlace(value - slider.setting.getIncrement(), slider.setting.getIncrement()));
            } else {
                slider.setting.setValue(slider.setting.getMin());
            }
        } else if (panel.visible && mouseX >= panel.x + xOff + slider.x + 38.5 && mouseY >= yOff + panel.y + slider.y && mouseX <= xOff + panel.x + slider.x + 41 && mouseY <= yOff + panel.y + slider.y + 2 && mouse == 0) {
            double value = slider.setting.getValue();
            if (value + slider.setting.getIncrement() <= slider.setting.getMax()) {
                slider.setting.setValue(MathUtils.roundToDecimalPlace(value + slider.setting.getIncrement(), slider.setting.getIncrement()));
            } else {
                slider.setting.setValue(slider.setting.getMax());
            }
        }
    }

    @Override
    public void SliderMouseMovedOrUp(Slider slider, int mouseX, int mouseY, int mouse, CategoryPanel panel) {
        if (mouse == 0) {
            if (slider.dragging) {
                final double percent = MathHelper.clamp_double(slider.dragX / 38, 0, 1);
                double value;
                if (percent <= 0) {
                    value = slider.setting.getMin();
                } else if (percent >= 1) {
                    value = slider.setting.getMax();
                } else {
                    value = MathUtils.roundToDecimalPlace((percent * (slider.setting.getMax() - slider.setting.getMin())) + slider.setting.getMin(), slider.setting.getIncrement());
                }
                slider.setting.setValue(value);
            }
            slider.dragging = false;
        }
    }

    @Override
    public void SliderDraw(Slider slider, float x, float y, CategoryPanel panel) {
        if (panel.visible) {
            int alpha = (int) (255 * progress);
            float xOff = panel.categoryButton.panel.dragX;
            float yOff = panel.categoryButton.panel.dragY;

            GlStateManager.pushMatrix();

            final double percent = MathHelper.clamp_double(slider.dragX / 38, 0, 1);
            double value;
            if (percent <= 0) value = slider.setting.getMin();
            else if (percent >= 1) value = slider.setting.getMax();
            else value = MathUtils.roundToDecimalPlace((percent * (slider.setting.getMax() - slider.setting.getMin())) + slider.setting.getMin(), slider.setting.getIncrement());

            float sliderX = (float) (((slider.setting.getValue() - slider.setting.getMin()) / (slider.setting.getMax() - slider.setting.getMin())) * 38);
            RenderingUtil.rectangle(slider.x + xOff - 0.3, slider.y + yOff - 0.3, slider.x + xOff + 38 + 0.3, slider.y + yOff + 2.5 + 0.3, Colors.getColor(10, alpha));
            RenderingUtil.drawGradient(slider.x + xOff, slider.y + yOff, slider.x + xOff + 38, slider.y + yOff + 2.5, Colors.getColor(46, alpha), Colors.getColor(27, alpha));

            if (slider.setting.getMin() < 0 && slider.setting.getMax() > 0 && slider.setting.getMax() == -slider.setting.getMin()) {
                if (sliderX >= 19) {
                    RenderingUtil.drawGradient(slider.x + xOff + 19, slider.y + yOff, slider.x + xOff + sliderX, slider.y + yOff + 2.5, Colors.getColor(161, 82, 230, alpha), Colors.getColor(161, 82, 230, (int) (120 * progress)));
                } else {
                    RenderingUtil.drawGradient(slider.x + xOff + sliderX, slider.y + yOff, slider.x + xOff + 19, slider.y + yOff + 2.5, Colors.getColor(161, 82, 230, alpha), Colors.getColor(161, 82, 230, (int) (120 * progress)));
                }
            } else {
                RenderingUtil.drawGradient(slider.x + xOff, slider.y + yOff, slider.x + xOff + sliderX, slider.y + yOff + 2.5, Colors.getColor(161, 82, 230, alpha), Colors.getColor(161, 82, 230, (int) (120 * progress)));
            }

            boolean hoverMinus = x >= panel.x + xOff + slider.x - 3 && y >= yOff + panel.y + slider.y && x <= xOff + panel.x + slider.x - 0.5 && y <= yOff + panel.y + slider.y + 2;
            boolean hoverPlus = x >= panel.x + xOff + slider.x + 38.5 && y >= yOff + panel.y + slider.y && x <= xOff + panel.x + slider.x + 41 && y <= yOff + panel.y + slider.y + 2;
            RenderingUtil.rectangle(slider.x + xOff - 2.5, slider.y + yOff + 1, slider.x + xOff - 1, slider.y + yOff + 1.5, Colors.getColor(hoverMinus ? 220 : 120, alpha));
            RenderingUtil.rectangle(slider.x + xOff + 39, slider.y + yOff + 1, slider.x + xOff + 40.5, slider.y + yOff + 1.5, Colors.getColor(hoverPlus ? 220 : 120, alpha));
            RenderingUtil.rectangle(slider.x + xOff + 39.5, slider.y + yOff + 0.5, slider.x + xOff + 40, slider.y + yOff + 2, Colors.getColor(hoverPlus ? 220 : 120, alpha));

            String xd = slider.setting.getLabel();
            if (xd.length() > 0) xd = xd.charAt(0) + xd.substring(1).toLowerCase();

            double settingValue = slider.setting.getValue();
            String labelText = formatSliderValue(settingValue, slider.setting);
            getSmallFont().drawStringWithShadow(xd, (slider.x + xOff), (slider.y - 6 + yOff), Colors.getColor(185, alpha));

            float strWidth = getFont().getStringWidth(labelText);
            getBoldFont().drawStringWithShadow(labelText, (slider.x + xOff + 42) - strWidth, (slider.y - 6 + yOff), Colors.getColor(220, alpha));

            if (slider.dragging) {
                float divide = Math.abs((y - (slider.y + yOff + 2))) / 4;
                if (divide < 1) divide = 1;
                double mouseDiff = (x - slider.lastDragX);
                slider.dragX = slider.dragX + (mouseDiff / divide);
                slider.lastDragX = x;
                slider.setting.setValue(value);
            }

            if (slider.setting.getValue() <= slider.setting.getMin()) {
                slider.setting.setValue(slider.setting.getMin());
            } else if (slider.setting.getValue() >= slider.setting.getMax()) {
                slider.setting.setValue(slider.setting.getMax());
            }

            GlStateManager.popMatrix();
        }
    }

    private String formatSliderValue(double value, NumberProperty prop) {
        if (prop.getRepresentation() == Representation.INT) {
            return String.valueOf((int) value);
        }
        if (prop.getRepresentation() == Representation.PERCENTAGE) {
            return (int) (value * 100) + "%";
        }
        if (prop.getRepresentation() == Representation.MILLISECONDS) {
            return (int) value + "ms";
        }
        if (prop.getRepresentation() == Representation.DISTANCE) {
            return String.format("%.1f", value) + "m";
        }
        if (prop.getRepresentation() == Representation.DOUBLE) {
            double inc = prop.getIncrement();
            int decimals = inc >= 1 ? 0 : (int) Math.ceil(-Math.log10(inc));
            return String.format("%." + decimals + "f", value);
        }
        return String.valueOf((int) value);
    }

    @Override
    public void textBoxDraw(TextBox textBox, float x, float y) {
        CategoryPanel panel = textBox.panel;
        int alpha = (int) (255 * progress);
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;

        if (textBox.cursorPos > textBox.textString.length()) textBox.cursorPos = textBox.textString.length();
        else if (textBox.cursorPos < 0) textBox.cursorPos = 0;

        if (!textBox.isFocused && !textBox.isTyping && !textBox.textString.equals(textBox.setting.getValue())) {
            textBox.textString = textBox.setting.getValue();
        }

        int selectedChar = textBox.cursorPos;
        boolean hovering = (x >= xOff + textBox.x) && (y >= yOff + textBox.y) && (x <= xOff + textBox.x + 84) && (y <= yOff + textBox.y + 9);

        RenderingUtil.rectangle(textBox.x + xOff - 0.3, textBox.y + yOff - 0.3, textBox.x + xOff + 84 + 0.3, textBox.y + yOff + 7.5F + 0.3, Colors.getColor(10, alpha));
        RenderingUtil.drawGradient(textBox.x + xOff, textBox.y + yOff, textBox.x + xOff + 84, textBox.y + yOff + 7.5F, Colors.getColor(31, alpha), Colors.getColor(36, alpha));
        if (hovering || textBox.isFocused) {
            RenderingUtil.rectangleBordered(textBox.x + xOff, textBox.y + yOff, textBox.x + xOff + 84, textBox.y + yOff + 7.5F, 0.3, Colors.getColor(0, 0), textBox.isFocused ? Colors.getColor(130, alpha) : Colors.getColor(90, alpha));
        }

        String xd = textBox.setting.getLabel();
        if (xd.length() > 0) xd = xd.charAt(0) + xd.substring(1).toLowerCase();
        getSmallFont().drawStringWithShadow(xd, (textBox.x + xOff + 1), (textBox.y - 6 + yOff), Colors.getColor(185, alpha));

        RenderingUtil.rectangle(textBox.x + xOff + 2, textBox.y + yOff, textBox.x + xOff + 82, textBox.y + yOff + 7.5F, Colors.getColor(90, alpha));
        getSmallFont().drawString(textBox.textString, (textBox.x + 1.5F + xOff) - textBox.offset, (textBox.y + 2 + yOff), Colors.getColor(151, alpha));

        if (textBox.cursorAlpha >= 270) textBox.backwards = true;
        else if (textBox.cursorAlpha <= 40) textBox.backwards = false;
        textBox.cursorAlpha += textBox.backwards ? -15 : 15;

        if (textBox.isFocused) {
            float width = getSmallFont().getStringWidth(textBox.textString.substring(0, Math.min(selectedChar, textBox.textString.length())));
            float posX = textBox.x + xOff + width - textBox.offset;
            RenderingUtil.rectangle(posX - 0.5, textBox.y + yOff + 1.5, posX, textBox.y + yOff + 6, Colors.getColor(220, (int) (textBox.cursorAlpha * progress)));
        } else {
            textBox.cursorAlpha = 255;
        }
    }

    @Override
    public void textBoxMouseClicked(TextBox textBox, int x, int y, int mouseID) {
        CategoryPanel panel = textBox.panel;
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        boolean hovering = (x >= xOff + textBox.x) && (y >= yOff + textBox.y) && (x <= xOff + textBox.x + 84) && (y <= yOff + textBox.y + 9);

        if (hovering && mouseID == 0 && !textBox.isFocused) {
            float width = getSmallFont().getStringWidth(textBox.textString.substring(0, textBox.cursorPos));
            float barOffset = (width - textBox.offset);
            if (barOffset < 0) textBox.offset += barOffset;
            if (barOffset > 82) textBox.offset += (barOffset - 82);
            textBox.isFocused = true;
            Keyboard.enableRepeatEvents(true);
        } else {
            if (!hovering) {
                textBox.isFocused = false;
                textBox.isTyping = false;
            }
        }
    }

    @Override
    public void textBoxKeyPressed(TextBox textBox, int key) {
        char letter = Keyboard.getEventCharacter();
        if (letter == '\r') {
            textBox.isFocused = false;
            textBox.isTyping = false;
            textBox.setting.setValue(textBox.textString);
            return;
        }

        if (textBox.isFocused) {
            if (GuiScreen.isKeyComboCtrlC(key)) {
                GuiScreen.setClipboardString(textBox.textString);
                return;
            }
            if (GuiScreen.isKeyComboCtrlV(key)) {
                String oldString = textBox.textString;
                StringBuilder stringBuilder = new StringBuilder(oldString);
                String input = ChatAllowedCharacters.filterAllowedCharacters(GuiScreen.getClipboardString());
                stringBuilder.insert(textBox.cursorPos, input);
                textBox.textString = ChatAllowedCharacters.filterAllowedCharacters(stringBuilder.toString());
                textBox.cursorPos += input.length();
                textBox.setting.setValue(textBox.textString);
                return;
            }

            switch (key) {
                case Keyboard.KEY_LEFT: {
                    if (textBox.cursorPos > 0) textBox.cursorPos--;
                    float width = getSmallFont().getStringWidth(textBox.textString.substring(0, textBox.cursorPos));
                    float barOffset = (width - textBox.offset) - 2;
                    if (barOffset < 0) textBox.offset += barOffset;
                    break;
                }
                case Keyboard.KEY_RIGHT: {
                    if (textBox.cursorPos < textBox.textString.length()) textBox.cursorPos++;
                    float width = getSmallFont().getStringWidth(textBox.textString.substring(0, textBox.cursorPos));
                    float barOffset = (width - textBox.offset);
                    if (barOffset > 82) textBox.offset += (barOffset - 82);
                    break;
                }
                case Keyboard.KEY_DOWN: {
                    textBox.cursorPos = textBox.textString.length();
                    float width = getSmallFont().getStringWidth(textBox.textString.substring(0, textBox.cursorPos));
                    float barOffset = (width - textBox.offset);
                    if (barOffset > 82) textBox.offset += (barOffset - 82);
                    break;
                }
                case Keyboard.KEY_UP: {
                    textBox.cursorPos = 0;
                    textBox.offset = 0;
                    break;
                }
                case Keyboard.KEY_BACK: {
                    try {
                        if (textBox.textString.length() == 0) break;
                        String oldString = textBox.textString;
                        StringBuilder stringBuilder = new StringBuilder(oldString);
                        stringBuilder.deleteCharAt(textBox.cursorPos - 1);
                        textBox.textString = ChatAllowedCharacters.filterAllowedCharacters(stringBuilder.toString());
                        textBox.cursorPos--;
                        if (getSmallFont().getStringWidth(oldString) > 82 && textBox.offset > 0) {
                            float newTextWidth = getSmallFont().getStringWidth(textBox.textString);
                            float oldTextWidth = getSmallFont().getStringWidth(oldString);
                            float charWidth = newTextWidth - oldTextWidth;
                            if (newTextWidth <= 82 && oldTextWidth - 82 > charWidth) charWidth = 82 - oldTextWidth;
                            textBox.offset += charWidth;
                        }
                        if (textBox.cursorPos > textBox.textString.length()) textBox.cursorPos = textBox.textString.length();
                        textBox.setting.setValue(textBox.textString);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        if (textBox.isFocused && ChatAllowedCharacters.isAllowedCharacter(letter)) {
            if (!Keyboard.areRepeatEventsEnabled()) Keyboard.enableRepeatEvents(true);
            if (!textBox.isTyping) textBox.isTyping = true;

            String oldString = textBox.textString;
            StringBuilder stringBuilder = new StringBuilder(oldString);
            stringBuilder.insert(textBox.cursorPos, letter);
            textBox.textString = ChatAllowedCharacters.filterAllowedCharacters(stringBuilder.toString());

            if (textBox.cursorPos > textBox.textString.length()) {
                textBox.cursorPos = textBox.textString.length();
            } else if (textBox.cursorPos == oldString.length() && textBox.textString.startsWith(oldString)) {
                textBox.cursorPos += textBox.textString.length() - oldString.length();
            } else {
                textBox.cursorPos++;
                float width = getSmallFont().getStringWidth(textBox.textString.substring(0, textBox.cursorPos));
                float barOffset = (width - textBox.offset);
                if (barOffset > 82) textBox.offset += (barOffset - 82);
            }
            float newTextWidth = getSmallFont().getStringWidth(textBox.textString);
            float oldTextWidth = getSmallFont().getStringWidth(oldString);
            if (newTextWidth > 82) {
                if (oldTextWidth < 82) oldTextWidth = 82;
                float charWidth = (newTextWidth - oldTextWidth);
                if (textBox.cursorPos == textBox.textString.length()) textBox.offset += charWidth;
            }
            textBox.setting.setValue(textBox.textString);
        }
    }

    private static class Translate {
        private double x, y;
        public Translate(double x, double y) { this.x = x; this.y = y; }
        public void interpolate(double newX, double newY, float speed) {
            x += (newX - x) * speed;
            y += (newY - y) * speed;
        }
        public double getX() { return x; }
        public double getY() { return y; }
    }

    private void prepareScissor(float x, float y, float width, float height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glScissor(
                (int) (x * scale),
                (int) ((sr.getScaledHeight() - (y + height)) * scale),
                (int) (width * scale),
                (int) (height * scale)
        );
    }
}