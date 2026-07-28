package ddlc.yuri.utils.client;

import net.minecraft.client.network.NetworkPlayerInfo;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class ClientInfoUtils {
    public static int getPing() {
        if (mc.thePlayer == null || mc.getNetHandler() == null)
            return 0;

        NetworkPlayerInfo info =
                mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());

        return info != null ? Math.max(info.getResponseTime(), 0) : 0;
    }

    public static String getServerProtocol() {
        return "1.8x";
    }
}
