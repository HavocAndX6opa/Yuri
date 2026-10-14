package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.managers.impl.TargetManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.misc.AntiBotModule;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.player.FriendUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.*;

@ModuleInfo(label = "Bow Aimbot", category = ModuleCategory.COMBAT, description = "Automatically aims your bow at targets using advanced trajectory prediction")
public final class BowAimbotModule extends Module {

    private static final int MAX_FLIGHT_TICKS = 400;
    private static final int MAX_PREDICT_TICKS = 50;
    private static final int HISTORY_SIZE = 5;
    private static final int MAX_CANDIDATES = 4;
    private static final double ARROW_SPEED = 3.0;
    private static final double ARROW_DRAG = 0.99;
    private static final double ARROW_GRAVITY = 0.05;

    private final MultiModeProperty<TargetManager.Targets> targets = new MultiModeProperty<>("Targets", TargetManager.Targets.PLAYERS, TargetManager.Targets.HOSTILES, TargetManager.Targets.TEAMMATES);
    private final ModeProperty<TargetMode> mode = new ModeProperty<>("Mode", TargetMode.DISTANCE);
    private final NumberProperty range = new NumberProperty("Range", 80, 0, 256, 1);
    private final NumberProperty fov = new NumberProperty("FOV", 360, 30, 360, 5);
    private final NumberProperty prediction = new NumberProperty("Prediction", 1.0, 0.0, 2.0, 0.1);
    private final Property<Boolean> highArc = new Property<>("High Arc", true);
    private final Property<Boolean> lockView = new Property<Boolean>("Lock View", true);
    private final NumberProperty minRotSpeed = new NumberProperty("Min Rotation Speed", 3, 0, 10, 0.5f, () -> !lockView.getValue());
    private final NumberProperty maxRotSpeed = new NumberProperty("Max Rotation Speed", 7, 0, 10, 0.5f, () -> !lockView.getValue());
    private final Property<Boolean> throughWalls = new Property<>("Through Walls", false);

    private final ArrayList<EntityLivingBase> attackList = new ArrayList<>();
    private final Map<Integer, LinkedList<double[]>> history = new HashMap<>();
    public static EntityLivingBase target;
    public float velocity;
    private float[] solved;

    public enum TargetMode {
        HEALTH("Health"),
        DISTANCE("Distance"),
        ANGLE("Angle");

        private final String name;

        TargetMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class Aim {
        float yaw;
        float pitch;
        double ticks;
        double sx;
        double sy;
        double sz;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearState();
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        ScaffoldModule scaffold = Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class);
        if (mc.thePlayer == null || mc.theWorld == null || (scaffold != null && scaffold.isEnabled())) {
            clearState();
            return;
        }

        collectTargets();

        ItemStack held = mc.thePlayer.getHeldItem();
        if (attackList.isEmpty() || held == null || !(held.getItem() instanceof ItemBow) || !mc.thePlayer.isUsingItem() || mc.thePlayer.getItemInUseDuration() <= 1) {
            target = null;
            solved = null;
            return;
        }

        velocity = charge();
        target = null;
        solved = null;
        if (velocity < 0.1f) return;

        double speed = velocity * ARROW_SPEED;
        sortTargets();

        float halfFov = fov.getValue().floatValue() / 2.0f;
        int checked = 0;
        for (EntityLivingBase candidate : attackList) {
            if (yawDiff(candidate) > halfFov) continue;
            if (checked++ >= MAX_CANDIDATES) break;
            Aim aim = solve(candidate, speed);
            if (aim != null) {
                target = candidate;
                solved = new float[]{aim.yaw, aim.pitch};
                break;
            }
        }

        if (target == null) return;

