package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
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
import net.minecraft.util.Vec3;

@ModuleInfo(label = "Auto Projectile", description = "Automatically throws projectiles and rod lines into player with predictive rotation", category = ModuleCategory.COMBAT)
public final class AutoProjectileModule extends Module {

    private static final int MAX_THROW_TICKS = 12;
    private static final int MAX_FLIGHT_TICKS = 60;
    private static final double ROD_MAX_RANGE = 6.0;
    private static final double LAUNCH_SPEED = 1.5;
    private static final double THROWN_GRAVITY = 0.03;
    private static final double THROWN_DRAG = 0.99;
    private static final double HOOK_GRAVITY = 0.03;
    private static final double HOOK_DRAG = 0.92;
    private static final float ROTATION_SPEED = 10.0f;
    private static final float ROTATION_THRESHOLD = 1.5f;

    private final NumberProperty targetRange = new NumberProperty("Target Range", 10.0f, 4.0f, 15.0f, 0.5f);
    private final ModeProperty<SwapMode> swapMode = new ModeProperty<>("Swap Mode", SwapMode.SERVER);
    private final Property<Boolean> lineOfSight = new Property<>("Line Of Sight", true);

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

    private enum State {
        IDLE, AIMING, THROWING
    }

    private State state = State.IDLE;
    private EntityLivingBase target = null;
    private float[] heldRots = null;
    private boolean overriding = false;
    private int pendingSlot = -1;
    private long lastThrowTime = 0L;
    private int swapBackTicks = -1;
    private int throwTicks = 0;
    private int cooldown = 0;
    private boolean serverBlocking = false;
    private boolean blockThisTick = false;
    private boolean sendingOwn = false;

    private boolean suppressing() {
        return state != State.IDLE || cooldown > 0;
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
            abort();
            return;
        }

        AuraModule aura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
        boolean hasAura = aura != null && aura.isEnabled();

        if (state == State.AIMING) {
            boolean slotValid = pendingSlot != -1 && mc.thePlayer.inventory.getStackInSlot(pendingSlot) != null;
            if (!slotValid || target == null || target.isDead) {
                abort();
                return;
            }
            holdRotations();
            performThrow(pendingSlot);
            return;
        }

        if (state == State.THROWING) {
            holdRotations();
            throwTicks++;
            swapBackTicks--;
            if (swapBackTicks <= 0 || throwTicks >= MAX_THROW_TICKS) {
                finishThrow();
            }
            return;
        }

        EntityLivingBase candidate = hasAura ? AuraModule.target : null;
        if (candidate == null || candidate.isDead || mc.thePlayer.getDistanceToEntity(candidate) > targetRange.getValue().floatValue()) {
            abort();
            return;
        }

        float dist = mc.thePlayer.getDistanceToEntity(candidate);
        int slot = getProjectileSlot(dist);
        if (slot == -1) {
            abort();
            return;
        }

        float auraRange = AuraModule.seekRange != null ? AuraModule.seekRange.getValue().floatValue() : 4.0f;
        if (dist >= Math.max(auraRange, 4.0f) || candidate.hurtTime >= 3) return;

        long delay = delayFor(dist);
        long now = System.currentTimeMillis();
        if (now - lastThrowTime < delay) return;
        if (blockThisTick || BadPacketsManager.bad(false, true, false, false, true)) return;

        beginThrow(slot, candidate);
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

    private long delayFor(float dist) {
        if (dist <= 3.0f) return Long.MAX_VALUE;
        if (dist <= 5.0f) return 550L;
        if (dist <= 6.0f) return 450L;
        if (dist <= 7.0f) return 350L;
        return 250L;
    }

