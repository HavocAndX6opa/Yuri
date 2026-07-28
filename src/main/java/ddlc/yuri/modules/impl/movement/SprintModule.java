package ddlc.yuri.modules.impl.movement;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.IMinecraft;

@ModuleInfo(label = "Sprint",
        category = ModuleCategory.MOVEMENT,
        description = "Automatically sprints for you"
)
public class SprintModule extends Module implements IMinecraft {
    private final Property<Boolean> cancelInvis = new Property<>("Cancel Invis", false);

    @EventHook
    public void onTick(ClientTickEvent event) {
        if (cancelInvis.getValue() && mc.thePlayer.isInvisible()) {
            return;
        }

        mc.gameSettings.keyBindSprint.pressed = true;
    }

    @Override
    public void onDisable() {
        mc.gameSettings.keyBindSprint.pressed = false;
        super.onDisable();
    }
}
