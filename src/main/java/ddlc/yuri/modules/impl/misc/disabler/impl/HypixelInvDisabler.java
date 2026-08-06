package ddlc.yuri.modules.impl.misc.disabler.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.modules.impl.misc.DisablerModule;
import ddlc.yuri.modules.impl.misc.disabler.DisablerMode;
import ddlc.yuri.modules.impl.player.ManagerModule;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.potion.Potion;

public final class HypixelInvDisabler implements DisablerMode {

    private final DisablerModule parentModule;

    public HypixelInvDisabler(DisablerModule parentModule) {
        this.parentModule = parentModule;
    }

    public static boolean sentFirstOpen = false;
    public static boolean caughtClientStatus = false;
    public static boolean caughtCloseWindow = false;

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;
        if (isInventoryMovePausedForChest()) {
            resetInventoryMoveState();
            return;
        }

        caughtClientStatus = false;
        caughtCloseWindow = false;

        if (mc.currentScreen instanceof GuiInventory || (Yuri.INSTANCE.getModuleManager().getModule(ManagerModule.class).isEnabled() && ManagerModule.isInvOpen)) {
            if (!sentFirstOpen) {
                PacketUtils.sendSilentPacket(new C0DPacketCloseWindow());
                sentFirstOpen = true;
            }

            if (mc.thePlayer.ticksExisted % (mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 3 : 4) == 0)
                PacketUtils.sendSilentPacket(new C0DPacketCloseWindow());
            else if (mc.thePlayer.ticksExisted % (mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 3 : 4) == 1)
                PacketUtils.sendPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
        } else sentFirstOpen = false;
    }


    @Override
    public void onPacketReceived(PacketReceivedEvent event) {
        if (mc.thePlayer == null) return;

        Packet<?> packet = event.getPacket();

        if (!isInventoryMovePausedForChest()) {
            if (packet instanceof C16PacketClientStatus) {
                if (caughtClientStatus)
                    event.setCancelled(true);

                caughtClientStatus = true;
            }
            if (packet instanceof C0DPacketCloseWindow) {
                if (caughtCloseWindow)
                    event.setCancelled(true);

                caughtCloseWindow = true;
            }
        }
    }

    @Override
    public void onWorldJoin(WorldJoinEvent event) {
        sentFirstOpen = false;
        caughtClientStatus = false;
        caughtCloseWindow = false;
    }


    public static boolean isInventoryMovePausedForChest() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;

       return mc.currentScreen instanceof GuiChest;
    }

    private static void resetInventoryMoveState() {
        sentFirstOpen = false;
        caughtClientStatus = false;
        caughtCloseWindow = false;
    }
}