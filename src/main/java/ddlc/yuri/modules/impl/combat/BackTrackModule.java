package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.TargetManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.Vec3;

import java.util.ArrayDeque;

@ModuleInfo(label = "Back Track", category = ModuleCategory.COMBAT, description = "Allows you to backtrack players' positions")
public class BackTrackModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty("Mode", Mode.BASIC);
    public final ModeProperty<Style> style = new ModeProperty("Style", Style.NEW, () -> mode.getValue() == Mode.BASIC);
    public final NumberProperty minRange = new NumberProperty("Min Range", 2.0, 0.0, 6.0, 0.1, () -> mode.getValue() != Mode.MMC);
    public final NumberProperty maxRange = new NumberProperty("Max Range", 4.0, 0.0, 10.0, 0.1, () -> mode.getValue() != Mode.MMC);
    public final NumberProperty delay = new NumberProperty("Delay", 400.0, 40.0, 2000.0, 10.0, () -> mode.getValue() == Mode.BASIC);
    public final Property<Boolean> onlyKillAura = new Property<>("Only Kill Aura", true);

    public enum Mode {
        BASIC("Basic"),
        MMC("MMC"),
        HYPIXEL("Hypixel");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Style {
        NEW("New"),
        OLD("Old");

        public final String name;

        Style(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class Held {
        private final Packet<INetHandlerPlayClient> packet;
        private final long releaseAt;

        private Held(Packet<INetHandlerPlayClient> packet, long releaseAt) {
            this.packet = packet;
            this.releaseAt = releaseAt;
        }
    }

    private final ArrayDeque<Held> held = new ArrayDeque<>();
    private final TimerUtils timer = new TimerUtils();
    private volatile boolean holding;
    private volatile boolean flush;
    private volatile int targetId = -1;
    private volatile Vec3 realPosition;
    private int realX, realY, realZ;
    private NetHandlerPlayClient connection;

    private boolean legacy() {
        Mode current = mode.getValue();
        if (current == Mode.HYPIXEL) return true;
        if (current == Mode.MMC) return false;
        return style.getValue() == Style.OLD;
    }

    private double getDelay() {
        Mode current = mode.getValue();
        if (current == Mode.MMC) return 1000.0;
        if (current == Mode.HYPIXEL) return 250.0;
        return delay.getValue();
    }

    private double getMinRange() {
        return mode.getValue() == Mode.MMC ? 0.0 : minRange.getValue();
    }

    private double getMaxRange() {
        return mode.getValue() == Mode.MMC ? 8.0 : maxRange.getValue();
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue() == Mode.BASIC ? (int) getDelay() + "ms" : mode.getValue().toString());

        NetHandlerPlayClient current = mc.getNetHandler();
        if (mc.thePlayer == null || mc.theWorld == null || current == null || current != connection) {
            synchronized (held) {
                holding = false;
                flush = false;
                held.clear();
                targetId = -1;
                realPosition = null;
            }
            connection = current;
            return;
        }

        if (flush) {
            stop();
            timer.reset();
        }

        EntityLivingBase target = resolveTarget();
        if (target == null || !mc.thePlayer.isEntityAlive() || !inRange(target)) {
            stop();
            timer.reset();
            return;
        }

        if (legacy() && holding && timer.hasTimeElapsed(getDelay())) {
            stop();
            timer.reset();
            return;
        }

        if (!holding || target.getEntityId() != targetId) {
            begin(target);
        }

        release(false);
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (flush) {
            stop();
            timer.reset();
            return;
        }
        release(false);
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onPacketReceived(PacketReceivedEvent event) {
        if (event.isCancelled()) return;

        Packet<?> packet = event.getPacket();

        synchronized (held) {
            if (!holding) return;

            Packet<INetHandlerPlayClient> typed = (Packet<INetHandlerPlayClient>) packet;

            if (legacy()) {
                if (isFlushPacket(packet)) {
                    held.addLast(new Held(typed, 0L));
                    flush = true;
                } else if (packet instanceof S14PacketEntity || packet instanceof S18PacketEntityTeleport) {
                    follow(packet);
                    held.addLast(new Held(typed, Long.MAX_VALUE));
                } else {
                    return;
                }
            } else {
                follow(packet);
                held.addLast(new Held(typed, System.currentTimeMillis() + (long) getDelay()));
            }
        }

        event.setCancelled(true);
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        if (!legacy() || !holding || mc.theWorld == null) return;

        Packet<?> packet = event.getPacket();
        Vec3 position = realPosition;

        if (packet instanceof C02PacketUseEntity && position != null) {
            Entity hit = ((C02PacketUseEntity) packet).getEntityFromWorld(mc.theWorld);
            if (hit != null && hit.getEntityId() == targetId) {
                hit.setPosition(position.xCoord, position.yCoord, position.zCoord);
            }
        }
    }

    private boolean isFlushPacket(Packet<?> packet) {
        if (packet instanceof S08PacketPlayerPosLook) return true;
        return packet instanceof S12PacketEntityVelocity
                && mc.thePlayer != null
                && ((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId();
    }

    private void follow(Packet<?> packet) {
        int id = targetId;

        if (packet instanceof S14PacketEntity) {
            S14PacketEntity p = (S14PacketEntity) packet;
            if (p.entityId == id) {
                realX += p.getPosX();
                realY += p.getPosY();
                realZ += p.getPosZ();
                updateRealPosition();
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
            if (p.getEntityId() == id) {
                realX = p.getX();
                realY = p.getY();
                realZ = p.getZ();
                updateRealPosition();
            }
        }
    }

    private void updateRealPosition() {
        realPosition = new Vec3(realX / 32.0, realY / 32.0, realZ / 32.0);
    }

    private void begin(EntityLivingBase target) {
        synchronized (held) {
            releaseLocked(true);
            targetId = target.getEntityId();
            realX = target.serverPosX;
            realY = target.serverPosY;
            realZ = target.serverPosZ;
            updateRealPosition();
            holding = true;
            flush = false;
        }
        timer.reset();
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        Vec3 pos = realPosition;
        if (mc.thePlayer == null || mc.theWorld == null || !holding || pos == null) return;

        RenderManager rm = mc.getRenderManager();

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();

        RenderUtils.renderPlayerPosition(pos.xCoord - rm.viewerPosX, pos.yCoord - rm.viewerPosY, pos.zCoord - rm.viewerPosZ);

        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private EntityLivingBase resolveTarget() {
        if (onlyKillAura.getValue()) {
            AuraModule aura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
            return aura != null && aura.isEnabled() ? AuraModule.target : null;
        }

        Entity entity = TargetManager.getTarget();
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    private Vec3 realPositionOf(EntityLivingBase target) {
        synchronized (held) {
            Vec3 tracked = realPosition;
            if (holding && tracked != null && target.getEntityId() == targetId) {
                return tracked;
            }
        }
        return new Vec3(target.serverPosX / 32.0, target.serverPosY / 32.0, target.serverPosZ / 32.0);
    }

    private boolean inRange(EntityLivingBase target) {
        if (!target.isEntityAlive()) return false;
        Vec3 pos = realPositionOf(target);
        double distance = calculateDistance(pos.xCoord, pos.yCoord, pos.zCoord);
        return distance >= getMinRange() && distance <= getMaxRange();
    }

    private double calculateDistance(double x, double y, double z) {
        Vec3 playerEyes = mc.thePlayer.getPositionEyes(mc.timer.renderPartialTicks);
        return playerEyes.distanceTo(new Vec3(x, y, z));
    }

    private void stop() {
        synchronized (held) {
            holding = false;
            flush = false;
            releaseLocked(true);
            targetId = -1;
            realPosition = null;
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
        synchronized (held) {
            holding = false;
            flush = false;
            held.clear();
            targetId = -1;
            realPosition = null;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        synchronized (held) {
            holding = false;
            flush = false;
            held.clear();
            targetId = -1;
            realPosition = null;
        }
        connection = mc.getNetHandler();
        timer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stop();
    }
}