package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(label = "Custom Minecraft", description = "Uses Yuri themed UI rather than default vanilla", category = ModuleCategory.MISC, enabledByDefault = true)
public class CustomMinecraftModule extends Module {

    public static Property<Boolean> chat = new Property<>("Chat", true);
    // default was the dirt bg
    public static Property<Boolean> shaderGuiBG = new Property<>("Shader GUI BG", true);
    public static Property<Boolean> guiButtons = new Property<>("GUI Buttons", true);
    // in the guis not for the hotbar
    public static Property<Boolean> itemToolTips = new Property<>("Item Tooltips", true);
    public static Property<Boolean> s45PacketTitle = new Property<>("S45 Packet Titles", true);

    /**
     * Starts as true so the Yuri styling still renders while the client is booting and the
     * module manager has not been built yet (the main menu draws before GameStartupEvent).
     */
    private static boolean enabled = true;

    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    /**
     * Whether the given style should be applied. Requires the module to be toggled on
     * and the individual property to be enabled, so every style can be reverted to the
     * vanilla rendering independently.
     */
    public static boolean isStyleActive(Property<Boolean> property) {
        return enabled && property.getValue();
    }
}