package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

@ModuleInfo(label = "Input Fixes", description = "Fixes some Minecraft input bugs", category = ModuleCategory.MISC)
public final class InputFixesModule extends Module {

    private boolean lastInScreen, toPress;

    @Override
    public void onEnable() {
        lastInScreen = mc.currentScreen != null;
        toPress = false;
    }

    @Override
    public void onDisable() {
        toPress = false;
    }
    @EventHook
    public void onTick(ClientTickEvent event) {
        if (mc == null || mc.gameSettings == null) return;

        boolean curInScreen = mc.currentScreen != null;

        if (lastInScreen && !curInScreen && !toPress) {
            toPress = true;
        }

        if (toPress) {
                KeyBinding[] bindings = new KeyBinding[] {
                        mc.gameSettings.keyBindForward,
                        mc.gameSettings.keyBindLeft,
                        mc.gameSettings.keyBindBack,
                        mc.gameSettings.keyBindRight,
                        mc.gameSettings.keyBindJump,
                        mc.gameSettings.keyBindSneak
                };

                for (KeyBinding binding : bindings) {
                    if (binding != null) {
                        boolean isPhysicallyPressed = Keyboard.isKeyDown(binding.getKeyCode());
                        binding.setPressed(isPhysicallyPressed);
                    }
                }

                toPress = false;
        }
        lastInScreen = curInScreen;
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if(mc.inGameHasFocus) mc.leftClickCounter = 0;
    }
}
