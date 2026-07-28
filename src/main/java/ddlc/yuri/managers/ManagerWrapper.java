package ddlc.yuri.managers;

import ddlc.yuri.api.events.EventBus;
import ddlc.yuri.managers.impl.*;

public class ManagerWrapper {
    private static RotationManager rotationManager;
    private static ColorManager colorManager;
    private static CommandManager commandManager;
    private static TargetManager targetManager;
    private static BlinkManager blinkManager;
    private static BadPacketsManager badPacketsManager;

    public static void init() {
        rotationManager = new RotationManager();
        colorManager = new ColorManager();
        commandManager = new CommandManager();
        targetManager = new TargetManager();
        blinkManager = new BlinkManager();
        badPacketsManager = new BadPacketsManager();
    }

    public static void subscribe(EventBus eventBus) {
        eventBus.subscribe(rotationManager);
        eventBus.subscribe(colorManager);
        eventBus.subscribe(commandManager);
        eventBus.subscribe(targetManager);
        eventBus.subscribe(blinkManager);
        eventBus.subscribe(badPacketsManager);
    }
}
