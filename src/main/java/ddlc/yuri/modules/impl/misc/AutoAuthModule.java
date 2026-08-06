package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.network.play.server.S02PacketChat;

@ModuleInfo(label = "Auto Auth", description = "Automatically authenticate on cracked servers", category = ModuleCategory.MISC)
public final class AutoAuthModule extends Module {

    private static final String[] REGISTER_KEYWORDS = {
            "/register",    // English
            "/reg",         // Short English
            "/registrar",   // Spanish/Portuguese
            "/зарег",       // Russian
            "/rejestracja", // Polish
            "/cadastrar",   // Portuguese
            "/kayit",       // Turkish
            "/enregistrer"  // French
    };

    private final Property<String> password = new Property<>("Password", "yuri420");
    private final Property<Boolean> doublePassword = new Property<>("Double Password", true);

    @EventHook
    public void onPacket(PacketReceivedEvent event) {
        if (mc.thePlayer == null || !(event.getPacket() instanceof S02PacketChat))
            return;

        S02PacketChat packetChat = (S02PacketChat) event.getPacket();
        String chatComponent = packetChat.getChatComponent().getUnformattedText().toLowerCase();

        String passwordMessage;

        if (doublePassword.getValue()) {
            passwordMessage = password.getValue() + " " + password.getValue();
        } else {
            passwordMessage = password.getValue();
        }

        for (String keyword : REGISTER_KEYWORDS) {
            if (chatComponent.contains(keyword.toLowerCase())) {
                mc.thePlayer.sendChatMessage(keyword + " " + passwordMessage);
                break;
            }
        }
    }
}
