package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.SimulatedPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

@ModuleInfo(label = "Hit Select", description = "Gates clicks based on hurt time, trades and counter hits", category = ModuleCategory.COMBAT)
public final class HitSelectModule extends Module {

    private static final int PREDICTION_TICKS = 2;

    private final NumberProperty range = new NumberProperty("Active Range", 3.0, 3.0, 6.0, 0.1);
    private final NumberProperty searchRange = new NumberProperty("Target Search Range", 3.5, 3.0, 6.0, 0.1);
    private final NumberProperty hurtTime = new NumberProperty("On Hurt Time", 3.0, 0.0, 10.0, 1.0);
    private final NumberProperty tradeTimeout = new NumberProperty("Trade Timeout", 5.0, 0.0, 10.0, 1.0);
    private final NumberProperty tradeHurtTime = new NumberProperty("Trade Hurt Time", 2.0, -5.0, 10.0, 1.0);
    private final Property<Boolean> counterHit = new Property<>("Counter Hit", true);

    private EntityLivingBase target;
    private boolean allowClick = true;
    private int trackedHurt;
    private double lastDamage;
    private long lastHurtAt;

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        reset();
    }

    @EventHook
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (AuraModule.target instanceof EntityLivingBase) {
            target = (EntityLivingBase) AuraModule.target;
        }

        if (target != null && (target.isDead || boxDistance(mc.thePlayer.getPositionEyes(1.0f), target.getEntityBoundingBox()) > searchRange.getValue())) {
            target = null;
        }

        if (mc.thePlayer.hurtTime != 0) {
            lastHurtAt = System.currentTimeMillis();
        }

        if (target == null) {
            allowClick = true;
            return;
        }

        if (target.hurtTime == 0) {
            trackedHurt--;
        } else {
            trackedHurt = target.hurtTime;
        }

        allowClick = evaluate();
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        Entity entity = AuraModule.target;
        if (entity == null && mc.objectMouseOver != null) {
            entity = mc.objectMouseOver.entityHit;
        }
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }

        target = (EntityLivingBase) entity;

        double damage = currentDamage();

        if (allowClick) {
            lastDamage = damage;
            return;
        }

        if (damage > lastDamage) {
            lastDamage = damage;
        } else {
            event.setCancelled(true);
        }
    }

    private double currentDamage() {
        float base = (float) mc.thePlayer.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
        float bonus = EnchantmentHelper.getModifierForCreature(mc.thePlayer.getHeldItem(), target.getCreatureAttribute());
        boolean crit = mc.thePlayer.fallDistance > 0.0f && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder()
                && !mc.thePlayer.isInWater() && !mc.thePlayer.isPotionActive(Potion.blindness) && mc.thePlayer.ridingEntity == null;

        if (crit && base > 0.0f) {
            base *= 1.5f;
        }

        return base + bonus;
    }

    private boolean evaluate() {
        EntityPlayerSP self = mc.thePlayer;
        Vec3 eyes = self.getPositionEyes(1.0f);

        SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(self.movementInput);
        for (int i = 0; i < PREDICTION_TICKS; i++) {
            sim.tick();
        }

        double distance = boxDistance(eyes, target.getEntityBoundingBox());
        boolean nearGround = target instanceof EntityPlayer && (target.onGround || groundDistance(target) < 0.7);

        boolean targetHittable = target.hurtTime <= hurtTime.getValue().intValue();
        boolean selfHittable = nearGround
                && boxDistance(target.getPositionEyes(1.0f), self.getEntityBoundingBox()) < 3.0
                && hittableBy(target);

        if (counterHit.getValue()) {
            double dx = target.posX - target.lastTickPosX;
            double dy = target.posY - target.lastTickPosY;
            double dz = target.posZ - target.lastTickPosZ;
            AxisAlignedBB predictedBox = target.getEntityBoundingBox().offset(dx, dy, dz);

            if (boxDistance(eyes, predictedBox) < range.getValue() && distance > 1.0 && !target.isSwingInProgress
                    && movingTowards(target, self) && facingDifference(target) < 45.0f
                    && !falling() && nearGround && target.hurtTime == 0) {
                return false;
            }
        }

        Vec3 simEyes = sim.getPos().addVector(0.0, self.getEyeHeight(), 0.0);
        boolean midTrade = rayDistance(simEyes, self.getLook(1.0f), target.getEntityBoundingBox()) <= range.getValue()
                && self.getHealth() >= target.getHealth()
                && !nearGround
                && distance < range.getValue() - 0.4;

        boolean recentlyHurt = System.currentTimeMillis() - lastHurtAt < tradeTimeout.getValue().intValue() * 50L;

        if (recentlyHurt && midTrade) {
            int ticksToGround = ticksToGround();
            int low = tradeHurtTime.getValue().intValue();
            int high = hurtTime.getValue().intValue();
            int threshold = (int) (low + (high - low) * (ticksToGround / 5.0f));

            if (trackedHurt > threshold && trackedHurt <= high) {
                return false;
            }
        }

        boolean targetOnFire = !target.isPotionActive(Potion.fireResistance) && target.isBurning();

        return targetHittable || recentlyHurt || selfHittable || targetOnFire;
    }

    private int ticksToGround() {
        SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);

        for (int i = 0; i < 5; i++) {
            sim.tick();
            if (sim.onGround) {
                return i + 1;
            }
        }

        return 5;
    }

    private boolean falling() {
        if (mc.thePlayer.fallDistance > 0.0f) {
            return true;
        }

        SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);
        sim.tick();
        return sim.fallDistance > 0.0f;
    }

    private boolean hittableBy(EntityLivingBase attacker) {
        EntityPlayerSP self = mc.thePlayer;
        double pred = ping(attacker) / 50.0;

        Vec3 from = attacker.getPositionEyes(1.0f).addVector(
                (attacker.posX - attacker.lastTickPosX) * pred,
                (attacker.posY - attacker.lastTickPosY) * pred,
                (attacker.posZ - attacker.lastTickPosZ) * pred
        );
        Vec3 direction = attacker.getLook(1.0f);
        Vec3 to = from.addVector(direction.xCoord * 3.0, direction.yCoord * 3.0, direction.zCoord * 3.0);

        AxisAlignedBB predictedBox = self.getEntityBoundingBox().contract(0.01, 0.01, 0.01).offset(
                (self.posX - self.lastTickPosX) * pred,
                (self.posY - self.lastTickPosY) * pred,
                (self.posZ - self.lastTickPosZ) * pred
        );

        MovingObjectPosition hit = predictedBox.calculateIntercept(from, to);

        return hit != null && hit.hitVec != null && predictedBox.expand(0.06, 0.06, 0.06).isVecInside(hit.hitVec);
    }

    private int ping(Entity entity) {
        if (entity instanceof EntityPlayer && mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(entity.getUniqueID());
            return info == null ? 50 : info.getResponseTime();
        }

        return 50;
    }

    private double rayDistance(Vec3 eyes, Vec3 look, AxisAlignedBB box) {
        if (box.isVecInside(eyes)) {
            return 0.0;
        }

        Vec3 end = eyes.addVector(look.xCoord * 64.0, look.yCoord * 64.0, look.zCoord * 64.0);
        MovingObjectPosition hit = box.calculateIntercept(eyes, end);

        return hit == null ? Double.MAX_VALUE : eyes.distanceTo(hit.hitVec);
    }

    private double boxDistance(Vec3 from, AxisAlignedBB box) {
        double dx = Math.max(Math.max(box.minX - from.xCoord, 0.0), from.xCoord - box.maxX);
        double dy = Math.max(Math.max(box.minY - from.yCoord, 0.0), from.yCoord - box.maxY);
        double dz = Math.max(Math.max(box.minZ - from.zCoord, 0.0), from.zCoord - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double groundDistance(Entity entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();

        for (double d = 0.05; d <= 6.0; d += 0.1) {
            if (!mc.theWorld.getCollidingBoundingBoxes(entity, box.offset(0.0, -d, 0.0)).isEmpty()) {
                return d;
            }
        }

        return 6.0;
    }

    private float facingDifference(EntityLivingBase source) {
        Vec3 from = source.getPositionEyes(1.0f);
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox();
        double dx = (box.minX + box.maxX) / 2.0 - from.xCoord;
        double dy = (box.minY + box.maxY) / 2.0 - from.yCoord;
        double dz = (box.minZ + box.maxZ) / 2.0 - from.zCoord;

        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(dy, Math.hypot(dx, dz)) * 180.0 / Math.PI);

        float yawDiff = MathHelper.wrapAngleTo180_float(yaw - source.rotationYawHead);
        float pitchDiff = pitch - source.rotationPitch;

        return (float) Math.hypot(yawDiff, pitchDiff);
    }

    private boolean movingTowards(EntityLivingBase source, EntityLivingBase destination) {
        double mx = source.posX - source.lastTickPosX;
        double mz = source.posZ - source.lastTickPosZ;
        double movementLength = Math.sqrt(mx * mx + mz * mz);

        if (movementLength == 0.0) {
            return false;
        }

        mx /= movementLength;
        mz /= movementLength;

        double tx = destination.posX - source.posX;
        double tz = destination.posZ - source.posZ;
        double targetLength = Math.sqrt(tx * tx + tz * tz);

        if (targetLength == 0.0) {
            return false;
        }

        tx /= targetLength;
        tz /= targetLength;

        return mx * tx + mz * tz >= Math.cos(Math.toRadians(60.0));
    }

    private void reset() {
        target = null;
        allowClick = true;
        trackedHurt = 0;
        lastDamage = 0.0;
        lastHurtAt = 0L;
    }
}