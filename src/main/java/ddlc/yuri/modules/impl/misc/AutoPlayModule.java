package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;

@ModuleInfo(
        label = "Auto Play",
        description = "Plays a new game when the current one is finished",
        category = ModuleCategory.MISC)
public final class AutoPlayModule extends Module {

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        Packet<?> packet = event.getPacket();

        if (packet instanceof S02PacketChat) {
            S02PacketChat chat = ((S02PacketChat) packet);
            if (chat.isChat()) return;

            if (chat.getChatComponent().getUnformattedText().contains("You were spawned in Limbo.")) {
                mc.thePlayer.sendChatMessage("/lobby");
            }

            if (chat.getChatComponent().getFormattedText().contains("play again?")) {
                for (IChatComponent iChatComponent : chat.getChatComponent().getSiblings()) {
                    for (String value : iChatComponent.toString().split("'")) {
                        if (value.startsWith("/play") && !value.contains(".")) {
                            mc.thePlayer.sendChatMessage(value);
                            break;
                        }
                    }
                }
            }
        }
    }
}