package ddlc.yuri.managers.impl;

import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.client.PolarNoiseUtils;
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

/**
 * MASSIVELY, HUGELY randomized polar rotation manager.
 * Stacks an absurd number of independent randomization algorithms on top of
 * each other so the resulting rotation path is essentially unpredictable yet
 * still aims at the target. Every single layer reseeds its own parameters on a
 * random timer, meaning the behaviour constantly rewrites itself.
 */
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

    // ---- NEW: multi-orbit layers ------------------------------------
    private static double orbit2Freq, orbit2Phase, orbit2RadiusYaw, orbit2RadiusPitch;
    private static double orbit3Freq, orbit3Phase, orbit3RadiusYaw, orbit3RadiusPitch;
    private static long orbit2ReseedAt, orbit3ReseedAt;

    // ---- NEW: lissajous path ----------------------------------------
    private static double lissFreqX, lissFreqY, lissPhase, lissAmpX, lissAmpY;
    private static long lissReseedAt;

    // ---- NEW: spirograph / rose path ---------------------------------
    private static double spiroPetal, spiroAmp, spiroPhase;
    private static long spiroReseedAt;

    // ---- NEW: random-walk brownian wander ----------------------------
    private static double walkX, walkY, walkTargetX, walkTargetY, walkSpeed;
    private static long walkReseedAt;

    // ---- NEW: logistic chaos seed -------------------------------------
    private static double logisticR, logisticX;
    private static long logisticReseedAt;

    // ---- NEW: beat interference ----------------------------------------
    private static double beatFreqA, beatFreqB, beatPhase;
    private static long beatReseedAt;

    // ---- NEW: independent per-axis amplitude/frequency noise ----------
    private static double yawNoiseAmp, yawNoiseFreq, pitchNoiseAmp, pitchNoiseFreq;
    private static long axisReseedAt;

    // ---- NEW: randomized base point blend ------------------------------
    private static double baseBlend;
    private static long baseReseedAt;

    private static Vec3 heldPoint;
    private static long holdUntil;
    private static long nextReseedAt;

    private static double weightAim, weightCenter, weightEye;
    private static long weightReseedAt;

    static {
        reseedChaos();
        reseedOrbit();
        reseedWeights();
        reseedOrbit2();
        reseedOrbit3();
        reseedLissajous();
        reseedSpirograph();
        reseedWalk();
        reseedLogistic();
        reseedBeats();
        reseedAxis();
        reseedBase();
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

    private static void reseedOrbit2() {
        orbit2Freq = 0.0009 + POLAR_RNG.nextDouble() * 0.0025;
        orbit2Phase = POLAR_RNG.nextDouble() * Math.PI * 2;
        orbit2RadiusYaw = 0.1 + POLAR_RNG.nextDouble() * 0.7;
        orbit2RadiusPitch = 0.05 + POLAR_RNG.nextDouble() * 0.4;
        orbit2ReseedAt = System.currentTimeMillis() + 900 + POLAR_RNG.nextInt(2600);
    }

    private static void reseedOrbit3() {
        orbit3Freq = 0.0012 + POLAR_RNG.nextDouble() * 0.003;
        orbit3Phase = POLAR_RNG.nextDouble() * Math.PI * 2;
        orbit3RadiusYaw = 0.05 + POLAR_RNG.nextDouble() * 0.45;
        orbit3RadiusPitch = 0.03 + POLAR_RNG.nextDouble() * 0.25;
        orbit3ReseedAt = System.currentTimeMillis() + 600 + POLAR_RNG.nextInt(2000);
    }

    private static void reseedLissajous() {
        lissFreqX = 1 + POLAR_RNG.nextInt(6);
        lissFreqY = 1 + POLAR_RNG.nextInt(6);
        lissPhase = POLAR_RNG.nextDouble() * Math.PI * 2;
        lissAmpX = 0.1 + POLAR_RNG.nextDouble() * 0.5;
        lissAmpY = 0.05 + POLAR_RNG.nextDouble() * 0.3;
        lissReseedAt = System.currentTimeMillis() + 1200 + POLAR_RNG.nextInt(3000);
    }

    private static void reseedSpirograph() {
        spiroPetal = 2 + POLAR_RNG.nextInt(6);
        spiroAmp = 0.1 + POLAR_RNG.nextDouble() * 0.6;
        spiroPhase = POLAR_RNG.nextDouble() * Math.PI * 2;
        spiroReseedAt = System.currentTimeMillis() + 1400 + POLAR_RNG.nextInt(3200);
    }

    private static void reseedWalk() {
        walkX = 0;
        walkY = 0;
        walkTargetX = (POLAR_RNG.nextDouble() - 0.5) * 2.0;
        walkTargetY = (POLAR_RNG.nextDouble() - 0.5) * 2.0;
        walkSpeed = 0.01 + POLAR_RNG.nextDouble() * 0.04;
        walkReseedAt = System.currentTimeMillis() + 700 + POLAR_RNG.nextInt(1800);
    }

    private static void reseedLogistic() {
        logisticR = 3.6 + POLAR_RNG.nextDouble() * 0.4;
        logisticX = POLAR_RNG.nextDouble();
        logisticReseedAt = System.currentTimeMillis() + 1000 + POLAR_RNG.nextInt(2600);
    }

    private static void reseedBeats() {
        beatFreqA = 0.0008 + POLAR_RNG.nextDouble() * 0.0015;
        beatFreqB = beatFreqA * (0.7 + POLAR_RNG.nextDouble() * 0.6);
        beatPhase = POLAR_RNG.nextDouble() * Math.PI * 2;
        beatReseedAt = System.currentTimeMillis() + 1500 + POLAR_RNG.nextInt(3000);
    }

    private static void reseedAxis() {
        yawNoiseAmp = 0.3 + POLAR_RNG.nextDouble() * 1.2;
        yawNoiseFreq = 0.4 + POLAR_RNG.nextDouble() * 1.4;
        pitchNoiseAmp = 0.15 + POLAR_RNG.nextDouble() * 0.7;
        pitchNoiseFreq = 0.3 + POLAR_RNG.nextDouble() * 1.2;
        axisReseedAt = System.currentTimeMillis() + 500 + POLAR_RNG.nextInt(1500);
    }

    private static void reseedBase() {
        baseBlend = 0.2 + POLAR_RNG.nextDouble() * 0.8;
        baseReseedAt = System.currentTimeMillis() + 900 + POLAR_RNG.nextInt(2200);
    }

    public static void reset() {
        flicking = false;
        heldPoint = null;
        holdUntil = 0;
        nextReseedAt = 0;
        reseedChaos();
        reseedOrbit();
        reseedWeights();
        reseedOrbit2();
        reseedOrbit3();
        reseedLissajous();
        reseedSpirograph();
        reseedWalk();
        reseedLogistic();
        reseedBeats();
        reseedAxis();
        reseedBase();
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
        if (now >= orbit2ReseedAt) reseedOrbit2();
        if (now >= orbit3ReseedAt) reseedOrbit3();
        if (now >= lissReseedAt) reseedLissajous();
        if (now >= spiroReseedAt) reseedSpirograph();
        if (now >= walkReseedAt) reseedWalk();
        if (now >= logisticReseedAt) reseedLogistic();
        if (now >= beatReseedAt) reseedBeats();
        if (now >= axisReseedAt) reseedAxis();
        if (now >= baseReseedAt) reseedBase();

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

        // ---- LAYER 1: classic perlin jitter ---------------------------
        float yawJitter = (float) (PolarNoiseUtils.noise(noiseTime + noiseDriftX, 0) * 1.4);
        float pitchJitter = (float) (PolarNoiseUtils.noise(0, noiseTime + noiseDriftY) * 0.7);
        baseRotation = new Vector2f(baseRotation.x + yawJitter, baseRotation.y + pitchJitter);

        // ---- LAYER 2: chaos doubles --------------------------------
        float chaosYaw = (float) (Math.sin(now * chaosFreqA + chaosPhaseA) * chaosAmpA
                + Math.sin(now * chaosFreqB * 1.7 + chaosPhaseB) * chaosAmpA * 0.4);
        float chaosPitch = (float) (Math.cos(now * chaosFreqB + chaosPhaseB) * chaosAmpB
                + Math.cos(now * chaosFreqA * 1.3 + chaosPhaseA) * chaosAmpB * 0.4);
        baseRotation = new Vector2f(baseRotation.x + chaosYaw, baseRotation.y + chaosPitch);

        // ---- LAYER 3: primary orbit ---------------------------------
        float orbitYaw = (float) (Math.sin(now * orbitFreq + orbitPhase) * orbitRadiusYaw);
        float orbitPitch = (float) (Math.cos(now * orbitFreq * 0.8 + orbitPhase) * orbitRadiusPitch);
        baseRotation = new Vector2f(baseRotation.x + orbitYaw, baseRotation.y + orbitPitch);

        // ---- LAYER 4: secondary orbit (independent freq) ------------
        float orbitYaw2 = (float) (Math.sin(now * orbit2Freq + orbit2Phase) * orbit2RadiusYaw);
        float orbitPitch2 = (float) (Math.cos(now * orbit2Freq * 1.13 + orbit2Phase) * orbit2RadiusPitch);
        baseRotation = new Vector2f(baseRotation.x + orbitYaw2, baseRotation.y + orbitPitch2);

        // ---- LAYER 5: tertiary micro-orbit ---------------------------
        float orbitYaw3 = (float) (Math.sin(now * orbit3Freq * 1.61 + orbit3Phase) * orbit3RadiusYaw);
        float orbitPitch3 = (float) (Math.cos(now * orbit3Freq + orbit3Phase * 1.7) * orbit3RadiusPitch);
        baseRotation = new Vector2f(baseRotation.x + orbitYaw3, baseRotation.y + orbitPitch3);

        // ---- LAYER 6: lissajous curve --------------------------------
        float lissT = (float) (now * 0.0006);
        float lissYaw = (float) (Math.sin(lissFreqX * lissT + lissPhase) * lissAmpX);
        float lissPitch = (float) (Math.sin(lissFreqY * lissT) * lissAmpY);
        baseRotation = new Vector2f(baseRotation.x + lissYaw, baseRotation.y + lissPitch);

        // ---- LAYER 7: spirograph / rose curve ------------------------
        float spiroT = (float) (now * 0.0004);
        float spiroYaw = (float) (Math.sin(spiroPetal * spiroT + spiroPhase) * Math.cos(spiroT) * spiroAmp);
        float spiroPitch = (float) (Math.cos(spiroPetal * spiroT + spiroPhase) * Math.sin(spiroT * 1.3) * spiroAmp * 0.5);
        baseRotation = new Vector2f(baseRotation.x + spiroYaw, baseRotation.y + spiroPitch);

        // ---- LAYER 8: brownian random walk ----------------------------
        walkX += (walkTargetX - walkX) * walkSpeed + (POLAR_RNG.nextDouble() - 0.5) * 0.03;
        walkY += (walkTargetY - walkY) * walkSpeed + (POLAR_RNG.nextDouble() - 0.5) * 0.02;
        if ((walkTargetX - walkX) * (walkTargetX - walkX) < 0.01 &&
                (walkTargetY - walkY) * (walkTargetY - walkY) < 0.01) {
            walkTargetX = (POLAR_RNG.nextDouble() - 0.5) * 2.0;
            walkTargetY = (POLAR_RNG.nextDouble() - 0.5) * 2.0;
        }
        baseRotation = new Vector2f(baseRotation.x + (float) walkX, baseRotation.y + (float) walkY);

        // ---- LAYER 9: logistic map chaos ------------------------------
        logisticX = logisticR * logisticX * (1.0 - logisticX);
        double logisticVal = (logisticX - 0.5) * 2.0;
        baseRotation = new Vector2f(baseRotation.x + (float) (logisticVal * 0.5), baseRotation.y + (float) (logisticVal * 0.35));

        // ---- LAYER 10: beat interference ------------------------------
        float beatYaw = (float) (Math.sin(now * beatFreqA + beatPhase) * Math.sin(now * beatFreqB) * 0.6);
        float beatPitch = (float) (Math.cos(now * beatFreqA * 0.9) * Math.cos(now * beatFreqB + beatPhase) * 0.35);
        baseRotation = new Vector2f(baseRotation.x + beatYaw, baseRotation.y + beatPitch);

        // ---- LAYER 11: independent per-axis fractal noise -------------
        float axisYaw = (float) (PolarNoiseUtils.simplex2D(now * yawNoiseFreq * 0.002, noiseDriftX) * yawNoiseAmp);
        float axisPitch = (float) (PolarNoiseUtils.simplex2D(now * pitchNoiseFreq * 0.0017, noiseDriftY) * pitchNoiseAmp);
        baseRotation = new Vector2f(baseRotation.x + axisYaw, baseRotation.y + axisPitch);

        // ---- LAYER 12: cellular / worley micro-stutter -----------------
        float cellYaw = (float) ((PolarNoiseUtils.cellularNoise(noiseTime * 0.5, noiseDriftY * 0.3) - 0.5) * 0.4);
        float cellPitch = (float) ((PolarNoiseUtils.cellularNoise(noiseDriftX * 0.3, noiseTime * 0.5) - 0.5) * 0.25);
        baseRotation = new Vector2f(baseRotation.x + cellYaw, baseRotation.y + cellPitch);

        // ---- LAYER 13: turbulence spikes -------------------------------
        float turbYaw = (float) ((PolarNoiseUtils.turbulence(noiseTime * 0.3, noiseDriftX, 3) - 0.9) * 0.7 * noiseScale * 0.2);
        float turbPitch = (float) ((PolarNoiseUtils.billow(noiseTime * 0.25, noiseDriftY, 3) - 0.9) * 0.4 * noiseScale * 0.2);
        baseRotation = new Vector2f(baseRotation.x + turbYaw, baseRotation.y + turbPitch);

        // ---- LAYER 14: ridged multifractal edges -----------------------
        float ridgedYaw = (float) ((float) PolarNoiseUtils.ridgedNoise(noiseTime * 0.35, noiseDriftY, 4, 2.0, 0.7) * 0.5);
        float ridgedPitch = (float) ((float) PolarNoiseUtils.ridgedNoise(noiseDriftX, noiseTime * 0.35, 4, 2.0, 0.7) * 0.3);
        baseRotation = new Vector2f(baseRotation.x + ridgedYaw, baseRotation.y + ridgedPitch);

        // ---- LAYER 15: rotating simplex domain warp --------------------
        double warpAngle = Math.sin(now * 0.0003) * 0.5 + baseBlend;
        double rotatingWarp = PolarNoiseUtils.domainWarpRotating(noiseTime * 0.2, noiseDriftY, warpStrength * 0.15, noiseScale * 0.2, noiseOctaves, warpAngle);
        baseRotation = new Vector2f(baseRotation.x + (float) (rotatingWarp * 0.6), baseRotation.y + (float) (rotatingWarp * 0.35));

        // ---- LAYER 16: dual-blend fractal -------------------------------
        double dual = PolarNoiseUtils.dualFbm(noiseTime * 0.18, noiseDriftY, noiseOctaves, 2.0, 0.5, 2.6, 0.35, baseBlend);
        baseRotation = new Vector2f(baseRotation.x + (float) (dual * 0.45), baseRotation.y + (float) (dual * 0.28));

        // ---- LAYER 17: blended perlin/simplex/value ---------------------
        double blended = PolarNoiseUtils.blendedNoise(noiseTime * 0.22, noiseDriftX, noiseOctaves, 0.4, 0.35, 0.25);
        baseRotation = new Vector2f(baseRotation.x + (float) (blended * 0.55), baseRotation.y + (float) (blended * 0.32));

        // ---- LAYER 18: white-noise micro flutter ------------------------
        float flutter = (float) PolarNoiseUtils.jitter(noiseTime * 0.11) * 0.18f;
        baseRotation = new Vector2f(baseRotation.x + flutter, baseRotation.y + flutter * 0.6f);

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

            // increase randomization: pick a random warp variant per octave pass
            boolean useSimplex = POLAR_RNG.nextBoolean();
            double warpX, warpY;

            if (useSimplex) {
                warpX = PolarNoiseUtils.fbmSimplex(nx, ny, octaves, 2.0, 0.5) * warpStrength * chaosScale;
                warpY = PolarNoiseUtils.fbmSimplex(ny + 1.7, nx + 4.3, octaves, 2.2, 0.45) * warpStrength * chaosScale;
            } else {
                warpX = PolarNoiseUtils.domainWarp(nx, ny, warpStrength, noiseScale, octaves) * warpStrength * chaosScale;
                warpY = PolarNoiseUtils.domainWarp(ny + 3.7, nx + 7.1, warpStrength, noiseScale, octaves) * warpStrength * chaosScale;
            }

            double fbmX = PolarNoiseUtils.fbm(nx + warpX, ny + warpY, octaves, 2.0, 0.5) * warpStrength * 0.5 * chaosScale;
            double fbmY = PolarNoiseUtils.fbm(nx + 5.3 + warpX, ny + 9.1 + warpY, octaves, 2.0, 0.5) * warpStrength * 0.5 * chaosScale;

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
