package ddlc.yuri.utils.client;

import net.minecraft.util.ChatComponentText;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class LoggingUtils {
    public static void sendChatMessage(String message) {
        if (mc.thePlayer != null) {
            String msg = "§5Yuri §8» §7" + message;
            mc.thePlayer.addChatMessage(new ChatComponentText(msg));
        }
    }
}
