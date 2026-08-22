package ddlc.yuri.modules.impl.misc.disabler.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
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

    private boolean sentFirstOpen;
    private boolean caughtClientStatus;
    private boolean caughtCloseWindow;

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;

        caughtClientStatus = false;
        caughtCloseWindow = false;

        if (isInventoryMovePausedForChest()) {
            sentFirstOpen = false;
            return;
        }

        if (!isInventoryOpen()) {
            sentFirstOpen = false;
            return;
        }

        if (!sentFirstOpen) {
            PacketUtils.sendSilentPacket(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
            sentFirstOpen = true;
            return;
        }

        int safePacketTick = mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 3 : 4;
        int tick = mc.thePlayer.ticksExisted % safePacketTick;

        if (tick == 0) {
            PacketUtils.sendSilentPacket(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
        } else if (tick == 1) {
            PacketUtils.sendSilentPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (mc.thePlayer == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof C16PacketClientStatus
                && ((C16PacketClientStatus) packet).getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
            if (caughtClientStatus) {
                event.setCancelled(true);
            }
            caughtClientStatus = true;
        }

        if (packet instanceof C0DPacketCloseWindow) {
            if (caughtCloseWindow) {
                event.setCancelled(true);
            }
            caughtCloseWindow = true;
        }
    }

    @Override
    public void onWorldJoin(WorldJoinEvent event) {
        sentFirstOpen = false;
        caughtClientStatus = false;
        caughtCloseWindow = false;
    }

    private boolean isInventoryOpen() {
        if (mc.currentScreen instanceof GuiInventory) return true;

        ManagerModule manager = Yuri.INSTANCE.getModuleManager().getModule(ManagerModule.class);
        return manager != null && manager.isEnabled() && ManagerModule.isInvOpen;
    }

    private boolean isInventoryMovePausedForChest() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;

        return mc.currentScreen instanceof GuiChest;
    }
}
