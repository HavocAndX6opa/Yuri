package ddlc.yuri;

import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.api.events.EventBus;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.GameStartupEvent;
import ddlc.yuri.api.events.impl.client.GameStoppingEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.gui.click.atmosphere.AtmosphereClickGui;
import ddlc.yuri.api.gui.click.csgo.CsgoClickGui;
import ddlc.yuri.api.gui.click.imgui.ImGuiClickGui;
import ddlc.yuri.api.gui.click.sigma.SigmaClickGui;
import ddlc.yuri.managers.ManagerWrapper;
import ddlc.yuri.modules.ModuleManager;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.misc.NotificationHandler;
import ddlc.yuri.utils.render.DragUtils;
import lombok.Getter;

public class Yuri {
    public static final Yuri INSTANCE = new Yuri();
    public static final String NAME = "Yuri";
    public static final String BUILD = "Beta";
    public static final String VERSION = "1.3.0";
    public static final String FULL = NAME + " " + VERSION + " " + BUILD;

    private EventBus eventBus;
    @Getter
    private ModuleManager moduleManager;
    @Getter
    private ConfigManager configManager;
    @Getter
    private final AtmosphereClickGui atmosphereClickGui = new AtmosphereClickGui();
    @Getter
    private final ImGuiClickGui imGuiClickGui = new ImGuiClickGui();
    @Getter
    private final CsgoClickGui csgoClickGui = new CsgoClickGui();
    @Getter
    private final SigmaClickGui sigmaClickGui = new SigmaClickGui();
    @Getter
    private NotificationHandler notificationHandler = new NotificationHandler();

    private Yuri() {
        getEventBus().subscribe(this);
    }

    @EventHook
    public void onGameStartup(GameStartupEvent event) {
        moduleManager = new ModuleManager();
        moduleManager.postInit();
        configManager = new ConfigManager();
        configManager.loadConfig(ConfigManager.DEFAULT_PROFILE);
        configManager.loadVisuals(ConfigManager.DEFAULT_PROFILE);
        configManager.getBindsConfig().loadFromFile();
        ManagerWrapper.init();
        ManagerWrapper.subscribe(getEventBus());
        if (getModuleManager().getModule(ClickGUIModule.class).isEnabled()) getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
    }

    public EventBus getEventBus() {
        if (eventBus == null) {
            eventBus = new EventBus();
        }

        return eventBus;
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        DragUtils.update();
    }

    @EventHook
    public void onGameStopping(GameStoppingEvent event) {
        configManager.saveConfig(ConfigManager.DEFAULT_PROFILE);
        configManager.saveVisuals(ConfigManager.DEFAULT_PROFILE);
        configManager.getBindsConfig().saveToFile();
        System.out.println("Autosaved modules and draggable positions.");
    }
}
