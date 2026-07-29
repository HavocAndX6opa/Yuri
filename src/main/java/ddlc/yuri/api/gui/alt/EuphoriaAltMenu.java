package ddlc.yuri.api.gui.alt;

import ddlc.yuri.api.gui.click.novoline.GuiTheme;
import ddlc.yuri.api.gui.alt.comp.CustomTextBox;
import ddlc.yuri.api.gui.alt.comp.MicrosoftOAuthTranslation;
import ddlc.yuri.api.gui.alt.comp.SessionChanger;
import ddlc.yuri.api.gui.alt.comp.TokenEncryption;
import ddlc.yuri.api.gui.main.YuriMenu;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class EuphoriaAltMenu extends GuiScreen {

    private static final Color BACKGROUND = new Color(20, 20, 24);
    private static final ResourceLocation PLACEHOLDER_HEAD = new ResourceLocation("euphoria/gui/steve.png");
    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM_SOURCE = new SecureRandom();

    private static final int HEADER_HEIGHT = 44;
    private static final int PADDING = 12;
    private static final int FIELD_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 26;
    private static final int BUTTON_SPACING = 8;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int COLUMNS = 3;
    private static final int ENTRY_PADDING = 6;
    private static final int ENTRY_HEIGHT = 42;
    private static final float ADD_PANEL_RATIO = 0.34f;

    private final Random random = new Random();
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final ArrayList<Integer> selectedAlts = new ArrayList<>();
    private final ArrayList<String> alts = new ArrayList<>();
    private final Map<String, ResourceLocation> headCache = new HashMap<>();
    private final Map<String, Boolean> headLoading = new HashMap<>();
    private final Map<String, Integer> headTries = new HashMap<>();

    private CustomTextBox username, password;
    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;
    private int dragStartY;
    private int scrollStart;
    private String statusString;
    private boolean isLoggingIn = false;

    private int contentY, contentHeight;
    private int addX, addY, addWidth, addHeight;
    private int accountsX, accountsY, accountsWidth, accountsHeight;
    private int accountsDividerY;

    private int primaryButtonX, primaryButtonY, primaryButtonWidth;
    private int oauthButtonX, oauthButtonY, oauthButtonWidth;
    private int generateButtonX, generateButtonY, generateButtonWidth;
    private int statusY, tipsY;
    private int backButtonWidth;

    private int gridListX, gridListY, gridListWidth, gridListHeight;
    private int gridCellWidth, gridRowStride, gridVisibleRows;
    private int scrollbarX, scrollbarY, scrollbarHeight;

    @Override
    public void initGui() {
        alts.clear();
        loadAltsFromFile();

        selectedAlts.clear();
        buttonList.clear();

        username = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        username.setPlaceholder("Username");
        password = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        password.setPlaceholder("Password");

        if (particles.isEmpty()) {
            ScaledResolution sr = new ScaledResolution(mc);
            for (int i = 0; i < 24; i++) particles.add(spawnParticle(sr, true));
        }

        super.initGui();
    }

    private void loadAltsFromFile() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Euphoria");
        File file = new File(dir, "alts.txt");
        if (!dir.exists()) dir.mkdirs();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && (line.startsWith("cracked|") || line.startsWith("microsoftOAuth|"))) {
                    alts.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAltsToFile() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Euphoria");
        File file = new File(dir, "alts.txt");

        try (PrintWriter out = new PrintWriter(file)) {
            for (String alt : alts) out.println(alt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(0, 0, width, height, BACKGROUND.getRGB());

        ScaledResolution sr = new ScaledResolution(mc);
        updateAndDrawParticles(sr);

        computeLayout();

        drawHeader(mouseX, mouseY);
        drawAddAccountPanel(mouseX, mouseY);
        drawAccountsPanel(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void computeLayout() {
        contentY = HEADER_HEIGHT + PADDING;
        contentHeight = height - contentY - PADDING;

        addWidth = (int) (width * ADD_PANEL_RATIO);
        addX = PADDING;
        addY = contentY;
        addHeight = contentHeight;

        accountsX = addX + addWidth + PADDING;
        accountsY = contentY;
        accountsWidth = width - accountsX - PADDING;
        accountsHeight = contentHeight;

        int fontHeight = FontUtils.getFont("sf", 18).getHeight();
        int fieldsStartY = addY + PADDING + fontHeight + PADDING;

        username.xPosition = addX + PADDING;
        username.yPosition = fieldsStartY;
        username.setWidth(addWidth - PADDING * 2);

        int passwordY = fieldsStartY + FIELD_HEIGHT + BUTTON_SPACING;
        password.xPosition = addX + PADDING;
        password.yPosition = passwordY;
        password.setWidth(addWidth - PADDING * 2);

        primaryButtonX = addX + PADDING;
        primaryButtonY = passwordY + FIELD_HEIGHT + PADDING;
        primaryButtonWidth = addWidth - PADDING * 2;

        int secondaryWidth = (primaryButtonWidth - BUTTON_SPACING) / 2;
        oauthButtonX = primaryButtonX;
        oauthButtonY = primaryButtonY + BUTTON_HEIGHT + BUTTON_SPACING;
        oauthButtonWidth = secondaryWidth;

        generateButtonX = oauthButtonX + secondaryWidth + BUTTON_SPACING;
        generateButtonY = oauthButtonY;
        generateButtonWidth = secondaryWidth;

        statusY = oauthButtonY + BUTTON_HEIGHT + PADDING;
        tipsY = statusY + fontHeight + PADDING * 2;

        backButtonWidth = FontUtils.getFont("sf", 18).getStringWidth("Back") + 8;

        accountsDividerY = accountsY + PADDING + fontHeight + 8;

        gridListX = accountsX + PADDING;
        gridListY = accountsDividerY + PADDING;
        gridListWidth = accountsWidth - PADDING * 2 - SCROLLBAR_WIDTH - 8;
        gridListHeight = accountsY + accountsHeight - gridListY - PADDING;

        gridCellWidth = gridListWidth / COLUMNS;
        gridRowStride = ENTRY_HEIGHT + ENTRY_PADDING;
        gridVisibleRows = Math.max(1, gridListHeight / gridRowStride);

        scrollbarX = accountsX + accountsWidth - PADDING - SCROLLBAR_WIDTH;
        scrollbarY = gridListY;
        scrollbarHeight = gridListHeight;
    }

    private void drawHeader(int mouseX, int mouseY) {
        int accent = GuiTheme.getAccent().getRGB();
        int fontHeight = FontUtils.getFont("sf", 18).getHeight();

        boolean backHovered = isMouseOverButton(mouseX, mouseY, PADDING, 0, backButtonWidth, HEADER_HEIGHT);
        FontUtils.getFont("sf", 18).drawStringWithShadow("Back", PADDING, (HEADER_HEIGHT - fontHeight) / 2, backHovered ? accent : Color.WHITE.getRGB());

        FontUtils.getFont("sf", 18).drawCenteredStringWithShadow("Account Manager", width / 2f, (HEADER_HEIGHT - fontHeight) / 2f, accent);

        String currentUser = Minecraft.getMinecraft().getSession().getUsername();
        String pillLabel = "Signed in as " + currentUser;
        int pillTextWidth = FontUtils.getFont("sf", 18).getStringWidth(pillLabel);
        int dotSize = 6;
        int pillPaddingX = 10;
        int pillHeight = 24;
        int pillWidth = dotSize + 6 + pillTextWidth + pillPaddingX * 2;
        int pillX = width - PADDING - pillWidth;
        int pillY = (HEADER_HEIGHT - pillHeight) / 2;

        Gui.drawRect(pillX, pillY, pillX + pillWidth, pillY + pillHeight, RenderUtils.withAlpha(GuiTheme.PANEL, 210));
        int dotX = pillX + pillPaddingX;
        int dotY = pillY + (pillHeight - dotSize) / 2;
        Gui.drawRect(dotX, dotY, dotX + dotSize, dotY + dotSize, accent);
        FontUtils.getFont("sf", 18).drawString(pillLabel, dotX + dotSize + 6, pillY + (pillHeight - fontHeight) / 2f, Color.WHITE.getRGB());

        Gui.drawRect(0, HEADER_HEIGHT, width, HEADER_HEIGHT + 1, RenderUtils.withAlpha(Color.WHITE, 20));
    }

    private void updateAndDrawParticles(ScaledResolution sr) {
        for (Particle particle : particles) {
            particle.y -= particle.speed;

            if (particle.y < -4) {
                Particle respawned = spawnParticle(sr, false);
                particle.x = respawned.x;
                particle.y = respawned.y;
                particle.speed = respawned.speed;
                particle.alpha = respawned.alpha;
                particle.size = respawned.size;
            }

            int alpha = (int) (particle.alpha * 255);
            Gui.drawRect(
                    (int) particle.x,
                    (int) particle.y,
                    (int) (particle.x + particle.size),
                    (int) (particle.y + particle.size),
                    new Color(255, 255, 255, alpha).getRGB()
            );
        }
    }

    private Particle spawnParticle(ScaledResolution sr, boolean randomY) {
        Particle particle = new Particle();
        particle.x = random.nextFloat() * sr.getScaledWidth();
        particle.y = randomY
                ? random.nextFloat() * sr.getScaledHeight()
                : sr.getScaledHeight() + random.nextFloat() * 20;
        particle.speed = 0.05F + random.nextFloat() * 0.15F;
        particle.alpha = 0.2F + random.nextFloat() * 0.5F;
        particle.size = 1.0F + random.nextFloat() * 1.5F;
        return particle;
    }

    private void drawPanelBackground(int x, int y, int w, int h) {
        Gui.drawRect(x - 1, y - 1, x + w + 1, y + h + 1, RenderUtils.withAlpha(GuiTheme.getAccent(), 35));
        Gui.drawRect(x, y, x + w, y + h, RenderUtils.withAlpha(GuiTheme.PANEL, 190));
    }

    private void drawAddAccountPanel(int mouseX, int mouseY) {
        drawPanelBackground(addX, addY, addWidth, addHeight);

        FontUtils.getFont("sf", 18).drawStringWithShadow("Add Account", addX + PADDING, addY + PADDING, Color.WHITE.getRGB());

        username.drawTextBox();
        password.drawTextBox();

        drawPrimaryButton(primaryButtonX, primaryButtonY, primaryButtonWidth, "Login", mouseX, mouseY);
        drawSecondaryButton(oauthButtonX, oauthButtonY, oauthButtonWidth, "Microsoft", mouseX, mouseY);
        drawSecondaryButton(generateButtonX, generateButtonY, generateButtonWidth, "Random Alt", mouseX, mouseY);

        if (statusString != null) {
            FontUtils.getFont("sf", 18).drawCenteredStringWithShadow(statusString, addX + addWidth / 2f, statusY, 0xAAAAAA);
        }

        Gui.drawRect(addX + PADDING, tipsY - 8, addX + addWidth - PADDING, tipsY - 7, RenderUtils.withAlpha(Color.WHITE, 25));
        FontUtils.getFont("sf", 18).drawString("Alt+Click select  路  Alt+A all  路  Alt+Backspace delete", addX + PADDING, tipsY, 0x888888);
    }

    private void drawPrimaryButton(int x, int y, int w, String label, int mouseX, int mouseY) {
        boolean hovered = isMouseOverButton(mouseX, mouseY, x, y, w, BUTTON_HEIGHT);
        Color accent = GuiTheme.getAccent();

        Gui.drawRect(x, y, x + w, y + BUTTON_HEIGHT, RenderUtils.withAlpha(accent, hovered ? 255 : 205));

        int textX = x + (w - FontUtils.getFont("sf", 18).getStringWidth(label)) / 2;
        int textY = y + (BUTTON_HEIGHT - FontUtils.getFont("sf", 18).getHeight()) / 2;
        FontUtils.getFont("sf", 18).drawStringWithShadow(label, textX, textY, Color.WHITE.getRGB());
    }

    private void drawSecondaryButton(int x, int y, int w, String label, int mouseX, int mouseY) {
        boolean hovered = isMouseOverButton(mouseX, mouseY, x, y, w, BUTTON_HEIGHT);
        Color accent = GuiTheme.getAccent();

        Gui.drawRect(x, y, x + w, y + BUTTON_HEIGHT, RenderUtils.withAlpha(GuiTheme.PANEL, hovered ? 255 : 190));

        int borderColor = hovered ? accent.getRGB() : RenderUtils.withAlpha(accent, 130);
        Gui.drawRect(x, y, x + w, y + 1, borderColor);
        Gui.drawRect(x, y + BUTTON_HEIGHT - 1, x + w, y + BUTTON_HEIGHT, borderColor);
        Gui.drawRect(x, y, x + 1, y + BUTTON_HEIGHT, borderColor);
        Gui.drawRect(x + w - 1, y, x + w, y + BUTTON_HEIGHT, borderColor);

        int textX = x + (w - FontUtils.getFont("sf", 18).getStringWidth(label)) / 2;
        int textY = y + (BUTTON_HEIGHT - FontUtils.getFont("sf", 18).getHeight()) / 2;
        FontUtils.getFont("sf", 18).drawStringWithShadow(label, textX, textY, hovered ? accent.getRGB() : Color.WHITE.getRGB());
    }

    private void drawAccountsPanel(int mouseX, int mouseY) {
        drawPanelBackground(accountsX, accountsY, accountsWidth, accountsHeight);

        int fontHeight = FontUtils.getFont("sf", 18).getHeight();
        String headerLabel = "Accounts (" + alts.size() + ")";
        FontUtils.getFont("sf", 18).drawStringWithShadow(headerLabel, accountsX + PADDING, accountsY + PADDING, Color.WHITE.getRGB());

        Gui.drawRect(accountsX + PADDING, accountsDividerY, accountsX + accountsWidth - PADDING, accountsDividerY + 1, RenderUtils.withAlpha(Color.WHITE, 25));

        if (alts.isEmpty()) {
            float centerX = gridListX + gridListWidth / 2f;
            float centerY = gridListY + gridListHeight / 2f - fontHeight;
            FontUtils.getFont("sf", 18).drawCenteredStringWithShadow("No accounts yet", centerX, centerY, Color.WHITE.getRGB());
            FontUtils.getFont("sf", 18).drawCenteredStringWithShadow("Add one using the form on the left", centerX, centerY + fontHeight + 4, 0xAAAAAA);
            return;
        }

        enableScissor(gridListX, gridListY, gridListWidth, gridListHeight);

        int startIndex = scrollOffset * COLUMNS;
        for (int row = 0; row < gridVisibleRows; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int altIndex = startIndex + row * COLUMNS + col;
                if (altIndex >= alts.size()) break;

                int x = gridListX + col * gridCellWidth;
                int y = gridListY + row * gridRowStride;

                String[] parts = alts.get(altIndex).split("\\|", 2);
                boolean premium = parts[0].equals("microsoftOAuth");
                String altName = parts[1];
                String uuid = premium ? altName : "";

                drawAccountCell(x, y, gridCellWidth - ENTRY_PADDING, ENTRY_HEIGHT, altName, uuid, premium, altIndex, mouseX, mouseY);
            }
        }

        disableScissor();

        enableScissor(accountsX, accountsY, accountsWidth, accountsHeight);

        int totalRows = (int) Math.ceil(alts.size() / (float) COLUMNS);
        int maxScroll = Math.max(0, totalRows - gridVisibleRows);
        int thumbHeight = Math.max(scrollbarHeight * gridVisibleRows / Math.max(1, totalRows), 20);
        int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / Math.max(1, maxScroll);

        Gui.drawRect(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, RenderUtils.withAlpha(GuiTheme.PANEL, 200));
        Gui.drawRect(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, GuiTheme.getAccent().getRGB());

        disableScissor();
    }

    private void drawAccountCell(int x, int y, int w, int h, String text, String uuid, boolean premium, int index, int mouseX, int mouseY) {
        boolean selected = selectedAlts.contains(index);
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        Color accent = GuiTheme.getAccent();

        int background;
        if (selected) background = RenderUtils.withAlpha(accent, 55);
        else if (hovered) background = RenderUtils.withAlpha(GuiTheme.PANEL, 235);
        else background = RenderUtils.withAlpha(GuiTheme.PANEL, 150);

        Gui.drawRect(x, y, x + w, y + h, background);

        if (selected) {
            int border = accent.getRGB();
            Gui.drawRect(x, y, x + w, y + 2, border);
            Gui.drawRect(x, y + h - 2, x + w, y + h, border);
            Gui.drawRect(x, y, x + 2, y + h, border);
            Gui.drawRect(x + w - 2, y, x + w, y + h, border);
        }

        loadHead(uuid);
        drawHead(x, y, uuid, h);

        int avatarSize = h - ENTRY_PADDING * 2;
        int dotSize = 6;
        int dotX = x + ENTRY_PADDING + avatarSize - dotSize + 2;
        int dotY = y + ENTRY_PADDING + avatarSize - dotSize + 2;
        Gui.drawRect(dotX - 1, dotY - 1, dotX + dotSize + 1, dotY + dotSize + 1, BACKGROUND.getRGB());
        Gui.drawRect(dotX, dotY, dotX + dotSize, dotY + dotSize, premium ? accent.getRGB() : new Color(130, 130, 140).getRGB());

        int textX = x + ENTRY_PADDING + avatarSize + ENTRY_PADDING;
        FontUtils.getFont("sf", 18).drawString(text, textX, y + (h - FontUtils.getFont("sf", 18).getHeight()) / 2f, selected ? accent.getRGB() : Color.WHITE.getRGB());
    }

    public void loadHead(String uuid) {
        if (uuid == null || uuid.isEmpty()) return;
        if (headCache.containsKey(uuid)) return;
        if (headLoading.getOrDefault(uuid, false)) return;
        if (headTries.getOrDefault(uuid, 0) > 5) return;

        headLoading.put(uuid, true);
        headTries.put(uuid, headTries.getOrDefault(uuid, 0) + 1);
        headCache.put(uuid, PLACEHOLDER_HEAD);

        new Thread(() -> {
            try {
                URI uri = URI.create("https://mc-heads.net/avatar/" + uuid);
                URLConnection connection = uri.toURL().openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setRequestProperty("Accept", "image/png");

                BufferedImage image = ImageIO.read(connection.getInputStream());
                if (image == null) throw new IOException("Failed to read image");

                mc.addScheduledTask(() -> {
                    DynamicTexture texture = new DynamicTexture(image);
                    ResourceLocation head = mc.getTextureManager().getDynamicTextureLocation("HEAD-" + uuid, texture);
                    headCache.put(uuid, head);
                    headLoading.put(uuid, false);
                });
            } catch (IOException e) {
                e.printStackTrace();
                headLoading.put(uuid, false);
            }
        }).start();
    }

    public void drawHead(int x, int y, String uuid, int cellHeight) {
        ResourceLocation head = uuid == null || uuid.isEmpty() ? PLACEHOLDER_HEAD : headCache.getOrDefault(uuid, PLACEHOLDER_HEAD);
        int size = cellHeight - (ENTRY_PADDING * 2);

        RenderUtils.drawImage(head, x + ENTRY_PADDING, y + ENTRY_PADDING, size, size);
    }

    private void enableScissor(int x, int y, int w, int h) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (sr.getScaledHeight() - y - h) * scale, w * scale, h * scale);
    }

    private void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private boolean isMouseOverButton(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        username.mouseClicked(mouseX, mouseY, mouseButton);
        password.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (isMouseOverButton(mouseX, mouseY, PADDING, 0, backButtonWidth, HEADER_HEIGHT)) {
            mc.displayGuiScreen(new YuriMenu());
            return;
        }

        if (!isLoggingIn && isMouseOverButton(mouseX, mouseY, primaryButtonX, primaryButtonY, primaryButtonWidth, BUTTON_HEIGHT)) {
            if (password.getText().isEmpty()) handleCrackedLogin(username.getText());
            return;
        }

        if (!isLoggingIn && isMouseOverButton(mouseX, mouseY, oauthButtonX, oauthButtonY, oauthButtonWidth, BUTTON_HEIGHT)) {
            handleOAuthLogin();
            return;
        }

        if (isMouseOverButton(mouseX, mouseY, generateButtonX, generateButtonY, generateButtonWidth, BUTTON_HEIGHT)) {
            handleCrackedLogin(generateRandomString());
            return;
        }

        int col = (mouseX - gridListX) / gridCellWidth;
        int row = (mouseY - gridListY) / gridRowStride;

        if (col >= 0 && col < COLUMNS && row >= 0 && mouseX >= gridListX && mouseY >= gridListY && mouseY < gridListY + gridListHeight) {
            int index = (scrollOffset + row) * COLUMNS + col;
            if (index >= 0 && index < alts.size()) {
                if (GuiScreen.isAltKeyDown()) {
                    if (selectedAlts.contains(index)) selectedAlts.remove((Integer) index);
                    else selectedAlts.add(index);
                } else {
                    loginWithAlt(alts.get(index));
                }
                return;
            }
        }

        if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
            draggingScrollbar = true;
            dragStartY = mouseY;
            scrollStart = scrollOffset;
        }
    }

    private void loginWithAlt(String alt) {
        if (alt.startsWith("cracked|")) {
            String user = alt.split("\\|")[1];
            SessionChanger.getInstance().setUserOffline(user);
        } else if (alt.startsWith("microsoftOAuth|")) {
            String user = alt.split("\\|")[1];
            String refreshToken = loadRefreshToken(user);
            if (refreshToken != null) {
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
            }
        }
    }

    private void saveOAuthAltToFile(String username, String refreshToken) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Euphoria");
        File altsFile = new File(dir, "alts.txt");
        if (!dir.exists()) dir.mkdirs();

        try (FileWriter fw = new FileWriter(altsFile, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("microsoftOAuth|" + username);
            alts.add("microsoftOAuth|" + username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File tokensFile = new File(dir, "tokens.txt");
        try (FileWriter fw = new FileWriter(tokensFile, true); PrintWriter out = new PrintWriter(fw)) {
            out.println(username + "|" + TokenEncryption.encrypt(refreshToken));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String loadRefreshToken(String username) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Euphoria");
        File file = new File(dir, "tokens.txt");
        if (!file.exists()) return null;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2 && parts[0].equals(username)) return TokenEncryption.decrypt(parts[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingScrollbar = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (!draggingScrollbar) return;

        int totalRows = (int) Math.ceil(alts.size() / (float) COLUMNS);
        int maxScrollLocal = Math.max(0, totalRows - gridVisibleRows);
        if (maxScrollLocal <= 0) return;

        int deltaY = mouseY - dragStartY;
        int thumbHeight = Math.max(scrollbarHeight * gridVisibleRows / Math.max(1, totalRows), 20);
        int scrollRange = scrollbarHeight - thumbHeight;
        int scrollDelta = scrollRange > 0 ? deltaY * maxScrollLocal / scrollRange : 0;
        scrollOffset = Math.min(maxScrollLocal, Math.max(0, scrollStart + scrollDelta));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        int totalRows = (int) Math.ceil(alts.size() / (float) COLUMNS);
        int maxScrollLocal = Math.max(0, totalRows - gridVisibleRows);

        if (wheel > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else scrollOffset = Math.min(maxScrollLocal, scrollOffset + 1);
    }

    private void handleCrackedLogin(String loginUsername) {
        if (isLoggingIn || loginUsername.isEmpty()) return;
        isLoggingIn = true;

        mc.setSession(new Session(loginUsername, loginUsername, "0", "legacy"));
        saveCrackedToFile(loginUsername);

        statusString = "Logged in with " + loginUsername;
        clearTextBoxes();
        isLoggingIn = false;
    }

    private void handleOAuthLogin() {
        if (isLoggingIn) return;
        isLoggingIn = true;
        statusString = "Awaiting response for Microsoft login...";

        MicrosoftOAuthTranslation.getRefreshToken(refreshToken -> {
            try {
                if (refreshToken != null) {
                    MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                    if (login.isGood()) {
                        mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                        saveOAuthAltToFile(login.username, login.newRefreshToken);
                        statusString = "Logged in with " + login.username;
                    } else {
                        statusString = "Failed to login with Microsoft OAuth";
                    }
                } else {
                    statusString = "Failed to get refresh token";
                }
            } finally {
                isLoggingIn = false;
            }
        });
    }

    private void saveCrackedToFile(String sessionUsername) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Euphoria");
        File file = new File(dir, "alts.txt");

        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("cracked|" + sessionUsername);
            alts.add("cracked|" + sessionUsername);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String generateRandomString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 4; i++) result.append(LETTERS.charAt(RANDOM_SOURCE.nextInt(LETTERS.length())));
        for (int i = 0; i < 4; i++) result.append(NUMBERS.charAt(RANDOM_SOURCE.nextInt(NUMBERS.length())));
        return result.toString();
    }

    private void clearTextBoxes() {
        username.setText("");
        password.setText("");
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        username.keyTyped(typedChar, keyCode);
        password.keyTyped(typedChar, keyCode);

        if (GuiScreen.isAltKeyDown() && keyCode == Keyboard.KEY_A) {
            selectedAlts.clear();
            for (int i = 0; i < alts.size(); i++) selectedAlts.add(i);
            return;
        }

        if (GuiScreen.isAltKeyDown() && keyCode == Keyboard.KEY_BACK) {
            if (!selectedAlts.isEmpty()) {
                selectedAlts.sort((a, b) -> b - a);
                for (int index : selectedAlts) {
                    if (index >= 0 && index < alts.size()) alts.remove(index);
                }
                selectedAlts.clear();
                saveAltsToFile();
            }
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    private static class Particle {
        float x;
        float y;
        float speed;
        float alpha;
        float size;
    }
}