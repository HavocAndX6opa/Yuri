package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.misc.AntiBotModule;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

@ModuleInfo(label = "Bow Aimbot", description = "Aims your bow at the nearest enemy while drawing", category = ModuleCategory.COMBAT)
public final class BowAimbotModule extends Module {

    private static final int MAX_TICKS = 120;
    private static final double UNREACHED = -1.0E9;

    private final NumberProperty range = new NumberProperty("Range", 40.0, 5.0, 100.0, 1.0);
    private final Property<Boolean> throughWalls = new Property<>("Through walls", true);
    private final Property<Boolean> predict = new Property<>("Predict", true);

    private EntityPlayer target;

    @Override
    public void onEnable() {
        target = null;
    }

    @Override
    public void onDisable() {
        target = null;
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            target = null;
            return;
        }

        if (target != null && !isValid(target)) {
            target = null;
        }

        double speed = arrowSpeed();
        if (speed <= 0.0) {
            return;
        }

        if (target == null) {
            target = acquire(speed);
        }

        if (target == null) {
            return;
        }

        double[] angles = solve(target, speed);
        if (angles == null) {
            return;
        }

        applyRotation((float) angles[0], (float) angles[1]);
    }

    private boolean isValid(EntityPlayer player) {
        if (player == null || player == mc.thePlayer || player.isDead || player.getHealth() <= 0.0f || player.isInvisible()) {
            return false;
        }

        if (!mc.theWorld.playerEntities.contains(player)) {
            return false;
        }

        if (Yuri.INSTANCE.getModuleManager().getModule(AntiBotModule.class).isBot(player)) {
            return false;
        }

        return mc.thePlayer.getDistanceToEntity(player) <= range.getValue();
    }

    private EntityPlayer acquire(double speed) {
        EntityPlayer best = null;
        double bestDistance = Double.MAX_VALUE;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isValid(player)) {
                continue;
            }

            double distance = mc.thePlayer.getDistanceToEntity(player);
            if (distance >= bestDistance) {
                continue;
            }

            if (solve(player, speed) == null) {
                continue;
            }

            best = player;
            bestDistance = distance;
        }

        return best;
    }

    private void applyRotation(float yaw, float pitch) {
        mc.thePlayer.rotationYaw = yaw;
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(pitch, -90.0f, 90.0f);
    }

    private double arrowSpeed() {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBow) || !mc.thePlayer.isUsingItem()) {
            return 0.0;
        }

        float charge = mc.thePlayer.getItemInUseDuration() / 20.0f;
        charge = (charge * charge + charge * 2.0f) / 3.0f;

        if (charge < 0.1f) {
            return 0.0;
        }
        if (charge > 1.0f) {
            charge = 1.0f;
        }

        return charge * 3.0;
    }

    private double[] solve(EntityLivingBase entity, double speed) {
        double[] result = solveFor(center(entity, 0.0), speed);

        if (result == null) {
            return null;
        }

        if (!predict.getValue()) {
            return result;
        }

        for (int i = 0; i < 3; i++) {
            double[] next = solveFor(center(entity, result[2]), speed);
            if (next == null) {
                break;
            }
            result = next;
        }

        return result;
    }

    private Vec3 center(EntityLivingBase entity, double ticks) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double dx = (entity.posX - entity.lastTickPosX) * ticks;
        double dz = (entity.posZ - entity.lastTickPosZ) * ticks;

        return new Vec3(
                (box.minX + box.maxX) / 2.0 + dx,
                (box.minY + box.maxY) / 2.0,
                (box.minZ + box.maxZ) / 2.0 + dz
        );
    }

    private double[] solveFor(Vec3 goal, double speed) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);

        double yaw = Math.toDegrees(Math.atan2(goal.zCoord - eyes.zCoord, goal.xCoord - eyes.xCoord)) - 90.0;
        double startX = eyes.xCoord;
        double startZ = eyes.zCoord;

        for (int i = 0; i < 3; i++) {
            double yawRad = Math.toRadians(yaw);
            startX = eyes.xCoord - Math.cos(yawRad) * 0.16;
            startZ = eyes.zCoord - Math.sin(yawRad) * 0.16;
            yaw = Math.toDegrees(Math.atan2(goal.zCoord - startZ, goal.xCoord - startX)) - 90.0;
        }

        double yawRad = Math.toRadians(yaw);
        startX = eyes.xCoord - Math.cos(yawRad) * 0.16;
        startZ = eyes.zCoord - Math.sin(yawRad) * 0.16;

        double startY = eyes.yCoord - 0.1;
        double dirX = -Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);
        double hDist = (goal.xCoord - startX) * dirX + (goal.zCoord - startZ) * dirZ;

        if (hDist < 0.5) {
            return null;
        }

        double prevPitch = 90.0;
        double prevError = trajectory(prevPitch, speed, hDist, startY, goal.yCoord)[0];

        for (double pitch = 89.0; pitch >= -90.0; pitch -= 1.0) {
            double[] current = trajectory(pitch, speed, hDist, startY, goal.yCoord);

            if (prevError < 0.0 && current[0] >= 0.0) {
                double low = pitch;
                double high = prevPitch;
                double ticks = current[1];

                for (int i = 0; i < 24; i++) {
                    double mid = (low + high) / 2.0;
                    double[] sample = trajectory(mid, speed, hDist, startY, goal.yCoord);
                    if (sample[0] >= 0.0) {
                        low = mid;
                        ticks = sample[1];
                    } else {
                        high = mid;
                    }
                }

                if (!throughWalls.getValue() && mc.theWorld.rayTraceBlocks(eyes, goal, false, true, false) != null) {
                    return null;
                }

                return new double[]{yaw, low, ticks};
            }

            prevPitch = pitch;
            prevError = current[0];
        }

        return null;
    }

    private double[] trajectory(double pitch, double speed, double hDist, double startY, double goalY) {
        double pitchRad = Math.toRadians(pitch);
        double hv = Math.cos(pitchRad) * speed;
        double vy = -Math.sin(pitchRad) * speed;
        double h = 0.0;
        double y = startY;

        for (int tick = 1; tick <= MAX_TICKS; tick++) {
            double prevH = h;
            double prevY = y;

            h += hv;
            y += vy;

            hv *= 0.99;
            vy *= 0.99;
            vy -= 0.05;

            if (h >= hDist) {
                double t = (hDist - prevH) / (h - prevH);
                double yAt = prevY + (y - prevY) * t;
                return new double[]{yAt - goalY, tick - 1 + t};
            }

            if (y < goalY - 40.0 && vy < 0.0) {
                break;
            }
        }

        return new double[]{UNREACHED, 0.0};
    }
}