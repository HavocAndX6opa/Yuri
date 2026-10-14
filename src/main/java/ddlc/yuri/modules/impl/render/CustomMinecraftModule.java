package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(label = "Custom Minecraft", description = "Uses Yuri themed UI rather than default vanilla", category = ModuleCategory.RENDER, enabledByDefault = true)
public class CustomMinecraftModule extends Module {

    public static Property<Boolean> chat = new Property<>("Chat", true);
    // default was the dirt bg
    public static Property<Boolean> shaderGuiBG = new Property<>("Shader GUI BG", true);
    public static Property<Boolean> guiButtons = new Property<>("GUI Buttons", true);
    // in the guis not for the hotbar
    public static Property<Boolean> itemToolTips = new Property<>("Item Tooltips", true);
    public static Property<Boolean> s45PacketTitle = new Property<>("S45 Packet Titles", true);
    // the xp level number drawn over the exp bar above the hotbar
    public static Property<Boolean> xpBarText = new Property<>("Hotbar XP Font", true);
    // the held item name popup that fades in above the hotbar when you switch slots
    public static Property<Boolean> heldItemName = new Property<>("Held Item Name", true);
    public static Property<Boolean> bossBarText = new Property<>("Boss Bar Text", true);
    // the "Recording..." stream indicator
    public static Property<Boolean> streamIndicator = new Property<>("Stream Indicator", true);
    // "Yuri Client Paused..." over the escape menu
    public static Property<Boolean> pausedScreenText = new Property<>("Paused Screen Text", true);
    // potion name + duration listed in container guis
    public static Property<Boolean> statusEffects = new Property<>("Status Effects", true);

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