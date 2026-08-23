package ddlc.yuri.modules.impl.movement.noslow.impl;

import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.modules.impl.movement.noslow.NoSlowMode;
import ddlc.yuri.utils.player.MoveUtils;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S30PacketWindowItems;

public final class ExperimentalNoSlow implements NoSlowMode {

    @Override
    public void onPacketReceived(PacketReceivedEvent event) {
        if (mc.thePlayer == null) return;

        if (event.getPacket() instanceof S30PacketWindowItems
                && (mc.thePlayer.isUsingItem() || mc.thePlayer.isBlocking())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (!event.isPre() || mc.thePlayer == null) return;
        if (!MoveUtils.isMoving() || !mc.thePlayer.isUsingItem()) return;

        int slot = mc.thePlayer.inventory.currentItem;

        send(new C09PacketHeldItemChange(slot - 1 < 0 ? 8 : slot - 1));
        send(new C09PacketHeldItemChange(slot));
    }

    private void send(Packet<?> packet) {
        if (mc.getNetHandler() == null) return;
        mc.getNetHandler().getNetworkManager().sendSilentPacket(packet);
    }
}
