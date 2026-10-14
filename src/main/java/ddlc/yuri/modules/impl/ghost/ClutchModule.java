package ddlc.yuri.modules.impl.ghost;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.BadPacketsManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.player.*;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import org.lwjgl.util.vector.Vector2f;

@ModuleInfo(label = "Clutch", description = "Automatically places blocks under you when over void", category = ModuleCategory.GHOST)
public class ClutchModule extends Module {
    private final NumberProperty rotationSpeedMin = new NumberProperty("Rotation Speed Min", 5, 0, 10, 0.5);
    private final NumberProperty rotationSpeedMax = new NumberProperty("Rotation Speed Max", 10, 0, 10, 0.5);
    private final NumberProperty placeDelayMin = new NumberProperty("Place Delay Min", 0, 0, 1, 1);
    private final NumberProperty placeDelayMax = new NumberProperty("Place Delay Max", 0, 0, 1, 1);

    private Vec3 targetBlock;
    private EnumFacingOffset enumFacing;
    private BlockPos blockFace;
    private float targetYaw, targetPitch;
    private int ticksOnAir;
    private int toggle;

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            targetYaw = mc.thePlayer.rotationYaw - 180;
            targetPitch = 90;
            targetBlock = null;
            ticksOnAir = 0;
            toggle = 0;
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        setSuffix("Shift");

        if (mc.thePlayer.ticksExisted <= 50 ||
                BadPacketsManager.bad() ||
                Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled() ||
                !mc.gameSettings.keyBindSneak.isKeyDown()) return;

        if (mc.thePlayer.offGroundTicks > 3 && !PlayerUtils.isBlockUnder(10)) {
            toggle = 10;
        }

        if (toggle-- < 0) return;

        final int blockSlot = BlockUtils.findBlock();
        if (blockSlot == -1) return;

        final ItemStack blockStack = mc.thePlayer.inventory.getStackInSlot(blockSlot);
        if (blockStack == null || !(blockStack.getItem() instanceof ItemBlock)) return;

        mc.thePlayer.inventory.currentItem = blockSlot;

        final Vec3i offset = new Vec3i(0, 0, 0);

        if (PlayerUtils.blockRelativeToPlayer(offset.getX(), -1 + offset.getY(), offset.getZ())
                .isReplaceable(mc.theWorld, new BlockPos(mc.thePlayer).down())) {
            ticksOnAir++;
        } else {
            ticksOnAir = 0;
        }

        targetBlock = PlayerUtils.getPlacePossibility(offset.getX(), offset.getY(), offset.getZ());

        if (targetBlock == null) return;

        enumFacing = PlayerUtils.getEnumFacing(targetBlock);

        if (enumFacing == null) return;

        final BlockPos position = new BlockPos(targetBlock.xCoord, targetBlock.yCoord, targetBlock.zCoord);
        blockFace = position.add(enumFacing.getOffset().xCoord, enumFacing.getOffset().yCoord, enumFacing.getOffset().zCoord);

        if (blockFace == null || enumFacing == null) return;

        calculateRotations();

        if (targetBlock == null || enumFacing == null || blockFace == null) return;

        if (mc.thePlayer.inventory.currentItem == blockSlot) {
            if (!BadPacketsManager.bad(false, true, false, false, true) &&
                    ticksOnAir > MathUtils.getRandom(placeDelayMin.getValue().intValue(), placeDelayMax.getValue().intValue()) &&
                    RayCastUtils.overBlock(enumFacing.getEnumFacing(), blockFace, true)) {

                Vec3 hitVec = RayCastUtils.rayCast(RotationManager.rotations, mc.playerController.getBlockReachDistance()).hitVec;

                if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                        blockStack, blockFace, enumFacing.getEnumFacing(), hitVec)) {
                    PacketUtils.sendPacket(new C0APacketAnimation());
                }

                mc.rightClickDelayTimer = 0;
                ticksOnAir = 0;

                if (blockStack != null && blockStack.stackSize == 0) {
                    mc.thePlayer.inventory.mainInventory[blockSlot] = null;
                }
            } else if (Math.random() > 0.92 && mc.rightClickDelayTimer <= 0) {
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(blockStack));
                mc.rightClickDelayTimer = 0;
            }
        }
    }

    public void calculateRotations() {
        if (ticksOnAir > 0 && !RayCastUtils.overBlock(RotationManager.rotations, enumFacing.getEnumFacing(), blockFace, true)) {
            getRotations(0);
        }

        final double minRotationSpeed = rotationSpeedMin.getValue();
        final double maxRotationSpeed = rotationSpeedMax.getValue();
        float rotationSpeed = (float) MathUtils.getRandom(minRotationSpeed, maxRotationSpeed);

        if (rotationSpeed != 0) {
            RotationManager.setRotations(new Vector2f(targetYaw, targetPitch), rotationSpeed,
                    RotationManager.MovementFix.NORMAL);
        }
    }

    public void getRotations(final int yawOffset) {
        double difference = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - targetBlock.yCoord - 0.1 - Math.random() * 0.8;

        MovingObjectPosition movingObjectPosition;

        for (int offset = -180 + yawOffset; offset <= 180; offset += 45) {
            mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - difference, mc.thePlayer.posZ);
            movingObjectPosition = RayCastUtils.rayCast(new Vector2f(mc.thePlayer.rotationYaw + offset, 0), 4.5);
            mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + difference, mc.thePlayer.posZ);

            if (movingObjectPosition != null && new BlockPos(blockFace).equals(movingObjectPosition.getBlockPos()) &&
                    enumFacing.getEnumFacing() == movingObjectPosition.sideHit) {
                Vector2f rotations = RotationUtils.calculate(movingObjectPosition.hitVec);

                targetYaw = rotations.x;
                targetPitch = rotations.y;
                return;
            }
        }

        final Vector2f rotations = RotationUtils.calculate(
                new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()), enumFacing.getEnumFacing());

        targetYaw = rotations.x;
        targetPitch = rotations.y;
    }
}
