package ddlc.yuri.modules.impl.movement.noslow.impl;

import ddlc.yuri.api.events.impl.player.ItemSlowdownEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.player.RightClickEvent;
import ddlc.yuri.modules.impl.movement.NoSlowModule;
import ddlc.yuri.modules.impl.movement.noslow.NoSlowMode;
import ddlc.yuri.utils.client.LoggingUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C09PacketHeldItemChange;

public final class HypixelNoSlow implements NoSlowMode {

    private final NoSlowModule parent;

    // no clue if this works. can someone test this for me? I don't have alts. -unlegit

    public HypixelNoSlow(NoSlowModule parentModule) {
        this.parent = parentModule;
    }

    public int watchdogOffGroundTicks;
    public boolean watchdogDisable;

    @Override
    public void onSlowdown(ItemSlowdownEvent event) {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) return;

        if (!watchdogDisable && (parent.isFoodActive() || parent.isPotionActive() || parent.isBowActive())) {
            event.setCancelled(true);
        }
        if (parent.isSwordActive()) {
            int slot = mc.thePlayer.inventory.currentItem;
            PacketUtils.sendPacket(new C09PacketHeldItemChange(slot % 7 + (int) (Math.random() * 2) + 1));
            PacketUtils.sendPacket(new C09PacketHeldItemChange(slot));
            event.setCancelled(true);
        }
    }

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) return;
        if (mc.theWorld != null
                && mc.theWorld.getBlockState(mc.thePlayer.getPosition().down()).getBlock() != net.minecraft.init.Blocks.air
                && !mc.thePlayer.isUsingItem()) {
            watchdogDisable = false;
        }

        if (Math.abs(mc.thePlayer.posY - Math.round(mc.thePlayer.posY)) > 0.03 && mc.thePlayer.onGround) {
            watchdogDisable = true;
        }

        boolean nonSwordUse = mc.thePlayer.isUsingItem() && !(mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword);
        if (nonSwordUse) {
            if (mc.thePlayer.onGround) {
                watchdogOffGroundTicks = 0;
            } else {
                watchdogOffGroundTicks++;
            }
        }

        if (parent.isSwordActive()) {
            int slot = mc.thePlayer.inventory.currentItem;
            PacketUtils.sendPacket(new C09PacketHeldItemChange(slot % 7 + (int) (Math.random() * 2) + 1));
            PacketUtils.sendPacket(new C09PacketHeldItemChange(slot));
        }
    }

    @Override
    public void onRightClick(RightClickEvent event) {
        if (mc.thePlayer == null) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return;

        boolean qualifies = held.getItem() instanceof ItemSword && mc.thePlayer.isUsingItem()
                || (held.getItem() instanceof ItemPotion && parent.potion.getValue())
                || (held.getItem() instanceof ItemFood && parent.food.getValue())
                || (held.getItem() instanceof ItemBow && parent.bow.getValue());
        if (!qualifies) return;

        if (watchdogOffGroundTicks < 2 && watchdogOffGroundTicks != 0 && !watchdogDisable) {
            LoggingUtils.sendChatMessage("You must start using the item in air for the No Slow to work!");
            event.setCancelled(true);
        }
    }
}
