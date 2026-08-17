package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.KillEvent;
import ddlc.yuri.api.events.impl.player.PlayerDamageEvent;
import ddlc.yuri.api.events.impl.player.PlayerDeathEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.DragUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

@ModuleInfo(label = "Yuri Chat", description = "Displays Yuri reacting to your actions in real time.", category = ModuleCategory.RENDER)
public class YuriChatModule extends Module implements IMinecraft {

    private static final String KEY = "YuriChat";

    private static final float LOGO_SIZE = 128f;
    private static final float MIN_TEXTBOX_WIDTH = 214f;
    private static final float TEXTBOX_HEIGHT = 48f;
    private static final float PADDING = 6f;
    private static final float TEXT_PADDING = 12f;

    private final DragUtils.DraggableComponent component = new DragUtils.DraggableComponent(20, 20);
    private final Deque<Message> messages = new ArrayDeque<>();
    private final Random random = new Random();

    private final ResourceLocation logo = new ResourceLocation("yuri/gui/logo.png");
    private final ResourceLocation textbox = new ResourceLocation("yuri/gui/textbox.png");

    private long lastIdleCheck = 0L;
    private int kills = 0;

    // Dialogue Pools
    private static final String[] START_MESSAGES = {
            "...Oh. You're here. I was just reading...",
            "A-Ah... hello. I'll stay here with you for a while.",
            "I brought some tea... and my thoughts. If that's alright."
    };

    private static final String[] KILL_MESSAGES_GENERIC = {
            "Careful... the sight of conflict makes my heart race.",
            "A swift resolution... almost poetic in a strange way.",
            "That was rather intense, wasn't it?",
            "You handle your blade with such singular focus...",
            "I... shouldn't enjoy watching that as much as I do."
    };

    private static final String[] KILL_MESSAGES_MILESTONE = {
            "You are remarkably relentless today...",
            "So much intensity... it's getting hard to breathe.",
            "Is it strange that I find this focus of yours captivating?"
    };

    private static final String[] DAMAGE_MESSAGES = {
            "Ah! Please be careful... my chest hurts just watching.",
            "D-Don't let them hurt you... I wouldn't like that at all.",
            "Are you alright? Take a breath... stay calm."
    };

    private static final String[] DEATH_MESSAGES = {
            "No...! Please don't leave me here alone...",
            "Everything just went dark... are you okay?",
            "I felt that... it hurt so much."
    };

    private static final String[] IDLE_MESSAGES = {
            "The atmosphere feels so heavy... I love it.",
            "I was thinking about a book I read recently... it was quite dark.",
            "Do you ever feel a strange urge to just... sink into a story?",
            "I brewed some jasmine tea... I wish I could share it with you.",
            "Your presence is... comforting. I hope I'm not being too distracting.",
            "Sometimes, the quiet moments feel more intense than the action.",
            "I heard something about this.. Baby Boy?? I don't know what it is, but it sounds intriguing.",
    };

    public YuriChatModule() {
        DragUtils.registerComponent(KEY, component);
    }

    @Override
    public void onEnable() {
        kills = 0;
        messages.clear();
        lastIdleCheck = System.currentTimeMillis();
        pushMessage(START_MESSAGES[random.nextInt(START_MESSAGES.length)], 7000);
    }

    @Override
    public void onDisable() {
        messages.clear();
    }

    @EventHook
    public void onKill(KillEvent event) {
        kills++;
        if (kills == 1) {
            pushMessage("Y-You took them down... so efficiently.", 7000);
        } else if (kills % 5 == 0) {
            pushMessage(KILL_MESSAGES_MILESTONE[random.nextInt(KILL_MESSAGES_MILESTONE.length)], 8000);
        } else {
            pushMessage(KILL_MESSAGES_GENERIC[random.nextInt(KILL_MESSAGES_GENERIC.length)], 6000);
        }
    }

