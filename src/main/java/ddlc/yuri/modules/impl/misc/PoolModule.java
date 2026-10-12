package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.FontUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Random;

@ModuleInfo(label = "8-Ball Pool", description = "Pool from Raven B4 ported to Yuri", category = ModuleCategory.MISC)
public final class PoolModule extends Module {

    private static final double PL = 3.4, PW = 1.7;
    private static final double R = 0.075;
    private static final double POCKET_R = 0.14;
    private static final double CUSH = 0.07;
    private static final double RAIL = 0.17;
    private static final double RAIL_TOP = 0.095;
    private static final double APRON_DROP = 0.34;
    private static final double POCKET_VIS = 0.115;
    private static final double POCKET_MOUTH = 0.150;
    private static final double CUSHION = 0.9;

    private static final Color C_BED     = new Color(48, 158, 88);
    private static final Color C_CUSHION = new Color(38, 132, 74);
    private static final Color C_RAIL    = new Color(156, 101, 55);
    private static final Color C_APRON   = new Color(104, 65, 34);
    private static final Color C_SIGHT   = new Color(232, 201, 140);
    private static final Color C_POCKET_RIM = new Color(214, 174, 104);
    private static final double FRICTION_STEP = 0.996;
    private static final double STOP_EPS = 0.05;
    private static final double MAXV = 4.2;
    private static final int SUBSTEPS = 12;
    private static final double STEP = 1.0 / (20.0 * SUBSTEPS);
    private static final double CHARGE_PER_TICK = MAXV / 22.0;

    private boolean placed;
    private double fX, fZ;
    private double rrX, rrZ;
    private double originX, originZ;
    private double topY;

    private final Ball[] balls = new Ball[16];
    private boolean charging;
    private double power;
    private boolean wasUse;
    private boolean ballInHand;

    private boolean aimValid;
    private double aimX, aimZ;

    private int pots, shots;
    private String message = "";
    private long messageUntil, rerackAt;
    private final Random rng = new Random();

    private static final double[][] POCKETS = {
            {0, 0}, {PL / 2, 0}, {PL, 0}, {0, PW}, {PL / 2, PW}, {PL, PW}
    };

    private static final class Ball {
        double x, z, vx, vz;

        double prevX, prevZ;
        boolean active = true;
        final int number;
        final Color color;
        final boolean striped;
        Ball(int number) {
            this.number = number;
            this.color = colorFor(number);
            this.striped = number >= 9 && number <= 15;
        }
    }

