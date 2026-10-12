package ddlc.yuri.managers.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.client.ModuleEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.modules.impl.misc.ToggleSoundsModule;
import ddlc.yuri.utils.client.SoundUtils;
import ddlc.yuri.utils.render.notifications.NotificationRenderer;

public class NotificationManager {

    @EventHook(value = EventPriority.VERY_HIGH)
    public void onRender(Render2DEvent e) {
        NotificationRenderer.update();
        NotificationRenderer.draw();
    }

    @EventHook(value = EventPriority.VERY_HIGH)
    public void onShader(Shader2DEvent e) {
        NotificationRenderer.draw();
    }

    @EventHook
    public void onModule(ModuleEvent e) {
        boolean enabled = e.getModule().isEnabled();

        ToggleSoundsModule toggleSounds = Yuri.INSTANCE.getModuleManager() != null ? Yuri.INSTANCE.getModuleManager().getModule(ToggleSoundsModule.class) : null;
        if (toggleSounds != null && (toggleSounds.isEnabled() || e.getModule() instanceof ToggleSoundsModule)) {
            float volume = toggleSounds.volume != null ? toggleSounds.volume.getValue().floatValue() : 1.0f;
            switch (toggleSounds.moduleToggleSounds.getValue()) {
                case EVISCERATE:
                    if (enabled) {
                        SoundUtils.playSound("eviscerate-enable.ogg", volume);
                    } else {
                        SoundUtils.playSound("eviscerate-disable.ogg", volume);
                    }
                    break;
                case YURI:
                    if (enabled) {
                        SoundUtils.playSound("yuri-enable.ogg", volume);
                    } else {
                        SoundUtils.playSound("yuri-disable.ogg", volume);
                    }
                    break;
                case SPOOKY:
                    if (enabled) {
                        SoundUtils.playSound("spooky-enable.ogg", volume);
                    } else {
                        SoundUtils.playSound("spooky-disable.ogg", volume);
                    }
                    break;
                case NURSULTAN:
                    if (enabled) {
                        SoundUtils.playSound("nursultan-enable.ogg", volume);
                    } else {
                        SoundUtils.playSound("nursultan-disable.ogg", volume);
                    }
                    break;
                case AUGUSTUS:
                    if (enabled) {
                        SoundUtils.playSound("augustus-enable.ogg", volume);
                    } else {
                        SoundUtils.playSound("augustus-disable.ogg", volume);
                    }
                    break;
                case MINECRAFT:
                    SoundUtils.playSound("minecraft-toggle.ogg", volume);
                    break;
                case SMOOTH:
                    if (enabled) {
                        SoundUtils.playSound("smooth-enable.ogg", volume);
                    } else {
                        SoundUtils.playSound("smooth-disable.ogg", volume);
                    }
                    break;
                case HANABI:
                    if (enabled) {
                        SoundUtils.playSound("hanabi-enable.ogg", volume);
                    } else {
                        SoundUtils.playSound("hanabi-disable.ogg", volume);
                    }
                    break;
                case SIGMA:
                    if (enabled) {
                        SoundUtils.playSound("sigma-enable.ogg", volume);
                    } else {
                        SoundUtils.playSound("sigma-disable.ogg", volume);
                    }
                    break;
            }
        }
    }
}
