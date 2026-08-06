package ddlc.yuri.utils.render.progress;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.awt.*;

public class ProgressBarUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void draw(float progress, float alpha, float centerX, float centerY, float width, float thickness) {
        float percentage = Math.min(1.0f, Math.max(0.0f, progress));
        float half = width / 2;

        int backgroundColor = RenderUtils.applyOpacity(Color.BLACK.getRGB(), alpha);
        int firstColor = RenderUtils.applyOpacity(ColorManager.getColors().getFirst().getRGB(), alpha);
        int secondColor = RenderUtils.applyOpacity(ColorManager.getColors().getSecond().getRGB(), alpha);

        Gui.drawRect(centerX - half - 0.5, centerY - 0.5, centerX + half + 0.5, centerY + thickness + 0.5, backgroundColor);
        Gui.drawGradientRect(centerX - half, centerY, centerX - half + (width * percentage), centerY + thickness, firstColor, secondColor);
    }
}
