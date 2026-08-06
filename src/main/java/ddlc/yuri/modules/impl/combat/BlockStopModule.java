package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.SlotManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

@ModuleInfo(label = "Block Stop", description = "Places a block between you and your target to block hits", category = ModuleCategory.COMBAT)
public class BlockStopModule extends Module {

    private final NumberProperty distance = new NumberProperty("Distance", 3.0, 1.0, 6.0, 0.1);
    private final NumberProperty placeDelay = new NumberProperty("Place Delay", 100.0, 10.0, 300.0, 5.0);
    private final ModeProperty<SwapMode> swapMode = new ModeProperty<>("Swap Mode", SwapMode.SERVER);

    private EntityLivingBase target = null;
    private long lastPlaceTime = 0L;
    private int swapBackTicks = -1;

    @EventHook
    public void onPlayerAttack(PlayerAttackEvent event) {
        target = event.target;
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (SlotManager.isActive()) {
            tickSwapBack();
        }

        if (target == null) return;
        placeDefensiveBlock(target);
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    private void resetState() {
        if (SlotManager.isActive()) {
            boolean wasServerSwap = SlotManager.isServerSwapActive();
            SlotManager.swapBack();
            if (wasServerSwap) {
                mc.playerController.updateController();
            }
        }
        swapBackTicks = -1;
        target = null;
    }

    private void tickSwapBack() {
        swapBackTicks--;
        if (swapBackTicks <= 0) {
            boolean wasServerSwap = SlotManager.isServerSwapActive();
            SlotManager.swapBack();
            if (wasServerSwap) {
                mc.playerController.updateController();
            }
            swapBackTicks = -1;
        }
    }

    private void placeDefensiveBlock(EntityLivingBase entity) {
        if (SlotManager.isActive()) return;
        if (System.currentTimeMillis() - lastPlaceTime < placeDelay.getValue().longValue() * 10) return;
        if (!mc.thePlayer.canEntityBeSeen(entity)) return;
        if (mc.thePlayer.getDistanceToEntity(entity) > distance.getValue()) return;

        int blockSlot = getHotbarBlockSlot();
        if (blockSlot == -1) return;

        double dx = entity.posX - mc.thePlayer.posX;
        double dz = entity.posZ - mc.thePlayer.posZ;

        double len = Math.sqrt(dx * dx + dz * dz);
        if (len == 0) return;
        dx /= len;
        dz /= len;

        double placeX = entity.posX - dx;
        double placeZ = entity.posZ - dz;
        double placeY = Math.floor(entity.posY);

        BlockPos pos = new BlockPos(placeX, placeY, placeZ);

        AxisAlignedBB bb = new AxisAlignedBB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
        );

        if (entity.getEntityBoundingBox().intersectsWith(bb)) return;

        EnumFacing placeFacing = null;
        BlockPos neighbor = null;
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos adj = pos.offset(facing);
            if (!mc.theWorld.isAirBlock(adj)) {
                placeFacing = facing.getOpposite();
                neighbor = adj;
                break;
            }
        }

        if (placeFacing == null || neighbor == null) return;

        EnumFacing opposite = placeFacing.getOpposite();

        double hitX = neighbor.getX() + 0.5 + 0.5 * opposite.getFrontOffsetX();
        double hitY = neighbor.getY() + 0.5 + ((float) Math.random()) * 0.44F;
        double hitZ = neighbor.getZ() + 0.5 + 0.5 * opposite.getFrontOffsetZ();
        Vec3 hitVec = new Vec3(hitX, hitY, hitZ);

        boolean serverSwap = swapMode.getValue() == SwapMode.SERVER;
        SlotManager.swap(blockSlot, serverSwap);
        if (serverSwap) {
            mc.playerController.updateController();
        }

        ItemStack itemstack = mc.thePlayer.inventory.getCurrentItem();
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, itemstack, neighbor, placeFacing, hitVec)) {
            mc.thePlayer.swingItem();
            if (itemstack != null) {
                if (itemstack.stackSize == 0) {
                    mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = null;
                } else if (mc.playerController.isInCreativeMode()) {
                    mc.entityRenderer.itemRenderer.resetEquippedProgress();
                }
            }
        }

        lastPlaceTime = System.currentTimeMillis();
        swapBackTicks = Math.max(1, 60 / 50);
    }

    private int getHotbarBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                return i;
            }
        }
        return -1;
    }

    public enum SwapMode {
        CLIENT("Client"), SERVER("Server");
        public final String name;

        SwapMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }
}