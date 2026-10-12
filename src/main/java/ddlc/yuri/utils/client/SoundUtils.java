package ddlc.yuri.utils.client;

import net.minecraft.client.Minecraft;

public class SoundUtils {
    public static void playSound(String resourceLocation) {
        playSound(resourceLocation, 1.0f);
    }

    public static void playSound(String resourceLocation, float volume) {
        Minecraft.getMinecraft().getSoundHandler().playSoundFromFile(resourceLocation, volume);
    }
}
