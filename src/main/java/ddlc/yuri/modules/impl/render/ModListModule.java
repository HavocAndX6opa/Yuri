package ddlc.yuri.modules.impl.render;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.client.ClickGUIModule;
import ddlc.yuri.modules.impl.render.WatermarkModule;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.misc.Translate;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@ModuleInfo(label = "Mod List", category = ModuleCategory.RENDER, description = "Shows the enabled mods on your HUD")
public class ModListModule extends Module implements IMinecraft {

    private final Property<Boolean> bg = new Property<>("Background", true);
    private final Property<Boolean> outline = new Property<>("Outline", false);
    private final Property<Boolean> line = new Property<>("Line", true, () -> !outline.getValue());
    private final Property<Boolean> hideVisuals = new Property<>("Hide Visuals", false);
    private static final Property<Boolean> useCustomFont = new Property<>("Use Custom Font", true);
    private final Property<Boolean> noSpaces = new Property<>("No Spaces", false);
    private final Property<Boolean> lowercase = new Property<>("Lowercase", false);
    private final ModeProperty<ColorMode> colorMode = new ModeProperty<>("Color Mode", ColorMode.FADE);

    private static final Map<Module, String> displayLabelCache = new HashMap<>();
    private static List<Module> moduleCache;

    public enum ColorMode {
        STATIC("Static"),
        FADE("Fade");

        public final String name;

        ColorMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (moduleCache == null) {
            return;
        }

        CustomFontRenderer fr = getActiveFont();
        if (fr == null) {
            return;
        }

        for (Module module : moduleCache) {
            displayLabelCache.put(module, getDisplayLabel(module));
        }

        moduleCache.sort(new LengthComparator());
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        renderArrayList();
    }

    private CustomFontRenderer getActiveFont() {
        return FontUtils.getFont("sf", 18);
    }

    private boolean isMcFontActive() {
        return !useCustomFont.getValue();
    }

    private int getTextWidth(CustomFontRenderer fr, String text) {
        if (text == null) {
            return 0;
        }

        if (isMcFontActive()) {
            return mc.fontRendererObj.getStringWidth(text);
        }

        return fr == null ? 0 : fr.getStringWidth(text);
    }

    private float drawText(CustomFontRenderer fr, String text, float x, float y, int color) {
        if (text == null) {
            return x;
        }

        if (isMcFontActive()) {
            return mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        }

        return fr.drawStringWithShadow(text, x, y, color);
    }

    private void renderArrayList() {
        CustomFontRenderer fr = getActiveFont();
        ScaledResolution sr = new ScaledResolution(mc);

        if (fr == null) {
            return;
        }

        boolean mcFont = isMcFontActive();

        float screenX = sr.getScaledWidth();
        float startY = 2;

        if (moduleCache == null) {
            updateModulePositions(sr);
        }

        float y = startY;
        float previousModuleWidth = -1;

        List<Module> filteredModules = new ArrayList<>();
        for (Module module : moduleCache) {
            if (hideVisuals.getValue() && module.getCategory() == ModuleCategory.RENDER) {
                continue;
            } else if (module instanceof ClickGUIModule || module instanceof WatermarkModule || module instanceof ModListModule) {
                continue;
            }
            filteredModules.add(module);
        }

        final int moduleCacheSize = filteredModules.size();
        int lastVisibleModuleIndex = moduleCacheSize - 1;

        for (; lastVisibleModuleIndex > 0; lastVisibleModuleIndex--) {
            if (filteredModules.get(lastVisibleModuleIndex).isVisible()) {
                break;
            }
        }

        int firstVisibleModuleIndex = -1;
        int visibleModuleCount = 0;

        for (int i = 0; i < moduleCacheSize; i++) {
            final Module module = filteredModules.get(i);
            final Translate translate = module.getTranslate();
            final String name = displayLabelCache.get(module);
            final float moduleWidth = getTextWidth(fr, name);
            final boolean visible = module.isVisible();

            if (visible) {
                if (firstVisibleModuleIndex == -1) {
                    firstVisibleModuleIndex = i;
                }
                translate.animate(screenX - moduleWidth - (line.getValue() && !outline.getValue() ? 2 : 1), y);
                y += 12;
            } else {
                translate.animate(screenX, y);
            }

            double translateX = translate.getX();
            double translateY = translate.getY();

            if (visible || translateX < screenX) {
                int aColor = getColorForModule(visibleModuleCount);
                double top = translateY - 2;

                if (bg.getValue()) {
                    Gui.drawRect(!line.getValue() && !outline.getValue() ? translateX - 2 : translateX - 3, translateY - 2, screenX + 3, translateY + 10, getColorForBG().getRGB());
                }

                float textX = line.getValue() ? (float) (translateX - 1f) : (float) translateX - 0.5f;

                drawText(fr,
                        name,
                        (float) textX,
                        (float) translateY,
                        aColor);

                if (outline.getValue()) {
                    Gui.drawRect(translateX - 3,
                            translateY - 2,
                            translateX - 2,
                            translateY + 10,
                            aColor);

                    double outlineTop = top - 1;
                    double outlineBottom = translateY + 10;

                    if (i != firstVisibleModuleIndex && moduleWidth - previousModuleWidth > 0) {
                        Gui.drawRect(translateX - 3,
                                outlineTop,
                                screenX - previousModuleWidth - 6,
                                outlineTop + 1,
                                aColor);
                    }

                    if (i != lastVisibleModuleIndex) {
                        Module nextModule = null;
                        int indexOffset = 1;

                        while (i + indexOffset <= lastVisibleModuleIndex) {
                            nextModule = filteredModules.get(i + indexOffset);
                            if (nextModule.isVisible()) {
                                break;
                            }
                            nextModule = null;
                            indexOffset++;
                        }

                        if (nextModule != null) {
                            String nextModuleName = displayLabelCache.get(nextModule);
                            float nextModuleWidth = getTextWidth(fr, nextModuleName);

                            if (moduleWidth - nextModuleWidth > 0.5) {
                                Gui.drawRect(translateX - 3,
                                        outlineBottom,
                                        screenX - nextModuleWidth - 3,
                                        outlineBottom + 1,
                                        aColor);
                            }
                        }
                    } else {
                        Gui.drawRect(translateX - 3,
                                outlineBottom,
                                screenX + 3,
                                outlineBottom + 1,
                                aColor);
                    }
                }

                if (line.getValue() && !outline.getValue()) {
                    if (i == firstVisibleModuleIndex) {
                        Gui.drawRect(screenX - 1,
                                startY - 2,
                                screenX,
                                translateY + 10,
                                aColor);
                    } else {
                        Module prevModule = null;
                        for (int j = i - 1; j >= 0; j--) {
                            if (filteredModules.get(j).isVisible()) {
                                prevModule = filteredModules.get(j);
                                break;
                            }
                        }
                    }
                }

                if (line.getValue() && !outline.getValue()) {
                    if (i == firstVisibleModuleIndex) {
                        Gui.drawRect(screenX - 1,
                                startY - 2,
                                screenX,
                                translateY + 10,
                                aColor);
                    } else {
                        Module prevModule = null;
                        for (int j = i - 1; j >= 0; j--) {
                            if (filteredModules.get(j).isVisible()) {
                                prevModule = filteredModules.get(j);
                                break;
                            }
                        }
                        if (prevModule != null) {
                            double prevY = prevModule.getTranslate().getY();

                            Gui.drawRect(screenX - 1,
                                    prevY + 10,
                                    screenX,
                                    translateY + 10,
                                    aColor);
                        }

                    }
                }

                previousModuleWidth = moduleWidth;
                visibleModuleCount++;
            }
        }
    }

