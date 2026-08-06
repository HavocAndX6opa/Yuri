package ddlc.yuri.modules.impl.combat.velocity.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.impl.combat.AuraModule;
import ddlc.yuri.modules.impl.combat.VelocityModule;
import ddlc.yuri.modules.impl.combat.velocity.VelocityMode;
import ddlc.yuri.utils.player.RotationUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import org.lwjgl.input.Keyboard;

public class HypixelVelocity implements VelocityMode {
    private final VelocityModule parent;

    private int ticksSinceVelocity = -1;
    private boolean hasReceivedVelocity = false;
    private boolean jumpFlag = false;

    private int reduceTick = 0;

    private int rotateTickCounter = 0;
    private double knockbackX = 0;
    private double knockbackZ = 0;
    private float[] targetRotation = null;

    public HypixelVelocity(VelocityModule parent) {
        this.parent = parent;
    }

    @Override
    public void onPacket(PacketReceivedEvent event) {
        if (mc.thePlayer == null) return;

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                if (parent.hypixelRotate.getValue() && packet.getMotionY() > 0) {
                    knockbackX = packet.getMotionX() / 8000.0;
                    knockbackZ = packet.getMotionZ() / 8000.0;
                    if (Math.abs(knockbackX) > 0.01 || Math.abs(knockbackZ) > 0.01) {
                        rotateTickCounter = 1;
                    }
                }

                hasReceivedVelocity = true;
                ticksSinceVelocity = 0;
                jumpFlag = packet.getMotionY() > 0;
            }
        }
    }

    @Override
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer == null) return;

        if (ticksSinceVelocity >= 0) {
            ticksSinceVelocity++;
        }
        if (ticksSinceVelocity >= 10) {
            ticksSinceVelocity = -1;
        }

        if (parent.hypixelJump.getValue()) {
            if (jumpFlag) {
                jumpFlag = false;
                if (mc.thePlayer.onGround && mc.thePlayer.isSprinting()
                        && !mc.thePlayer.isPotionActive(Potion.jump) && !isInLiquidOrWeb()) {
                    mc.gameSettings.keyBindJump.pressed = true;
                }
            } else {
                mc.gameSettings.keyBindJump.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
            }
        }
    }

    @Override
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;

        if (parent.ignoreOnFire.getValue() && mc.thePlayer.isBurning()) return;

        if (parent.hypixelReduce.getValue() && hasReceivedVelocity) {
            if (reduceTick >= parent.attackTimes.getValue()) {
                reduceTick = 0;
                hasReceivedVelocity = false;
            }

            AuraModule killAura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
            if (killAura != null && killAura.isEnabled() && AuraModule.target != null) {
                EntityLivingBase target = AuraModule.target;
                if (mc.thePlayer.getDistanceToEntity(target) <= 3 && !AuraModule.autoBlocking) {
                    if (mc.thePlayer.isSprinting() || !parent.onlySprinting.getValue()) {
                        if (!parent.reduceWhenCanAttack.getValue() || AuraModule.canAttack) {
                            PacketUtils.sendPacket(new C0APacketAnimation());
                            mc.playerController.attackEntity(mc.thePlayer, target);
                        }
                    }
                }
            }
            reduceTick++;
        }

        int maxTick = parent.rotateTicks.getValue().intValue();
        if (rotateTickCounter > 0 && rotateTickCounter <= maxTick) {
            if (rotateTickCounter == 1) {
                double targetX = mc.thePlayer.posX - knockbackX;
                double targetZ = mc.thePlayer.posZ - knockbackZ;
                targetRotation = RotationUtils.getRotationFromPosition(targetX, mc.thePlayer.posY, targetZ);
            }
            if (targetRotation != null) {
                RotationManager.setRotations(targetRotation[0], targetRotation[1], 10, RotationManager.MovementFix.NORMAL);
            }
        }

        if (rotateTickCounter > 0) {
            rotateTickCounter++;
            if (rotateTickCounter > maxTick) {
                rotateTickCounter = 0;
                targetRotation = null;
                knockbackX = 0;
                knockbackZ = 0;
            }
        }
    }

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || mc.thePlayer.isInWeb;
    }

    public void reset() {
        ticksSinceVelocity = -1;
        hasReceivedVelocity = false;
        jumpFlag = false;
        reduceTick = 0;
        rotateTickCounter = 0;
        knockbackX = 0;
        knockbackZ = 0;
        targetRotation = null;
    }
}
