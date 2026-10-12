
package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.TargetManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.Vec3;

import java.util.ArrayDeque;

@ModuleInfo(label = "Back Track", category = ModuleCategory.COMBAT, description = "Allows you to backtrack players' positions")
public class BackTrackModule extends Module {

    public final NumberProperty minRange = new NumberProperty("Min Range", 2.0, 0.0, 6.0, 0.1);
    public final NumberProperty maxRange = new NumberProperty("Max Range", 4.0, 0.0, 10.0, 0.1);
    public final NumberProperty maxDelay = new NumberProperty("Max Delay", 400.0, 40.0, 2000.0, 10.0);
    public final Property<Boolean> onlyKillAura = new Property<>("Only Kill Aura", true);

    private static final class Held {
        private final Packet<INetHandlerPlayClient> packet;
        private final long releaseAt;

        private Held(Packet<INetHandlerPlayClient> packet, long releaseAt) {
            this.packet = packet;
            this.releaseAt = releaseAt;
        }
    }

    private final ArrayDeque<Held> held = new ArrayDeque<>();
    private volatile boolean holding;
    private volatile int targetId = -1;
    private volatile Vec3 spoofedPosition;
    private int realX, realY, realZ;
    private NetHandlerPlayClient connection;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(maxDelay.getValue().intValue() + "ms");

        NetHandlerPlayClient current = mc.getNetHandler();
        if (mc.thePlayer == null || mc.theWorld == null || current == null || current != connection) {
            synchronized (held) {
                holding = false;
                held.clear();
                targetId = -1;
                spoofedPosition = null;
            }
            connection = current;
            return;
        }

        EntityLivingBase target = resolveTarget();
        if (target == null || !mc.thePlayer.isEntityAlive() || !inRange(target)) {
            stop();
            return;
        }

        if (!holding || target.getEntityId() != targetId) {
            synchronized (held) {
                releaseLocked(true);
                targetId = target.getEntityId();
                realX = target.serverPosX;
                realY = target.serverPosY;
                realZ = target.serverPosZ;
                updateSpoofedPosition();
                holding = true;
            }
        }

        release(false);
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        release(false);
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onPacketReceived(PacketReceivedEvent event) {
        if (event.isCancelled()) return;

        synchronized (held) {
            if (!holding) return;

            Packet<?> packet = event.getPacket();
            follow(packet);
            held.addLast(new Held((Packet<INetHandlerPlayClient>) packet, System.currentTimeMillis() + maxDelay.getValue().longValue()));
        }
        event.setCancelled(true);
    }

    private void follow(Packet<?> packet) {
        int target = targetId;

        if (packet instanceof S14PacketEntity) {
            S14PacketEntity p = (S14PacketEntity) packet;
            if (p.entityId == target) {
                realX += p.getPosX();
                realY += p.getPosY();
                realZ += p.getPosZ();
                updateSpoofedPosition();
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
            if (p.getEntityId() == target) {
                realX = p.getX();
                realY = p.getY();
                realZ = p.getZ();
                updateSpoofedPosition();
            }
        }
    }

    private void updateSpoofedPosition() {
        spoofedPosition = new Vec3(realX / 32.0, realY / 32.0, realZ / 32.0);
    }

/*    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || target == null || !spoofing || spoofedPosition == null) return;

        double drawX = spoofedPosition.xCoord - mc.getRenderManager().viewerPosX;
        double drawY = spoofedPosition.yCoord - mc.getRenderManager().viewerPosY;
        double drawZ = spoofedPosition.zCoord - mc.getRenderManager().viewerPosZ;

        double half = target.width / 2.0;
        double height = target.height;

        AxisAlignedBB box = new AxisAlignedBB(
                drawX - half, drawY, drawZ - half,
                drawX + half, drawY + height, drawZ + half);

        Color color = ColorManager.getColors().getFirst();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();

        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.16f);
        RenderUtils.drawBoundingBox(box);

        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.resetColor();
    }*/

    private EntityLivingBase resolveTarget() {
        if (onlyKillAura.getValue()) {
            AuraModule aura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
            return aura != null && aura.isEnabled() ? AuraModule.target : null;
        }

        Entity entity = TargetManager.getTarget();
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    private boolean inRange(EntityLivingBase target) {
        if (!target.isEntityAlive()) return false;
        float distance = calculateDistance(target.posX, target.posY, target.posZ);
        return distance > minRange.getValue() && distance < maxRange.getValue();
    }

    private float calculateDistance(double x, double y, double z) {
        Vec3 playerEyes = mc.thePlayer.getPositionEyes(mc.timer.renderPartialTicks);
        return (float) playerEyes.distanceTo(new Vec3(x, y, z));
    }

    private void stop() {
        synchronized (held) {
            holding = false;
            releaseLocked(true);
            targetId = -1;
            spoofedPosition = null;
        }
    }

    private void release(boolean all) {
        synchronized (held) {
            releaseLocked(all);
        }
    }

    private void releaseLocked(boolean all) {
        NetHandlerPlayClient handler = mc.getNetHandler();
        long now = System.currentTimeMillis();

        while (!held.isEmpty() && (all || held.peekFirst().releaseAt <= now)) {
            Held next = held.pollFirst();
            if (handler == null || handler != connection) continue;

            try {
                next.packet.processPacket(handler);
            } catch (Exception ignored) {
            }
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        targetId = -1;
        spoofedPosition = null;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        synchronized (held) {
            holding = false;
            held.clear();
            targetId = -1;
            spoofedPosition = null;
        }
        connection = mc.getNetHandler();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stop();
    }
}
