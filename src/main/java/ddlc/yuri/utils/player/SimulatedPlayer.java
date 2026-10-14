package ddlc.yuri.utils.player;

import ddlc.yuri.utils.misc.IMinecraft;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import java.util.List;

public class SimulatedPlayer implements IMinecraft {

    private static final float AIR_SPEED = 0.02f;

    public final EntityPlayerSP player;
    public final MovementInput movementInput;
    public AxisAlignedBB box;
    public double posX;
    public double posY;
    public double posZ;
    public double motionX;
    public double motionY;
    public double motionZ;
    public float rotationYaw;
    public float fallDistance;
    public boolean onGround;
    public boolean isCollidedHorizontally;
    public boolean isInWeb;
    public boolean safeWalk;
    public final float height;

    private final float width;
    private final float stepHeight;
    private final World worldObj;
    private final IChunkProvider chunkProvider;
    private final Entity ridingEntity;
    private final Entity riddenByEntity;
    private final boolean isSpectator;
    private final boolean allowFlying;
    private final float flySpeed;
    private final int foodLevel;

    private boolean flying;
    private boolean noClip;
    private boolean inWater;
    private boolean sprinting;
    private boolean airBorne;
    private boolean jumping;
    private float jumpMovementFactor;
    private float moveForward;
    private float moveStrafing;
    private int jumpTicks;

    private SimulatedPlayer(MovementInput input) {
        this.player = mc.thePlayer;

        this.movementInput = new MovementInput();
        this.movementInput.jump = input.jump;
        this.movementInput.moveForward = input.moveForward;
        this.movementInput.moveStrafe = input.moveStrafe;
        this.movementInput.sneak = input.sneak;

        this.box = player.getEntityBoundingBox();
        this.posX = player.posX;
        this.posY = player.posY;
        this.posZ = player.posZ;
        this.motionX = player.motionX;
        this.motionY = player.motionY;
        this.motionZ = player.motionZ;
        this.rotationYaw = player.movementYaw;
        this.fallDistance = player.fallDistance;
        this.onGround = player.onGround;
        this.isCollidedHorizontally = player.isCollidedHorizontally;
        this.height = player.height;
        this.width = player.width;
        this.stepHeight = player.stepHeight;
        this.worldObj = player.worldObj;
        this.chunkProvider = player.worldObj.getChunkProvider();
        this.ridingEntity = player.ridingEntity;
        this.riddenByEntity = player.riddenByEntity;
        this.isSpectator = player.isSpectator();
        this.allowFlying = player.capabilities.allowFlying;
        this.flySpeed = player.capabilities.getFlySpeed();
        this.flying = player.capabilities.isFlying;
        this.foodLevel = player.getFoodStats().getFoodLevel();
        this.inWater = player.isInWater();
        this.sprinting = player.isSprinting();
        this.airBorne = player.isAirBorne;
        this.jumpMovementFactor = player.jumpMovementFactor;
    }

    public static SimulatedPlayer fromClientPlayer(MovementInput input) {
        return new SimulatedPlayer(input);
    }

    public Vec3 getPos() {
        return new Vec3(posX, posY, posZ);
    }

    public float getEyeHeight() {
        return player.getEyeHeight();
    }

    public void tick() {
        handleWaterMovement();

        if (isInLava()) {
            fallDistance *= 0.5f;
        }

        if (posY < -64.0 || player.isRiding()) {
            return;
        }

        noClip = isSpectator;
        if (isSpectator) {
            onGround = false;
        }

        livingUpdate();
        clampPosition();
    }

