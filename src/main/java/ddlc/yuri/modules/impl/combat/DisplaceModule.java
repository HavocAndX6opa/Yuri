package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.BadPacketsManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.Random;

@ModuleInfo(label = "Displace", description = "Offsets your hit angle to change the knockback direction of your target", category = ModuleCategory.COMBAT)
public final class DisplaceModule extends Module {

    private static final int MAX_WAIT_TICKS = 2;

    public enum Mode {
        LEFT("Left"), RIGHT("Right"), RANDOM("Random"), VOID("Void");
        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.VOID);
    private final NumberProperty angle = new NumberProperty("Angle", 45.0, 5.0, 90.0, 1.0);
    private final NumberProperty range = new NumberProperty("Range", 3.0, 3.0, 6.0, 0.1);
    private final NumberProperty cooldown = new NumberProperty("Cooldown", 1.0, 0.0, 5.0, 0.1);
    private final NumberProperty maxHurtTime = new NumberProperty("Max Hurt Time", 3, 0, 10, 1);
    private final Property<Boolean> onlyKnockbackItems = new Property<>("Only Knockback Items", false);
    private final Property<Boolean> playersOnly = new Property<>("Players Only", true);
    private final Property<Boolean> renderArrow = new Property<>("Render Arrow", true);

    private final Random random = new Random();
    private EntityLivingBase hitTarget = null;
    private boolean armed = false;
    private boolean finishing = false;
    private boolean overriding = false;
    private boolean attacking = false;
    private int waitTicks = 0;
    private float heldYaw = 0.0f;
    private float heldPitch = 0.0f;
    private long lastAt = 0L;

    private boolean attackedThisTick = false;
    private boolean sendingOwn = false;
    private Entity lastAttacked = null;

