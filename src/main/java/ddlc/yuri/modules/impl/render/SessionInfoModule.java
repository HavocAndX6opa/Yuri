package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.KillEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.DragUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.StringUtils;

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

    private static final Color BG_COLOR = new Color(40, 40, 44, 220);
    private static final Color HEADER_COLOR = new Color(40, 40, 44, 100);
    private static final Color BODY_COLOR = new Color(18, 18, 20, 150);

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

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        if (event.getPacket() instanceof S45PacketTitle) {
            S45PacketTitle s45 = (S45PacketTitle) event.getPacket();
            if (s45.getMessage() == null) return;

            if (StringUtils.stripControlCodes(s45.getMessage().getUnformattedText()).equals("VICTORY!")) {
                addWin();
            }
        }
    }

    public void renderPulsive() {
        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer body = FontUtils.getFont("sf", 16);
        CustomFontRenderer timeFont = FontUtils.getFont("sf-bold", 24);
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

        RoundedUtils.drawCustomRoundedRect(x, y, width, headerHeight, RADIUS,
                true, true, false, false, HEADER_COLOR);

        float seamFixOffset = 1.35f;
        RoundedUtils.drawCustomRoundedRect(x, y + headerHeight + seamFixOffset, width, bodyHeight + seamFixOffset, RADIUS,
                false, false, true, true, BODY_COLOR);

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
        CustomFontRenderer title = FontUtils.getFont("sf-bold", 20);
        CustomFontRenderer welcome = FontUtils.getFont("sf-bold", 22);
        CustomFontRenderer body = FontUtils.getFont("sf", 16);
        if (title == null || welcome == null || body == null) return;

        String titleText = "Session Info";
        String welcomeText = "Welcome, " + mc.getSession().getUsername() + "!";
        String singleplayerText = "No stats to render.";
        String killsText = "You have " + kills + " kills.";
        String timeText = "You have been playing for " + formatTimeYuri(System.currentTimeMillis() - sessionStart) + ".";
        String serverText = "Server: " + getServerName();

        float titleWidth = title.getStringWidth(titleText);
        float welcomeWidth = welcome.getStringWidth(welcomeText);
        float singleplayerWidth = body.getStringWidth(singleplayerText);
        float killsWidth = body.getStringWidth(killsText);
        float timeWidth = body.getStringWidth(timeText);
        float serverWidth = body.getStringWidth(serverText);

        float contentWidth = Math.max(titleWidth, Math.max(welcomeWidth, Math.max(killsWidth, Math.max(timeWidth, serverWidth))));
        float width = Math.max(MIN_WIDTH, contentWidth + PADDING_X * 2);

        float titleHeight = title.getHeight();
        float welcomeHeight = welcome.getHeight();
        float lineHeight = body.getHeight();

        float gapTitleWelcome = 10f;
        float gapWelcomeKills = 8f;
        float gapLine = 6f;
        float gapKillsServer = 14f;

        float height = PADDING_Y * 2 + titleHeight + gapTitleWelcome + welcomeHeight + gapWelcomeKills
                + lineHeight + gapLine + lineHeight + gapKillsServer + lineHeight;

        component.setWidth(width);
        component.setHeight(height);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - height;

        RoundedUtils.drawRoundOutline(x, y, width, height, RADIUS, 0.2f, BG_COLOR,
                ColorManager.getColor());

        float cx = x + width / 2f;
        float cursorY = y + PADDING_Y;

        title.drawStringWithShadow(titleText, cx - titleWidth / 2f, cursorY, Color.WHITE.getRGB());
        cursorY += getServerName().equals("Singleplayer") ? titleHeight + gapTitleWelcome + 10f : titleHeight + gapTitleWelcome;

        welcome.drawStringWithShadow(welcomeText, cx - welcomeWidth / 2f, cursorY, Color.WHITE.getRGB());
        cursorY += welcomeHeight + gapWelcomeKills;

        if (getServerName().equals("Singleplayer")) {
            body.drawStringWithShadow(singleplayerText, cx - singleplayerWidth / 2f, cursorY, new Color(220, 220, 220).getRGB());
        } else {
            body.drawStringWithShadow(killsText, cx - killsWidth / 2f, cursorY, new Color(220, 220, 220).getRGB());
            cursorY += lineHeight + gapLine;

            body.drawStringWithShadow(timeText, cx - timeWidth / 2f, cursorY, new Color(220, 220, 220).getRGB());
            cursorY += lineHeight + gapKillsServer;

            body.drawStringWithShadow(serverText, cx - serverWidth / 2f, cursorY, new Color(220, 220, 220).getRGB());
        }
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    private String formatTimeYuri(long millis) {
        long totalMinutes = millis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours == 0 && minutes == 0) {
            return "less than a minute";
        }
        if (hours == 0 && minutes > 0) {
            return minutes + " mins";
        }
        return hours + " hrs and " + minutes + " mins";
    }

    private String getServerName() {
        return mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Singleplayer";
    }
}