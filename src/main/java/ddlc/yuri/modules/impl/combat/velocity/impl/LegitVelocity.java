package ddlc.yuri.modules.impl.combat.velocity.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.managers.impl.BadPacketsManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.managers.impl.TargetManager;
import ddlc.yuri.modules.impl.combat.AuraModule;
import ddlc.yuri.modules.impl.combat.VelocityModule;
import ddlc.yuri.modules.impl.combat.velocity.VelocityMode;
import ddlc.yuri.modules.impl.misc.AntiBotModule;
import ddlc.yuri.utils.player.FriendUtils;
import ddlc.yuri.utils.player.PlayerUtils;
import ddlc.yuri.utils.player.RotationUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class LegitVelocity implements VelocityMode {

    private static final double CLOSE_RANGE = 3.0D;
    private static final double ROTATION_SPEED = 18.0D;
    private static final int USE_ITEM_DELAY = 1;

    private final VelocityModule parent;

    private int reduceTicksCounter = -1;
    private int lastAttackTick = -1;
    private boolean shouldJump;
    private boolean isCancellingAction;
    private Entity pendingAttack;
    private int useItemTicks;

    public LegitVelocity(VelocityModule parent) {
        this.parent = parent;
    }

    @Override
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        this.useItemTicks = mc.thePlayer.isUsingItem() ? 0 : this.useItemTicks + 1;

        if (this.pendingAttack != null && this.useItemTicks >= USE_ITEM_DELAY) {
            Entity target = this.pendingAttack;
            this.pendingAttack = null;

            if (mc.currentScreen == null && isValidTarget(target)
                    && mc.thePlayer.getDistanceToEntity(target) <= 3) {
                rotateTo(target);
                attackEntity(target);
            }

            return;
        }

        if (mc.currentScreen == null && this.reduceTicksCounter >= 0
                && this.reduceTicksCounter < parent.reduceTicks.getValue()) {
            this.reduceTicksCounter++;
            airPush();
        }
    }

    private void rotateTo(Entity target) {
        RotationManager.setRotations(RotationUtils.calculate(target), ROTATION_SPEED,
                RotationManager.MovementFix.NORMAL);
    }

    private void airPush() {
        if (this.lastAttackTick == mc.thePlayer.ticksExisted) {
            return;
        }

        EntityLivingBase furthest = findFurthestLiving(3);
        if (furthest != null) {
            rotateTo(furthest);
            tryAttack(furthest);
            return;
        }

        EntityLivingBase closest = findClosestLiving(CLOSE_RANGE);
        if (!(closest instanceof EntityPlayer)) {
            return;
        }

        rotateTo(closest);
        tryAttack(closest);
    }

    private void tryAttack(Entity target) {
        if (mc.thePlayer.isUsingItem()) {
            this.pendingAttack = target;
            return;
        }

        attackEntity(target);
    }

    private void attackEntity(Entity target) {
        AuraModule aura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
        if (aura.isEnabled() && AuraModule.target != null && BadPacketsManager.bad(false, true, true, true ,false)) {
            return;
        }

        if (mc.thePlayer == null || mc.playerController == null) {
            return;
        }

        this.isCancellingAction = true;
        PacketUtils.sendPacket(new C0APacketAnimation());
        mc.playerController.attackEntity(mc.thePlayer, target);
        this.isCancellingAction = false;
    }

    private EntityLivingBase findFurthestLiving(double range) {
        EntityLivingBase furthest = null;
        double furthestDistance = 0.0D;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!isValidTarget(entity)) {
                continue;
            }
            double distance = mc.thePlayer.getDistanceToEntity(entity);
            if (distance > range) {
                continue;
            }
            if (furthest == null || distance > furthestDistance) {
                furthest = (EntityLivingBase) entity;
                furthestDistance = distance;
            }
        }

        return furthest;
    }

    private EntityLivingBase findClosestLiving(double range) {
        EntityLivingBase closest = null;
        double closestDistance = range;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!isValidTarget(entity)) {
                continue;
            }
            double distance = mc.thePlayer.getDistanceToEntity(entity);
            if (distance < closestDistance) {
                closest = (EntityLivingBase) entity;
                closestDistance = distance;
            }
        }

        return closest;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == mc.thePlayer || entity.isDead || !(entity instanceof EntityLivingBase)) {
            return false;
        }
        if (((EntityLivingBase) entity).getHealth() <= 0.0F) {
            return false;
        }
        if (entity instanceof EntityArmorStand) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (FriendUtils.isFriend(player)) {
                return false;
            }
            AntiBotModule antiBot = Yuri.INSTANCE.getModuleManager().getModule(AntiBotModule.class);
            if (antiBot != null && antiBot.isBot(player)) {
                return false;
            }
            if (TargetManager.inTeam(mc.thePlayer, player)) {
                return false;
            }
        }

        return PlayerUtils.canSeeEntity(entity);
    }

    @Override
    public void onPacket(PacketReceivedEvent event) {
        if (mc.thePlayer == null) {
            return;
        }
        if (!(event.getPacket() instanceof S12PacketEntityVelocity)) {
            return;
        }

        S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
        if (packet.getEntityID() != mc.thePlayer.getEntityId()) {
            return;
        }

        if (packet.getMotionY() <= 0) {
            return;
        }


        if (parent.reduceTicks.getValue() != 0.0D) {
            this.reduceTicksCounter = 0;
        }


        if (mc.thePlayer.onGround && mc.thePlayer.isSprinting()) {
            this.shouldJump = true;
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (mc.thePlayer == null) {
            return;
        }

        if (event.getPacket() instanceof C02PacketUseEntity
                && ((C02PacketUseEntity) event.getPacket()).getAction() == C02PacketUseEntity.Action.ATTACK) {
            this.lastAttackTick = mc.thePlayer.ticksExisted;
        }

        if (this.isCancellingAction && event.getPacket() instanceof C07PacketPlayerDigging) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onStrafe(StrafeEvent event) {
        if (this.shouldJump) {
            this.shouldJump = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                mc.thePlayer.jump();
            }
        }
    }

    @Override
    public void reset() {
        this.reduceTicksCounter = -1;
        this.lastAttackTick = -1;
        this.shouldJump = false;
        this.isCancellingAction = false;
        this.pendingAttack = null;
        this.useItemTicks = 0;
    }
}