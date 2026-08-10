package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.misc.Pair;
import ddlc.yuri.utils.render.RenderUtils;
import lombok.Getter;

import java.awt.*;

public class ColorManager {

    @Getter
    public static Pair<Color, Color> colors = Pair.of(new Color(161, 82, 230), new Color(130, 58, 185));
    @Getter
    private static Color color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, colors.getFirst(), colors.getSecond(), false);;
    @EventHook
    public void onRender(Render2DEvent event) {
        Color first;
        Color second;

        switch (ClickGUIModule.color.getValue()) {
            case RAINBOW: {
                float hue = (System.currentTimeMillis() % 3000) / 3000f;
                color = Color.getHSBColor(hue, 0.55f, 0.9f);
                colors = Pair.of(color, color);
                break;
            }

            case ASTOLFO: {
                colors = Pair.of(RenderUtils.astolfoColors(15, 75), RenderUtils.astolfoColors(15, 75));
                color = RenderUtils.astolfoColors(15, 75);
                break;
            }

            case NOVOLINE: {
                float hue = (System.currentTimeMillis() % 3000) / 3000f;
                color = Color.getHSBColor(hue, 0.25f, 0.9f);
                colors = Pair.of(color, color);
                break;
            }

            case YURI: {
                first = new Color(161, 82, 230);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case SUNSET: {
                first = new Color(161, 82, 230);
                second = new Color(255, 104, 69);
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case TENACITY: {
                first = new Color(236, 133, 209);
                second = new Color(28, 167, 222);
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }
            case AMETHYST: {
                first = new Color(106, 43, 170);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case ROYAL: {
                first = new Color(74, 37, 184);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case INDIGO: {
                first = new Color(22, 84, 147);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case OCEAN: {
                first = new Color(36, 150, 179);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case CRYSTAL: {
                first = new Color(142, 197, 203);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case PETAL: {
                first = new Color(218, 76, 152);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case CITRUS: {
                first = new Color(138, 166, 25);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case EVERGREEN: {
                first = new Color(19, 129, 56);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case LEMON: {
                first = new Color(194, 181, 29);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case EMBER: {
                first = new Color(160, 64, 16);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case CRIMSON: {
                first = new Color(117, 23, 39);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case ICE: {
                first = new Color(255, 255, 255);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case GRAPHITE: {
                first = new Color(150, 150, 150);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case LAVENDER: {
                first = new Color(106, 81, 144);
                second = first.darker().darker().darker();
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }

            case AZURE: {
                first = new Color(128, 128, 255);
                second = new Color(168, 168, 255);
                color = RenderUtils.interpolateColorsBackAndForth(ClickGUIModule.colorSpeed.getValue().intValue(), 10, first, second, false);
                colors = Pair.of(first, second);
                break;
            }
        }
    }
}