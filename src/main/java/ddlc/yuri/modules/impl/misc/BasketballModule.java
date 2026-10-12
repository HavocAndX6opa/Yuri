package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.FontUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(label = "Basketball", description = "Basketball from Raven B4 ported to Yuri", category = ModuleCategory.MISC)
public final class BasketballModule extends Module {

    private final NumberProperty power = new NumberProperty("Power", 1.0, 0.4, 2.0, 0.05);
    private final NumberProperty distance = new NumberProperty("Hoop Distance", 6, 3, 12, 1);

    private static final double GRAVITY = 0.045;
    private static final double RING_RADIUS = 0.5;
    private static final int MAX_BALLS = 12;
    private static final double BOARD_HALF_W = 0.9;
    private static final double BOARD_HALF_H = 0.6;
    private static final double BALL_RADIUS = 0.13;

    private boolean hoopPlaced;
    private double hoopX, hoopY, hoopZ;
    private double boardX, boardY, boardZ;
    private double groundY;
    private double dirX, dirZ;
    private double rightX, rightZ;

    private final List<Ball> balls = new ArrayList<>();
    private int score;
    private int streak;
    private boolean wasClicking;

    private static final class Ball {
        double x, y, z, px, py, pz, mx, my, mz;
        int ticks;
        boolean scored;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        balls.clear();
        score = 0;
        streak = 0;
        wasClicking = true;
        placeHoop();
    }

    @Override
    public void onDisable() {
        balls.clear();
        hoopPlaced = false;
        super.onDisable();
    }

    private void placeHoop() {
        if (mc.thePlayer == null) {
            hoopPlaced = false;
            return;
        }
        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double dist = distance.getValue();

        hoopX = mc.thePlayer.posX + fx * dist;
        hoopZ = mc.thePlayer.posZ + fz * dist;
        groundY = Math.floor(mc.thePlayer.posY);
        hoopY = groundY + 3.05;

        dirX = -fx;
        dirZ = -fz;
        rightX = -dirZ;
        rightZ = dirX;

        boardX = hoopX - dirX * 0.28;
        boardZ = hoopZ - dirZ * 0.28;
        boardY = hoopY + 0.5;
        hoopPlaced = true;
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || !hoopPlaced) return;

        boolean clicking = mc.currentScreen == null && mc.gameSettings.keyBindAttack.isKeyDown();
        if (clicking && !wasClicking) {
            throwBall();
        }

        wasClicking = clicking;

