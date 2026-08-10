package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.LagManager;
import ddlc.yuri.managers.impl.TargetManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.animations.impl.ContinualAnimation;
import javafx.beans.property.BooleanProperty;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.Vec3;

import java.util.Comparator;
import java.util.List;

@ModuleInfo(label = "Back Track", category = ModuleCategory.COMBAT, description = "Allows you to backtrack players' positions")
public class BackTrackModule extends Module {

    public final NumberProperty minDelayProperty = new NumberProperty("Min Delay", 50.0, 0.0, 5000.0, 10.0);
    public final NumberProperty maxDelayProperty = new NumberProperty("Max Delay", 200.0, 0.0, 5000.0, 10.0);
    public final NumberProperty activateDist = new NumberProperty("Activate Distance", 0.0, 0.0, 10.0, 0.1);
    public final NumberProperty deactivateDist = new NumberProperty("Deactivate Distance", 10.0, 0.0, 10.0, 0.1);
    public final ModeProperty<Mode> modeProperty = new ModeProperty<>("Mode", Mode.Constant);
    public final Property<Boolean> cancelClientPacketsProperty = new Property<>("Cancel Client Packets", true);
    public final Property<Boolean> onlyWithKillAura = new Property<>("Only With Kill Aura", true);
    public final Property<Boolean> releaseOnDamageProperty = new Property<>("Release On Damage", true);

    public enum Mode {
        Constant, Hit, Zero
    }

    private boolean lagging;
    private Vec3 realPosition;
    private EntityPlayer target;
    private int trackedEntityId = -1;
    private int ping;

    private final ContinualAnimation animatedX = new ContinualAnimation();
    private final ContinualAnimation animatedY = new ContinualAnimation();
    private final ContinualAnimation animatedZ = new ContinualAnimation();

    private void stopLagging() {
        if (lagging) {
            LagManager.dispatch();
            LagManager.disable();
            lagging = false;
        }
    }

    private EntityPlayer resolveTarget() {
        if (onlyWithKillAura.getValue()) {
            AuraModule aura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
            if (!aura.isEnabled() || !(AuraModule.target instanceof EntityPlayer)) return null;
            return (EntityPlayer) AuraModule.target;
        }

        return TargetManager.getTarget() instanceof EntityPlayer ? (EntityPlayer) TargetManager.getTarget() : null;
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre()) return;

        if (mc.thePlayer.isDead) {
            stopLagging();
            clearTarget();
            return;
        }

        EntityPlayer resolved = resolveTarget();

        if (resolved != target) {
            stopLagging();
            realPosition = null;
            trackedEntityId = -1;
        }

        target = resolved;

        if (target == null) {
            stopLagging();
            return;
        }

        if (realPosition == null) {
            realPosition = new Vec3(target.posX, target.posY, target.posZ);
            trackedEntityId = target.getEntityId();
        }

        setSuffix(ping + "ms");

        double realDist = realPosition.distanceTo(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
        boolean inRange = realDist >= activateDist.getValue() && realDist <= deactivateDist.getValue();
        boolean notHurt = !releaseOnDamageProperty.getValue() || mc.thePlayer.hurtTime == 0;

        if (inRange && notHurt && shouldActivate(target)) {
            ping = (int) MathUtils.getRandom(minDelayProperty.getValue().intValue(), maxDelayProperty.getValue().intValue());
            LagManager.spoof(ping, true, true, true, true, cancelClientPacketsProperty.getValue(), cancelClientPacketsProperty.getValue());
            lagging = true;
        } else {
            stopLagging();
        }
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        if (target == null || !lagging || realPosition == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof S14PacketEntity) {
            S14PacketEntity p = (S14PacketEntity) packet;
            if (p.entityId == trackedEntityId) {
                realPosition = new Vec3(
                        realPosition.xCoord + p.getPosX() / 32D,
                        realPosition.yCoord + p.getPosY() / 32D,
                        realPosition.zCoord + p.getPosZ() / 32D
                );
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
            if (p.getEntityId() == trackedEntityId) {
                realPosition = new Vec3(p.getX() / 32D, p.getY() / 32D, p.getZ() / 32D);
            }
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (target == null || !lagging || realPosition == null) return;
        if (mc.thePlayer.getDistanceToEntity(target) > deactivateDist.getValue()) return;
        if (!shouldActivate(target)) return;

        double x = realPosition.xCoord - mc.getRenderManager().viewerPosX;
        double y = realPosition.yCoord - mc.getRenderManager().viewerPosY;
        double z = realPosition.zCoord - mc.getRenderManager().viewerPosZ;

        animatedX.animate((float) x, 10);
        animatedY.animate((float) y, 10);
        animatedZ.animate((float) z, 10);

        RenderUtils.renderPlayerPosition(animatedX.getOutput(), animatedY.getOutput(), animatedZ.getOutput());
    }

    public boolean shouldActivate(EntityPlayer target) {
        Mode mode = modeProperty.getValue();
        return mode == Mode.Constant
                || mode == Mode.Hit && target.hurtTime != 0
                || mode == Mode.Zero && target.hurtTime == 0;
    }

    private void clearTarget() {
        target = null;
        realPosition = null;
        trackedEntityId = -1;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clearTarget();
        lagging = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopLagging();
        clearTarget();
    }
}