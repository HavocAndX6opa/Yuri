package ddlc.yuri.modules.impl.combat.criticals.impl;

import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.modules.impl.combat.criticals.CriticalsMode;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;

public class PacketCriticals implements CriticalsMode {
    @Override
    public void onAttack(PlayerAttackEvent event) {
        PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + 0.0625D,
                mc.thePlayer.posZ,
                false));

        PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY,
                mc.thePlayer.posZ,
                false));
    }
}
