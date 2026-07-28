package ddlc.yuri.modules.impl.combat.velocity.impl;

import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.modules.impl.combat.VelocityModule;
import ddlc.yuri.modules.impl.combat.velocity.VelocityMode;
import ddlc.yuri.utils.player.MoveUtils;

import java.util.concurrent.ThreadLocalRandom;

public class LegitVelocity implements VelocityMode {
    private final VelocityModule parent;

    private boolean hitProcessed;
    private int jumpTicks;

    public LegitVelocity(VelocityModule parent) {
        this.parent = parent;
    }

    @Override
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer == null) {
            return;
        }

        if (parent.ignoreOnFire.getValue() && mc.thePlayer.isBurning()) {
            return;
        }

        boolean damaged = mc.thePlayer.hurtTime > 0;

        if (damaged) {
            if (!hitProcessed) {
                hitProcessed = true;

                if (parent.polar.getValue()) {
                    jumpTicks = ThreadLocalRandom.current().nextDouble() <= 0.75D
                            ? ThreadLocalRandom.current().nextInt(5)
                            : -1;
                } else {
                    jumpTicks = 0;
                }
            }
        } else {
            hitProcessed = false;
        }

        if (jumpTicks < 0) {
            return;
        }

        if (jumpTicks > 0) {
            jumpTicks--;
            return;
        }

        mc.gameSettings.keyBindJump.pressed =
                MoveUtils.isMoving() && MoveUtils.isOnGround();

        jumpTicks = -1;
    }
}