    private int getProjectileSlot(float dist) {
        int rod = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;
            Item item = stack.getItem();
            if (item instanceof ItemSnowball || item instanceof ItemEgg) return i;
            if (item instanceof ItemFishingRod && rod == -1 && dist <= ROD_MAX_RANGE) rod = i;
        }
        return rod;
    }

    private void beginThrow(int slot, EntityLivingBase entity) {
        Item item = mc.thePlayer.inventory.getStackInSlot(slot).getItem();
        float[] rots = solveRotations(entity, item);
        if (rots == null) return;

        AuraModule.canAttack = false;
        AutoSwapModule.shouldSwap = false;
        lastThrowTime = System.currentTimeMillis();

        target = entity;
        pendingSlot = slot;

        boolean rotate = needsRotation(rots);
        heldRots = rotate ? rots : null;
        if (rotate) holdRotations();

        boolean unblock = serverBlocking;
        if (unblock) {
            sendingOwn = true;
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            sendingOwn = false;
            serverBlocking = false;
            mc.thePlayer.stopUsingItem();
        }

        if (rotate || unblock) {
            state = State.AIMING;
            return;
        }

        performThrow(slot);
    }

    private void performThrow(int slot) {
        boolean serverSwap = swapMode.getValue() == SwapMode.SERVER;

        AuraModule.canAttack = false;
        AutoSwapModule.shouldSwap = false;

        state = State.THROWING;
        throwTicks = 0;
        lastThrowTime = System.currentTimeMillis();
        pendingSlot = -1;

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

        restoreAura();

        state = State.IDLE;
        target = null;
        heldRots = null;
        swapBackTicks = -1;
        throwTicks = 0;
        cooldown = 1;
    }

    private void abort() {
        if (state == State.THROWING) {
            finishThrow();
        } else if (state == State.AIMING) {
            restoreAura();
        } else {
            releaseOverride();
        }
        state = State.IDLE;
        target = null;
        heldRots = null;
        pendingSlot = -1;
        swapBackTicks = -1;
        throwTicks = 0;
    }

    private void forceReset() {
        if (state == State.THROWING && mc.thePlayer != null && mc.playerController != null) {
            SlotManager.swapBack();
            if (swapMode.getValue() == SwapMode.SERVER) {
                mc.playerController.updateController();
            }
        }
        if (state != State.IDLE) {
            AuraModule.canAttack = true;
            AutoSwapModule.shouldSwap = true;
        }
        releaseOverride();
        state = State.IDLE;
        target = null;
        heldRots = null;
        pendingSlot = -1;
        swapBackTicks = -1;
        throwTicks = 0;
        cooldown = 0;
        serverBlocking = false;
        blockThisTick = false;
        sendingOwn = false;
    }

    private void restoreAura() {
        AuraModule.canAttack = true;
        AutoSwapModule.shouldSwap = true;
        releaseOverride();
    }

    private void holdRotations() {
        if (heldRots == null) return;
        AuraModule.rotationOverride = true;
        overriding = true;
        RotationManager.setRotations(heldRots, ROTATION_SPEED, RotationManager.MovementFix.NORMAL);
    }

    private void releaseOverride() {
        if (overriding) {
            AuraModule.rotationOverride = false;
            overriding = false;
        }
    }

    private boolean needsRotation(float[] rots) {
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rots[0] - mc.thePlayer.rotationYaw));
        float pitchDiff = Math.abs(rots[1] - mc.thePlayer.rotationPitch);
        return yawDiff > ROTATION_THRESHOLD || pitchDiff > ROTATION_THRESHOLD;
    }

    private float[] solveRotations(EntityLivingBase entity, Item item) {
        boolean rod = item instanceof ItemFishingRod;
        double gravity = rod ? HOOK_GRAVITY : THROWN_GRAVITY;
        double drag = rod ? HOOK_DRAG : THROWN_DRAG;

        double eyeX = mc.thePlayer.posX;
        double eyeY = mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight() - 0.1;
        double eyeZ = mc.thePlayer.posZ;

        double velX = entity.posX - entity.lastTickPosX;
        double velZ = entity.posZ - entity.lastTickPosZ;
        double aimY = entity.posY + (double) entity.height * 0.7;

        double ticks = Math.max(1.0, (double) mc.thePlayer.getDistanceToEntity(entity) / LAUNCH_SPEED);
        double tx = entity.posX;
        double tz = entity.posZ;
        double pitch = 0.0;

        for (int i = 0; i < 4; i++) {
            tx = entity.posX + velX * ticks;
            tz = entity.posZ + velZ * ticks;
            double dx = tx - eyeX;
            double dz = tz - eyeZ;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            double[] sol = solvePitch(horiz, aimY - eyeY, drag, gravity);
            if (sol == null) return null;
            pitch = sol[0];
            ticks = Math.min(sol[1], 30.0);
        }

        if (lineOfSight.getValue()) {
            Vec3 from = new Vec3(eyeX, eyeY + 0.1, eyeZ);
            Vec3 to = new Vec3(tx, aimY, tz);
            if (mc.theWorld.rayTraceBlocks(from, to) != null) return null;
        }

        double dx = tx - eyeX;
        double dz = tz - eyeZ;
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        return new float[]{
                mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
                (float) pitch
        };
    }

    private double[] solvePitch(double horiz, double dy, double drag, double gravity) {
        double prevErr = Double.NaN;
        double prevPitch = 0.0;
        double prevTicks = 0.0;
        for (double pitch = 70.0; pitch >= -80.0; pitch -= 0.5) {
            double[] r = simulate(pitch, horiz, drag, gravity);
            if (r == null) {
                prevErr = Double.NaN;
                continue;
            }
            double err = r[0] - dy;
            if (!Double.isNaN(prevErr) && prevErr < 0.0 && err >= 0.0) {
                double f = -prevErr / (err - prevErr);
                return new double[]{
                        prevPitch + (pitch - prevPitch) * f,
                        prevTicks + (r[1] - prevTicks) * f
                };
            }
            prevErr = err;
            prevPitch = pitch;
            prevTicks = r[1];
        }
        return null;
    }

    private double[] simulate(double pitch, double horiz, double drag, double gravity) {
        double rad = Math.toRadians(pitch);
        double mh = Math.cos(rad) * LAUNCH_SPEED;
        double my = -Math.sin(rad) * LAUNCH_SPEED;
        if (mh < 0.02) return null;

        double h = 0.0;
        double y = 0.0;
        for (int t = 1; t <= MAX_FLIGHT_TICKS; t++) {
            double nh = h + mh;
            double ny = y + my;
            if (nh >= horiz) {
                double f = (horiz - h) / (nh - h);
                return new double[]{y + (ny - y) * f, (t - 1) + f};
            }
            h = nh;
            y = ny;
            mh *= drag;
            my *= drag;
            my -= gravity;
            if (mh < 0.02) return null;
        }
        return null;
    }
}