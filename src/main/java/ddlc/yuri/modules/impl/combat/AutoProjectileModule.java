package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.BadPacketsManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.managers.impl.SlotManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.misc.AutoSwapModule;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;


@ModuleInfo(label = "Auto Projectile", description = "Automatically throws projectiles and rod lines into player with predictive rotation", category = ModuleCategory.COMBAT)
public final class AutoProjectileModule extends Module {

    private static final int MAX_THROW_TICKS = 12;

    private final NumberProperty targetRange = new NumberProperty("Target Range", 10.0f, 4.0f, 15.0f, 0.5f);
    private final ModeProperty<SwapMode> swapMode = new ModeProperty<>("Swap Mode", SwapMode.SERVER);

    public enum SwapMode {
        CLIENT("Client"), SERVER("Server");
        public final String name;

        SwapMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private EntityLivingBase target = null;
    private boolean isThrowing = false;
    private boolean preRelease = false;
    private int pendingSlot = -1;
    private long lastThrowTime = 0L;
    private int swapBackTicks = -1;
    private int throwTicks = 0;
    private int cooldown = 0;
    private boolean serverBlocking = false;
    private boolean blockThisTick = false;
    private boolean sendingOwn = false;

    private boolean suppressing() {
        return preRelease || isThrowing || cooldown > 0;
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        if (sendingOwn) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof C03PacketPlayer) {
            blockThisTick = false;
            return;
        }

        if (packet instanceof C09PacketHeldItemChange) {
            serverBlocking = false;
            return;
        }

        if (packet instanceof C07PacketPlayerDigging) {
            if (((C07PacketPlayerDigging) packet).getStatus() != C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) return;
            if (suppressing() || !serverBlocking) {
                event.setCancelled(true);
                return;
            }
            serverBlocking = false;
            blockThisTick = true;
            return;
        }

        if (packet instanceof C08PacketPlayerBlockPlacement) {
            ItemStack stack = ((C08PacketPlayerBlockPlacement) packet).getStack();
            if (stack == null || !(stack.getItem() instanceof ItemSword)) return;
            if (suppressing()) {
                event.setCancelled(true);
                return;
            }
            serverBlocking = true;
            blockThisTick = true;
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            forceReset();
            return;
        }

        if (cooldown > 0) cooldown--;

        ScaffoldModule scaffold = Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class);
        if (scaffold != null && scaffold.isEnabled()) {
            resetState();
            return;
        }

        AuraModule auraModule = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
        boolean hasAura = auraModule != null && auraModule.isEnabled();

        if (preRelease) {
            preRelease = false;
            int slot = pendingSlot;
            pendingSlot = -1;
            if (slot != -1 && target != null && mc.thePlayer.inventory.getStackInSlot(slot) != null) {
                performThrow(slot, target);
            } else {
                AuraModule.canAttack = true;
                AutoSwapModule.shouldSwap = true;
            }
            return;
        }

        if (isThrowing) {
            throwTicks++;
            swapBackTicks--;
            if (swapBackTicks <= 0 || throwTicks >= MAX_THROW_TICKS) {
                finishThrow();
            }
            return;
        }

        int projectileSlot = getProjectileSlot();
        if (projectileSlot == -1) {
            resetState();
            return;
        }

        target = hasAura ? AuraModule.target : null;
        if (target == null || mc.thePlayer.getDistanceToEntity(target) > targetRange.getValue()) {
            resetState();
            return;
        }

        float dist = mc.thePlayer.getDistanceToEntity(target);
        float auraRange = AuraModule.seekRange != null ? AuraModule.seekRange.getValue().floatValue() : 4.0f;

        if (dist < Math.max(auraRange, 4.0f) && target.hurtTime < 3) {
            long delay = 350;
            if (dist <= 3.0f) delay = 9999999;
            else if (dist <= 5.0f) delay = 550;
            else if (dist <= 6.0f) delay = 450;
            else if (dist <= 7.0f) delay = 350;
            else if (dist <= 8.0f) delay = 250;

            long now = System.currentTimeMillis();
            if (now - lastThrowTime >= delay && !blockThisTick && !BadPacketsManager.bad(false, true, false, false, true)) {
                beginThrow(projectileSlot, target);
            }
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        forceReset();
    }

