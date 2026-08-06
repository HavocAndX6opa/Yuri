package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.BlinkManager;
import ddlc.yuri.managers.impl.LagManager;
import ddlc.yuri.managers.impl.TargetManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.player.MoveUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.entity.Entity;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(label = "Lag Range", description = "Causes you to lag when attacking entities when inside of a certain range", category = ModuleCategory.COMBAT)
public final class LagRangeModule extends Module {

    public static ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.LAG);
    public static NumberProperty minRange = new NumberProperty("Min Range", 3.5, 1.0, 6, 0.1);
    public static NumberProperty maxRange = new NumberProperty("Max Range", 4.2, 1.0, 6, 0.1);
    public static NumberProperty minDelay = new NumberProperty("Min Delay", 50.0, 0.0, 5000.0, 10.0, () -> mode.getValue() == Mode.LAG);
    public static NumberProperty maxDelay = new NumberProperty("Max Delay", 200.0, 0.0, 5000.0, 10.0, () -> mode.getValue() == Mode.LAG);
    private final Property<Boolean> teleports = new Property<Boolean>("Delay Teleports", true, () -> mode.getValue() == Mode.LAG);
    private final Property<Boolean> velocity = new Property<Boolean>("Delay Velocity", true, () -> mode.getValue() == Mode.LAG);
    private final Property<Boolean> entities = new Property<Boolean>("Delay Entity Movements", true, () -> mode.getValue() == Mode.LAG);
    public static final Property<Boolean> onlyWithKillAura = new Property<Boolean>("Only With Kill Aura", true);
    public static final Property<Boolean> renderLagPos = new Property<Boolean>("Render Lag Pos", true);

    public enum Mode {
        LAG("Lag"),
        BLINK("Blink");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private boolean lagging = false;
    private double blinkedX, blinkedY, blinkedZ;
    private long lagStartTime = 0;
    private int currentLagDuration = 0;
    public static List<Entity> targetList = new CopyOnWriteArrayList<>();

    @EventHook
    public void onWorldLoad(WorldJoinEvent event) {
        if (lagging) {
            disableLag();
        }
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;

        if (!MoveUtils.isMoving()) {
            if (lagging) disableLag();
            return;
        }

        Entity target = null;

        if (onlyWithKillAura.getValue()) {
            if (Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class).isEnabled()) {
                target = AuraModule.target;
            }
        } else {
           target = TargetManager.getTarget();
        }

        if (target == null) {
            if (lagging) disableLag();
            return;
        }

        double distance = mc.thePlayer.getDistanceToEntity(target);

        if (lagging) {
            if (mode.getValue() == Mode.LAG) {
                if (System.currentTimeMillis() - lagStartTime >= currentLagDuration) {
                    disableLag();
                }
            } else {
                if (distance > maxRange.getValue() || distance <= minRange.getValue()) {
                    disableLag();
                }
            }
            return;
        }

        if (distance <= maxRange.getValue() && distance > minRange.getValue()) {
            lagging = true;
            lagStartTime = System.currentTimeMillis();
            currentLagDuration = (int) MathUtils.getRandom(minDelay.getValue().intValue(), maxDelay.getValue().intValue());

            if (mode.getValue() == Mode.LAG) {
                LagManager.spoof(currentLagDuration, true, velocity.getValue(),
                        teleports.getValue(), entities.getValue());
                setSuffix(currentLagDuration + "ms");
            } else {
                blinkedX = mc.thePlayer.posX;
                blinkedY = mc.thePlayer.posY;
                blinkedZ = mc.thePlayer.posZ;
                BlinkManager.enable(false);
                setSuffix(maxRange.getValue().toString());
            }
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (!renderLagPos.getValue() || !lagging) return;
        if (mc.gameSettings.thirdPersonView == 0) return;

        if (mode.getValue() == Mode.LAG) {
            if (!LagManager.hasServerPosition) return;
            double x = LagManager.serverX - mc.getRenderManager().viewerPosX;
            double y = LagManager.serverY - mc.getRenderManager().viewerPosY;
            double z = LagManager.serverZ - mc.getRenderManager().viewerPosZ;
            RenderUtils.renderPlayerPosition(x, y, z);
        } else {
            double x = blinkedX - mc.getRenderManager().viewerPosX;
            double y = blinkedY - mc.getRenderManager().viewerPosY;
            double z = blinkedZ - mc.getRenderManager().viewerPosZ;
            RenderUtils.renderPlayerPosition(x, y, z);
        }
    }

    private void disableLag() {
        if (mode.getValue() == Mode.LAG) {
            LagManager.dispatch();
            LagManager.disable();
        } else {
            BlinkManager.disable();
        }
        lagging = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (lagging) {
            disableLag();
        }
    }
}