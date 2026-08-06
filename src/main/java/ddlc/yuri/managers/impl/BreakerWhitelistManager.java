package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.utils.misc.IMinecraft;
import net.minecraft.block.BlockBed;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;

public class BreakerWhitelistManager implements IMinecraft {
    private static final ArrayList<BlockPos> whitelisted = new ArrayList<>();
    private boolean check = true;

    public static boolean isWhitelisted(BlockPos pos) {
        for (BlockPos bp : whitelisted) {
            if (bp.distanceTo(pos) < 1.5) {
                return true;
            }
        }
        return false;
    }

    @EventHook
    private void onPlayerTick(PreUpdateEvent event) {
        if (check) {
            BlockPos bed = findBed(16);

            if (bed != null) {
                whitelisted.clear();
                whitelisted.add(bed);

                for (EnumFacing facing : EnumFacing.VALUES) {
                    whitelisted.add(bed.offset(facing));
                }

                check = false;
            }
        }
    }

    @EventHook
    private void onPacketReceive(PacketReceivedEvent event) {
        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat s02PacketChat = (S02PacketChat) event.getPacket();
            if (s02PacketChat.getChatComponent().getUnformattedText().contains("Protect your bed")) {
                whitelisted.clear();
                check = true;
            }
        }
    }

    private BlockPos findBed(float distance) {
        BlockPos bedPos = null;

        for (float x = -distance; x < distance; x++) {
            for (float z = -distance; z < distance; z++) {
                for (float y = -distance; y < distance; y++) {
                    BlockPos cPos = new BlockPos(x + mc.thePlayer.posX, y + mc.thePlayer.posY, z + mc.thePlayer.posZ);
                    if (mc.theWorld.getBlockState(cPos).getBlock() instanceof BlockBed) {
                        bedPos = cPos;
                    }
                }
            }
        }

        return bedPos;
    }

}