    @EventHook
    public void onDamage(PlayerDamageEvent event) {
        if (random.nextFloat() < 0.4f) {
            pushMessage(DAMAGE_MESSAGES[random.nextInt(DAMAGE_MESSAGES.length)], 6000);
        }
    }

    @EventHook
    public void onDeath(PlayerDeathEvent event) {
        pushMessage(DEATH_MESSAGES[random.nextInt(DEATH_MESSAGES.length)], 9000);
    }

    private void pushMessage(String text, long lifetimeMs) {
        messages.clear();
        messages.addLast(new Message(text, System.currentTimeMillis(), lifetimeMs));
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        render(false);
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        render(true);
    }

    private void render(boolean shaderPass) {
        long now = System.currentTimeMillis();
        messages.removeIf(m -> now - m.time >= m.lifetimeMs);

        if (messages.isEmpty() && (now - lastIdleCheck > 12_000)) {
            lastIdleCheck = now;
            if (random.nextFloat() < 0.5f) {
                pushMessage(IDLE_MESSAGES[random.nextInt(IDLE_MESSAGES.length)], 7500);
            }
        }

        CustomFontRenderer font = FontUtils.getFont("sf", 15);
        if (font == null) return;

        // Calculate dynamic width of textbox
        float calculatedTextboxWidth = MIN_TEXTBOX_WIDTH;
        Message activeMessage = messages.peekFirst();

        if (activeMessage != null) {
            float textWidth = font.getStringWidth(activeMessage.text);
            calculatedTextboxWidth = Math.max(MIN_TEXTBOX_WIDTH, textWidth + (TEXT_PADDING * 2f));
        }

        // Layout Dimensions
        // Total component bounds accommodate both logo and textbox stacked vertically
        float totalWidth = Math.max(LOGO_SIZE, calculatedTextboxWidth) + (PADDING * 2f);
        float totalHeight = (LOGO_SIZE / 2f) + TEXTBOX_HEIGHT + (PADDING * 2f);

        // Update Draggable Component Hitbox & Bounds
        component.setWidth(totalWidth);
        component.setHeight(totalHeight);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();

        // Screen boundary clamp using updated dynamic dimensions
        if (x > sr.getScaledWidth() - totalWidth) {
            x = sr.getScaledWidth() - totalWidth;
            component.setX(x);
        }
        if (x < 0) {
            x = 0;
            component.setX(0);
        }

        if (y > sr.getScaledHeight() - totalHeight) {
            y = sr.getScaledHeight() - totalHeight;
            component.setY(y);
        }
        if (y < 0) {
            y = 0;
            component.setY(0);
        }

        // Origin offsets
        float renderX = x + PADDING;
        float logoY = y + PADDING;
        float textboxY = logoY + (LOGO_SIZE / 2f);

        // Compute horizontal center alignment
        float textboxX = renderX + Math.max(0, (LOGO_SIZE - calculatedTextboxWidth) / 2f);
        float textboxCenterX = textboxX + (calculatedTextboxWidth / 2f);
        float logoX = textboxCenterX - (LOGO_SIZE / 2f);

        // Render logo centered over the textbox
        RoundedUtils.drawImage(logo, logoX, logoY, LOGO_SIZE, LOGO_SIZE);

        // Render textbox
        RoundedUtils.drawImage(textbox, textboxX, textboxY, calculatedTextboxWidth, TEXTBOX_HEIGHT);

        // Render active message string centered inside the textbox vertically
        if (activeMessage != null) {
            float textX = textboxX + TEXT_PADDING;
            float textY = textboxY + (TEXTBOX_HEIGHT / 2f) - (font.getHeight() / 2f);
            font.drawStringWithShadow(activeMessage.text, textX, textY, new Color(245, 240, 250).getRGB());
        }
    }

    private static class Message {
        final String text;
        final long time;
        final long lifetimeMs;

        Message(String text, long time, long lifetimeMs) {
            this.text = text;
            this.time = time;
            this.lifetimeMs = lifetimeMs;
        }
    }
}