    private boolean arrow = false;
    private long arrowAt = 0L;
    private double arrowX, arrowY, arrowZ, dirX, dirZ;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            reset();
            return;
        }

        AuraModule aura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
        if (aura == null || !aura.isEnabled()) {
            reset();
            return;
        }

        if (armed) {
            if (hitTarget == null || hitTarget.isDead || mc.thePlayer.getDistanceToEntity(hitTarget) > range.getValue().doubleValue() + 1.0) {
                reset();
                return;
            }

            AuraModule.rotationOverride = true;
            AuraModule.canAttack = false;
            RotationManager.setRotations(new float[]{heldYaw, heldPitch}, 10.0f, RotationManager.MovementFix.NORMAL);

            boolean packetsBad = BadPacketsManager.bad();
            boolean blocking = mc.thePlayer.isUsingItem() || mc.thePlayer.isBlocking();

            if (waitTicks < MAX_WAIT_TICKS && (packetsBad || blocking)) {
                waitTicks++;
                return;
            }

            if (packetsBad) {
                reset();
                return;
            }

            armed = false;

            if (blocking) {
                PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                mc.thePlayer.stopUsingItem();
            }

            attacking = true;
            mc.clickMouse();
            attacking = false;
            finishing = true;
            return;
        }

        EntityLivingBase target = AuraModule.target;
        if (target == null || !canDisplace(target)) return;
        if (BadPacketsManager.bad()) return;

        float offset = pickOffset(target);
        if (Float.isNaN(offset)) return;

        float[] aim = aimRotations(target);
        heldYaw = aim[0] + offset;
        heldPitch = aim[1];
        hitTarget = target;
        lastAt = System.currentTimeMillis();
        waitTicks = 0;
        armed = true;
        overriding = true;
        AuraModule.rotationOverride = true;
        AuraModule.canAttack = false;
        RotationManager.setRotations(new float[]{heldYaw, heldPitch}, 10.0f, RotationManager.MovementFix.NORMAL);
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        if (sendingOwn || mc.theWorld == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof C03PacketPlayer) {
            attackedThisTick = false;
            lastAttacked = null;
            return;
        }

        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity use = (C02PacketUseEntity) packet;
            if (use.getAction() == C02PacketUseEntity.Action.ATTACK) {
                Entity hit = use.getEntityFromWorld(mc.theWorld);
                if (hit != null) {
                    lastAttacked = hit;
                    attackedThisTick = true;
                }
            } else if (use.getAction() == C02PacketUseEntity.Action.INTERACT) {
                attackedThisTick = false;
            }
            return;
        }

        if (packet instanceof C08PacketPlayerBlockPlacement && attackedThisTick && lastAttacked != null) {
            C08PacketPlayerBlockPlacement place = (C08PacketPlayerBlockPlacement) packet;
            ItemStack stack = place.getStack();
            if (place.getPlacedBlockDirection() != 255 || stack == null || !(stack.getItem() instanceof ItemSword))
                return;

            Entity hit = lastAttacked;
            attackedThisTick = false;
            sendingOwn = true;
            PacketUtils.sendPacket(new C02PacketUseEntity(hit, new Vec3(0.0, hit.height * 0.5, 0.0)));
            PacketUtils.sendPacket(new C02PacketUseEntity(hit, C02PacketUseEntity.Action.INTERACT));
            sendingOwn = false;
        }
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (!attacking || !renderArrow.getValue()) return;
        Entity target = event.target;
        if (target == null) return;
        setArrow(target, heldYaw);
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre()) return;
        if (finishing) {
            finishing = false;
            hitTarget = null;
            release();
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (!arrow || !renderArrow.getValue() || mc.thePlayer == null) return;

        long l = System.currentTimeMillis() - arrowAt;
        if (l > 600L) {
            arrow = false;
            return;
        }

        double alpha = 1.0;
        double t = clamp01((double) l / 100.0);
        alpha *= 0.08 + 0.92 * (t * t * (3.0 - 2.0 * t));
        if (l > 500L) {
            double f = clamp01((double) (l - 500L) / 100.0);
            alpha *= 1.0 - 0.92 * (f * f * (3.0 - 2.0 * f));
        }
        double p = clamp01((double) l / 450.0);
        double inv = 1.0 - p;
        double eased = 1.0 - inv * inv * inv;
        double bob = Math.sin((double) l * 0.015) * 0.02 * (0.4 + 0.6 * alpha);
        double dist = 0.8 * eased;

        double x = arrowX - mc.getRenderManager().viewerPosX + dirX * dist;
        double y = arrowY - mc.getRenderManager().viewerPosY + bob;
        double z = arrowZ - mc.getRenderManager().viewerPosZ + dirZ * dist;
        double yaw = Math.toDegrees(Math.atan2(-dirZ, dirX));

        int fill = (int) (225.0 * alpha) << 24 | 0xFFFFFF;
        int outline = (int) (255.0 * alpha) << 24 | 0xF6F6F6;
        int shadow = (int) (140.0 * alpha) << 24 | 0x05081A;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate((float) yaw, 0.0f, 1.0f, 0.0f);

        GL11.glPushMatrix();
        GL11.glTranslated(0.08 * 0.48, -0.08 * 0.24, -0.02);
        color(shadow);
        drawShaft(1.01, GL11.GL_QUADS);
        drawHead(1.01, GL11.GL_TRIANGLES);
        GL11.glPopMatrix();

        color(fill);
        drawShaft(1.0, GL11.GL_QUADS);
        drawHead(1.0, GL11.GL_TRIANGLES);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        color(outline);
        drawShaft(1.0, GL11.GL_LINE_LOOP);
        drawHead(1.0, GL11.GL_LINE_LOOP);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
        arrow = false;
        super.onDisable();
    }

    private void drawShaft(double s, int glMode) {
        GL11.glBegin(glMode);
        GL11.glVertex3d(-0.2784 * s, -0.0816 * s, 0.0);
        GL11.glVertex3d(0.0, -0.0288 * s, 0.0);
        GL11.glVertex3d(0.0, 0.0288 * s, 0.0);
        GL11.glVertex3d(-0.2784 * s, 0.0816 * s, 0.0);
        GL11.glEnd();
    }

    private void drawHead(double s, int glMode) {
        GL11.glBegin(glMode);
        GL11.glVertex3d(0.0, -0.144 * s, 0.0);
        GL11.glVertex3d(0.288 * s, 0.0, 0.0);
        GL11.glVertex3d(0.0, 0.144 * s, 0.0);
        GL11.glEnd();
    }

    private void color(int c) {
        GlStateManager.color((float) (c >> 16 & 0xFF) / 255.0f, (float) (c >> 8 & 0xFF) / 255.0f, (float) (c & 0xFF) / 255.0f, (float) (c >> 24 & 0xFF) / 255.0f);
    }

    private double clamp01(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }

    private void setArrow(Entity entity, float yaw) {
        double rad = Math.toRadians(yaw);
        dirX = -Math.sin(rad);
        dirZ = Math.cos(rad);
        arrowX = entity.posX + dirX * 0.8;
        arrowY = entity.posY + 1.2;
        arrowZ = entity.posZ + dirZ * 0.8;
        arrowAt = System.currentTimeMillis();
        arrow = true;
    }

    private boolean canDisplace(EntityLivingBase target) {
        if (target.isDead) return false;
        if (playersOnly.getValue() && !(target instanceof EntityPlayer)) return false;
        if (mc.thePlayer.getDistanceToEntity(target) > range.getValue().doubleValue()) return false;
        if (target.hurtTime > maxHurtTime.getValue().intValue()) return false;
        int knockback = EnchantmentHelper.getKnockbackModifier(mc.thePlayer);
        if (!mc.thePlayer.isSprinting() && knockback == 0) return false;
        if (onlyKnockbackItems.getValue() && knockback == 0) return false;
        long wait = (long) (cooldown.getValue().doubleValue() * 1000.0) - 20L;
        return System.currentTimeMillis() - lastAt >= wait;
    }

    private float pickOffset(EntityLivingBase target) {
        float a = angle.getValue().floatValue();
        boolean left;
        switch (mode.getValue()) {
            case LEFT:
                left = true;
                break;
            case RIGHT:
                left = false;
                break;
            case RANDOM:
                left = random.nextBoolean();
                break;
            default:
                int side = voidSide(target, a);
                if (side == -1) return Float.NaN;
                left = side == 0;
                break;
        }
        float jitter = random.nextFloat() * 10.0f - 5.0f;
        return (left ? -a : a) + jitter;
    }

    private int voidSide(EntityLivingBase target, float a) {
        float baseYaw = aimRotations(target)[0];
        for (int side = 0; side < 2; side++) {
            float yaw = baseYaw + (side == 0 ? -a : a);
            double rad = Math.toRadians(yaw);
            double x = target.posX + (-Math.sin(rad)) * 2.0;
            double z = target.posZ + Math.cos(rad) * 2.0;
            int startY = MathHelper.floor_double(target.posY) - 1;
            boolean ground = false;
            for (int y = startY; y >= Math.max(0, startY - 8); y--) {
                if (mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock().getMaterial() != Material.air) {
                    ground = true;
                    break;
                }
            }
            if (!ground) return side;
        }
        return -1;
    }

    private float[] aimRotations(EntityLivingBase target) {
        double x = target.posX - mc.thePlayer.posX;
        double y = (target.posY + (double) target.getEyeHeight() - 0.2) - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
        double z = target.posZ - mc.thePlayer.posZ;
        double dist = (double) MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, dist) * 180.0D / Math.PI));
        return new float[]{
                mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
                mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)
        };
    }

    private void release() {
        if (overriding) {
            AuraModule.rotationOverride = false;
            AuraModule.canAttack = true;
            overriding = false;
        }
    }

    private void reset() {
        armed = false;
        finishing = false;
        attacking = false;
        waitTicks = 0;
        hitTarget = null;
        attackedThisTick = false;
        lastAttacked = null;
        sendingOwn = false;
        release();
    }
}