    private void livingUpdate() {
        double offset = width * 0.35;
        pushOutOfBlocks(posX - offset, box.minY + 0.5, posZ + offset);
        pushOutOfBlocks(posX - offset, box.minY + 0.5, posZ - offset);
        pushOutOfBlocks(posX + offset, box.minY + 0.5, posZ - offset);
        pushOutOfBlocks(posX + offset, box.minY + 0.5, posZ + offset);

        boolean hungerAllows = foodLevel > 6 || allowFlying;
        boolean wantsSprint = player.isSprinting();

        if (!sprinting && movementInput.moveForward >= 0.8f && hungerAllows && !player.isUsingItem()
                && !player.isPotionActive(Potion.blindness) && wantsSprint) {
            sprinting = true;
        }

        if (movementInput.sneak) {
            sprinting = false;
        }

        if (sprinting && (movementInput.moveForward < 0.8f || isCollidedHorizontally || !hungerAllows)) {
            sprinting = false;
        }

        if (allowFlying && mc.playerController.isSpectatorMode() && !flying) {
            flying = true;
        }

        if (flying) {
            if (movementInput.sneak) {
                motionY -= flySpeed * 3.0f;
            }
            if (movementInput.jump) {
                motionY += flySpeed * 3.0f;
            }
        }

        if (jumpTicks > 0) {
            jumpTicks--;
        }

        if (Math.abs(motionX) < 0.005) {
            motionX = 0.0;
        }
        if (Math.abs(motionY) < 0.005) {
            motionY = 0.0;
        }
        if (Math.abs(motionZ) < 0.005) {
            motionZ = 0.0;
        }

        if (player.getHealth() <= 0f || player.isPlayerSleeping()) {
            jumping = false;
            moveStrafing = 0.0f;
            moveForward = 0.0f;
        } else {
            moveForward = movementInput.moveForward;
            moveStrafing = movementInput.moveStrafe;
            jumping = movementInput.jump;
        }

        if (jumping) {
            if (inWater || isInLava()) {
                motionY += 0.04;
            } else if (onGround && jumpTicks == 0) {
                jump();
                jumpTicks = 10;
            }
        } else {
            jumpTicks = 0;
        }

        moveStrafing *= 0.98f;
        moveForward *= 0.98f;

        if (flying) {
            double savedY = motionY;
            float savedFactor = jumpMovementFactor;
            jumpMovementFactor = flySpeed * (sprinting ? 2 : 1);
            travel(moveStrafing, moveForward);
            motionY = savedY * 0.6;
            jumpMovementFactor = savedFactor;
        } else {
            travel(moveStrafing, moveForward);
        }

        jumpMovementFactor = AIR_SPEED;
        if (sprinting) {
            jumpMovementFactor = (float) (jumpMovementFactor + AIR_SPEED * 0.3);
        }

        if (onGround && flying && !isSpectator) {
            flying = false;
        }
    }

    private void clampPosition() {
        double x = MathHelper.clamp_double(posX, -2.9999999E7, 2.9999999E7);
        double z = MathHelper.clamp_double(posZ, -2.9999999E7, 2.9999999E7);
        if (x != posX || z != posZ) {
            posX = x;
            posZ = z;
            float half = width / 2.0f;
            box = new AxisAlignedBB(x - half, posY, z - half, x + half, posY + height, z + half);
        }
    }

    private void pushOutOfBlocks(double x, double y, double z) {
        if (noClip) {
            return;
        }

        BlockPos blockPos = new BlockPos(x, y, z);
        double dx = x - blockPos.getX();
        double dz = z - blockPos.getZ();
        int entHeight = (int) Math.ceil(height);

        if (isHeadspaceFree(blockPos, entHeight)) {
            return;
        }

        int side = -1;
        double best = 9999.0;

        if (isHeadspaceFree(blockPos.west(), entHeight) && dx < best) {
            best = dx;
            side = 0;
        }
        if (isHeadspaceFree(blockPos.east(), entHeight) && 1.0 - dx < best) {
            best = 1.0 - dx;
            side = 1;
        }
        if (isHeadspaceFree(blockPos.north(), entHeight) && dz < best) {
            best = dz;
            side = 4;
        }
        if (isHeadspaceFree(blockPos.south(), entHeight) && 1.0 - dz < best) {
            side = 5;
        }

        float push = 0.1f;
        if (side == 0) {
            motionX = -push;
        }
        if (side == 1) {
            motionX = push;
        }
        if (side == 4) {
            motionZ = -push;
        }
        if (side == 5) {
            motionZ = push;
        }
    }

