package ddlc.yuri.modules.impl.player;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;

@ModuleInfo(label = "Regen", description = "Regenerate health at a faster rate than normal", category = ModuleCategory.PLAYER)
public class RegenModule extends Module {

    private final NumberProperty health = new NumberProperty("Minimum Health", 15, 1, 20, 1);
    private final NumberProperty packets = new NumberProperty("Speed", 20, 1, 100, 1);

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;
        setSuffix(String.valueOf(packets.getValue().intValue()));
        if (mc.thePlayer.getHealth() < this.health.getValue().floatValue()) {
            for (int i = 0; i < this.packets.getValue().intValue(); i++) {
                PacketUtils.sendPacket(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround));
            }
        }
    }
}
