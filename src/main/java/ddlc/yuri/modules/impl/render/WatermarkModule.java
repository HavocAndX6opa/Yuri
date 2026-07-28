package ddlc.yuri.modules.impl.render;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.ClientInfoUtils;
import ddlc.yuri.utils.client.NetworkUtils;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@ModuleInfo(label = "Watermark", category = ModuleCategory.RENDER, description = "Renders the client watermark on your screen")
public class WatermarkModule extends Module implements IMinecraft {
    public static final ModeProperty<Type> type = new ModeProperty<>("Type", Type.YURISENSE);
    public static final Property<String> name = new Property<>("Client Name", "Yuri");
    public static final Property<Boolean> protocol = new Property<>("Protocol", true, () -> type.getValue() == Type.CLASSIC);
    public static final Property<Boolean> time = new Property<>("Time", true, () -> type.getValue() == Type.CLASSIC);
    public static final Property<Boolean> fps = new Property<>("FPS", true, () -> type.getValue() == Type.CLASSIC);
    public static final Property<Boolean> ping = new Property<>("Ping", true, () -> type.getValue() == Type.CLASSIC);
    public static final Property<Boolean> tps = new Property<>("TPS", true, () -> type.getValue() == Type.CLASSIC);

    public enum Type {
        YURISENSE("Yurisense"),
        VIRTUE("Virtue"),
        SIMPLE("Simple"),
        CLASSIC("Classic"),
        LOGO("Logo");

        public final String name;