    private boolean isHeadspaceFree(BlockPos pos, int blocks) {
        for (int y = 0; y < blocks; y++) {
            if (!worldObj.getBlockState(pos.add(0, y, 0)).getBlock().isNormalCube()) {
                return false;
            }
        }
        return true;
    }

    private float groundSlipperiness() {
        BlockPos below = new BlockPos(MathHelper.floor_double(posX), MathHelper.floor_double(box.minY) - 1, MathHelper.floor_double(posZ));
        return worldObj.getBlockState(below).getBlock().slipperiness;
    }

    private float aiMoveSpeed() {
        return (float) player.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue();
    }

    private void travel(float strafing, float forward) {
        if (inWater && !flying) {
            double startY = posY;
            float waterDrag = 0.8f;
            float waterSpeed = 0.02f;
            float depthStrider = EnchantmentHelper.getDepthStriderModifier(player);

            if (depthStrider > 3.0f) {
                depthStrider = 3.0f;
            }
            if (!onGround) {
                depthStrider *= 0.5f;
            }
            if (depthStrider > 0.0f) {
                waterDrag += (0.54600006f - waterDrag) * depthStrider / 3.0f;
                waterSpeed += (aiMoveSpeed() - waterSpeed) * depthStrider / 3.0f;
            }

            moveFlying(strafing, forward, waterSpeed);
            moveEntity(motionX, motionY, motionZ);
            motionX *= waterDrag;
            motionY *= 0.8;
            motionZ *= waterDrag;
            motionY -= 0.02;

            if (isCollidedHorizontally && isOffsetPositionInLiquid(motionX, motionY + 0.6 - posY + startY, motionZ)) {
                motionY = 0.3;
            }
            return;
        }

        if (isInLava() && !flying) {
            double startY = posY;
            moveFlying(strafing, forward, 0.02f);
            moveEntity(motionX, motionY, motionZ);
            motionX *= 0.5;
            motionY *= 0.5;
            motionZ *= 0.5;
            motionY -= 0.02;

            if (isCollidedHorizontally && isOffsetPositionInLiquid(motionX, motionY + 0.6 - posY + startY, motionZ)) {
                motionY = 0.3;
            }
            return;
        }

        float friction = 0.91f;
        if (onGround) {
            friction = groundSlipperiness() * 0.91f;
        }

        float groundFactor = 0.16277136f / (friction * friction * friction);
        float acceleration = onGround ? aiMoveSpeed() * groundFactor : jumpMovementFactor;

        moveFlying(strafing, forward, acceleration);

        friction = 0.91f;
        if (onGround) {
            friction = groundSlipperiness() * 0.91f;
        }

        if (isOnLadder()) {
            float clamp = 0.15f;
            motionX = MathHelper.clamp_double(motionX, -clamp, clamp);
            motionZ = MathHelper.clamp_double(motionZ, -clamp, clamp);
            fallDistance = 0.0f;

            if (motionY < -0.15) {
                motionY = -0.15;
            }
            if (isSneaking() && motionY < 0.0) {
                motionY = 0.0;
            }
        }

        moveEntity(motionX, motionY, motionZ);

        if (isCollidedHorizontally && isOnLadder()) {
            motionY = 0.2;
        }

        BlockPos column = new BlockPos(posX, 0, posZ);
        if (worldObj.isRemote && (!worldObj.isBlockLoaded(column) || !worldObj.getChunkFromBlockCoords(column).isLoaded())) {
            motionY = posY > 0.0 ? -0.1 : 0.0;
        } else {
            motionY -= 0.08;
        }

        motionY *= 0.98;
        motionX *= friction;
        motionZ *= friction;
    }