    @Override
    public void onDisable() {
        forceReset();
        super.onDisable();
    }

    private int getProjectileSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) != null) {
                Item item = mc.thePlayer.inventory.getStackInSlot(i).getItem();
                if (item instanceof ItemSnowball || item instanceof ItemEgg || item instanceof ItemFishingRod) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void beginThrow(int slot, EntityLivingBase entity) {
        AuraModule.canAttack = false;
        AutoSwapModule.shouldSwap = false;
        lastThrowTime = System.currentTimeMillis();

        if (serverBlocking) {
            pendingSlot = slot;
            preRelease = true;
            sendingOwn = true;
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            sendingOwn = false;
            serverBlocking = false;
            mc.thePlayer.stopUsingItem();
            return;
        }

        performThrow(slot, entity);
    }

    private void performThrow(int slot, EntityLivingBase entity) {
        boolean serverSwap = swapMode.getValue() == SwapMode.SERVER;

        AuraModule.canAttack = false;
        AutoSwapModule.shouldSwap = false;

        double multiplier = (double) mc.thePlayer.getDistanceToEntity(entity) / 1.25;
        double deltaX = (entity.posX - entity.lastTickPosX) * multiplier;
        double deltaZ = (entity.posZ - entity.lastTickPosZ) * multiplier;
        double targetPosX = entity.posX + deltaX;
        double targetPosZ = entity.posZ + deltaZ;
        double targetPosY = entity.posY + (double) entity.getEyeHeight() - 0.4;

        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != entity) {
            float[] rots = getRotations(targetPosX, targetPosY, targetPosZ);
            RotationManager.setRotations(rots, 10.0f, RotationManager.MovementFix.NORMAL);
        }

        isThrowing = true;
        throwTicks = 0;
        lastThrowTime = System.currentTimeMillis();

        Item item = mc.thePlayer.inventory.getStackInSlot(slot).getItem();
        int storeDelay = item instanceof ItemFishingRod ? 200 : 80;
        swapBackTicks = Math.max(2, storeDelay / 50);

        SlotManager.swap(slot, serverSwap);
        if (serverSwap && mc.playerController != null) {
            mc.playerController.updateController();
        }

        if (mc.thePlayer.inventory.getCurrentItem() != null) {
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem());
            mc.entityRenderer.itemRenderer.resetEquippedProgress2();
        }
    }

    private void finishThrow() {
        boolean serverSwap = swapMode.getValue() == SwapMode.SERVER;
        SlotManager.swapBack();
        if (serverSwap && mc.playerController != null) {
            mc.playerController.updateController();
        }

        AuraModule.canAttack = true;
        AutoSwapModule.shouldSwap = true;

        isThrowing = false;
        swapBackTicks = -1;
        throwTicks = 0;
        cooldown = 1;
    }

    private void resetState() {
        if (isThrowing) {
            finishThrow();
        }
        if (preRelease) {
            AuraModule.canAttack = true;
            AutoSwapModule.shouldSwap = true;
        }
        target = null;
        isThrowing = false;
        preRelease = false;
        pendingSlot = -1;
        swapBackTicks = -1;
        throwTicks = 0;
    }

    private void forceReset() {
        if (isThrowing && mc.thePlayer != null && mc.playerController != null) {
            SlotManager.swapBack();
            if (swapMode.getValue() == SwapMode.SERVER) {
                mc.playerController.updateController();
            }
        }
        if (isThrowing || preRelease) {
            AuraModule.canAttack = true;
            AutoSwapModule.shouldSwap = true;
        }
        target = null;
        isThrowing = false;
        preRelease = false;
        pendingSlot = -1;
        swapBackTicks = -1;
        throwTicks = 0;
        cooldown = 0;
        serverBlocking = false;
        blockThisTick = false;
        sendingOwn = false;
    }

    private float[] getRotations(double posX, double posY, double posZ) {
        double x = posX - mc.thePlayer.posX;
        double y = posY - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
        double z = posZ - mc.thePlayer.posZ;
        double dist = (double) MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, dist) * 180.0D / Math.PI));
        return new float[]{
                mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
                mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)
        };
    }
}