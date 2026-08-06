package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.JumpEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.TargetManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.movement.FlightModule;
import ddlc.yuri.modules.impl.movement.SpeedModule;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.player.BlockUtils;
import ddlc.yuri.utils.player.MoveUtils;
import ddlc.yuri.utils.player.RotationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vector3d;
import java.util.List;

@ModuleInfo(label = "Target Strafe", category = ModuleCategory.COMBAT, description = "Automatically strafes around your target")
public final class TargetStrafeModule extends Module {

    private final NumberProperty range = new NumberProperty("Range", 1, 0.2, 6, 0.1);
    public final Property<Boolean> holdJump = new Property<>("Hold Jump", false);

    private float yaw;
    private Entity target;
    private boolean left, colliding;
    private boolean active;

    @EventHook
    public void onJump(JumpEvent event) {
        if (target != null && active) {
            event.setYaw(yaw);
        }
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {
        if (target != null && active) {
            event.setYaw(yaw);
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {

        setSuffix(String.valueOf(range.getValue()));

        ScaffoldModule scaffold = Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class);
        AuraModule killaura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);

        if (scaffold == null || scaffold.isEnabled() || killaura == null || !killaura.isEnabled()) {
            active = false;
            return;
        }

        active = true;

        /*
         * Getting targets and selecting the nearest one
         */
        Module speed = Yuri.INSTANCE.getModuleManager().getModule(SpeedModule.class);
        Module flight = Yuri.INSTANCE.getModuleManager().getModule(FlightModule.class);

        if (holdJump.getValue() && !mc.gameSettings.keyBindJump.isKeyDown() || !(mc.gameSettings.keyBindForward.isKeyDown() && (flight != null && flight.isEnabled() || speed != null && speed.isEnabled()))) {
            target = null;
            return;
        }

        final List<Entity> targets = TargetManager.getTargetList();

        if (targets.isEmpty()) {
            target = null;
            return;
        }

        if (mc.thePlayer.isCollidedHorizontally || !BlockUtils.isBlockUnder(5, false)) {
            if (!colliding) {
                MoveUtils.strafe();
                left = !left;
            }
            colliding = true;
        } else {
            colliding = false;
        }

        target = targets.get(0);

        if (target == null) {
            return;
        }

        float yaw = RotationUtils.calculate(target).getX() + (90 + 45) * (left ? -1 : 1);

        final double range = this.range.getValue() + Math.random() / 100f;
        final double posX = -MathHelper.sin((float) Math.toRadians(yaw)) * range + target.posX;
        final double posZ = MathHelper.cos((float) Math.toRadians(yaw)) * range + target.posZ;

        yaw = RotationUtils.calculate(new Vector3d(posX, target.posY, posZ)).getX();

        this.yaw = yaw;
        mc.thePlayer.movementYaw = this.yaw;
    }
}
