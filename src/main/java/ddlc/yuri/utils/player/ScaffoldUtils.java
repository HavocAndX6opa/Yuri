package ddlc.yuri.utils.player;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.PriorityQueue;

public final class ScaffoldUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private ScaffoldUtils() {}

    private static final class AStarNode implements Comparable<AStarNode> {
        final float yaw, pitch;
        final float g, h, f;
        final Vec3 hitVec;

        AStarNode(float yaw, float pitch, float g, float h, Vec3 hitVec) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.hitVec = hitVec;
        }

        @Override
        public int compareTo(AStarNode o) {
            return Float.compare(this.f, o.f);
        }
    }

    private static Vector2f serverRotations() {
        return RotationManager.rotations != null ? RotationManager.rotations : new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }

    private static float stabilityPenalty(BlockPos blockFace) {
        int exposed = 0;
        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            BlockPos neighbor = blockFace.offset(face);
            if (mc.theWorld.getBlockState(neighbor).getBlock().isReplaceable(mc.theWorld, neighbor)) {
                exposed++;
            }
        }
        return exposed * 2.5f;
    }

    private static float directionalCost(BlockPos blockFace, EnumFacingOffset enumFacing) {
        double dx = blockFace.getX() + 0.5 - mc.thePlayer.posX;
        double dz = blockFace.getZ() + 0.5 - mc.thePlayer.posZ;
        double movYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        double playerYaw = mc.thePlayer.rotationYaw;
        double diff = Math.abs(MathHelper.wrapAngleTo180_double(movYaw - playerYaw));
        return (float) (diff / 180.0) * 4.0f;
    }

    private static void computeAStarRotations(BlockPos blockFace, EnumFacingOffset enumFacing,
                                              float[] target, boolean strict) {
        Vector2f server = serverRotations();
        float serverYaw = server.x;
        float serverPitch = server.y;

        double deltaX = blockFace.getX() + 0.5 - mc.thePlayer.posX;
        double deltaY = blockFace.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double deltaZ = blockFace.getZ() + 0.5 - mc.thePlayer.posZ;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float baseYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        float basePitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontal));

        float hBase = stabilityPenalty(blockFace) + directionalCost(blockFace, enumFacing);

        float[][] passes = {
                {20f, 2.5f},
                {45f, 5f},
                {90f, 7.5f},
                {180f, 10f}
        };

        AStarNode best = null;
        AStarNode bestUnstrict = null;

        for (float[] pass : passes) {
            float range = pass[0];
            float step = pass[1];
            PriorityQueue<AStarNode> open = new PriorityQueue<>();
            PriorityQueue<AStarNode> openUnstrict = strict ? new PriorityQueue<>() : null;

            for (float dy = -range; dy <= range; dy += step) {
                for (float dp = -range; dp <= range; dp += step) {
                    float testYaw = MathHelper.wrapAngleTo180_float(baseYaw + dy);
                    float testPitch = MathHelper.clamp_float(basePitch + dp, -90, 90);
                    Vector2f testRot = new Vector2f(testYaw, testPitch);

                    float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(testYaw - serverYaw));
                    float pitchDiff = Math.abs(testPitch - serverPitch);
                    float g = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

                    Vec3 hitVec = new Vec3(
                            blockFace.getX() + 0.5,
                            blockFace.getY() + 0.5,
                            blockFace.getZ() + 0.5);

                    if (RayCastUtils.overBlock(testRot, enumFacing.getEnumFacing(), blockFace, strict)) {
                        open.add(new AStarNode(testYaw, testPitch, g, hBase, hitVec));
                    } else if (openUnstrict != null && RayCastUtils.overBlock(testRot, enumFacing.getEnumFacing(), blockFace, false)) {
                        openUnstrict.add(new AStarNode(testYaw, testPitch, g, hBase, hitVec));
                    }
                }
            }

            if (!open.isEmpty()) {
                best = open.poll();
                break;
            }
            if (openUnstrict != null && bestUnstrict == null && !openUnstrict.isEmpty()) {
                bestUnstrict = openUnstrict.poll();
            }
        }

        if (best != null) {
            target[0] = best.yaw;
            target[1] = best.pitch;
            return;
        }

        if (bestUnstrict != null) {
            target[0] = bestUnstrict.yaw;
            target[1] = bestUnstrict.pitch;
            return;
        }

        for (EnumFacing face : EnumFacing.VALUES) {
            if (face == enumFacing.getEnumFacing()) continue;
            Vector2f fallback = RotationUtils.calculate(
                    new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()), face);
            if (RayCastUtils.overBlock(fallback, face, blockFace, false)) {
                target[0] = fallback.x;
                target[1] = fallback.y;
                return;
            }
        }

        final Vector2f fallback = RotationUtils.calculate(
                new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()),
                enumFacing.getEnumFacing());
        target[0] = fallback.x;
        target[1] = fallback.y;
    }


    public static void computeNormalRotations(BlockPos blockFace, EnumFacingOffset enumFacing,
                                              float[] target, float[] drift,
                                              ScaffoldModule.SearchAlgorithm algorithm,
                                              boolean strict) {
        switch (algorithm) {
            case NORMAL: {
                double difference = mc.thePlayer.posY + mc.thePlayer.getEyeHeight()
                        - blockFace.getY() - 0.5 - (Math.random() - 0.5) * 0.1;

                for (int offset = -180; offset <= 180; offset += 45) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - difference, mc.thePlayer.posZ);
                    MovingObjectPosition mop = RayCastUtils.rayCast(
                            new Vector2f((float) (mc.thePlayer.rotationYaw + (offset * 3)), 0), 4.5);
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + difference, mc.thePlayer.posZ);

                    if (mop == null || mop.hitVec == null) return;

                    Vector2f rotations = RotationUtils.calculate(mop.hitVec);
                    if (RayCastUtils.overBlock(rotations, blockFace, enumFacing.getEnumFacing())) {
                        target[0] = rotations.x;
                        target[1] = rotations.y;
                        return;
                    }
                }

                final Vector2f rotations = RotationUtils.calculate(
                        new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()),
                        enumFacing.getEnumFacing());

                if (!RayCastUtils.overBlock(new Vector2f(target[0], target[1]), blockFace, enumFacing.getEnumFacing())) {
                    target[0] = rotations.x;
                    target[1] = rotations.y;
                }
                break;
            }
            case SECONDARY: {
                double deltaX = blockFace.getX() - mc.thePlayer.posX + 0.5;
                double deltaY = blockFace.getY() - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) + 0.5;
                double deltaZ = blockFace.getZ() - mc.thePlayer.posZ + 0.5;
                double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                float baseYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
                float basePitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

                float bestYaw = baseYaw;
                float bestPitch = basePitch;
                double bestDistance = Double.MAX_VALUE;

                Vector2f server = serverRotations();

                for (float yawOff = -15; yawOff <= 15; yawOff += 3) {
                    for (float pitchOff = -15; pitchOff <= 15; pitchOff += 3) {
                        float testYaw = baseYaw + yawOff;
                        float testPitch = MathHelper.clamp_float(basePitch + pitchOff, -90, 90);
                        Vector2f testRot = new Vector2f(testYaw, testPitch);

                        if (RayCastUtils.overBlock(testRot, enumFacing.getEnumFacing(), blockFace, strict)) {
                            double yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(testYaw - server.x));
                            double pitchDiff = Math.abs(testPitch - server.y);
                            double totalDist = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

                            if (totalDist < bestDistance) {
                                bestDistance = totalDist;
                                bestYaw = testYaw;
                                bestPitch = testPitch;
                            }
                        }
                    }
                }

                if (bestDistance != Double.MAX_VALUE) {
                    target[0] = bestYaw;
                    target[1] = bestPitch;
                } else {
                    final Vector2f rotations = RotationUtils.calculate(
                            new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()),
                            enumFacing.getEnumFacing());
                    target[0] = rotations.x;
                    target[1] = rotations.y;
                }
                break;
            }
            case ULTRA_SAFE: {
                computeAStarRotations(blockFace, enumFacing, target, true);
                break;
            }
        }
    }

    public static float[] predictionHypixelRotations(BlockPos blockFace, EnumFacingOffset enumFacing, ScaffoldModule.SearchAlgorithm algorithm) {
        float[] target = new float[2];
        ScaffoldUtils.computeNormalRotations(blockFace, enumFacing, target, new float[]{0, 0}, algorithm, true);
        float originalYaw = target[0];
        float originalPitch = target[1];

        float directionalOffset = 0.0f;
        if (mc.thePlayer != null) {
            int horizontalFacingIndex = mc.thePlayer.getHorizontalFacing().getHorizontalIndex();
            directionalOffset = horizontalFacingIndex * 90.0f;
        }

        float bestYaw = originalYaw;
        float bestPitch = originalPitch;
        boolean foundValid = false;

        for (int i = 0; i < 20; i++) {
            float randomYawOffset = (float) ((Math.random() - 0.5) * 10.0f); // ±5 degrees
            float randomPitchOffset = (float) ((Math.random() - 0.5) * 6.0f); // ±3 degrees

            float testYaw = originalYaw + 2.5f + randomYawOffset + (directionalOffset % 45.0f != 0 ? 0 : 0);
            float testPitch = MathHelper.clamp_float(originalPitch + randomPitchOffset, -90.0f, 90.0f);

            Vector2f testRotation = new Vector2f(testYaw, testPitch);

            if (RayCastUtils.overBlock(testRotation, enumFacing.getEnumFacing(), blockFace, true)) {
                bestYaw = testYaw;
                bestPitch = testPitch;
                foundValid = true;
                break;
            }
        }

        if (!foundValid) {
            bestYaw = originalYaw - 20.0f;
            bestPitch = originalPitch;
        }

        return new float[]{bestYaw, bestPitch};
    }

    public static Vec3 computeHitVec(BlockPos blockFace, EnumFacingOffset enumFacing) {
        Vec3 hitVec = new Vec3(
                blockFace.getX() + Math.random(),
                blockFace.getY() + Math.random(),
                blockFace.getZ() + Math.random());

        final MovingObjectPosition mop = RayCastUtils.rayCast(
                serverRotations(),
                mc.playerController.getBlockReachDistance());

        switch (enumFacing.getEnumFacing()) {
            case DOWN:  hitVec.yCoord = blockFace.getY();     break;
            case UP:    hitVec.yCoord = blockFace.getY() + 1; break;
            case NORTH: hitVec.zCoord = blockFace.getZ();     break;
            case EAST:  hitVec.xCoord = blockFace.getX() + 1; break;
            case SOUTH: hitVec.zCoord = blockFace.getZ() + 1; break;
            case WEST:  hitVec.xCoord = blockFace.getX();     break;
        }

        if (mop != null && mop.getBlockPos() != null && mop.hitVec != null
                && mop.getBlockPos().equals(blockFace)
                && mop.sideHit == enumFacing.getEnumFacing()) {
            hitVec = mop.hitVec;
        }

        return hitVec;
    }

    public static boolean doesNotContainBlock(Vec3i offset, int down) {
        return BlockUtils.blockRelativeToPlayer(offset.getX(), -down + offset.getY(), offset.getZ())
                .isReplaceable(mc.theWorld, new BlockPos(mc.thePlayer).down(down));
    }

    public static float getNearestDiagonalYaw(float yaw) {
        float normalizedYaw = MathHelper.wrapAngleTo180_float(yaw);
        float[] diagonals = {-135.0f, -45.0f, 45.0f, 135.0f};

        float closest = diagonals[0];
        float minDiff = Math.abs(MathHelper.wrapAngleTo180_float(normalizedYaw - closest));

        for (int i = 1; i < diagonals.length; i++) {
            float diff = Math.abs(MathHelper.wrapAngleTo180_float(normalizedYaw - diagonals[i]));
            if (diff < minDiff) {
                minDiff = diff;
                closest = diagonals[i];
            }
        }

        float yawDelta = MathHelper.wrapAngleTo180_float(closest - yaw);
        if (Math.abs(yawDelta) > 89.0f) {
            closest = yaw + Math.signum(yawDelta) * 89.0f;
        }

        return closest;
    }

    public static int countBlocks() {
        int count = 0;
        for (ItemStack stack : mc.thePlayer.inventory.mainInventory) {
            if (stack != null && stack.getItem() instanceof ItemBlock
                    && !BlockUtils.blacklist.contains(((ItemBlock) stack.getItem()).getBlock())) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    public static void renderBlockCounter(String counterMode, float alpha, int blockCount) {
        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int textWidth = FontUtils.getFont("sf", 14).getStringWidth(String.valueOf(blockCount) + EnumChatFormatting.WHITE + " blocks");
        int textX = centerX - textWidth / 2;
        int textY = sr.getScaledHeight() / 2 + 13;

        Color accent = ColorManager.getColor();
        int accentAlpha = RenderUtils.applyOpacity(accent.getRGB(), alpha);

        if (counterMode.equals("Simple")) {
            FontUtils.getFont("sf", 14).drawStringWithShadow(
                    String.valueOf(blockCount) + EnumChatFormatting.WHITE + " blocks", textX, textY, accentAlpha);
        }
    }
}