    private double[] edgeClamp(double velocityX, double velocityZ) {
        double step = 0.05;
        double clampedX = velocityX;
        double clampedZ = velocityZ;

        while (clampedX != 0 && worldObj.getCollidingBoundingBoxes(player, box.offset(clampedX, -1, 0)).isEmpty()) {
            if (clampedX < step && clampedX >= -step) {
                clampedX = 0;
            } else if (clampedX > 0) {
                clampedX -= step;
            } else {
                clampedX += step;
            }
        }

        while (clampedZ != 0 && worldObj.getCollidingBoundingBoxes(player, box.offset(0, -1, clampedZ)).isEmpty()) {
            if (clampedZ < step && clampedZ >= -step) {
                clampedZ = 0;
            } else if (clampedZ > 0) {
                clampedZ -= step;
            } else {
                clampedZ += step;
            }
        }

        while (clampedX != 0 && clampedZ != 0 && worldObj.getCollidingBoundingBoxes(player, box.offset(clampedX, -1, clampedZ)).isEmpty()) {
            if (clampedX < step && clampedX >= -step) {
                clampedX = 0;
            } else if (clampedX > 0) {
                clampedX -= step;
            } else {
                clampedX += step;
            }

            if (clampedZ < step && clampedZ >= -step) {
                clampedZ = 0;
            } else if (clampedZ > 0) {
                clampedZ -= step;
            } else {
                clampedZ += step;
            }
        }

        return new double[]{clampedX, clampedZ};
    }

    public void moveEntity(double x, double y, double z) {
        if (noClip) {
            box = box.offset(x, y, z);
            resetPositionToBB();
            return;
        }

        if (isInWeb) {
            isInWeb = false;
            x *= 0.25;
            y *= 0.05;
            z *= 0.25;
            motionX = 0.0;
            motionY = 0.0;
            motionZ = 0.0;
        }

        double origX = x;
        double origY = y;
        double origZ = z;

        boolean edgeGuard = onGround && (isSneaking() || safeWalk);

        if (edgeGuard) {
            double[] clamped = edgeClamp(x, z);
            x = clamped[0];
            z = clamped[1];
            origX = x;
            origZ = z;
        }

        List<AxisAlignedBB> colliders = worldObj.getCollidingBoundingBoxes(player, box.addCoord(x, y, z));
        AxisAlignedBB startBox = box;

        for (AxisAlignedBB collider : colliders) {
            y = collider.calculateYOffset(box, y);
        }
        box = box.offset(0.0, y, 0.0);

        boolean grounded = onGround || (origY != y && origY < 0.0);

        for (AxisAlignedBB collider : colliders) {
            x = collider.calculateXOffset(box, x);
        }
        box = box.offset(x, 0.0, 0.0);

        for (AxisAlignedBB collider : colliders) {
            z = collider.calculateZOffset(box, z);
        }
        box = box.offset(0.0, 0.0, z);

        if (stepHeight > 0.0f && grounded && (origX != x || origZ != z)) {
            double flatX = x;
            double flatY = y;
            double flatZ = z;
            AxisAlignedBB flatBox = box;

            box = startBox;
            y = stepHeight;

            List<AxisAlignedBB> stepColliders = worldObj.getCollidingBoundingBoxes(player, box.addCoord(origX, y, origZ));
            AxisAlignedBB boxA = box;
            AxisAlignedBB sweepA = boxA.addCoord(origX, 0.0, origZ);
            double riseA = y;

            for (AxisAlignedBB collider : stepColliders) {
                riseA = collider.calculateYOffset(sweepA, riseA);
            }
            boxA = boxA.offset(0.0, riseA, 0.0);

            double moveAX = origX;
            for (AxisAlignedBB collider : stepColliders) {
                moveAX = collider.calculateXOffset(boxA, moveAX);
            }
            boxA = boxA.offset(moveAX, 0.0, 0.0);

            double moveAZ = origZ;
            for (AxisAlignedBB collider : stepColliders) {
                moveAZ = collider.calculateZOffset(boxA, moveAZ);
            }
            boxA = boxA.offset(0.0, 0.0, moveAZ);

            AxisAlignedBB boxB = box;
            double riseB = y;
            for (AxisAlignedBB collider : stepColliders) {
                riseB = collider.calculateYOffset(boxB, riseB);
            }
            boxB = boxB.offset(0.0, riseB, 0.0);

            double moveBX = origX;
            for (AxisAlignedBB collider : stepColliders) {
                moveBX = collider.calculateXOffset(boxB, moveBX);
            }
            boxB = boxB.offset(moveBX, 0.0, 0.0);

            double moveBZ = origZ;
            for (AxisAlignedBB collider : stepColliders) {
                moveBZ = collider.calculateZOffset(boxB, moveBZ);
            }
            boxB = boxB.offset(0.0, 0.0, moveBZ);

            double distA = moveAX * moveAX + moveAZ * moveAZ;
            double distB = moveBX * moveBX + moveBZ * moveBZ;

            if (distA > distB) {
                x = moveAX;
                z = moveAZ;
                y = -riseA;
                box = boxA;
            } else {
                x = moveBX;
                z = moveBZ;
                y = -riseB;
                box = boxB;
            }

            for (AxisAlignedBB collider : stepColliders) {
                y = collider.calculateYOffset(box, y);
            }
            box = box.offset(0.0, y, 0.0);

            if (flatX * flatX + flatZ * flatZ >= x * x + z * z) {
                x = flatX;
                y = flatY;
                z = flatZ;
                box = flatBox;
            }
        }

        resetPositionToBB();

        isCollidedHorizontally = origX != x || origZ != z;
        boolean collidedVertically = origY != y;
        onGround = collidedVertically && origY < 0.0;

        BlockPos landing = new BlockPos(MathHelper.floor_double(posX), MathHelper.floor_double(posY - 0.2), MathHelper.floor_double(posZ));
        Block landingBlock = worldObj.getBlockState(landing).getBlock();

        if (landingBlock.getMaterial() == Material.air) {
            Block below = worldObj.getBlockState(landing.down()).getBlock();
            if (below instanceof BlockFence || below instanceof BlockWall || below instanceof BlockFenceGate) {
                landingBlock = below;
            }
        }

        updateFallState(y, onGround);

        if (origX != x) {
            motionX = 0.0;
        }
        if (origZ != z) {
            motionZ = 0.0;
        }
        if (origY != y) {
            onLanded(landingBlock);
        }

        if (!flying && !edgeGuard && ridingEntity == null && onGround && landingBlock instanceof BlockSlime) {
            if (Math.abs(motionY) < 0.1 && !isSneaking()) {
                double scale = 0.4 + Math.abs(motionY) * 0.2;
                motionX *= scale;
                motionZ *= scale;
            }
        }

        doBlockCollisions();
    }

