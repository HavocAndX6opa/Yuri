package ddlc.yuri.modules.impl.player.antivoid.impl;

import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.modules.impl.player.AntiVoidModule;
import ddlc.yuri.modules.impl.player.antivoid.AntiVoidMode;
import ddlc.yuri.utils.player.PlayerUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;

public class VanillaAntiVoid implements AntiVoidMode {

    private final AntiVoidModule parentModule;

    public VanillaAntiVoid(AntiVoidModule parentModule) {
        this.parentModule = parentModule;
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (event.isPre()) {
            if (mc.thePlayer.fallDistance > parentModule.dist.getValue().floatValue() && !PlayerUtils.isBlockUnder()) {
                PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition());
            }
        }
    }
}
