package ddlc.yuri.modules.impl.movement.noslow.impl;

import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.player.RightClickEvent;
import ddlc.yuri.modules.impl.movement.NoSlowModule;
import ddlc.yuri.modules.impl.movement.noslow.NoSlowMode;
import ddlc.yuri.utils.player.InvUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public final class HypixelNoSlow implements NoSlowMode {

    private final NoSlowModule parent;

    public HypixelNoSlow(NoSlowModule parentModule) {
        this.parent = parentModule;
    }

    private static int nextCycleTick = -1;
    private static boolean runThisTick = false;
    private static boolean stopUse = false;
    private static boolean blocking = false;
    private int slotChangeTick = -1;

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.currentScreen != null || mc.theWorld == null || !InvUtils.isHoldingSword()) {
            if (blocking) {
                release();
            }
            resetCycle();
            return;
        }

        if (!parent.isSwordActive()) {
            if (blocking) {
                release();
            }
            resetCycle();
            return;
        }

        if (stopUse) {
            if (mc.thePlayer.isUsingItem() && parent.isSwordActive()) {
                block();
                mc.thePlayer.stopUsingItem();
            }
            stopUse = false;
        }

        int age = mc.thePlayer.ticksExisted;
        boolean rightPressed = mc.gameSettings.keyBindUseItem.isKeyDown();

        if (rightPressed) {
            if (nextCycleTick < 0) {
                nextCycleTick = age;
            }

            if (age >= nextCycleTick) {
                if (blocking) {
                    release();
                }
                runThisTick = true;
                nextCycleTick = age + 2;
            } else if (!blocking) {
                block();
            }
        } else {
            resetCycle();
            if (blocking) {
                release();
            }
        }

        if (runThisTick) {
            if (rightPressed) {
                if (!mc.thePlayer.isUsingItem() || !blocking) {
                    if (mc.objectMouseOver != null
                            && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                            && mc.objectMouseOver.getBlockPos() != null) {
                        net.minecraft.block.Block block =
                                mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();
                        PlayerControllerMP accessor = mc.playerController;
                        if (isInteractableBlock(block) || accessor.getIsHittingBlock()) {
                            runThisTick = false;
                            return;
                        }
                    }

                    this.stopUse = true;
                    mc.gameSettings.keyBindUseItem.setPressed(true);
                } else {
                    mc.gameSettings.keyBindUseItem.setPressed(false);
                }
            } else {
                this.stopUse = false;
            }
            runThisTick = false;
        }
    }

    @Override
    public void onRightClick(RightClickEvent event) {
        if (mc.thePlayer == null || mc.currentScreen != null || mc.theWorld == null) {
            return;
        }
        if (parent.isSwordActive()) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (mc.thePlayer == null || mc.currentScreen != null || mc.theWorld == null) {
            return;
        }

        if (event.getPacket() instanceof C09PacketHeldItemChange) {
            if (mc.thePlayer.ticksExisted - slotChangeTick != 1 && blocking) {
                release();
                resetCycle();
            }
            slotChangeTick = mc.thePlayer.ticksExisted;
        }

        if (event.getPacket() instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus statusPacket = (S19PacketEntityStatus) event.getPacket();
            if (statusPacket.getEntity(mc.theWorld) == mc.thePlayer && statusPacket.getOpCode() == 9 && blocking) {
                release();
            }
        }
    }

    private void block() {
        if (mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            mc.thePlayer.setItemInUse(
                    mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
            blocking = true;
        }
    }

    public static void release() {
        if (blocking) {
            PacketUtils.sendPacket(
                    new C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            mc.thePlayer.stopUsingItem();
            blocking = false;
        }
    }

    public static void resetCycle() {
        stopUse = false;
        runThisTick = false;
        nextCycleTick = -1;
    }

    private boolean isInteractableBlock(net.minecraft.block.Block block) {
        return block instanceof net.minecraft.block.BlockDoor
                || block instanceof net.minecraft.block.BlockChest
                || block instanceof net.minecraft.block.BlockFurnace
                || block instanceof net.minecraft.block.BlockWorkbench
                || block instanceof net.minecraft.block.BlockAnvil
                || block instanceof net.minecraft.block.BlockEnchantmentTable
                || block instanceof net.minecraft.block.BlockBrewingStand
                || block instanceof net.minecraft.block.BlockBeacon
                || block instanceof net.minecraft.block.BlockLever
                || block instanceof net.minecraft.block.BlockButtonWood
                || block instanceof net.minecraft.block.BlockButtonStone
                || block instanceof net.minecraft.block.BlockTrapDoor
                || block instanceof net.minecraft.block.BlockFenceGate
                || block instanceof net.minecraft.block.BlockRedstoneRepeater
                || block instanceof net.minecraft.block.BlockRedstoneComparator
                || block instanceof net.minecraft.block.BlockHopper
                || block instanceof net.minecraft.block.BlockDropper
                || block instanceof net.minecraft.block.BlockDispenser
                || block instanceof net.minecraft.block.BlockEnderChest
                || block == Blocks.anvil
                || block == Blocks.enchanting_table
                || block == Blocks.brewing_stand;
    }
}