    private void doBlockCollisions() {
        BlockPos min = new BlockPos(box.minX + 0.001, box.minY + 0.001, box.minZ + 0.001);
        BlockPos max = new BlockPos(box.maxX - 0.001, box.maxY - 0.001, box.maxZ - 0.001);

        if (!isAreaLoaded(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ())) {
            return;
        }

        for (int i = min.getX(); i <= max.getX(); i++) {
            for (int j = min.getY(); j <= max.getY(); j++) {
                for (int k = min.getZ(); k <= max.getZ(); k++) {
                    Block block = worldObj.getBlockState(new BlockPos(i, j, k)).getBlock();
                    if (block instanceof BlockWeb) {
                        isInWeb = true;
                    } else if (block instanceof BlockSoulSand) {
                        motionX *= 0.4;
                        motionZ *= 0.4;
                    }
                }
            }
        }
    }

    private void updateFallState(double dy, boolean grounded) {
        if (!inWater) {
            handleWaterMovement();
        }

        if (grounded) {
            if (fallDistance > 0.0f) {
                fallDistance = 0.0f;
            }
        } else if (dy < 0.0) {
            fallDistance = (float) (fallDistance - dy);
        }
    }

    private boolean handleWaterMovement() {
        AxisAlignedBB region = box.expand(0.0, -0.4, 0.0).contract(0.001, 0.001, 0.001);

        if (handleMaterialAcceleration(region, Material.water)) {
            fallDistance = 0.0f;
            inWater = true;
        } else {
            inWater = false;
        }

        return inWater;
    }

