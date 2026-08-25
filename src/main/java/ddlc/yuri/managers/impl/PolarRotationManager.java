package ddlc.yuri.managers.impl;

import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.client.PolarNoise;
import ddlc.yuri.utils.player.RotationUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class PolarRotationManager {

    private static final int BASE_SCAN_POINTS = 20;
    private static final long SEED = MathUtils.getRandomInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    private static final Random POLAR_RNG = new Random(SEED);

    // Flick state
    private static boolean flicking;
    private static int flickTick;
    private static float flickOvershootYaw;
    private static float flickOvershootPitch;
    private static float flickDirectionYaw;
    private static float flickDirectionPitch;
    private static float flickSpeed;
    private static float flickRecoverySpeed;
    private static int flickDuration;
    private static float flickStrength;

    // Rotational history for the overshoot/correct motion
    private static float previousRotationYaw;
    private static float previousRotationPitch;
    private static boolean hasPrevious;

    // Noise time accumulator (never resets = never repeats)
    private static double noiseTime = POLAR_RNG.nextDouble() * 10000;

    public static void reset() {
        flicking = false;
        flickTick = 0;
        hasPrevious = false;
    }

    public static void resetFlick() {
        flicking = false;
        flickTick = 0;
    }

    /**
     * Triggered when the player is attacked mid-rotation.
     * Generates an overshoot offset in the direction opposite to where we were rotating,
     * then corrects back toward the target - simulating a natural mouse flick response.
     */
    public static void triggerFlick(float flickChance) {
        if (flicking) return;

        float baseStrength;
        float baseSpeed;
        float baseRecovery;
        int baseDuration;

        if (flickChance <= 0) return;

        // Scale intensity by chance value
        float intensity = (float) (flickChance / 100.0);

        // When chance is high (e.g. 80-100), make it snappy and strong
        // When chance is low (e.g. 10-30), make it subtle
        if (intensity > 0.7f) {
            baseStrength = 8.0f + POLAR_RNG.nextFloat() * 12.0f; // 8-20 degrees overshoot
            baseSpeed = 4.0f + POLAR_RNG.nextFloat() * 3.0f;     // fast flick
            baseRecovery = 2.0f + POLAR_RNG.nextFloat() * 1.5f;   // moderate correction
            baseDuration = 3 + POLAR_RNG.nextInt(3);               // 3-5 ticks
        } else if (intensity > 0.3f) {
            baseStrength = 4.0f + POLAR_RNG.nextFloat() * 8.0f;   // 4-12 degrees overshoot
            baseSpeed = 3.0f + POLAR_RNG.nextFloat() * 2.0f;     // medium flick
            baseRecovery = 1.5f + POLAR_RNG.nextFloat() * 1.0f;   // moderate correction
            baseDuration = 4 + POLAR_RNG.nextInt(4);               // 4-7 ticks
        } else {
            baseStrength = 2.0f + POLAR_RNG.nextFloat() * 4.0f;   // 2-6 degrees overshoot
            baseSpeed = 2.0f + POLAR_RNG.nextFloat() * 1.5f;     // slow flick
            baseRecovery = 1.0f + POLAR_RNG.nextFloat() * 0.5f;   // slow correction
            baseDuration = 5 + POLAR_RNG.nextInt(5);               // 5-9 ticks
        }

        flicking = true;
        flickTick = 0;
        flickStrength = baseStrength;
        flickSpeed = baseSpeed;
        flickRecoverySpeed = baseRecovery;
        flickDuration = baseDuration;

        // Flick in a random perpendicular-ish direction relative to current aim
        // This simulates the hand jerking when taking damage
        flickDirectionYaw = (POLAR_RNG.nextFloat() - 0.5f) * 2.0f;
        flickDirectionPitch = (POLAR_RNG.nextFloat() - 0.5f) * 0.5f;
        // Normalize yaw direction
        float yawMag = Math.abs(flickDirectionYaw);
        if (yawMag > 0.001f) {
            flickDirectionYaw /= yawMag;
            flickDirectionYaw *= (POLAR_RNG.nextBoolean() ? 1 : -1);
        } else {
            flickDirectionYaw = POLAR_RNG.nextBoolean() ? 1 : -1;
        }
    }

    /**
     * Generates the 20-point body scan, subdivides by distance, applies domain warp + fractal noise,
     * then picks the best point to aim at. If a flick is active, applies overshoot/correction.
     */
    public static Vector2f getPolarRotation(EntityLivingBase target,
                                            double noiseScale,
                                            double warpStrength,
                                            int noiseOctaves,
                                            float flickChance,
                                            double seekRange) {
        if (target == null || mc.thePlayer == null) return null;

        AxisAlignedBB box = target.getEntityBoundingBox();
        Vec3 eyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

        // Step 1: Generate 20 scan points across the body
        List<Vec3> scanPoints = generateBodyScan(box);

        // Step 2: Linear subdivisions by distance
        scanPoints = subdivideByDistance(scanPoints);

        // Step 3: Domain warping + fractal noise pass
        scanPoints = applyNoiseTransform(scanPoints, noiseScale, warpStrength, noiseOctaves);

        // Step 4: Pick best point and compute rotation
        Vec3 bestPoint = selectBestPoint(scanPoints, eyePos, target, seekRange);

        if (bestPoint == null) {
            // Fallback to center of bounding box
            bestPoint = new Vec3(
                    (box.minX + box.maxX) / 2.0,
                    (box.minY + box.maxY) / 2.0,
                    (box.minZ + box.maxZ) / 2.0
            );
        }

        Vector2f baseRotation = RotationUtils.calculate(bestPoint);

        // Add per-frame noise jitter (always nonzero = never the same rotation twice)
        noiseTime += 0.001 + POLAR_RNG.nextDouble() * 0.001;
        float yawJitter = (float) (PolarNoise.noise(noiseTime, 0) * 1.2);
        float pitchJitter = (float) (PolarNoise.noise(0, noiseTime) * 0.6);
        baseRotation = new Vector2f(baseRotation.x + yawJitter, baseRotation.y + pitchJitter);

        // Step 5: Flick overshoot/correction
        if (flicking) {
            baseRotation = applyFlick(baseRotation);
        }

        // Track previous for next frame
        previousRotationYaw = baseRotation.x;
        previousRotationPitch = baseRotation.y;
        hasPrevious = true;

        return baseRotation;
    }

    private static List<Vec3> generateBodyScan(AxisAlignedBB box) {
        List<Vec3> points = new ArrayList<>(BASE_SCAN_POINTS);
        double w = box.maxX - box.minX;
        double h = box.maxY - box.minY;
        double d = box.maxZ - box.minZ;

        // Vertical ring at torso level
        double torsoY = box.minY + h * 0.5;
        double ringRadius = Math.min(w, d) * 0.4;
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i) / 8.0;
            points.add(new Vec3(
                    (box.minX + box.maxX) / 2.0 + Math.cos(angle) * ringRadius,
                    torsoY + (POLAR_RNG.nextDouble() - 0.5) * h * 0.15,
                    (box.minZ + box.maxZ) / 2.0 + Math.sin(angle) * ringRadius
            ));
        }

        // Upper body ring
        double upperY = box.minY + h * 0.78;
        double upperRadius = Math.min(w, d) * 0.3;
        for (int i = 0; i < 5; i++) {
            double angle = (Math.PI * 2 * i) / 5.0 + POLAR_RNG.nextDouble() * 0.3;
            points.add(new Vec3(
                    (box.minX + box.maxX) / 2.0 + Math.cos(angle) * upperRadius,
                    upperY + (POLAR_RNG.nextDouble() - 0.5) * h * 0.08,
                    (box.minZ + box.maxZ) / 2.0 + Math.sin(angle) * upperRadius
            ));
        }

        // Lower body ring
        double lowerY = box.minY + h * 0.25;
        double lowerRadius = Math.min(w, d) * 0.35;
        for (int i = 0; i < 5; i++) {
            double angle = (Math.PI * 2 * i) / 5.0 + POLAR_RNG.nextDouble() * 0.3;
            points.add(new Vec3(
                    (box.minX + box.maxX) / 2.0 + Math.cos(angle) * lowerRadius,
                    lowerY + (POLAR_RNG.nextDouble() - 0.5) * h * 0.08,
                    (box.minZ + box.maxZ) / 2.0 + Math.sin(angle) * lowerRadius
            ));
        }

        // Head and feet cap points
        points.add(new Vec3(
                (box.minX + box.maxX) / 2.0 + (POLAR_RNG.nextDouble() - 0.5) * w * 0.3,
                box.maxY - h * 0.02,
                (box.minZ + box.maxZ) / 2.0 + (POLAR_RNG.nextDouble() - 0.5) * d * 0.3
        ));
        points.add(new Vec3(
                (box.minX + box.maxX) / 2.0 + (POLAR_RNG.nextDouble() - 0.5) * w * 0.3,
                box.minY + h * 0.02,
                (box.minZ + box.maxZ) / 2.0 + (POLAR_RNG.nextDouble() - 0.5) * d * 0.3
        ));

        return points;
    }

    private static List<Vec3> subdivideByDistance(List<Vec3> scanPoints) {
        if (scanPoints.size() < 2) return scanPoints;

        // Compute average distance between consecutive points
        double totalDist = 0;
        for (int i = 0; i < scanPoints.size(); i++) {
            Vec3 a = scanPoints.get(i);
            Vec3 b = scanPoints.get((i + 1) % scanPoints.size());
            double dx = a.xCoord - b.xCoord;
            double dy = a.yCoord - b.yCoord;
            double dz = a.zCoord - b.zCoord;
            totalDist += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        double avgDist = totalDist / scanPoints.size();

        List<Vec3> result = new ArrayList<>();

        for (int i = 0; i < scanPoints.size(); i++) {
            result.add(scanPoints.get(i));

            Vec3 current = scanPoints.get(i);
            Vec3 next = scanPoints.get((i + 1) % scanPoints.size());

            double dx = next.xCoord - current.xCoord;
            double dy = next.yCoord - current.yCoord;
            double dz = next.zCoord - current.zCoord;
            double segDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (segDist > avgDist * 0.5) {
                // More subdivisions for longer gaps
                int subdivisions = MathUtils.getRandomInt(2, Math.min(8, (int) Math.ceil(segDist / avgDist * 2)));
                for (int s = 1; s < subdivisions; s++) {
                    double t = (double) s / subdivisions;
                    result.add(new Vec3(
                            current.xCoord + dx * t,
                            current.yCoord + dy * t,
                            current.zCoord + dz * t
                    ));
                }
            }
        }

        return result;
    }

    private static List<Vec3> applyNoiseTransform(List<Vec3> points, double noiseScale,
                                                  double warpStrength, int octaves) {
        List<Vec3> warped = new ArrayList<>(points.size());

        for (int i = 0; i < points.size(); i++) {
            Vec3 p = points.get(i);
            double nx = p.xCoord * noiseScale + i * 0.5;
            double ny = p.yCoord * noiseScale + i * 0.3;

            // Domain warping - noise of noise for organic distortion
            double warpX = PolarNoise.domainWarp(nx, ny, warpStrength, noiseScale, octaves) * warpStrength;
            double warpY = PolarNoise.domainWarp(ny + 3.7, nx + 7.1, warpStrength, noiseScale, octaves) * warpStrength;

            // Fractal Brownian Motion for high-frequency detail
            double fbmX = PolarNoise.fbm(nx + warpX, ny + warpY, octaves, 2.0, 0.5) * warpStrength * 0.5;
            double fbmY = PolarNoise.fbm(nx + 5.3 + warpX, ny + 9.1 + warpY, octaves, 2.0, 0.5) * warpStrength * 0.5;

            warped.add(new Vec3(
                    p.xCoord + warpX + fbmX,
                    p.yCoord + warpY + fbmY,
                    p.zCoord + (warpX + fbmX) * 0.5
            ));
        }

        return warped;
    }

    /**
     * Selects the point that best balances crosshair proximity and hit validity.
     */
    private static Vec3 selectBestPoint(List<Vec3> points, Vec3 eyePos, EntityLivingBase target, double seekRange) {
        Vec3 best = null;
        double bestScore = Double.MAX_VALUE;

        Vec3 targetCenter = new Vec3(
                (target.getEntityBoundingBox().minX + target.getEntityBoundingBox().maxX) / 2.0,
                (target.getEntityBoundingBox().minY + target.getEntityBoundingBox().maxY) / 2.0,
                (target.getEntityBoundingBox().minZ + target.getEntityBoundingBox().maxZ) / 2.0
        );

        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;

        for (Vec3 point : points) {
            float[] rot = RotationUtils.getRotationsTo(eyePos, point);
            float deltaYaw = MathHelper.wrapAngleTo180_float(rot[0] - currentYaw);
            float deltaPitch = rot[1] - currentPitch;

            // How far off crosshair is (angular distance)
            double aimError = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

            // Distance to target center (closer = better for hit registration)
            double dx = point.xCoord - targetCenter.xCoord;
            double dy = point.yCoord - targetCenter.yCoord;
            double dz = point.zCoord - targetCenter.zCoord;
            double centerDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Distance to eye (closer = more natural aim point)
            double ex = point.xCoord - eyePos.xCoord;
            double ey = point.yCoord - eyePos.yCoord;
            double ez = point.zCoord - eyePos.zCoord;
            double eyeDist = Math.sqrt(ex * ex + ey * ey + ez * ez);

            // Score: weighted combination - favor crosshair proximity + center proximity
            double score = aimError * 3.0 + centerDist * 2.0 + eyeDist * 0.3;

            if (score < bestScore && eyeDist <= seekRange) {
                bestScore = score;
                best = point;
            }
        }

        return best;
    }

    /**
     * Applies the overshoot/correction flick to a base rotation.
     * Returns a new Vector2f with the flick offset applied.
     */
    private static Vector2f applyFlick(Vector2f baseRotation) {
        flickTick++;

        float progress = (float) flickTick / (float) flickDuration;

        float offsetYaw;
        float offsetPitch;

        if (progress < 0.4f) {
            // Overshoot phase: rotate away from target (fast)
            float overshootProgress = progress / 0.4f;
            float eased = overshootProgress * overshootProgress; // quadratic ease-in
            offsetYaw = flickDirectionYaw * flickStrength * eased * flickSpeed * 0.15f;
            offsetPitch = flickDirectionPitch * flickStrength * eased * flickSpeed * 0.1f;
        } else {
            // Correction phase: ease back toward target (slower, with slight oscillation)
            float correctionProgress = (progress - 0.4f) / 0.6f;
            float eased = 1.0f - (1.0f - correctionProgress) * (1.0f - correctionProgress); // quadratic ease-out

            // Slight overshoot past target then settle (like a real mouse correction)
            float overshoot = (float) Math.sin(correctionProgress * Math.PI * 1.5f) * 0.15f;
            float settleAmount = 1.0f - eased + overshoot * (1.0f - eased);

            offsetYaw = flickDirectionYaw * flickStrength * settleAmount * flickRecoverySpeed * 0.1f;
            offsetPitch = flickDirectionPitch * flickStrength * settleAmount * flickRecoverySpeed * 0.08f;
        }

        if (flickTick >= flickDuration) {
            flicking = false;
            flickTick = 0;
        }

        return new Vector2f(baseRotation.x + offsetYaw, baseRotation.y + offsetPitch);
    }
}