    private Color getColorForBG() {
        return new Color(0, 0, 0, 130);
    }

    private int getColorForModule(int visibleModuleIndex) {
        int offset = colorMode.getValue() == ColorMode.FADE ? visibleModuleIndex * 75 : 75;

        if (ClickGUIModule.color.getValue() == ClickGUIModule.Color.ASTOLFO) {
            return RenderUtils.astolfoColors(offset / 2, offset).getRGB();
        }

        if (ClickGUIModule.color.getValue() == ClickGUIModule.Color.RAINBOW) {
            float hue = (System.currentTimeMillis() % 3000) / 3000f;
            if (colorMode.getValue() == ColorMode.FADE) {
                hue += visibleModuleIndex * 0.035f;
            }
            if (hue > 1.0f) {
                hue %= 1.0f;
            }
            return Color.getHSBColor(hue, 0.55f, 0.9f).getRGB();
        }

        if (ClickGUIModule.color.getValue() == ClickGUIModule.Color.NOVOLINE) {
            float hue = (System.currentTimeMillis() % 3000) / 3000f;
            if (colorMode.getValue() == ColorMode.FADE) {
                hue += visibleModuleIndex * 0.035f;
            }
            if (hue > 1.0f) {
                hue %= 1.0f;
            }
            return  Color.getHSBColor(hue, 0.25f, 0.9f).getRGB();
        }

        return RenderUtils.interpolateColorsBackAndForth(15, offset, ColorManager.colors.getFirst(), ColorManager.colors.getSecond(), false).getRGB();
    }

    private String getDisplayLabel(Module m) {
        String label = m.getLabel();
        String suffix = m.getSuffix();

        if (noSpaces.getValue()) {
            label = label.replace(" ", "");
            if (suffix != null) {
                suffix = suffix.replace(" ", "");
            }
        }

        if (lowercase.getValue()) {
            label = label.toLowerCase();
            if (suffix != null) {
                suffix = suffix.toLowerCase();
            }
        }

        if (suffix != null) {
            return label + " \2477" + suffix;
        }
        return label;
    }

    private void updateModulePositions(ScaledResolution scaledResolution) {
        CustomFontRenderer fr = getActiveFont();
        if (fr == null) {
            return;
        }
        if (moduleCache == null) {
            moduleCache = new ArrayList<>(Yuri.INSTANCE.getModuleManager().getModules());
        }

        float y = 2;
        float screenX = scaledResolution.getScaledWidth();

        for (Module module : moduleCache) {
            if (hideVisuals.getValue() && module.getCategory() == ModuleCategory.RENDER) {
                continue;
            }

            if (module.isEnabled()) {
                module.getTranslate().setX(screenX - getTextWidth(fr, getDisplayLabel(module)) + 2);
            } else {
                module.getTranslate().setX(screenX);
            }
            module.getTranslate().setY(y);
            if (module.isEnabled()) {
                y += 12;
            }
        }
    }

    private class LengthComparator implements Comparator<Module> {
        @Override
        public int compare(Module o1, Module o2) {
            CustomFontRenderer fr = getActiveFont();
            return Float.compare(
                    getTextWidth(fr, displayLabelCache.get(o2)),
                    getTextWidth(fr, displayLabelCache.get(o1)));
        }
    }
}