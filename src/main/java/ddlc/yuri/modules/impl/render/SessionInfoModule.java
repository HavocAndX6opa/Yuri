package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.KillEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.DragUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;

@ModuleInfo(label = "Session Info", description = "Displays session information on the screen.", category = ModuleCategory.RENDER)
public class SessionInfoModule extends Module implements IMinecraft {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.PULSIVE);

    private enum Mode {
        PULSIVE("Pulsive"),
        YURI("Yuri");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private static final String KEY = "SessionInfo";
    private static final float PADDING_X = 14f;
    private static final float MIN_WIDTH = 145f;
    private static final float PADDING_Y = 8f;
    private static final float RADIUS = 6f;
    private static final float HEADER_PADDING_Y = 6f;
    private static final float GAP_HEADER_TIME = 8f;
    private static final float GAP_TIME = 3f;
    private static final float GAP_LINE = 2f;

    private final DragUtils.DraggableComponent component = new DragUtils.DraggableComponent(20, 20);

    private static long sessionStart;
    private static int kills;
    private static int gamesWon;

    public SessionInfoModule() {
        DragUtils.registerComponent(KEY, component);
    }

    @Override
    public void onEnable() {
        sessionStart = System.currentTimeMillis();
        kills = 0;
        gamesWon = 0;
        component.setWidth(0);
        component.setHeight(0);
    }

    @Override
    public void onDisable() {
        component.setWidth(0);
        component.setHeight(0);
    }

    public static void addWin() {
        gamesWon++;
    }

    @EventHook
    public void onKill(KillEvent event) {
        kills++;
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        switch (mode.getValue()) {
            case PULSIVE:
                renderPulsive();
                break;
            case YURI:
                renderYuri();
                break;
        }
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        switch (mode.getValue()) {
            case PULSIVE:
                renderPulsive();
                break;
            case YURI:
                renderYuri();
                break;
        }
    }

    public void renderPulsive() {
        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer body = FontUtils.getFont("sf", 16);
        CustomFontRenderer timeFont = FontUtils.getFont("sf-bold", 24); // Scaled down slightly to fit narrower width
        if (bold == null || regular == null || body == null || timeFont == null) return;

        String sessionWord = "session";
        String infoWord = "information";
        String timeText = formatTime(System.currentTimeMillis() - sessionStart);
        String killsText = "You have gotten " + kills + " kills";
        String winsText = "Games won " + gamesWon + " times";

        float sessionWidth = bold.getStringWidth(sessionWord);
        float infoWidth = regular.getStringWidth(infoWord);
        float titleWidth = sessionWidth + infoWidth;
        float titleHeight = Math.max(bold.getHeight(), regular.getHeight());

        float timeWidth = timeFont.getStringWidth(timeText);
        float timeHeight = timeFont.getHeight();

        float killsWidth = body.getStringWidth(killsText);
        float winsWidth = body.getStringWidth(winsText);
        float lineHeight = body.getHeight();

        float contentWidth = Math.max(titleWidth, Math.max(timeWidth, Math.max(killsWidth, winsWidth)));

        float headerHeight = titleHeight + HEADER_PADDING_Y * 2;
        float bodyContentHeight = timeHeight + GAP_TIME + lineHeight + GAP_LINE + lineHeight;

        float width = Math.max(MIN_WIDTH, contentWidth + PADDING_X * 2);
        float bodyHeight = GAP_HEADER_TIME + bodyContentHeight + PADDING_Y;
        float totalHeight = headerHeight + bodyHeight;

        component.setWidth(width);
        component.setHeight(totalHeight);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - totalHeight;

        Color headerColor = new Color(40, 40, 44, 100);
        Color bodyColor = new Color(18, 18, 20, 150);

        RoundedUtils.drawCustomRoundedRect(x, y, width, headerHeight, RADIUS,
                true, true, false, false, headerColor);

        float seamFixOffset = 1.35f;
        RoundedUtils.drawCustomRoundedRect(x, y + headerHeight + seamFixOffset, width, bodyHeight + seamFixOffset, RADIUS,
                false, false, true, true, bodyColor);

        float cx = x + width / 2f;
        float titleX = cx - titleWidth / 2f;
        float titleY = y + HEADER_PADDING_Y;
        bold.drawStringWithShadow(sessionWord, titleX, titleY, Color.WHITE.getRGB());
        regular.drawStringWithShadow(infoWord, titleX + sessionWidth, titleY, new Color(220, 220, 220).getRGB());

        float leftX = x + PADDING_X;
        float cursorY = y + headerHeight + GAP_HEADER_TIME;

        timeFont.drawStringWithShadow(timeText, leftX, cursorY, Color.WHITE.getRGB());
        cursorY += timeHeight + GAP_TIME;

        body.drawStringWithShadow(killsText, leftX, cursorY, new Color(190, 190, 190).getRGB());
        cursorY += lineHeight + GAP_LINE;

        body.drawStringWithShadow(winsText, leftX, cursorY, new Color(190, 190, 190).getRGB());
    }

    public void renderYuri() {

    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }
}