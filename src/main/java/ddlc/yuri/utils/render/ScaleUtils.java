package ddlc.yuri.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public final class ScaleUtils {

    private ScaleUtils() {
    }

    public static void scale(Minecraft mc) {
        ScaledResolution resolution = new ScaledResolution(mc);
        if (mc.gameSettings.guiScale <= 1) {
            return;
        }

        float scale = resolution.getScaleFactor() / 2.0F;
        GlStateManager.scale(scale, scale, scale);
    }

    public static int[] getScaledMouseCoordinates(Minecraft mc, int mouseX, int mouseY) {
        ScaledResolution resolution = new ScaledResolution(mc);
        if (mc.gameSettings.guiScale <= 1) {
            return new int[]{mouseX, mouseY};
        }

        float scale = resolution.getScaleFactor() / 2.0F;
        return new int[]{(int) (mouseX / scale), (int) (mouseY / scale)};
    }
}