        for (
                int i = balls.size() - 1;
                i >= 0; i--) {
            Ball b = balls.get(i);
            b.px = b.x;
            b.py = b.y;
            b.pz = b.z;
            b.x += b.mx;
            b.y += b.my;
            b.z += b.mz;
            b.my -= GRAVITY;
            b.mx *= 0.995;
            b.mz *= 0.995;

            double dPrev = (b.px - boardX) * dirX + (b.pz - boardZ) * dirZ;
            double dCur = (b.x - boardX) * dirX + (b.z - boardZ) * dirZ;
            double vDotN = b.mx * dirX + b.mz * dirZ;
            if (vDotN < 0 && dPrev > BALL_RADIUS && dCur <= BALL_RADIUS) {
                double relRight = (b.x - boardX) * rightX + (b.z - boardZ) * rightZ;
                double relUp = b.y - boardY;
                if (Math.abs(relRight) <= BOARD_HALF_W && Math.abs(relUp) <= BOARD_HALF_H) {
                    b.mx -= 2 * vDotN * dirX;
                    b.mz -= 2 * vDotN * dirZ;
                    b.mx *= 0.55;
                    b.mz *= 0.55;
                    b.my *= 0.75;
                    double push = BALL_RADIUS - dCur;
                    b.x += dirX * push;
                    b.z += dirZ * push;
                }
            }

            if (!b.scored && b.py > hoopY && b.y <= hoopY && b.my < 0) {
                double dx = b.x - hoopX, dz = b.z - hoopZ;
                if (dx * dx + dz * dz < RING_RADIUS * RING_RADIUS) {
                    b.scored = true;
                    score++;
                    streak++;
                    mc.thePlayer.playSound("random.orb", 0.7f, 1.4f + Math.min(streak, 8) * 0.05f);
                }
            }

            if (b.ticks++ > 140 || b.y < groundY - 2) {
                if (!b.scored) streak = 0;
                balls.remove(i);
            }
        }
    }

    private void throwBall() {
        if (balls.size() >= MAX_BALLS) balls.remove(0);

        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        double pitch = Math.toRadians(mc.thePlayer.rotationPitch);
        double lx = -Math.sin(yaw) * Math.cos(pitch);
        double ly = -Math.sin(pitch);
        double lz = Math.cos(yaw) * Math.cos(pitch);

        double sp = power.getValue();
        Ball b = new Ball();
        b.x = mc.thePlayer.posX + lx * 0.4;
        b.y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() + ly * 0.4;
        b.z = mc.thePlayer.posZ + lz * 0.4;
        b.mx = lx * sp;
        b.my = ly * sp + 0.12;
        b.mz = lz * sp;
        balls.add(b);

        mc.thePlayer.playSound("random.bow", 0.5f, 1.6f);
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (!hoopPlaced || mc.theWorld == null) return;

        final RenderManager rm = mc.getRenderManager();
        final double vx = rm.viewerPosX, vy = rm.viewerPosY, vz = rm.viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        renderHoop(vx, vy, vz);
        for (Ball b : new ArrayList<>(balls)) {
            renderBall(b, vx, vy, vz);
        }

        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    private void renderHoop(double vx, double vy, double vz) {
        final double cx = hoopX - vx, cy = hoopY - vy, cz = hoopZ - vz;


        final double bx = boardX - vx, by = boardY - vy, bz = boardZ - vz;

        GL11.glLineWidth(2f);
        GlStateManager.color(0.9f, 0.9f, 0.95f, 0.55f);
        GL11.glBegin(GL11.GL_QUADS);
        boardVertex(bx, by, bz, -BOARD_HALF_W, -BOARD_HALF_H, BOARD_HALF_W, BOARD_HALF_H, 0);
        GL11.glEnd();

        GlStateManager.color(1f, 0.5f, 0.0f, 0.95f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        boardVertex(bx, by, bz, -BOARD_HALF_W, -BOARD_HALF_H, BOARD_HALF_W, BOARD_HALF_H, 1);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINE_LOOP);
        boardVertex(bx, by - 0.15, bz, -0.28, -0.2, 0.28, 0.2, 1);
        GL11.glEnd();

        GlStateManager.color(0.55f, 0.55f, 0.58f, 1f);
        GL11.glLineWidth(4f);
        drawLine(bx, groundY - vy, bz, bx, by - BOARD_HALF_H, bz);

        GlStateManager.color(1f, 0.45f, 0.0f, 1f);
        GL11.glLineWidth(3f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int a = 0; a < 360; a += 12) {
            double r = Math.toRadians(a);
            GL11.glVertex3d(cx + Math.cos(r) * RING_RADIUS, cy, cz + Math.sin(r) * RING_RADIUS);
        }
        GL11.glEnd();

        GlStateManager.color(1f, 1f, 1f, 0.7f);
        GL11.glLineWidth(1f);
        GL11.glBegin(GL11.GL_LINES);
        for (int a = 0; a < 360; a += 30) {
            double r = Math.toRadians(a);
            double rx = cx + Math.cos(r) * RING_RADIUS;
            double rz = cz + Math.sin(r) * RING_RADIUS;
            GL11.glVertex3d(rx, cy, rz);
            GL11.glVertex3d(cx + Math.cos(r) * RING_RADIUS * 0.35, cy - 0.45, cz + Math.sin(r) * RING_RADIUS * 0.35);
        }
        GL11.glEnd();
    }

    private void boardVertex(double bx, double by, double bz, double l, double b, double rr, double t, int mode) {
        vtx(bx, by, bz, l, b);
        vtx(bx, by, bz, rr, b);
        vtx(bx, by, bz, rr, t);
        vtx(bx, by, bz, l, t);
    }

    private void vtx(double bx, double by, double bz, double w, double h) {
        GL11.glVertex3d(bx + rightX * w, by + h, bz + rightZ * w);
    }

    private void renderBall(Ball b, double vx, double vy, double vz) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(b.x - vx, b.y - vy, b.z - vz);

        drawSphere(BALL_RADIUS, 18, 12);

        GlStateManager.color(0.08f, 0.04f, 0.02f, 0.85f);
        GL11.glLineWidth(1.3f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int a = 0; a < 360; a += 20) {
            double r = Math.toRadians(a);
            GL11.glVertex3d(Math.cos(r) * BALL_RADIUS, 0, Math.sin(r) * BALL_RADIUS);
        }
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int a = 0; a < 360; a += 20) {
            double r = Math.toRadians(a);
            GL11.glVertex3d(Math.cos(r) * BALL_RADIUS, Math.sin(r) * BALL_RADIUS, 0);
        }
        GL11.glEnd();

        GlStateManager.popMatrix();
    }

    private void drawSphere(double r, int slices, int stacks) {
        final double lx = 0.35, ly = 0.85, lz = 0.4;
        for (int i = 0; i < stacks; i++) {
            double lat0 = Math.PI * (-0.5 + (double) i / stacks);
            double lat1 = Math.PI * (-0.5 + (double) (i + 1) / stacks);
            double y0 = Math.sin(lat0), rr0 = Math.cos(lat0);
            double y1 = Math.sin(lat1), rr1 = Math.cos(lat1);

            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int j = 0; j <= slices; j++) {
                double lng = 2 * Math.PI * (double) j / slices;
                double cxx = Math.cos(lng), szz = Math.sin(lng);
                sphereVertex(cxx * rr1, y1, szz * rr1, r, lx, ly, lz);
                sphereVertex(cxx * rr0, y0, szz * rr0, r, lx, ly, lz);
            }
            GL11.glEnd();
        }
    }

    private void sphereVertex(double nx, double ny, double nz, double r, double lx, double ly, double lz) {
        double d = nx * lx + ny * ly + nz * lz;
        float shade = (float) (0.5 + 0.5 * Math.max(0, d));
        GlStateManager.color(0.9f * shade, 0.42f * shade, 0.13f * shade, 1f);
        GL11.glVertex3d(nx * r, ny * r, nz * r);
    }

    private void drawLine(double x1, double y1, double z1, double x2, double y2, double z2) {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glEnd();
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        String text = "Basketball | Score: " + score + " | Streak: " + streak;
        float w = FontUtils.getFont("sf", 18).getStringWidth(text);
        float x = sr.getScaledWidth() / 2f - w / 2f;
        float y = 6;
        FontUtils.getFont("sf", 18).drawStringWithShadow(text, x, y, -1);
    }
}
