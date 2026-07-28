package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;

@ModuleInfo(
        label = "Criticals",
        description = "Allows you to always get critical hits",
        category = ModuleCategory.COMBAT
)
public class CriticalsModule extends Module {

    private enum Mode {
        PACKET("Packet"),
        ;

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.PACKET);


    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (mc.thePlayer != null && mc.theWorld != null && mc.thePlayer.onGround) {
            switch (mode.getValue()) {
                case PACKET: {
                    PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.0625, mc.thePlayer.posZ, false));
                    PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false));
                    break;
                }
            }
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());
    }
}
