package ddlc.yuri.modules.impl.client;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.IMinecraft;
import org.lwjgl.input.Keyboard;

@ModuleInfo(label = "ClickGUI", category = ModuleCategory.RENDER, key = Keyboard.KEY_RSHIFT, description = "Opens the click GUI")
public class ClickGUIModule extends Module implements IMinecraft {

    public static final ModeProperty<Color> color = new ModeProperty<>("Color", Color.YURI);
    private final Property<Boolean> closePrevious = new Property<>("Close Previous", true);

    public enum Color {
        YURI("Yuri"),
        NOVOLINE("Novoline"),
        RAINBOW("Rainbow"),
        ASTOLFO("Astolfo"),
        TENACITY("Tenacity"),
        SUNSET("Sunset"),
        AMETHYST("Amethyst"),
        ROYAL("Royal"),
        LAVENDER("Lavender"),
        AZURE("Azure"),
        INDIGO("Indigo"),
        OCEAN("Ocean"),
        CRYSTAL("Crystal"),
        PETAL("Petal"),
        CITRUS("Citrus"),
        EVERGREEN("Evergreen"),
        LEMON("Lemon"),
        EMBER("Ember"),
        CRIMSON("Crimson"),
        ICE("Ice"),
        GRAPHITE("Graphite");

        public final String name;

        Color(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public Property<Boolean> getClosePrevious() {
        return closePrevious;
    }

    @Override
    public void onEnable() {
        mc.displayGuiScreen(Yuri.INSTANCE.getClickGui());
    }

    @Override
    public void onDisable() {
        if (mc.currentScreen == Yuri.INSTANCE.getClickGui() && !Yuri.INSTANCE.getClickGui().isClosing()) {
            Yuri.INSTANCE.getClickGui().beginClose();
        }
    }
}