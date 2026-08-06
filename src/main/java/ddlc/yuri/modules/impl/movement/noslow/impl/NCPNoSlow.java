package ddlc.yuri.modules.impl.movement.noslow.impl;

import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.modules.impl.movement.noslow.NoSlowMode;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class NCPNoSlow implements NoSlowMode {

    @Override
    public void onMotion(MotionEvent event) {
        EnumFacing packetDirection = EnumFacing.DOWN;

        if (event.isPre() && mc.thePlayer.isUsingItem()) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN, packetDirection));
        }
    }
}
