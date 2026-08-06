package ddlc.yuri.modules.impl.combat.velocity.impl;

import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.modules.impl.combat.VelocityModule;
import ddlc.yuri.modules.impl.combat.velocity.VelocityMode;
import ddlc.yuri.utils.player.MoveUtils;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ThreadLocalRandom;

public class PolarVelocity implements VelocityMode {
    private final VelocityModule parent;

    private boolean hitProcessed;
    private int jumpTicks;
    private boolean receivedVelocity;

    public PolarVelocity(VelocityModule parent) {
        this.parent = parent;
    }

    @Override
    public void onPacket(PacketReceivedEvent event) {
        if (mc.thePlayer == null) {
            return;
        }

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();

            if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                if (packet.getMotionX() != 0 || packet.getMotionZ() != 0) {
                    receivedVelocity = true;
                }
            }
        }
    }

    @Override
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer == null) {
            return;
        }

        if (parent.ignoreOnFire.getValue() && mc.thePlayer.isBurning()) {
            mc.gameSettings.keyBindJump.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
            return;
        }

        boolean damaged = mc.thePlayer.hurtTime > 0 && receivedVelocity;

        if (damaged) {
            if (!hitProcessed) {
                hitProcessed = true;

                jumpTicks = ThreadLocalRandom.current().nextDouble() <= 0.75D
                        ? ThreadLocalRandom.current().nextInt(5)
                        : -1;
            }
        } else {
            if (mc.thePlayer.hurtTime == 0) {
                receivedVelocity = false;
            }
            mc.gameSettings.keyBindJump.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
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