    private boolean handleMaterialAcceleration(AxisAlignedBB bb, Material material) {
        int minX = MathHelper.floor_double(bb.minX);
        int maxX = MathHelper.floor_double(bb.maxX + 1.0);
        int minY = MathHelper.floor_double(bb.minY);
        int maxY = MathHelper.floor_double(bb.maxY + 1.0);
        int minZ = MathHelper.floor_double(bb.minZ);
        int maxZ = MathHelper.floor_double(bb.maxZ + 1.0);

        if (!isAreaLoaded(minX, minY, minZ, maxX, maxY, maxZ)) {
            return false;
        }

        boolean found = false;
        Vec3 flow = new Vec3(0.0, 0.0, 0.0);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    cursor.set(x, y, z);
                    IBlockState state = worldObj.getBlockState(cursor);
                    Block block = state.getBlock();

                    if (block.getMaterial() == material) {
                        double surface = (y + 1) - BlockLiquid.getLiquidHeightPercent(state.getValue(BlockLiquid.LEVEL));
                        if (maxY >= surface) {
                            found = true;
                            flow = block.modifyAcceleration(worldObj, cursor, player, flow);
                        }
                    }
                }
            }
        }

        if (flow.lengthVector() > 0.0 && !flying) {
            flow = flow.normalize();
            double push = 0.014;
            motionX += flow.xCoord * push;
            motionY += flow.yCoord * push;
            motionZ += flow.zCoord * push;
        }

        return found;
    }

    private boolean isAreaLoaded(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (maxY < 0 || minY >= 256) {
            return false;
        }

        int cMinX = minX >> 4;
        int cMinZ = minZ >> 4;
        int cMaxX = maxX >> 4;
        int cMaxZ = maxZ >> 4;

        for (int i = cMinX; i <= cMaxX; i++) {
            for (int j = cMinZ; j <= cMaxZ; j++) {
                if (!chunkProvider.chunkExists(i, j)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isOnLadder() {
        int i = MathHelper.floor_double(posX);
        int j = MathHelper.floor_double(box.minY);
        int k = MathHelper.floor_double(posZ);
        Block block = worldObj.getBlockState(new BlockPos(i, j, k)).getBlock();
        return (block == Blocks.ladder || block == Blocks.vine) && !isSpectator;
    }

    private void moveFlying(float strafe, float forward, float friction) {
        float magnitude = strafe * strafe + forward * forward;

        if (magnitude >= 1.0E-4f) {
            magnitude = MathHelper.sqrt_float(magnitude);
            if (magnitude < 1.0f) {
                magnitude = 1.0f;
            }

            magnitude = friction / magnitude;
            strafe *= magnitude;
            forward *= magnitude;

            float sin = MathHelper.sin(rotationYaw * 3.1415927f / 180.0f);
            float cos = MathHelper.cos(rotationYaw * 3.1415927f / 180.0f);

            motionX += strafe * cos - forward * sin;
            motionZ += forward * cos + strafe * sin;
        }
    }

    private void jump() {
        motionY = 0.42f;

        if (player.isPotionActive(Potion.jump)) {
            motionY += (player.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1f;
        }

        if (sprinting) {
            float yawRad = rotationYaw * 0.017453292f;
            motionX -= MathHelper.sin(yawRad) * 0.2f;
            motionZ += MathHelper.cos(yawRad) * 0.2f;
        }

        airBorne = true;
    }

    private boolean isInLava() {
        return worldObj.isMaterialInBB(box.expand(-0.1, -0.4, -0.1), Material.lava);
    }

    private boolean isOffsetPositionInLiquid(double x, double y, double z) {
        AxisAlignedBB shifted = box.offset(x, y, z);
        return worldObj.getCollidingBoundingBoxes(player, shifted).isEmpty() && !worldObj.isAnyLiquid(shifted);
    }

    private void resetPositionToBB() {
        posX = (box.minX + box.maxX) / 2.0;
        posY = box.minY;
        posZ = (box.minZ + box.maxZ) / 2.0;
    }

    private void onLanded(Block block) {
        if (block instanceof BlockSlime) {
            if (isSneaking()) {
                motionY = 0.0;
            } else if (motionY < 0.0) {
                motionY = -motionY;
            }
        } else {
            motionY = 0.0;
        }
    }

    private boolean isSneaking() {
        return movementInput.sneak && !player.isPlayerSleeping();
    }
}