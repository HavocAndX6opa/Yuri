package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.HitSlowDownEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.*;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.client.LoggingUtils;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.player.InvUtils;
import ddlc.yuri.utils.player.RayCastUtils;
import ddlc.yuri.utils.player.RotationUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Arrays;

@ModuleInfo(label = "Aura", description = "Automatically attacks entities around you", category = ModuleCategory.COMBAT)
public class AuraModule extends Module {

    private final MultiModeProperty<TargetManager.Targets> targets = new MultiModeProperty<>("Targets", TargetManager.Targets.PLAYERS, TargetManager.Targets.HOSTILES, TargetManager.Targets.TEAMMATES, TargetManager.Targets.INVISIBLES);
    private static final ModeProperty<TargetManager.Mode> mode = new ModeProperty<>("Mode", TargetManager.Mode.SINGLE);
    public static NumberProperty seekRange = new NumberProperty("Seek Range", 6.0, 3, 6, 0.1);
    public static final Property<Boolean> useOnlyMouse = new Property<>("Simulate Mouse Clicks", true);
    public static NumberProperty attackRange = new NumberProperty("Attack Range", 3.0, 3, 6, 0.1, () -> !useOnlyMouse.getValue());
    public static NumberProperty swingRange = new NumberProperty("Swing Range", 6.0, 3, 6, 0.1);
    public static NumberProperty blockRange = new NumberProperty("Block Range", 6.0, 3, 6, 0.1);
    private static final NumberProperty min = new NumberProperty("Min CPS", 9.0, 0.0, 20.0, 0.5);
    private static final NumberProperty max = new NumberProperty("Max CPS", 13.0, 0.0, 20.0, 0.5);
    public static ModeProperty<AutoBlock> ab = new ModeProperty<>("Auto Block", AutoBlock.FAKE);
    public static Property<Boolean> onlyBlockIfHurt = new Property<>("Only Block If Hurt", false, () -> ab.getValue() != AutoBlock.LEGIT);
    private final NumberProperty blockOnHurtTicks = new NumberProperty("Block On Hurt Ticks", 4, 0, 10, 1, () -> ab.getValue() != AutoBlock.LEGIT && onlyBlockIfHurt.getValue());
    public static ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.NORMAL);
    private final NumberProperty minRotSpeed = new NumberProperty("Min Rotation Speed", 3, 0, 10, 0.5f);
    private final NumberProperty maxRotSpeed = new NumberProperty("Max Rotation Speed", 7, 0, 10, 0.5f);
    private final NumberProperty bodyEase = new NumberProperty("Body Ease", 0.2, 0.01, 1.0, 0.01, () -> rotations.getValue() == Rotations.ML);
    private final NumberProperty mlEase = new NumberProperty("ML Ease", 0.2, 0.01, 1.0, 0.01, () -> rotations.getValue() == Rotations.ML);
    public static final Property<Boolean> rayCast = new Property<>("Ray Cast", true);
    public static final ModeProperty<MoveFix> fix = new ModeProperty<>("Move Fix", MoveFix.SILENT);
    public static final Property<Boolean> sprint = new Property<>("Keep Sprint", false);
    public static final Property<Boolean> hypixelSprint = new Property<>("Hypixel Keep Sprint", false, sprint::getValue);
    public static final Property<Boolean> targetEsp = new Property<>("Target ESP", true);
    public static final Property<Boolean> astolfoPentagram = new Property<>("Astolfo Pentagram", true);
    public static final Property<Boolean> autoDisable = new Property<>("Auto Disable", true);

    public enum MoveFix {
        NONE("None"),
        STRICT("Strict"),
        SILENT("Silent");

        public final String name;

        MoveFix(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Rotations {
        NORMAL("Normal"),
        ML("ML"),
        NONE("None");

        public final String name;

        Rotations(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AutoBlock {
        FAKE("Fake"),
        VANILLA("Vanilla"),
        HYPIXEL("Hypixel"),
        NCP("NCP"),
        LEGIT("Legit"),
        NONE("None");

        public final String name;

        AutoBlock(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum HypixelPhase {
        BLOCK,
        RELEASE
    }

    public static EntityLivingBase target;
    private boolean hitSlow = false;
    public static boolean autoBlocking = false;
    public static boolean canAttack = true;
    private static final TimerUtils attackTimer = new TimerUtils();
    private int blockTicks = 0;
    private static long delay = 0;
    public int hitTicks;
    private EntityLivingBase lastTarget;
    private Vec3 smoothedBodyPoint;
    private HypixelPhase hypixelPhase = HypixelPhase.BLOCK;

    private static int nextCycleTick = -1;
    private static boolean runThisTick = false;
    private static boolean stopUse = false;
    private static boolean blocking = false;
    private int slotChangeTick = -1;


    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        if (mc.thePlayer == null || mc.theWorld == null || Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            resetCombatState();
            return;
        }

        TargetManager.setTargets(targets.getValue());
        getTarget();

        if (target == null) {
            target = null;
            unblock();
            canAttack = true;
            return;
        }

        calculateRotations();

        if (ab.getValue() != AutoBlock.NONE && ab.getValue() != AutoBlock.NCP) {
            if (mc.thePlayer.getDistanceToEntity(target) <= blockRange.getValue() && InvUtils.isHoldingSword()) {
                autoblock();
            }
        }

        if ((ab.getValue() == AutoBlock.HYPIXEL ||
                ab.getValue() == AutoBlock.LEGIT) &&
                mc.gameSettings.keyBindAttack.isPressed()) {
            mc.gameSettings.keyBindAttack.setPressed(false);
        }

        attack();
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre()) {
            this.hitTicks++;
            return;
        }

        if (target == null) return;

        if (ab.getValue() == AutoBlock.NCP) {
            if (!autoBlocking && InvUtils.isHoldingSword() && mc.thePlayer.getDistanceToEntity(target) <= blockRange.getValue()) {
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                autoBlocking = true;
            }
        }
    }

    @EventHook
    public void onHitSlowDown(HitSlowDownEvent e) {
        hitSlow = true;
        if (sprint.getValue() && !hypixelSprint.getValue()) {
            e.setSprint(true);
            e.setSlowDown(1.0);
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent e) {
        resetCombatState();
        if (autoDisable.getValue()) {
            toggle();
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        render3D();
        renderAstolfoPentagram();
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacket() instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction packet = (C0BPacketEntityAction) event.getPacket();

            if (packet.getAction() == C0BPacketEntityAction.Action.STOP_SPRINTING && target != null && hitSlow && mc.thePlayer.isSprinting()) {
                if (sprint.getValue() && hypixelSprint.getValue() && mc.thePlayer.hurtTime == 0) {
                    event.setCancelled(true);
                    PacketUtils.sendSilentPacket(packet);
                }
            }
        }
    }

    private void calculateRotations() {
        if (mc.thePlayer == null || target == null || rotations.getValue() == Rotations.NONE) return;

        if (target != lastTarget) {
            smoothedBodyPoint = null;
            RotationLearnerManager.resetSmoothing();
            lastTarget = target;
        }

        float rotSpeed = (float) MathUtils.getRandom(minRotSpeed.getValue(), maxRotSpeed.getValue());
        Vector2f rotation;

        if (rotations.getValue() == Rotations.ML && RotationLearnerManager.hasModelLoaded()) {
            rotation = RotationLearnerManager.humanize(getWholeBodyRotation(target), 1.0f, mlEase.getValue().floatValue());
        } else {
            rotation = RotationUtils.calculate(target, false, seekRange.getValue());
        }

        RotationManager.setRotations(rotation, rotSpeed, fix.getValue() != MoveFix.NONE ? fix.getValue() == MoveFix.SILENT ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.TRADITIONAL : RotationManager.MovementFix.OFF);
    }

    private Vector2f getWholeBodyRotation(EntityLivingBase entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double targetX = box.minX + (box.maxX - box.minX) * MathUtils.getRandom(0.0, 1.0);
        double targetY = box.minY + (box.maxY - box.minY) * MathUtils.getRandom(0.0, 1.0);
        double targetZ = box.minZ + (box.maxZ - box.minZ) * MathUtils.getRandom(0.0, 1.0);

        Vec3 desired = new Vec3(targetX, targetY, targetZ);

        if (smoothedBodyPoint == null) {
            smoothedBodyPoint = desired;
        } else {
            double ease = MathUtils.getRandom(0.0, 0.0);
            ease = bodyEase.getValue();
            smoothedBodyPoint = new Vec3(
                    smoothedBodyPoint.xCoord + (desired.xCoord - smoothedBodyPoint.xCoord) * ease,
                    smoothedBodyPoint.yCoord + (desired.yCoord - smoothedBodyPoint.yCoord) * ease,
                    smoothedBodyPoint.zCoord + (desired.zCoord - smoothedBodyPoint.zCoord) * ease
            );
        }

        Vec3 eyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        float[] rot = RotationUtils.getRotationsTo(eyePos, smoothedBodyPoint);

        return new Vector2f(rot[0], rot[1]);
    }

    private int getSwapSlot() {
        int current = mc.thePlayer.inventory.currentItem;
        for (int i = 0; i < 9; i++) {
            if (i == current) continue;
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                return i;
            }
        }
        return current == 0 ? 1 : 0;
    }

    private void autoblock() {
        if (mc.thePlayer == null || mc.playerController == null) return;

        if (target == null || !InvUtils.isHoldingSword()) {
            if (autoBlocking) unblock();
            return;
        }

        if (onlyBlockIfHurt.getValue() && mc.thePlayer.hurtTime < blockOnHurtTicks.getValue().intValue() && ab.getValue() != AutoBlock.LEGIT) {
            if (autoBlocking) unblock();
            return;
        }

        switch (ab.getValue()) {
            case FAKE:
                autoBlocking = true;
                break;
            case LEGIT:
                mc.gameSettings.keyBindUseItem.setPressed(hitTicks <= 5 && mc.thePlayer.hurtTime >= 5);
                autoBlocking = true;
                blockTicks++;
                if (mc.gameSettings.keyBindUseItem.isPressed() || mc.thePlayer.isUsingItem()) {
                    blockTicks = 0;
                }
                canAttack = !BadPacketsManager.bad(false, false, false, true, false) && blockTicks >= 2;
                break;
            case HYPIXEL:
                mc.gameSettings.keyBindUseItem.setPressed(hitTicks <= 1);
                autoBlocking = true;
                blockTicks++;
                if (mc.gameSettings.keyBindUseItem.isPressed() || mc.thePlayer.isUsingItem()) {
                    blockTicks = 0;
                }
                canAttack = !BadPacketsManager.bad(false, false, false, true, false) && blockTicks >= 1;
                break;
            case VANILLA:
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                autoBlocking = true;
                break;
            case NCP:
                canAttack = true;
                if (autoBlocking) {
                    PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                    autoBlocking = false;
                }
                break;
        }
    }

    private void unblock() {
        if (!autoBlocking) {
            canAttack = true;
            return;
        }

        blockTicks = -1;

        if (ab.getValue() == AutoBlock.FAKE) {
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (ab.getValue() == AutoBlock.HYPIXEL) {
            if (SlotManager.isActive()) {
                SlotManager.swapBack();
            }
            mc.gameSettings.keyBindUseItem.setPressed(false);
            hypixelPhase = HypixelPhase.BLOCK;
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (ab.getValue() == AutoBlock.LEGIT && mc.gameSettings.keyBindUseItem.isKeyDown()) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (InvUtils.isHoldingSword() && ab.getValue() != AutoBlock.LEGIT) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }

        autoBlocking = false;
        canAttack = true;
    }

    private void attack() {
        if (mc.thePlayer == null || mc.playerController == null || target == null || !canAttack)
            return;
        if (ab.getValue() == AutoBlock.HYPIXEL && BadPacketsManager.bad(true, false, false, true, false))
            return;
        if (!hitTimerDone()) return;

        double dist = mc.thePlayer.getDistanceToEntity(target);

        if (dist <= attackRange.getValue() && !useOnlyMouse.getValue()) {
            if (rayCast.getValue() && !(RayCastUtils.rayCast(RotationManager.rotations, blockRange.getValue().floatValue()) != null
                    && RayCastUtils.rayCast(RotationManager.rotations, blockRange.getValue().floatValue()).entityHit != null
                    && RayCastUtils.rayCast(RotationManager.rotations, blockRange.getValue().floatValue()).entityHit == target))
                return;
            swing();
            mc.playerController.attackEntity(mc.thePlayer, target);
            this.hitTicks = 0;
        }
        else if (dist <= swingRange.getValue()) {
            mc.clickMouse();
            this.hitTicks = 0;
        }
    }

    private void swing() {
        mc.thePlayer.swingItem();
    }

    private static boolean hitTimerDone() {
        boolean returnVal = false;
        if (attackTimer.hasTimeElapsed(delay, false)) {
            returnVal = true;
            attackTimer.reset();
            delay = (long) (1000.0 / getCPS());
        }
        return returnVal;
    }

    @Override
    public void onEnable() {
        delay = (long) (1000.0 / getCPS());
        canAttack = true;
        autoBlocking = false;
        blockTicks = -1;
        hypixelPhase = HypixelPhase.BLOCK;
        TargetManager.configure(Arrays.asList(targets.getValues()));
        attackTimer.reset();
        if (rotations.getValue() == Rotations.ML) {
            if (!RotationLearnerManager.hasModelLoaded()) {
                LoggingUtils.sendChatMessage("ML model not loaded, use .rot load <name> to load a rotation model!");
            }
        }
        super.onEnable();
    }

    private static double getCPS() {
        double minVal = min.getValue();
        double maxVal = max.getValue();
        if (maxVal <= 0) maxVal = 1.0;
        if (minVal < 0) minVal = 0.0;
        if (minVal > maxVal) {
            double t = minVal;
            minVal = maxVal;
            maxVal = t;
        }
        double cps = MathUtils.getRandom(minVal, maxVal);
        return Math.max(1.0, cps);
    }

    @Override
    public void onDisable() {
        resetCombatState();
        super.onDisable();
    }

    private void resetCombatState() {
        if (autoBlocking) {
            unblock();
        } else {
            canAttack = true;
        }
        if (SlotManager.isActive()) {
            SlotManager.swapBack();
        }
        hypixelPhase = HypixelPhase.BLOCK;
        target = null;
        lastTarget = null;
        smoothedBodyPoint = null;
        hitSlow = false;
        RotationLearnerManager.resetSmoothing();
        delay = 0;
        blockTicks = -1;
        attackTimer.reset();
    }

    private void getTarget() {
        target = TargetManager.getTarget();
    }

    public void render3D() {
        if (!targetEsp.getValue() || target == null) return;

        final float partialTicks = mc.timer.renderPartialTicks;
        EntityLivingBase player = target;
        final Color color = ColorManager.getColor();

        if (mc.getRenderManager() == null || player == null) return;

        final double x = player.prevPosX + (player.posX - player.prevPosX) * partialTicks - (mc.getRenderManager()).renderPosX;
        final double y = player.prevPosY + (player.posY - player.prevPosY) * partialTicks + Math.sin(System.currentTimeMillis() / 2E+2) + 1 - (mc.getRenderManager()).renderPosY;
        final double z = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks - (mc.getRenderManager()).renderPosZ;

        GL11.glPushMatrix();
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glEnable(2832);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glHint(3154, 4354);
        GL11.glHint(3155, 4354);
        GL11.glHint(3153, 4354);
        GL11.glDepthMask(false);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableCull();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        for (float i = 0; i <= Math.PI * 2 + ((Math.PI * 2) / 25); i += (float) ((Math.PI * 2) / 25)) {
            double vecX = x + 0.67 * Math.cos(i);
            double vecZ = z + 0.67 * Math.sin(i);

            RenderUtils.color(RenderUtils.withAlpha(color, (int) (255 * 0.25)));
            GL11.glVertex3d(vecX, y, vecZ);
        }

        for (float i = 0; i <= Math.PI * 2 + (Math.PI * 2) / 25; i += (Math.PI * 2) / 25) {
            double vecX = x + 0.67 * Math.cos(i);
            double vecZ = z + 0.67 * Math.sin(i);

            RenderUtils.color(RenderUtils.withAlpha(color, (int) (255 * 0.25)));
            GL11.glVertex3d(vecX, y, vecZ);

            RenderUtils.color(RenderUtils.withAlpha(color, 0));
            GL11.glVertex3d(vecX, y - Math.cos(System.currentTimeMillis() / 2E+2) / 2.0F, vecZ);
        }

        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDepthMask(true);
        GL11.glEnable(2929);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableCull();
        GL11.glDisable(2848);
        GL11.glEnable(2832);
        GL11.glEnable(3553);
        GL11.glPopMatrix();
        RenderUtils.color(ColorManager.getColor().getRGB());
    }

    private void renderAstolfoPentagram() {
        if (!astolfoPentagram.getValue() || mc.thePlayer == null) return;

        final float partialTicks = mc.timer.renderPartialTicks;
        final Color color = ColorManager.getColor();

        final double x = mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * partialTicks - mc.getRenderManager().renderPosX;
        final double y = mc.thePlayer.prevPosY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * partialTicks - mc.getRenderManager().renderPosY + 0.02;
        final double z = mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * partialTicks - mc.getRenderManager().renderPosZ;

        final double radius = 1.4;
        final double rotation = (System.currentTimeMillis() % 6000L) / 6000.0 * 360.0;

        GL11.glPushMatrix();
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glLineWidth(2.0F);
        GL11.glDepthMask(false);
        GlStateManager.disableCull();

        RenderUtils.color(color.getRGB());
        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(rotation + i * 144.0);
            double vecX = x + radius * Math.cos(angle);
            double vecZ = z + radius * Math.sin(angle);
            GL11.glVertex3d(vecX, y, vecZ);
        }

        GL11.glEnd();

        GL11.glDepthMask(true);
        GlStateManager.enableCull();
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glPopMatrix();
        RenderUtils.color(ColorManager.getColor().getRGB());
    }
}