        setSuffix(target.getName());
        applyRotations();
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (target == null || solved == null || !lockView.getValue() || mc.thePlayer == null) return;

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBow) || !mc.thePlayer.isUsingItem()) return;

        mc.thePlayer.rotationYaw = solved[0];
        mc.thePlayer.rotationPitch = solved[1];
    }

    public boolean isValidTarget(Entity entity) {
        if (!(entity instanceof EntityLivingBase) || entity instanceof EntityArmorStand) {
            return false;
        }

        if (entity == mc.thePlayer || entity == mc.thePlayer.ridingEntity || !entity.isEntityAlive()) {
            return false;
        }

        if (((EntityLivingBase) entity).getHealth() <= 0.0f) {
            return false;
        }

        if (mc.thePlayer.getDistanceToEntity(entity) > range.getValue()) {
            return false;
        }

        if (entity instanceof EntityPlayer && !targets.getValue().contains(TargetManager.Targets.TEAMMATES)) {
            if (TargetManager.inTeam(mc.thePlayer, (EntityPlayer) entity)) {
                return false;
            }
        }

        AntiBotModule antiBot = Yuri.INSTANCE.getModuleManager().getModule(AntiBotModule.class);

        for (TargetManager.Targets t : targets.getValue()) {
            switch (t) {
                case PLAYERS:
                    if (entity instanceof EntityPlayer) {
                        boolean isFriend = FriendUtils.isFriend(entity.getName());
                        boolean isBot = antiBot != null && antiBot.isBot((EntityPlayer) entity);
                        if (!isFriend && !isBot) return true;
                    }
                    break;
                case HOSTILES:
                    if (entity instanceof IMob) {
                        return true;
                    }
                    break;
                case ANIMALS:
                    if (entity instanceof IAnimals && !(entity instanceof IMob)) {
                        return true;
                    }
                    break;
                case TEAMMATES:
                    if (entity instanceof EntityPlayer && TargetManager.inTeam(mc.thePlayer, (EntityPlayer) entity)) {
                        return true;
                    }
                    break;
                case INVISIBLES:
                    if (entity.isInvisible()) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }

        return false;
    }

    public void sortTargets() {
        switch (mode.getValue()) {
            case DISTANCE:
                attackList.sort(Comparator.comparingDouble(ent -> mc.thePlayer.getDistanceToEntity(ent)));
                break;
            case HEALTH:
                attackList.sort((a, b) -> Float.compare(a.getHealth(), b.getHealth()));
                break;
            case ANGLE:
                attackList.sort(Comparator.comparingDouble(ent -> yawDiff(ent)));
                break;
        }
    }

    private void clearState() {
        attackList.clear();
        history.clear();
        target = null;
        solved = null;
    }

    private void collectTargets() {
        attackList.clear();
        Set<Integer> seen = new HashSet<>();
        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof Entity)) continue;
            Entity entity = (Entity) obj;
            if (!isValidTarget(entity)) continue;
            EntityLivingBase living = (EntityLivingBase) entity;
            attackList.add(living);
            seen.add(living.getEntityId());
            record(living);
        }
        history.keySet().retainAll(seen);
    }

    private void record(EntityLivingBase entity) {
        LinkedList<double[]> list = history.get(entity.getEntityId());
        if (list == null) {
            list = new LinkedList<>();
            history.put(entity.getEntityId(), list);
        }
        if (!list.isEmpty()) {
            double[] last = list.getLast();
            double dx = entity.posX - last[0];
            double dy = entity.posY - last[1];
            double dz = entity.posZ - last[2];
            if (dx * dx + dy * dy + dz * dz > 64.0) list.clear();
        }
        list.add(new double[]{entity.posX, entity.posY, entity.posZ});
        while (list.size() > HISTORY_SIZE) list.removeFirst();
    }

    private double[] velocityOf(EntityLivingBase entity) {
        LinkedList<double[]> list = history.get(entity.getEntityId());
        if (list == null || list.size() < 2) return new double[]{0.0, 0.0, 0.0};

        double sx = 0.0;
        double sz = 0.0;
        double w = 0.0;
        double lastDy = 0.0;
        double[] prev = null;
        int weight = 1;
        for (double[] p : list) {
            if (prev != null) {
                sx += (p[0] - prev[0]) * weight;
                sz += (p[2] - prev[2]) * weight;
                w += weight;
                lastDy = p[1] - prev[1];
                weight++;
            }
            prev = p;
        }
        return new double[]{sx / w, lastDy, sz / w};
    }

    private float charge() {
        float f = (float) (72000 - mc.thePlayer.getItemInUseCount()) / 20.0f;
        f = (f * f + f * 2.0f) / 3.0f;
        return f > 1.0f ? 1.0f : f;
    }

    private float yawDiff(Entity entity) {
        double dx = entity.posX - mc.thePlayer.posX;
        double dz = entity.posZ - mc.thePlayer.posZ;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        return Math.abs(MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw));
    }

    private void applyRotations() {
        if (solved == null) return;

        if (lockView.getValue()) {
            mc.thePlayer.rotationYaw = solved[0];
            mc.thePlayer.rotationPitch = solved[1];
            return;
        }

        float rotSpeed = (float) MathUtils.getRandom(minRotSpeed.getValue(), maxRotSpeed.getValue());
        RotationManager.setRotations(solved[0], solved[1], rotSpeed, RotationManager.MovementFix.NORMAL);
    }

    private Aim solve(EntityLivingBase entity, double speed) {
        double[] vel = velocityOf(entity);
        double ground = groundBelow(entity);

        Aim low = aim(entity, vel, ground, speed, false);
        if (low == null) return null;
        if (throughWalls.getValue() || clear(low, speed)) return low;
        if (!highArc.getValue()) return null;

        Aim high = aim(entity, vel, ground, speed, true);
        if (high != null && clear(high, speed)) return high;
        return null;
    }

    private double groundBelow(EntityLivingBase entity) {
        if (entity.onGround) return entity.posY;
        Vec3 from = new Vec3(entity.posX, entity.posY, entity.posZ);
        Vec3 to = new Vec3(entity.posX, entity.posY - 64.0, entity.posZ);
        MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(from, to, false, true, false);
        return hit != null ? hit.hitVec.yCoord : -1.0E9;
    }

    private double[] predict(EntityLivingBase entity, double[] vel, double ground, double ticks) {
        double t = Math.min(ticks, (double) MAX_PREDICT_TICKS) * prediction.getValue().doubleValue();
        double x = entity.posX + vel[0] * t;
        double z = entity.posZ + vel[2] * t;
        double y = entity.posY;

        if (!entity.onGround) {
            double vy = vel[1];
            int whole = (int) t;
            for (int i = 0; i < whole; i++) {
                y += vy;
                vy = (vy - 0.08) * 0.98;
                if (y <= ground) {
                    y = ground;
                    vy = 0.0;
                    break;
                }
            }
            y += vy * (t - whole);
            if (y < ground) y = ground;
        }

        return new double[]{x, y + (double) entity.height * 0.5, z};
    }

    private Aim aim(EntityLivingBase entity, double[] vel, double ground, double speed, boolean high) {
        double eyeX = mc.thePlayer.posX;
        double eyeY = mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight();
        double eyeZ = mc.thePlayer.posZ;

        double ticks = Math.max(1.0, (double) mc.thePlayer.getDistanceToEntity(entity) / speed);
        Aim result = null;

        for (int i = 0; i < 6; i++) {
            double[] p = predict(entity, vel, ground, ticks);

            double yawRad = Math.atan2(p[2] - eyeZ, p[0] - eyeX) - Math.PI / 2.0;
            double sx = eyeX;
            double sz = eyeZ;
            for (int j = 0; j < 2; j++) {
                sx = eyeX - Math.cos(yawRad) * 0.16;
                sz = eyeZ - Math.sin(yawRad) * 0.16;
                yawRad = Math.atan2(p[2] - sz, p[0] - sx) - Math.PI / 2.0;
            }
            double sy = eyeY - 0.1;

            double dx = p[0] - sx;
            double dz = p[2] - sz;
            double horiz = Math.sqrt(dx * dx + dz * dz);

            double[] sol = solvePitch(horiz, p[1] - sy, speed, high);
            if (sol == null) return null;

            float yaw = (float) Math.toDegrees(yawRad);
            Aim a = new Aim();
            a.yaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
            a.pitch = (float) -sol[0];
            a.ticks = sol[1];
            a.sx = sx;
            a.sy = sy;
            a.sz = sz;
            result = a;

            if (Math.abs(sol[1] - ticks) < 0.05) break;
            ticks = sol[1];
        }

        return result;
    }

    private double[] trajectory(double theta, double horiz, double speed) {
        double rad = Math.toRadians(theta);
        double mh = Math.cos(rad) * speed;
        double my = Math.sin(rad) * speed;
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
            mh *= ARROW_DRAG;
            my = my * ARROW_DRAG - ARROW_GRAVITY;
            if (mh < 0.005) return null;
        }
        return null;
    }

    private double[] solvePitch(double horiz, double dy, double speed, boolean high) {
        double prevErr = Double.NaN;
        double prevTheta = 0.0;

        for (double theta = -80.0; theta <= 85.0; theta += 1.5) {
            double[] r = trajectory(theta, horiz, speed);
            if (r == null) {
                prevErr = Double.NaN;
                continue;
            }
            double err = r[0] - dy;
            if (!Double.isNaN(prevErr)) {
                boolean crossing = high ? (prevErr >= 0.0 && err < 0.0) : (prevErr < 0.0 && err >= 0.0);
                if (crossing) return refine(prevTheta, theta, horiz, dy, speed, high);
            }
            prevErr = err;
            prevTheta = theta;
        }
        return null;
    }

    private double[] refine(double a, double b, double horiz, double dy, double speed, boolean high) {
        double lo = a;
        double hi = b;
        double bestTheta = b;
        double[] best = trajectory(b, horiz, speed);

        for (int i = 0; i < 14; i++) {
            double mid = (lo + hi) * 0.5;
            double[] r = trajectory(mid, horiz, speed);
            if (r == null) break;
            boolean below = r[0] - dy < 0.0;
            if (high ? !below : below) lo = mid;
            else hi = mid;
            bestTheta = mid;
            best = r;
        }

        if (best == null) return null;
        return new double[]{bestTheta, best[1]};
    }

    private boolean clear(Aim a, double speed) {
        double yawRad = Math.toRadians(a.yaw);
        double pitchRad = Math.toRadians(a.pitch);
        double mx = -Math.sin(yawRad) * Math.cos(pitchRad) * speed;
        double my = -Math.sin(pitchRad) * speed;
        double mz = Math.cos(yawRad) * Math.cos(pitchRad) * speed;

        double x = a.sx;
        double y = a.sy;
        double z = a.sz;
        int steps = (int) Math.ceil(a.ticks);

        for (int i = 0; i < steps; i++) {
            double f = (i == steps - 1) ? a.ticks - (double) i : 1.0;
            if (f <= 0.0) break;
            Vec3 from = new Vec3(x, y, z);
            Vec3 to = new Vec3(x + mx * f, y + my * f, z + mz * f);
            if (mc.theWorld.rayTraceBlocks(from, to, false, true, false) != null) return false;
            x = to.xCoord;
            y = to.yCoord;
            z = to.zCoord;
            mx *= ARROW_DRAG;
            my = my * ARROW_DRAG - ARROW_GRAVITY;
            mz *= ARROW_DRAG;
        }
        return true;
    }
}