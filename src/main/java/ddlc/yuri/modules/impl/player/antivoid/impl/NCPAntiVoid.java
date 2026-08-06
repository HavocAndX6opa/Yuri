package ddlc.yuri.modules.impl.player.antivoid.impl;

import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.modules.impl.player.AntiVoidModule;
import ddlc.yuri.modules.impl.player.antivoid.AntiVoidMode;
import ddlc.yuri.utils.player.PlayerUtils;

public class NCPAntiVoid implements AntiVoidMode {

    private final AntiVoidModule parentModule;

    public NCPAntiVoid(AntiVoidModule parentModule) {
        this.parentModule = parentModule;
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (event.isPre()) {
            if (mc.thePlayer.fallDistance > parentModule.dist.getValue().floatValue() && !PlayerUtils.isBlockUnder() && mc.thePlayer.posY + mc.thePlayer.motionY < Math.floor(mc.thePlayer.posY)) {
                mc.thePlayer.motionY = Math.floor(mc.thePlayer.posY) - mc.thePlayer.posY;
                if (mc.thePlayer.motionY == 0) {
                    mc.thePlayer.onGround = true;
                    event.setOnGround(true);
                }
            }
        }
    }
}