        Type(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        ScaledResolution sr = new ScaledResolution(mc);
        int main = ColorManager.getColor().getRGB();
        int white = 0xFFFFFFFF;
        switch (type.getValue()) {
            case CLASSIC:
                StringBuilder infoBuilder = new StringBuilder();
                if (protocol.getValue())
                    infoBuilder.append(" \u00a77[\u00a7r").append(ClientInfoUtils.getServerProtocol()).append("\u00a77]\u00a7f");
                if (time.getValue()) infoBuilder.append(" \u00a77[\u00a7r").append(getTime()).append("\u00a77]\u00a7f");
                if (fps.getValue())
                    infoBuilder.append(" \u00a77[\u00a7r").append(Minecraft.getDebugFPS()).append(" FPS\u00a77]\u00a7f");
                if (ping.getValue())
                    infoBuilder.append(" \u00a77[\u00a7r").append(ClientInfoUtils.getPing()).append("ms\u00a77]\u00a7f");
                if (tps.getValue()) infoBuilder.append(" \u00a77[\u00a7r").append("20").append("\u00a77]\u00a7f");

                String prefix = "\u00a7l" + name.getValue().charAt(0);
                String rest = new ChatComponentText(name.getValue().substring(1) + infoBuilder.toString()
                ).getFormattedText();

                mc.fontRendererObj.drawStringWithShadow(prefix, 2, 2, main);
                float preWidth = mc.fontRendererObj.getStringWidth(prefix + "\u00a7r" + name.getValue().charAt(1))
                        - mc.fontRendererObj.getStringWidth(prefix)
                        - mc.fontRendererObj.getStringWidth(name.getValue().substring(1, 2));
                mc.fontRendererObj.drawStringWithShadow(
                        rest,
                        2 + mc.fontRendererObj.getStringWidth(prefix) + preWidth,
                        2,
                        white
                );
                break;
            case VIRTUE:
                RenderUtils.drawBorderedRect(2.0f, 2.0f, 60.0f, 34.0f, 1.0f, -1603704471, -16777216, true, true, true, true);

                mc.fontRendererObj.drawString(name.getValue(), (29 - mc.fontRendererObj.getStringWidth(name.getValue()) / 2f + 2), 4.0f,
                        -4210753, true);
                mc.fontRendererObj.drawString(getTime(), (29 - mc.fontRendererObj.getStringWidth(getTime()) / 2f + 2), 15.0f,
                        -4210753, true);
                mc.fontRendererObj.drawString("Fps: " + Minecraft.getDebugFPS(), (29 - mc.fontRendererObj.getStringWidth("Fps: " + Minecraft.getDebugFPS()) / 2f + 2), 26.0f,
                        -4210753, true);
                break;
            case SIMPLE:
                String a = "\u00a7l" + name.getValue().charAt(0);
                String b = new ChatComponentText(name.getValue().substring(1)).getFormattedText();
                FontUtils.getFont("sf", 18).drawStringWithShadow(a, 2, 2, main);
                float prefixWidth = FontUtils.getFont("sf", 18).getStringWidth(a + "\u00a7r" + name.getValue().charAt(1))
                        - FontUtils.getFont("sf", 18).getStringWidth(a)
                        - FontUtils.getFont("sf", 18).getStringWidth(name.getValue().substring(1, 2));
                FontUtils.getFont("sf", 18).drawStringWithShadow(
                        b,
                        2 + FontUtils.getFont("sf", 18).getStringWidth(a) + prefixWidth,
                        2,
                        white);
                FontUtils.getFont("sf", 14).drawStringWithShadow("dev", FontUtils.getFont("sf", 14).getStringWidth("dev") + FontUtils.getFont("sf", 18).getStringWidth(a) + prefixWidth, FontUtils.getFont("sf", 18).getHeight(), white);
                break;
            case LOGO:
                RenderUtils.drawImage(
                        new ResourceLocation("yuri/gui/logo.png"),
                        -32,
                        -6,
                        128,
                        128
                );
                break;
            case YURISENSE:
                String server = mc.isSingleplayer()
                        ? "singleplayer"
                        : (mc.getCurrentServerData() != null
                           ? mc.getCurrentServerData().serverIP
                           : "unknown");

                String text = "yurisense - "
                        + mc.thePlayer.getName()
                        + " - "
                        + server
                        + " - "
                        + ClientInfoUtils.getPing()
                        + "ms";

                float x = 4.5f;
                float y = 4.5f;

                float textWidth = FontUtils.getFont("sf", 18).getStringWidth(text) + 2;

                int lineColor = new Color(59, 57, 57).darker().getRGB();

                Gui.drawRect2(x, y, textWidth + 7, 18.5, new Color(59, 57, 57).getRGB());

                Gui.drawRect2(x + 2.5, y + 2.5, textWidth + 2, 13, new Color(23, 23, 23).getRGB());

                // Top small bar
                Gui.drawRect2(x + 1, y + 1, textWidth + 5, .5, lineColor);

                // Bottom small bar
                Gui.drawRect2(x + 1, y + 17, textWidth + 5, .5, lineColor);

                // Left bar
                Gui.drawRect2(x + 1, y + 1.5, .5, 16, lineColor);

                // Right Bar
                Gui.drawRect2((x + 1.5) + textWidth, y + 1.5, .5, 16, lineColor);

                // Accent bar
                RenderUtils.drawGradientRect((int) (x + 2.5f), (int) (y + 14.5f), (int) (x + textWidth + 4.5f), (int) (y + 15.5f), true, ColorManager.getColors().getFirst().getRGB(), ColorManager.getColors().getSecond().getRGB());

                // Bottom of the rainbow bar
                Gui.drawRect2(x + 2.5, y + 16, textWidth + 2, .5, lineColor);

                FontUtils.getFont("sf", 18).drawStringWithShadow("yuri", x + 4.5f, y + 4.3f, main);

                FontUtils.getFont("sf", 18).drawStringWithShadow("sense - "
                                + mc.thePlayer.getName()
                                + " - "
                                + server
                                + " - "
                                + ClientInfoUtils.getPing()
                                + "ms",
                        x + 4.5f + FontUtils.getFont("sf", 18).getStringWidth("yuri"),
                        y + 4.3f,
                        white
                );
                break;
        }
    }

    private String getTime() {
        String time = SimpleDateFormat.getDateInstance().format(new Date());
        if (time.startsWith("0"))
            time = time.replaceFirst("0", "");
        return time;
    }
}
