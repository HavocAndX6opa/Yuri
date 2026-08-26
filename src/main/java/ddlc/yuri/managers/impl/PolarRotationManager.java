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

    private static final long SEED = MathUtils.getRandomInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    private static final Random POLAR_RNG = new Random(SEED);

    private static boolean flicking;
    private static long flickStartTime;
    private static long flickDurationMs;
    private static float flickDirectionYaw;
    private static float flickDirectionPitch;
    private static float flickSpeed;
    private static float flickRecoverySpeed;
    private static float flickStrength;

    private static double noiseTime = POLAR_RNG.nextDouble() * 10000;
    private static double noiseDriftX = POLAR_RNG.nextDouble() * 1000;
    private static double noiseDriftY = POLAR_RNG.nextDouble() * 1000;

    private static double chaosFreqA, chaosFreqB, chaosPhaseA, chaosPhaseB, chaosAmpA, chaosAmpB;

    private static double orbitFreq, orbitPhase, orbitRadiusYaw, orbitRadiusPitch;
    private static long orbitReseedAt;

    private static Vec3 heldPoint;
    private static long holdUntil;
    private static long nextReseedAt;

    private static double weightAim, weightCenter, weightEye;
    private static long weightReseedAt;

    static {
        reseedChaos();
        reseedOrbit();
        reseedWeights();
    }

    private static void reseedChaos() {
        chaosFreqA = 0.0015 + POLAR_RNG.nextDouble() * 0.003;
        chaosFreqB = 0.002 + POLAR_RNG.nextDouble() * 0.004;
        chaosPhaseA = POLAR_RNG.nextDouble() * Math.PI * 2;
        chaosPhaseB = POLAR_RNG.nextDouble() * Math.PI * 2;
        chaosAmpA = 0.4 + POLAR_RNG.nextDouble() * 0.8;
        chaosAmpB = 0.2 + POLAR_RNG.nextDouble() * 0.5;
    }

    private static void reseedOrbit() {
        orbitFreq = 0.0006 + POLAR_RNG.nextDouble() * 0.0014;
        orbitPhase = POLAR_RNG.nextDouble() * Math.PI * 2;
        orbitRadiusYaw = 0.3 + POLAR_RNG.nextDouble() * 1.1;
        orbitRadiusPitch = 0.15 + POLAR_RNG.nextDouble() * 0.6;
        orbitReseedAt = System.currentTimeMillis() + 2000 + POLAR_RNG.nextInt(4000);
    }

    private static void reseedWeights() {
        weightAim = 3.0 + (POLAR_RNG.nextDouble() - 0.5) * 1.6;
        weightCenter = 2.0 + (POLAR_RNG.nextDouble() - 0.5) * 1.2;
        weightEye = 0.3 + (POLAR_RNG.nextDouble() - 0.5) * 0.3;
        weightReseedAt = System.currentTimeMillis() + 800 + POLAR_RNG.nextInt(1600);
    }

    public static void reset() {
        flicking = false;
        heldPoint = null;
        holdUntil = 0;
        nextReseedAt = 0;
        reseedChaos();
        reseedOrbit();
        reseedWeights();
    }

    public static void resetFlick() {
        flicking = false;
    }

    public static void triggerFlick(float flickChance) {
        if (flicking) return;
        if (flickChance <= 0) return;
        if (MathUtils.getRandom(0.0, 100.0) > flickChance) return;

        float intensity = (float) (flickChance / 100.0);
        float roll = POLAR_RNG.nextFloat();

        if (intensity > 0.7f) {
            flickStrength = 10.0f + roll * 16.0f;
            flickSpeed = 4.5f + POLAR_RNG.nextFloat() * 3.5f;
            flickRecoverySpeed = 2.0f + POLAR_RNG.nextFloat() * 2.0f;
            flickDurationMs = 130 + POLAR_RNG.nextInt(140);
        } else if (intensity > 0.3f) {
            flickStrength = 5.0f + roll * 10.0f;
            flickSpeed = 3.0f + POLAR_RNG.nextFloat() * 2.5f;
            flickRecoverySpeed = 1.5f + POLAR_RNG.nextFloat() * 1.5f;
            flickDurationMs = 160 + POLAR_RNG.nextInt(200);
        } else {
            flickStrength = 2.0f + roll * 5.0f;
            flickSpeed = 2.0f + POLAR_RNG.nextFloat() * 2.0f;
            flickRecoverySpeed = 1.0f + POLAR_RNG.nextFloat() * 1.0f;
            flickDurationMs = 200 + POLAR_RNG.nextInt(260);
        }

        flicking = true;
        flickStartTime = System.currentTimeMillis();

        flickDirectionYaw = POLAR_RNG.nextBoolean() ? 1f : -1f;
        flickDirectionPitch = (POLAR_RNG.nextFloat() - 0.5f);
    }

    public static Vector2f getPolarRotation(EntityLivingBase target,
                                            double noiseScale,
                                            double warpStrength,
                                            int noiseOctaves,
                                            float flickChance,
                                            double seekRange) {
        if (target == null || mc.thePlayer == null) return null;

        long now = System.currentTimeMillis();

        AxisAlignedBB box = target.getEntityBoundingBox();
        Vec3 eyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

        if (now >= weightReseedAt) reseedWeights();
        if (now >= orbitReseedAt) reseedOrbit();

        Vec3 bestPoint;

        if (heldPoint != null && now < holdUntil) {
            bestPoint = heldPoint;
        } else {
            List<Vec3> scanPoints = generateBodyScan(box);
            scanPoints = subdivideByDistance(scanPoints);
            scanPoints = applyNoiseTransform(scanPoints, noiseScale, warpStrength, noiseOctaves);

            bestPoint = selectWeightedPoint(scanPoints, eyePos, target, seekRange);

            if (bestPoint == null) {
                bestPoint = new Vec3(
                        (box.minX + box.maxX) / 2.0,
                        (box.minY + box.maxY) / 2.0,
                        (box.minZ + box.maxZ) / 2.0
                );
            }

            heldPoint = bestPoint;
            if (POLAR_RNG.nextDouble() < 0.35) {
                holdUntil = now + 40 + POLAR_RNG.nextInt(90);
            } else {
                holdUntil = now;
            }
        }

        Vector2f baseRotation = RotationUtils.calculate(bestPoint);

        noiseTime += 0.001 + POLAR_RNG.nextDouble() * 0.001;
        noiseDriftX += (POLAR_RNG.nextDouble() - 0.5) * 0.02;
        noiseDriftY += (POLAR_RNG.nextDouble() - 0.5) * 0.02;

        float yawJitter = (float) (PolarNoise.noise(noiseTime + noiseDriftX, 0) * 1.4);
        float pitchJitter = (float) (PolarNoise.noise(0, noiseTime + noiseDriftY) * 0.7);
        baseRotation = new Vector2f(baseRotation.x + yawJitter, baseRotation.y + pitchJitter);

        float chaosYaw = (float) (Math.sin(now * chaosFreqA + chaosPhaseA) * chaosAmpA
                + Math.sin(now * chaosFreqB * 1.7 + chaosPhaseB) * chaosAmpA * 0.4);
        float chaosPitch = (float) (Math.cos(now * chaosFreqB + chaosPhaseB) * chaosAmpB
                + Math.cos(now * chaosFreqA * 1.3 + chaosPhaseA) * chaosAmpB * 0.4);
        baseRotation = new Vector2f(baseRotation.x + chaosYaw, baseRotation.y + chaosPitch);

        float orbitYaw = (float) (Math.sin(now * orbitFreq + orbitPhase) * orbitRadiusYaw);
        float orbitPitch = (float) (Math.cos(now * orbitFreq * 0.8 + orbitPhase) * orbitRadiusPitch);
        baseRotation = new Vector2f(baseRotation.x + orbitYaw, baseRotation.y + orbitPitch);

        if (flicking) {
            baseRotation = applyFlick(baseRotation);
        }

        return baseRotation;
    }

    private static List<Vec3> generateBodyScan(AxisAlignedBB box) {
        List<Vec3> points = new ArrayList<>();
        double w = box.maxX - box.minX;
        double h = box.maxY - box.minY;
        double d = box.maxZ - box.minZ;
        double midX = (box.minX + box.maxX) / 2.0;
        double midZ = (box.minZ + box.maxZ) / 2.0;

        double cloudRotation = POLAR_RNG.nextDouble() * Math.PI * 2;
        double eccentricity = 0.7 + POLAR_RNG.nextDouble() * 0.6;

        int torsoCount = 6 + POLAR_RNG.nextInt(6);
        double torsoY = box.minY + h * (0.40 + POLAR_RNG.nextDouble() * 0.18);
        double torsoRadius = Math.min(w, d) * (0.30 + POLAR_RNG.nextDouble() * 0.22);
        for (int i = 0; i < torsoCount; i++) {
            double angle = cloudRotation + (Math.PI * 2 * i) / torsoCount + (POLAR_RNG.nextDouble() - 0.5) * 0.5;
            points.add(new Vec3(
                    midX + Math.cos(angle) * torsoRadius * eccentricity,
                    torsoY + (POLAR_RNG.nextDouble() - 0.5) * h * 0.22,
                    midZ + Math.sin(angle) * torsoRadius
            ));
        }

        int upperCount = 2 + POLAR_RNG.nextInt(5);
        double upperY = box.minY + h * (0.70 + POLAR_RNG.nextDouble() * 0.18);
        double upperRadius = Math.min(w, d) * (0.20 + POLAR_RNG.nextDouble() * 0.2);
        for (int i = 0; i < upperCount; i++) {
            double angle = cloudRotation + (Math.PI * 2 * i) / upperCount + POLAR_RNG.nextDouble() * 0.6;
            points.add(new Vec3(
                    midX + Math.cos(angle) * upperRadius,
                    upperY + (POLAR_RNG.nextDouble() - 0.5) * h * 0.12,
                    midZ + Math.sin(angle) * upperRadius
            ));
        }

        int lowerCount = 2 + POLAR_RNG.nextInt(5);
        double lowerY = box.minY + h * (0.15 + POLAR_RNG.nextDouble() * 0.18);
        double lowerRadius = Math.min(w, d) * (0.26 + POLAR_RNG.nextDouble() * 0.2);
        for (int i = 0; i < lowerCount; i++) {
            double angle = cloudRotation + (Math.PI * 2 * i) / lowerCount + POLAR_RNG.nextDouble() * 0.6;
            points.add(new Vec3(
                    midX + Math.cos(angle) * lowerRadius,
                    lowerY + (POLAR_RNG.nextDouble() - 0.5) * h * 0.12,
                    midZ + Math.sin(angle) * lowerRadius
            ));
        }

        int limbCount = 1 + POLAR_RNG.nextInt(4);
        for (int i = 0; i < limbCount; i++) {
            double side = POLAR_RNG.nextBoolean() ? 1.0 : -1.0;
            double angle = cloudRotation + POLAR_RNG.nextDouble() * Math.PI * 2;
            double radius = Math.min(w, d) * (0.45 + POLAR_RNG.nextDouble() * 0.45);
            points.add(new Vec3(
                    midX + Math.cos(angle) * radius * side,
                    box.minY + h * (0.25 + POLAR_RNG.nextDouble() * 0.55),
                    midZ + Math.sin(angle) * radius * side
            ));
        }

        if (POLAR_RNG.nextDouble() < 0.6) {
            points.add(new Vec3(
                    midX + (POLAR_RNG.nextDouble() - 0.5) * w * 0.3,
                    box.maxY - h * 0.02,
                    midZ + (POLAR_RNG.nextDouble() - 0.5) * d * 0.3
            ));
        }
        if (POLAR_RNG.nextDouble() < 0.4) {
            points.add(new Vec3(
                    midX + (POLAR_RNG.nextDouble() - 0.5) * w * 0.3,
                    box.minY + h * 0.02,
                    midZ + (POLAR_RNG.nextDouble() - 0.5) * d * 0.3
            ));
        }

        return points;
    }

    private static List<Vec3> subdivideByDistance(List<Vec3> scanPoints) {
        if (scanPoints.size() < 2) return scanPoints;

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
        double chaosScale = 0.7 + POLAR_RNG.nextDouble() * 0.8;

        for (int i = 0; i < points.size(); i++) {
            Vec3 p = points.get(i);
            double nx = p.xCoord * noiseScale + i * 0.5 + noiseDriftX;
            double ny = p.yCoord * noiseScale + i * 0.3 + noiseDriftY;

            double warpX = PolarNoise.domainWarp(nx, ny, warpStrength, noiseScale, octaves) * warpStrength * chaosScale;
            double warpY = PolarNoise.domainWarp(ny + 3.7, nx + 7.1, warpStrength, noiseScale, octaves) * warpStrength * chaosScale;

            double fbmX = PolarNoise.fbm(nx + warpX, ny + warpY, octaves, 2.0, 0.5) * warpStrength * 0.5 * chaosScale;
            double fbmY = PolarNoise.fbm(nx + 5.3 + warpX, ny + 9.1 + warpY, octaves, 2.0, 0.5) * warpStrength * 0.5 * chaosScale;

            warped.add(new Vec3(
                    p.xCoord + warpX + fbmX,
                    p.yCoord + warpY + fbmY,
                    p.zCoord + (warpX + fbmX) * 0.5
            ));
        }

        return warped;
    }

    private static Vec3 selectWeightedPoint(List<Vec3> points, Vec3 eyePos, EntityLivingBase target, double seekRange) {
        Vec3 targetCenter = new Vec3(
                (target.getEntityBoundingBox().minX + target.getEntityBoundingBox().maxX) / 2.0,
                (target.getEntityBoundingBox().minY + target.getEntityBoundingBox().maxY) / 2.0,
                (target.getEntityBoundingBox().minZ + target.getEntityBoundingBox().maxZ) / 2.0
        );

        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;

        List<Vec3> candidates = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        for (Vec3 point : points) {
            float[] rot = RotationUtils.getRotationsTo(eyePos, point);
            float deltaYaw = MathHelper.wrapAngleTo180_float(rot[0] - currentYaw);
            float deltaPitch = rot[1] - currentPitch;

            double aimError = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

            double dx = point.xCoord - targetCenter.xCoord;
            double dy = point.yCoord - targetCenter.yCoord;
            double dz = point.zCoord - targetCenter.zCoord;
            double centerDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double ex = point.xCoord - eyePos.xCoord;
            double ey = point.yCoord - eyePos.yCoord;
            double ez = point.zCoord - eyePos.zCoord;
            double eyeDist = Math.sqrt(ex * ex + ey * ey + ez * ez);

            if (eyeDist > seekRange) continue;

            double score = aimError * weightAim + centerDist * weightCenter + eyeDist * weightEye;
            candidates.add(point);
            scores.add(score);
        }

        if (candidates.isEmpty()) return null;

        int poolSize = Math.min(candidates.size(), 4 + POLAR_RNG.nextInt(3));
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) order.add(i);
        order.sort((a, b) -> Double.compare(scores.get(a), scores.get(b)));
        List<Integer> pool = order.subList(0, poolSize);

        double totalWeight = 0;
        double[] weights = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            double s = scores.get(pool.get(i));
            double w = 1.0 / (1.0 + s);
            weights[i] = w;
            totalWeight += w;
        }

        double roll = POLAR_RNG.nextDouble() * totalWeight;
        double acc = 0;
        for (int i = 0; i < pool.size(); i++) {
            acc += weights[i];
            if (roll <= acc) {
                return candidates.get(pool.get(i));
            }
        }

        return candidates.get(pool.get(0));
    }

    private static Vector2f applyFlick(Vector2f baseRotation) {
        long elapsed = System.currentTimeMillis() - flickStartTime;
        float progress = elapsed / (float) flickDurationMs;

        if (progress >= 1.0f) {
            flicking = false;
            return baseRotation;
        }

        float offsetYaw;
        float offsetPitch;

        if (progress < 0.35f) {
            float p = progress / 0.35f;
            float eased = p * p;
            offsetYaw = flickDirectionYaw * flickStrength * eased * flickSpeed * 0.18f;
            offsetPitch = flickDirectionPitch * flickStrength * eased * flickSpeed * 0.12f;
        } else {
            float p = (progress - 0.35f) / 0.65f;
            float eased = 1.0f - (1.0f - p) * (1.0f - p);
            float overshoot = (float) Math.sin(p * Math.PI * 1.4f) * 0.18f;
            float settle = (1.0f - eased) + overshoot * (1.0f - eased);
            offsetYaw = flickDirectionYaw * flickStrength * settle * flickRecoverySpeed * 0.12f;
            offsetPitch = flickDirectionPitch * flickStrength * settle * flickRecoverySpeed * 0.09f;
        }

        return new Vector2f(baseRotation.x + offsetYaw, baseRotation.y + offsetPitch);
    }
}