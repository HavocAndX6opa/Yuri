package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.player.*;
import ddlc.yuri.utils.player.MoveUtils;
import ddlc.yuri.utils.player.RotationUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector2f;

import java.util.function.Function;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class RotationManager {
    @Setter
    @Getter
    private static boolean active, smoothed;
    public static Vector2f rotations, lastRotations = new Vector2f(0, 0), targetRotations, lastServerRotations;
    private static double rotationSpeed;
    private static MovementFix correctMovement;
    private static Function<Vector2f, Boolean> raycast;
    private static float randomAngle;
    private static final Vector2f offset = new Vector2f(0, 0);
    private static float lastYawDelta = 0;

    private static boolean releasing;
    private static float transitionProgress = 1f;
    // 1 = snap, 0.5 = half of that, 0.25 = quarter of that, etc. -lumie
    private static float transitionSpeed = 1.0f;
    private static Vector2f transitionStart;
    private static Vector2f releaseTarget;

    // added more call methods so it's easier to call. that's what I wanted with completely making my own rotation manager tbh, but I just pasted simp.

    // also quick note you can call any of these, and it sets rotations so it's up to you which is easier to call.

    // yours truly, lumie :3

    public static void setRotations(final float yaw, final float pitch, final double rotationSpeed, final MovementFix correctMovement, final Function<Vector2f, Boolean> raycast) {
        setRotations(new Vector2f(yaw, pitch), rotationSpeed, correctMovement, raycast);
    }

    public static void setRotations(final float[] rotations, final double rotationSpeed, final MovementFix correctMovement,  final Function<Vector2f, Boolean> raycast) {
        setRotations(new Vector2f(rotations[0], rotations[1]), rotationSpeed, correctMovement, raycast);
    }

    public static void setRotations(final float yaw, final float pitch, final double rotationSpeed, final MovementFix correctMovement) {
        setRotations(new Vector2f(yaw, pitch), rotationSpeed, correctMovement, null);
    }

    public static void setRotations(final float[] rotations, final double rotationSpeed, final MovementFix correctMovement) {
        setRotations(new Vector2f(rotations[0], rotations[1]), rotationSpeed, correctMovement, null);
    }

    public static void setRotations(final Vector2f rotations, final double rotationSpeed, final MovementFix correctMovement) {
        setRotations(rotations, rotationSpeed, correctMovement, null);
    }

    public static void setRotations(final Vector2f rotations, final double rotationSpeed, final MovementFix correctMovement, final Function<Vector2f, Boolean> raycast) {
        RotationManager.targetRotations = rotations;
        RotationManager.rotationSpeed = rotationSpeed * 36;
        RotationManager.correctMovement = correctMovement;
        RotationManager.raycast = raycast;

        if (!active) {
            lastRotations = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            lastYawDelta = 0;
            transitionStart = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            transitionProgress = 0f;
            releasing = false;
        }

        active = true;
        smooth();
    }

    public static void stopRotation() {
        active = false;
        releasing = false;
        rotations = null;
        targetRotations = null;
        raycast = null;
    }

    public static boolean isRotating() {
        return active;
    }

    public static float getRotationYaw() {
        return rotations != null ? rotations.x : mc.thePlayer.rotationYaw;
    }

    public static float getRotationPitch() {
        return rotations != null ? rotations.y : mc.thePlayer.rotationPitch;
    }

    @EventHook(value = EventPriority.HIGH)
    public void onPreUpdate(PreUpdateEvent event) {
        if (!active || rotations == null || lastRotations == null || targetRotations == null || lastServerRotations == null) {
            rotations = lastRotations = targetRotations = lastServerRotations = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }

        if (releasing) {
            release();
        } else if (active) {
            smooth();
        }
    }

    @EventHook(value = EventPriority.LOW)
    public void onMove(MoveEvent event) {
        if (active && correctMovement == MovementFix.NORMAL && rotations != null) {
            final float yaw = rotations.x;
            MoveUtils.fixMovement(event, yaw);
        }
    }

    @EventHook(value = EventPriority.VERY_LOW)
    public void onLook(LookEvent event) {
        if (active && rotations != null) {
            event.setRotation(rotations);
        }
    }

    @EventHook(value = EventPriority.VERY_LOW)
    public void onStrafe(StrafeEvent event) {
        if (active && (correctMovement == MovementFix.NORMAL || correctMovement == MovementFix.TRADITIONAL) && rotations != null) {
            event.setYaw(rotations.x);
        }
    }

    @EventHook(value = EventPriority.VERY_LOW)
    public void onJump(JumpEvent event) {
        if (active && (correctMovement == MovementFix.NORMAL || correctMovement == MovementFix.TRADITIONAL) && rotations != null) {
            event.setYaw(rotations.x);
        }
    }

    @EventHook(value = EventPriority.VERY_LOW)
    public void onMotion(MotionEvent event) {
        if(!event.isPre()) return;
        if (active && rotations != null) {
            final float yaw = rotations.x;
            final float pitch = rotations.y;

            event.setYaw(yaw);
            event.setPitch(pitch);

            mc.thePlayer.rotationYawHead = yaw;
            mc.thePlayer.renderPitchHead = pitch;

            lastServerRotations = new Vector2f(yaw, pitch);

            if (!releasing && Math.abs((rotations.x - mc.thePlayer.rotationYaw) % 360) < 1 && Math.abs((rotations.y - mc.thePlayer.rotationPitch)) < 1) {
                releasing = true;
                transitionProgress = 0f;
                transitionStart = rotations;
                releaseTarget = RotationUtils.resetRotation(RotationUtils.applySensitivityPatch(rotations, lastRotations));
            }

            lastRotations = rotations;
        } else {
            lastRotations = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }

        targetRotations = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        smoothed = false;
        lastYawDelta = 0;
    };

    private void release() {
        transitionProgress = Math.min(1f, transitionProgress + transitionSpeed);

        float yaw = transitionStart.x + MathHelper.wrapAngleTo180_float(releaseTarget.x - transitionStart.x) * transitionProgress;
        float pitch = transitionStart.y + (releaseTarget.y - transitionStart.y) * transitionProgress;

        rotations = new Vector2f(yaw, pitch);
        lastRotations = rotations;

        if (transitionProgress >= 1f) {
            active = false;
            releasing = false;
        }
    }

    public static void smooth() {
        if (!smoothed) {
            float targetYaw = targetRotations.x;
            float targetPitch = targetRotations.y;

            if (raycast != null && (Math.abs(targetYaw - rotations.x) > 5 || Math.abs(targetPitch - rotations.y) > 5)) {
                final Vector2f trueTargetRotations = new Vector2f(targetRotations.getX(), targetRotations.getY());

                double speed = (Math.random() * Math.random() * Math.random()) * 20;
                randomAngle += (float) ((20 + (float) (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360)) * (mc.thePlayer.ticksExisted / 10 % 2 == 0 ? -1 : 1));

                offset.setX((float) (offset.getX() + -MathHelper.sin((float) Math.toRadians(randomAngle)) * speed));
                offset.setY((float) (offset.getY() + MathHelper.cos((float) Math.toRadians(randomAngle)) * speed));

                targetYaw += offset.getX();
                targetPitch += offset.getY();

                if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                    randomAngle = (float) Math.toDegrees(Math.atan2(trueTargetRotations.getX() - targetYaw, targetPitch - trueTargetRotations.getY())) - 180;

                    targetYaw -= offset.getX();
                    targetPitch -= offset.getY();

                    offset.setX((float) (offset.getX() + -MathHelper.sin((float) Math.toRadians(randomAngle)) * speed));
                    offset.setY((float) (offset.getY() + MathHelper.cos((float) Math.toRadians(randomAngle)) * speed));

                    targetYaw = targetYaw + offset.getX();
                    targetPitch = targetPitch + offset.getY();
                }

                if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                    offset.setX(0);
                    offset.setY(0);

                    targetYaw = (float) (targetRotations.x + Math.random() * 2);
                    targetPitch = (float) (targetRotations.y + Math.random() * 2);
                }
            }

            float yawDelta = MathHelper.wrapAngleTo180_float(targetYaw - lastRotations.x);

            float maxDelta = 30f;
            if (Math.abs(lastYawDelta) < 30 && Math.abs(yawDelta) > 320) {
                yawDelta = Math.signum(yawDelta) * maxDelta;
            }

            targetYaw = lastRotations.x + yawDelta;

            rotations = RotationUtils.smooth(new Vector2f(targetYaw, targetPitch),
                    rotationSpeed + Math.random());

            if (transitionProgress < 1f) {
                transitionProgress = Math.min(1f, transitionProgress + transitionSpeed);

                float easedYaw = transitionStart.x + MathHelper.wrapAngleTo180_float(rotations.x - transitionStart.x) * transitionProgress;
                float easedPitch = transitionStart.y + (rotations.y - transitionStart.y) * transitionProgress;

                rotations = new Vector2f(easedYaw, easedPitch);
            }

            lastYawDelta = MathHelper.wrapAngleTo180_float(rotations.x - lastRotations.x);

            if (correctMovement == MovementFix.NORMAL || correctMovement == MovementFix.TRADITIONAL) {
                mc.thePlayer.movementYaw = rotations.x;
            }

            mc.thePlayer.velocityYaw = rotations.x;
        }

        smoothed = true;
        mc.entityRenderer.getMouseOver(1);
    }

    @AllArgsConstructor
    public enum MovementFix {
        OFF("Off"),
        NORMAL("Normal"),
        TRADITIONAL("Traditional"),
        BACKWARDS_SPRINT("Backwards Sprint");

        final String name;

        @Override
        public String toString() {
            return name;
        }
    }
}
