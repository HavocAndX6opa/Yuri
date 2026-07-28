package ddlc.yuri.modules.impl.player.nofall.impl;

import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.modules.impl.player.nofall.NoFallMode;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;

public class VanillaNoFall implements NoFallMode {
    @Override
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer.fallDistance >= 3) {
            PacketUtils.sendPacket(new C03PacketPlayer(true));
        }
    }
}