    private static Color colorFor(int n) {
        int c = n >= 9 ? n - 8 : n;
        switch (c) {
            case 1: return new Color(0xF2C200);
            case 2: return new Color(0x2246C8);
            case 3: return new Color(0xCC2222);
            case 4: return new Color(0x7A2A9A);
            case 5: return new Color(0xE87A18);
            case 6: return new Color(0x1F8A3B);
            case 7: return new Color(0x8A2A2A);
            case 8: return new Color(0x121212);
            default: return Color.WHITE;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        placeTable();
    }

    @Override
    public void onDisable() {
        placed = false;
        charging = false;
        power = 0;
        super.onDisable();
    }

    private void placeTable() {
        if (mc.thePlayer == null) { placed = false; return; }
        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        fX = -Math.sin(yaw);
        fZ = Math.cos(yaw);
        rrX = -fZ;
        rrZ = fX;

        double centerX = mc.thePlayer.posX + fX * 1.2;
        double centerZ = mc.thePlayer.posZ + fZ * 1.2;
        originX = centerX - rrX * (PW / 2);
        originZ = centerZ - rrZ * (PW / 2);
        topY = Math.floor(mc.thePlayer.posY) + 0.9;
        placed = true;
        rack();
    }

    private void rack() {
        for (int i = 0; i < 16; i++) balls[i] = new Ball(i);
        balls[0].x = PL * 0.26;
        balls[0].z = PW / 2;

        int[] order = {1, 9, 2, 10, 8, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15};
        double footX = PL * 0.70, footZ = PW / 2;
        double spacing = 2 * R + 0.004;
        int idx = 0;
        for (int row = 0; row < 5; row++) {
            for (int i = 0; i <= row; i++) {
                Ball b = byNumber(order[idx++]);
                b.x = footX + row * spacing * (Math.sqrt(3) / 2.0);
                b.z = footZ + (i - row / 2.0) * spacing;
            }
        }
        for (Ball b : balls) { b.prevX = b.x; b.prevZ = b.z; }
        pots = 0; shots = 0; ballInHand = false; rerackAt = 0;
    }

    private Ball byNumber(int n) {
        for (Ball b : balls) if (b.number == n) return b;
        return balls[0];
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (!placed || mc.thePlayer == null) return;

        for (Ball b : balls) { b.prevX = b.x; b.prevZ = b.z; }

        for (int s = 0; s < SUBSTEPS; s++) step();

        if (objectBallsCleared()) {
            if (rerackAt == 0) rerackAt = System.currentTimeMillis() + 1400;
            else if (System.currentTimeMillis() >= rerackAt) rack();
        } else {
            rerackAt = 0;
        }

        computeAim();
        boolean use = mc.currentScreen == null && mc.gameSettings.keyBindUseItem.isKeyDown();

        if (ballInHand) {
            if (use && !wasUse && aimValid && placeCue(aimX, aimZ)) ballInHand = false;
        } else {
            boolean canShoot = balls[0].active && ballsStopped();
            if (use && canShoot && aimValid) {
                charging = true;
                power = Math.min(MAXV, power + CHARGE_PER_TICK);
            } else {
                if (charging && canShoot && aimValid) fireCue();
                charging = false;
                power = 0;
            }
        }
        wasUse = use;
    }

    private void step() {
        for (Ball b : balls) {
            if (!b.active) continue;
            b.x += b.vx * STEP;
            b.z += b.vz * STEP;
            b.vx *= FRICTION_STEP;
            b.vz *= FRICTION_STEP;
            if (b.vx * b.vx + b.vz * b.vz < STOP_EPS * STOP_EPS) { b.vx = 0; b.vz = 0; }
        }

        for (int i = 0; i < balls.length; i++) {
            Ball a = balls[i];
            if (!a.active) continue;
            for (int j = i + 1; j < balls.length; j++) {
                Ball c = balls[j];
                if (!c.active) continue;
                double dx = c.x - a.x, dz = c.z - a.z;
                double d2 = dx * dx + dz * dz, min = 2 * R;
                if (d2 <= 0 || d2 >= min * min) continue;
                double d = Math.sqrt(d2), nx = dx / d, nz = dz / d, overlap = min - d;
                a.x -= nx * overlap / 2; a.z -= nz * overlap / 2;
                c.x += nx * overlap / 2; c.z += nz * overlap / 2;
                double aN = a.vx * nx + a.vz * nz, cN = c.vx * nx + c.vz * nz;
                if (cN - aN >= 0) continue;
                a.vx += (cN - aN) * nx; a.vz += (cN - aN) * nz;
                c.vx += (aN - cN) * nx; c.vz += (aN - cN) * nz;
                a.vx *= 0.99; a.vz *= 0.99; c.vx *= 0.99; c.vz *= 0.99;
            }
        }

        for (Ball b : balls) {
            if (!b.active) continue;
            boolean pocketed = false;
            for (double[] p : POCKETS) {
                double dx = b.x - p[0], dz = b.z - p[1];
                if (dx * dx + dz * dz <= POCKET_R * POCKET_R) {
                    b.active = false; b.vx = 0; b.vz = 0; onPocketed(b, p); pocketed = true; break;
                }
            }
            if (pocketed) continue;
            if (b.x < R) { b.x = R; b.vx = -b.vx * CUSHION; }
            else if (b.x > PL - R) { b.x = PL - R; b.vx = -b.vx * CUSHION; }
            if (b.z < R) { b.z = R; b.vz = -b.vz * CUSHION; }
            else if (b.z > PW - R) { b.z = PW - R; b.vz = -b.vz * CUSHION; }
        }
    }

    private void onPocketed(Ball b, double[] pocket) {
        if (b.number == 0) { ballInHand = true; setMessage("§cScratch! §7Look + right-click to place the cue"); }
        else { pots++; if (b.number == 8) setMessage("§68-ball down!"); }
        potEffects(pocket, b.number == 0);
    }

    private void potEffects(double[] pocket, boolean cue) {
        double px = wx(pocket[0], pocket[1]);
        double py = topY;
        double pz = wz(pocket[0], pocket[1]);

        if (mc.thePlayer != null) {
            mc.thePlayer.playSound(cue ? "random.break" : "random.orb", 0.7f, cue ? 0.8f : 1.35f);
        }
        if (mc.theWorld != null) {
            for (int i = 0; i < 16; i++) {
                double ox = (rng.nextDouble() - 0.5) * 0.12;
                double oz = (rng.nextDouble() - 0.5) * 0.12;

                EnumParticleTypes type = cue ? EnumParticleTypes.SPELL_INSTANT : EnumParticleTypes.VILLAGER_HAPPY;
                mc.theWorld.spawnParticle(type, px + ox, py + 0.02, pz + oz,
                        (rng.nextDouble() - 0.5) * 0.6, 0.15 + rng.nextDouble() * 0.35, (rng.nextDouble() - 0.5) * 0.6);
            }
        }
    }

    private boolean ballsStopped() {
        for (Ball b : balls) if (b.active && (b.vx != 0 || b.vz != 0)) return false;
        return true;
    }

    private boolean objectBallsCleared() {
        for (Ball b : balls) if (b.number != 0 && b.active) return false;
        return true;
    }

    private void setMessage(String m) { message = m; messageUntil = System.currentTimeMillis() + 2200; }

    private void computeAim() {
        aimValid = false;
        Vec3 look = mc.thePlayer.getLookVec();
        if (look.yCoord >= -0.02) return;
        double eyeX = mc.thePlayer.posX;
        double eyeY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double eyeZ = mc.thePlayer.posZ;
        double planeY = topY + R;
        double t = (planeY - eyeY) / look.yCoord;
        if (t <= 0) return;
        double hx = eyeX + look.xCoord * t, hz = eyeZ + look.zCoord * t;
        double dx = hx - originX, dz = hz - originZ;
        aimX = dx * fX + dz * fZ;
        aimZ = dx * rrX + dz * rrZ;
        aimValid = true;
    }

    private void fireCue() {
        Ball cue = balls[0];
        double dx = aimX - cue.x, dz = aimZ - cue.z, d = Math.sqrt(dx * dx + dz * dz);
        if (d < 0.02) return;
        cue.vx = dx / d * power;
        cue.vz = dz / d * power;
        shots++;
    }

    private boolean placeCue(double lx, double lz) {
        lx = Math.max(R, Math.min(PL - R, lx));
        lz = Math.max(R, Math.min(PW - R, lz));
        for (Ball b : balls) {
            if (b.number == 0 || !b.active) continue;
            double dx = b.x - lx, dz = b.z - lz;
            if (dx * dx + dz * dz < (2 * R) * (2 * R)) return false;
        }
        balls[0].x = lx; balls[0].z = lz; balls[0].vx = 0; balls[0].vz = 0; balls[0].active = true;
        balls[0].prevX = lx; balls[0].prevZ = lz;
        return true;
    }

    private double wx(double lx, double lz) { return originX + fX * lx + rrX * lz; }
    private double wz(double lx, double lz) { return originZ + fZ * lx + rrZ * lz; }

    private double[] predict(double cx, double cz, double dx, double dz) {
        double best = Double.MAX_VALUE;
        int hit = -1;

        if (dx > 1e-6) best = Math.min(best, (PL - R - cx) / dx);
        else if (dx < -1e-6) best = Math.min(best, (R - cx) / dx);
        if (dz > 1e-6) best = Math.min(best, (PW - R - cz) / dz);
        else if (dz < -1e-6) best = Math.min(best, (R - cz) / dz);

        for (int i = 0; i < balls.length; i++) {
            Ball c = balls[i];
            if (!c.active || c.number == 0) continue;
            double fx = cx - c.x, fz = cz - c.z;
            double b = 2 * (fx * dx + fz * dz);
            double cc = fx * fx + fz * fz - (2 * R) * (2 * R);
            double disc = b * b - 4 * cc;
            if (disc < 0) continue;
            double t = (-b - Math.sqrt(disc)) / 2;
            if (t > 1e-4 && t < best) { best = t; hit = i; }
        }

        if (best == Double.MAX_VALUE || best < 0) best = 0.3;
        return new double[]{cx + dx * best, cz + dz * best, hit};
    }

    private void ringOnBed(double lx, double lz, double radius, double y, double vx, double vy, double vz, Color color) {
        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        GL11.glLineWidth(1.4f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int a = 0; a < 360; a += 24) {
            double r = Math.toRadians(a);
            double ox = Math.cos(r) * radius, oz = Math.sin(r) * radius;
            GL11.glVertex3d(wx(lx + ox, lz + oz) - vx, y - vy, wz(lx + ox, lz + oz) - vz);
        }
        GL11.glEnd();
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (!placed || mc.theWorld == null) return;
        final RenderManager rm = mc.getRenderManager();
        final double vx = rm.viewerPosX, vy = rm.viewerPosY, vz = rm.viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();

        final double bedY   = topY;
        final double railY  = topY + RAIL_TOP;
        final double out    = CUSH + RAIL;
        final double botY   = railY - APRON_DROP;

        box(-out, -out, PL + out, PW + out, botY, railY, vx, vy, vz, C_APRON);

        frame(-out, -out, PL + out, PW + out, -CUSH, -CUSH, PL + CUSH, PW + CUSH, railY, vx, vy, vz, C_RAIL);

        quad(-CUSH, -CUSH, PL + CUSH, PW + CUSH, bedY, vx, vy, vz, shade(C_CUSHION, 0.9f));
        quad(0, 0, PL, PW, bedY + 0.001, vx, vy, vz, C_BED);

        cushions(bedY, railY, vx, vy, vz);

        GlStateManager.color(1f, 1f, 1f, 0.55f);
        GL11.glBegin(GL11.GL_QUADS);
        v(PL * 0.25 - 0.008, 0, bedY + 0.001, vx, vy, vz);
        v(PL * 0.25 + 0.008, 0, bedY + 0.001, vx, vy, vz);
        v(PL * 0.25 + 0.008, PW, bedY + 0.001, vx, vy, vz);
        v(PL * 0.25 - 0.008, PW, bedY + 0.001, vx, vy, vz);
        GL11.glEnd();

        for (double[] p : POCKETS) {
            flatRing(p[0], p[1], POCKET_VIS, POCKET_MOUTH, bedY + 0.002, vx, vy, vz, C_POCKET_RIM);
            holeDisc(p[0], p[1], POCKET_VIS, bedY + 0.003, vx, vy, vz);
        }

        drawSights(railY, vx, vy, vz);

        if (aimValid && balls[0].active && ballsStopped() && !ballInHand) {
            Ball cue = balls[0];
            double dx = aimX - cue.x, dz = aimZ - cue.z, d = Math.sqrt(dx * dx + dz * dz);
            if (d > 0.02) {
                double dirx = dx / d, dirz = dz / d;
                double[] pr = predict(cue.x, cue.z, dirx, dirz);
                double ex = pr[0], ez = pr[1];
                int hit = (int) pr[2];
                double y = topY + R;

                GlStateManager.color(1f, 1f, 1f, 0.75f);
                GL11.glLineWidth(1.6f);
                GL11.glBegin(GL11.GL_LINES);
                GL11.glVertex3d(wx(cue.x, cue.z) - vx, y - vy, wz(cue.x, cue.z) - vz);
                GL11.glVertex3d(wx(ex, ez) - vx, y - vy, wz(ex, ez) - vz);
                GL11.glEnd();

                ringOnBed(ex, ez, R, y, vx, vy, vz, new Color(255, 255, 255, 150));

                if (hit >= 0) {
                    Ball c = balls[hit];
                    double ox = c.x - ex, oz = c.z - ez, od = Math.sqrt(ox * ox + oz * oz);
                    if (od > 1e-4) {
                        double tx = c.x + ox / od * 0.5, tz = c.z + oz / od * 0.5;
                        GlStateManager.color(1f, 0.85f, 0.2f, 0.85f);
                        GL11.glBegin(GL11.GL_LINES);
                        GL11.glVertex3d(wx(c.x, c.z) - vx, y - vy, wz(c.x, c.z) - vz);
                        GL11.glVertex3d(wx(tx, tz) - vx, y - vy, wz(tx, tz) - vz);
                        GL11.glEnd();
                    }
                }
            }
        }

        final float pt = mc.timer.renderPartialTicks;
        for (Ball b : balls) {
            if (!b.active) continue;
            double ix = b.prevX + (b.x - b.prevX) * pt;
            double iz = b.prevZ + (b.z - b.prevZ) * pt;
            GlStateManager.pushMatrix();
            GlStateManager.translate(wx(ix, iz) - vx, topY + R - vy, wz(ix, iz) - vz);
            drawBall(b);
            GlStateManager.popMatrix();
        }

        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    private void quad(double x1, double z1, double x2, double z2, double y, double vx, double vy, double vz, Color c) {
        GlStateManager.color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        v(x1, z1, y, vx, vy, vz);
        v(x2, z1, y, vx, vy, vz);
        v(x2, z2, y, vx, vy, vz);
        v(x1, z2, y, vx, vy, vz);
        GL11.glEnd();
    }

    private void box(double x1, double z1, double x2, double z2, double yBot, double yTop,
                     double vx, double vy, double vz, Color c) {
        Color near = shade(c, 1.06f), far = shade(c, 0.74f), side = shade(c, 0.88f), floor = shade(c, 0.55f);

        GlStateManager.color(near.getRed() / 255f, near.getGreen() / 255f, near.getBlue() / 255f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        v(x1, z1, yTop, vx, vy, vz); v(x2, z1, yTop, vx, vy, vz); v(x2, z1, yBot, vx, vy, vz); v(x1, z1, yBot, vx, vy, vz);
        GL11.glEnd();

        GlStateManager.color(far.getRed() / 255f, far.getGreen() / 255f, far.getBlue() / 255f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        v(x1, z2, yTop, vx, vy, vz); v(x2, z2, yTop, vx, vy, vz); v(x2, z2, yBot, vx, vy, vz); v(x1, z2, yBot, vx, vy, vz);
        GL11.glEnd();

        GlStateManager.color(side.getRed() / 255f, side.getGreen() / 255f, side.getBlue() / 255f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        v(x1, z1, yTop, vx, vy, vz); v(x1, z2, yTop, vx, vy, vz); v(x1, z2, yBot, vx, vy, vz); v(x1, z1, yBot, vx, vy, vz);
        v(x2, z1, yTop, vx, vy, vz); v(x2, z2, yTop, vx, vy, vz); v(x2, z2, yBot, vx, vy, vz); v(x2, z1, yBot, vx, vy, vz);
        GL11.glEnd();

        quad(x1, z1, x2, z2, yBot, vx, vy, vz, floor);
    }

    private static Color shade(Color c, float f) {
        return new Color(
                Math.min(255, (int) (c.getRed() * f)),
                Math.min(255, (int) (c.getGreen() * f)),
                Math.min(255, (int) (c.getBlue() * f)));
    }

    private void frame(double ox1, double oz1, double ox2, double oz2,
                       double ix1, double iz1, double ix2, double iz2,
                       double y, double vx, double vy, double vz, Color c) {
        GlStateManager.color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1f);
        GL11.glBegin(GL11.GL_QUADS);

        v(ox1, oz1, y, vx, vy, vz); v(ox2, oz1, y, vx, vy, vz); v(ox2, iz1, y, vx, vy, vz); v(ox1, iz1, y, vx, vy, vz);
        v(ox1, iz2, y, vx, vy, vz); v(ox2, iz2, y, vx, vy, vz); v(ox2, oz2, y, vx, vy, vz); v(ox1, oz2, y, vx, vy, vz);
        v(ox1, iz1, y, vx, vy, vz); v(ix1, iz1, y, vx, vy, vz); v(ix1, iz2, y, vx, vy, vz); v(ox1, iz2, y, vx, vy, vz);
        v(ix2, iz1, y, vx, vy, vz); v(ox2, iz1, y, vx, vy, vz); v(ox2, iz2, y, vx, vy, vz); v(ix2, iz2, y, vx, vy, vz);
        GL11.glEnd();
    }

    private void cushions(double yBed, double yTop, double vx, double vy, double vz) {
        final double gap = POCKET_MOUTH * 1.05;

        GlStateManager.color(C_CUSHION.getRed() / 255f, C_CUSHION.getGreen() / 255f, C_CUSHION.getBlue() / 255f, 1f);
        GL11.glBegin(GL11.GL_QUADS);

        double[][] longRuns = {{gap, PL / 2 - gap}, {PL / 2 + gap, PL - gap}};
        for (double[] run : longRuns) {
            if (run[1] <= run[0]) continue;

            v(run[0], 0, yBed, vx, vy, vz); v(run[1], 0, yBed, vx, vy, vz);
            v(run[1], -CUSH, yTop, vx, vy, vz); v(run[0], -CUSH, yTop, vx, vy, vz);

            v(run[0], PW, yBed, vx, vy, vz); v(run[1], PW, yBed, vx, vy, vz);
            v(run[1], PW + CUSH, yTop, vx, vy, vz); v(run[0], PW + CUSH, yTop, vx, vy, vz);
        }

        if (PW - gap > gap) {
            v(0, gap, yBed, vx, vy, vz); v(0, PW - gap, yBed, vx, vy, vz);
            v(-CUSH, PW - gap, yTop, vx, vy, vz); v(-CUSH, gap, yTop, vx, vy, vz);

            v(PL, gap, yBed, vx, vy, vz); v(PL, PW - gap, yBed, vx, vy, vz);
            v(PL + CUSH, PW - gap, yTop, vx, vy, vz); v(PL + CUSH, gap, yTop, vx, vy, vz);
        }
        GL11.glEnd();

        GlStateManager.color(shade(C_CUSHION, 0.8f).getRed() / 255f, shade(C_CUSHION, 0.8f).getGreen() / 255f,
                shade(C_CUSHION, 0.8f).getBlue() / 255f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        for (double[] run : longRuns) {
            if (run[1] <= run[0]) continue;
            for (double x : run) {
                v(x, 0, yBed, vx, vy, vz); v(x, -CUSH, yTop, vx, vy, vz);
                v(x, -CUSH, yBed, vx, vy, vz); v(x, 0, yBed, vx, vy, vz);
                v(x, PW, yBed, vx, vy, vz); v(x, PW + CUSH, yTop, vx, vy, vz);
                v(x, PW + CUSH, yBed, vx, vy, vz); v(x, PW, yBed, vx, vy, vz);
            }
        }
        GL11.glEnd();
    }

    private void flatRing(double lx, double lz, double rIn, double rOut, double y,
                          double vx, double vy, double vz, Color c) {
        Color inner = shade(c, 0.72f);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int a = 0; a <= 360; a += 15) {
            double r = Math.toRadians(a);
            double cs = Math.cos(r), sn = Math.sin(r);
            GlStateManager.color(inner.getRed() / 255f, inner.getGreen() / 255f, inner.getBlue() / 255f, 1f);
            GL11.glVertex3d(wx(lx + cs * rIn, lz + sn * rIn) - vx, y - vy, wz(lx + cs * rIn, lz + sn * rIn) - vz);
            GlStateManager.color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1f);
            GL11.glVertex3d(wx(lx + cs * rOut, lz + sn * rOut) - vx, y - vy, wz(lx + cs * rOut, lz + sn * rOut) - vz);
        }
        GL11.glEnd();
    }

    private void holeDisc(double lx, double lz, double radius, double y, double vx, double vy, double vz) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GlStateManager.color(0.02f, 0.02f, 0.02f, 1f);
        GL11.glVertex3d(wx(lx, lz) - vx, y - vy, wz(lx, lz) - vz);
        GlStateManager.color(0.17f, 0.16f, 0.15f, 1f);
        for (int a = 0; a <= 360; a += 15) {
            double r = Math.toRadians(a);
            double ox = Math.cos(r) * radius, oz = Math.sin(r) * radius;
            GL11.glVertex3d(wx(lx + ox, lz + oz) - vx, y - vy, wz(lx + ox, lz + oz) - vz);
        }
        GL11.glEnd();
    }

    private void drawSights(double y, double vx, double vy, double vz) {
        double mid = CUSH + RAIL / 2.0;
        double s = 0.022;
        for (double f : new double[]{1 / 8.0, 2 / 8.0, 3 / 8.0, 5 / 8.0, 6 / 8.0, 7 / 8.0}) {
            marker(PL * f, -mid, s, y, vx, vy, vz);
            marker(PL * f, PW + mid, s, y, vx, vy, vz);
        }
        for (double f : new double[]{1 / 4.0, 2 / 4.0, 3 / 4.0}) {
            marker(-mid, PW * f, s, y, vx, vy, vz);
            marker(PL + mid, PW * f, s, y, vx, vy, vz);
        }
    }

    private void marker(double lx, double lz, double s, double y, double vx, double vy, double vz) {
        GlStateManager.color(C_SIGHT.getRed() / 255f, C_SIGHT.getGreen() / 255f, C_SIGHT.getBlue() / 255f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        v(lx - s, lz - s, y + 0.001, vx, vy, vz);
        v(lx + s, lz - s, y + 0.001, vx, vy, vz);
        v(lx + s, lz + s, y + 0.001, vx, vy, vz);
        v(lx - s, lz + s, y + 0.001, vx, vy, vz);
        GL11.glEnd();
    }

    private void v(double lx, double lz, double y, double vx, double vy, double vz) {
        GL11.glVertex3d(wx(lx, lz) - vx, y - vy, wz(lx, lz) - vz);
    }

    private static final int BALL_SLICES = 30, BALL_STACKS = 22;

    private static final double NUMBER_CAP = 0.86;

    private static final double STRIPE_BAND = 0.45;

    private void drawBall(Ball b) {
        drawSphere(R, BALL_SLICES, BALL_STACKS, b);
        if (b.number != 0) drawBallNumber(b);
    }

    private Color ballColorAt(Ball b, double sinLat) {
        if (b.number == 0) return Color.WHITE;
        if (sinLat > NUMBER_CAP) return Color.WHITE;
        if (b.striped && Math.abs(sinLat) > STRIPE_BAND) return Color.WHITE;
        return b.color;
    }

    private void drawSphere(double r, int slices, int stacks, Ball b) {
        final double lxD = 0.35, lyD = 0.85, lzD = 0.4;
        for (int i = 0; i < stacks; i++) {
            double lat0 = Math.PI * (-0.5 + (double) i / stacks);
            double lat1 = Math.PI * (-0.5 + (double) (i + 1) / stacks);
            double y0 = Math.sin(lat0), r0 = Math.cos(lat0);
            double y1 = Math.sin(lat1), r1 = Math.cos(lat1);

            Color base = ballColorAt(b, Math.sin((lat0 + lat1) / 2));
            float br = base.getRed() / 255f, bg = base.getGreen() / 255f, bb = base.getBlue() / 255f;

            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int j = 0; j <= slices; j++) {
                double lng = 2 * Math.PI * (double) j / slices;
                double cx = Math.cos(lng), sz = Math.sin(lng);
                sphereVtx(cx * r1, y1, sz * r1, r, lxD, lyD, lzD, br, bg, bb);
                sphereVtx(cx * r0, y0, sz * r0, r, lxD, lyD, lzD, br, bg, bb);
            }
            GL11.glEnd();
        }
    }

    private void drawBallNumber(Ball b) {
        CustomFontRenderer fr = FontUtils.getFont("sf", 18);
        float s = (float) (R * 0.062);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, R * 1.012, 0);
        GlStateManager.rotate(90f, 1f, 0f, 0f);
        GlStateManager.scale(s, s, s);
        GlStateManager.enableTexture2D();
        fr.drawCenteredString(String.valueOf(b.number), 0, -fr.getHeight() / 2f, 0x111111);
        GlStateManager.disableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    private void sphereVtx(double nx, double ny, double nz, double r, double lx, double ly, double lz, float br, float bg, float bb) {
        float shade = (float) (0.5 + 0.5 * Math.max(0, nx * lx + ny * ly + nz * lz));
        GlStateManager.color(br * shade, bg * shade, bb * shade, 1f);
        GL11.glVertex3d(nx * r, ny * r, nz * r);
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (!placed || mc.thePlayer == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int accent = ColorManager.getColor().getRGB();

        FontUtils.getFont("sf", 18).drawStringWithShadow("8-Ball Potted: " + pots + "  Shots: " + shots, 4, 4, accent);

        String status = ballInHand ? "§eBall in hand — look at the table & right-click to place"
                : (System.currentTimeMillis() < messageUntil ? message
                : "§7Look to aim, hold §fright-click§7 to charge, release to shoot");
        int sw = FontUtils.getFont("sf", 18).getStringWidth(status.replaceAll("§.", ""));
        FontUtils.getFont("sf", 18).drawStringWithShadow(status, sr.getScaledWidth() / 2 - sw / 2, 16, -1);

        if (charging && power > 0) {
            int barW = 90, x = sr.getScaledWidth() / 2 - barW / 2, y = sr.getScaledHeight() / 2 + 14;
            int fill = (int) (barW * (power / MAXV));
            net.minecraft.client.gui.Gui.drawRect(x - 1, y - 1, x + barW + 1, y + 6, new Color(0, 0, 0, 160).getRGB());
            net.minecraft.client.gui.Gui.drawRect(x, y, x + fill, y + 5, accent);
            String pct = (int) (power / MAXV * 100) + "%";
            FontUtils.getFont("sf", 18).drawStringWithShadow(pct, sr.getScaledWidth() / 2 - FontUtils.getFont("sf", 18).getStringWidth(pct) / 2, y - 10, -1);
        }
    }
}
