package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.util.*;

@ModuleInfo(label = "Bed Wars Utils", description = "Utility module for BedWars", category = ModuleCategory.MISC)
public class BedWarsUtilsModule extends Module {

    private final Property<Boolean> walkToGen = new Property<>("Walk To Gen", true);
    private final Property<Boolean> notifySweats = new Property<>("Notify Sweats", true);
    private final Property<Boolean> notifyEvents = new Property<>("Notify Events", true);
    private final Property<Boolean> swords = new Property<>("Notify Sword", true);
    private final Property<Boolean> includeStone = new Property<>("Include Stone Swords", false, () -> !swords.getValue());
    private final Property<Boolean> armor = new Property<>("Notify Armor", true);

    private final Collection<EntityPlayer> ironSword = new HashSet<>();
    private final Collection<EntityPlayer> diamondSword = new HashSet<>();
    private final Collection<EntityPlayer> stoneSword = new HashSet<>();
    private final Collection<EntityPlayer> diamondArmor = new HashSet<>();
    private final Collection<EntityPlayer> chainArmor = new HashSet<>();
    private final Collection<EntityPlayer> ironArmor = new HashSet<>();
    private final Collection<UUID> warnedSweatPlayers = new HashSet<>();
    private final Set<String> notifiedEvents = new HashSet<>();

    private boolean check = false;
    private boolean gameStarted = false;
    private long gameStartTime = 0;
    private int walkTicks = 0;
    private static final int MAX_WALK_TICKS = 400;

    private static final class TimerEntry {
        final String label;
        final int atSeconds;

        TimerEntry(String label, int atSeconds) {
            this.label = label;
            this.atSeconds = atSeconds;
        }
    }

    private static final List<TimerEntry> BEDWARS_TIMERS = Arrays.asList(
            new TimerEntry("Diamond II", 5 * 60 + 22),
            new TimerEntry("Emerald II", 11 * 60 + 22),
            new TimerEntry("Diamond III", 17 * 60 + 22),
            new TimerEntry("Emerald III", 23 * 60 + 22),
            new TimerEntry("Bed gone", 29 * 60 + 22),
            new TimerEntry("Sudden Death", 39 * 60 + 22),
            new TimerEntry("Game End", 49 * 60 + 22)
    );

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            mc.gameSettings.keyBindBack.setPressed(false);
        }
        resetState();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        if (mc.gameSettings != null) {
            mc.gameSettings.keyBindBack.setPressed(false);
        }
        resetState();
    }

    private void resetState() {
        this.diamondSword.clear();
        this.ironSword.clear();
        this.stoneSword.clear();
        this.chainArmor.clear();
        this.ironArmor.clear();
        this.diamondArmor.clear();
        this.warnedSweatPlayers.clear();
        this.notifiedEvents.clear();
        this.check = false;
        this.gameStarted = false;
        this.gameStartTime = 0;
        this.walkTicks = 0;
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        if (mc.thePlayer == null || !(event.getPacket() instanceof S02PacketChat))
            return;

        S02PacketChat packetChat = (S02PacketChat) event.getPacket();
        String chatComponent = packetChat.getChatComponent().getUnformattedText();
        String strippedMessage = StringUtils.stripColor(chatComponent).trim();

        if (strippedMessage.contains("Protect your bed and destroy the enemy beds.") ||
                strippedMessage.contains("Goodluck with your BedWars Game") ||
                strippedMessage.contains("The game has started!")) {

            Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), EnumChatFormatting.GREEN + "Game has started!");
            mc.thePlayer.playSound("note.pling", 1.0f, 1.0f);
            check = true;
            gameStarted = true;
            gameStartTime = System.currentTimeMillis();
            walkTicks = 0;
            notifiedEvents.clear();
            if (walkToGen.getValue()) {
                mc.gameSettings.keyBindBack.setPressed(true);
            }
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (notifySweats.getValue() && mc.getNetHandler() != null) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info == null || info.getGameProfile() == null) continue;

                UUID uuid = info.getGameProfile().getId();
                if (uuid == null || warnedSweatPlayers.contains(uuid)) continue;

                String name = info.getGameProfile().getName();
                if (name == null || name.equalsIgnoreCase(mc.thePlayer.getName())) continue;

                String displayName = info.getDisplayName() != null ? info.getDisplayName().getFormattedText() : name;
                String unformattedDisplayName = StringUtils.stripColor(displayName);

                boolean isMvpPlusPlus = unformattedDisplayName.contains("[MVP++]");
                boolean isShortName = name.length() <= 3;

                if (isMvpPlusPlus || isShortName) {
                    warnedSweatPlayers.add(uuid);
                    StringBuilder reason = new StringBuilder();
                    if (isMvpPlusPlus) reason.append("MVP++");
                    if (isShortName) {
                        if (reason.length() > 0) reason.append(" & ");
                        reason.append("Short Name (").append(name.length()).append(" chars)");
                    }

                    Yuri.INSTANCE.getNotificationHandler().pop(
                            getLabel(),
                            EnumChatFormatting.GOLD + "Sweat Warning: " + EnumChatFormatting.RED + name + EnumChatFormatting.WHITE + " (" + reason.toString() + ")"
                    );
                }
            }
        }

        if (gameStarted && notifyEvents.getValue()) {
            long elapsedSeconds = (System.currentTimeMillis() - gameStartTime) / 1000L;
            for (TimerEntry timer : BEDWARS_TIMERS) {
                long remaining = Math.max(0, timer.atSeconds - elapsedSeconds);
                if (remaining <= 0 && !notifiedEvents.contains(timer.label)) {
                    notifiedEvents.add(timer.label);
                    mc.thePlayer.playSound("note.pling", 1.0f, 1.0f);
                    Yuri.INSTANCE.getNotificationHandler().pop(
                            getLabel(),
                            EnumChatFormatting.GOLD + "Event: " + EnumChatFormatting.WHITE + timer.label + EnumChatFormatting.GREEN + " has started!"
                    );
                }
            }
        }

        if (!check) return;

        if (walkToGen.getValue() && mc.gameSettings.keyBindBack.isKeyDown()) {
            walkTicks++;
            boolean manualInput = mc.gameSettings.keyBindForward.isKeyDown() ||
                    mc.gameSettings.keyBindLeft.isKeyDown() ||
                    mc.gameSettings.keyBindRight.isKeyDown() ||
                    mc.gameSettings.keyBindJump.isKeyDown();

            if (mc.thePlayer.isCollidedHorizontally || walkTicks >= MAX_WALK_TICKS || manualInput) {
                mc.gameSettings.keyBindBack.setPressed(false);
                walkTicks = 0;
            }
        }

        for (final EntityPlayer entity : mc.theWorld.playerEntities) {
            if (entity == mc.thePlayer) continue;

            if (this.swords.getValue() && entity.getHeldItem() != null) {
                final Item heldItem = entity.getHeldItem().getItem();

                if (heldItem instanceof ItemSword) {
                    final String type = ((ItemSword) heldItem).getToolMaterialName().toLowerCase();

                    if (type.contains("iron")) {
                        if (this.ironSword.add(entity)) {
                            Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has an " + EnumChatFormatting.AQUA + "Iron Sword");
                        }
                    } else if (type.contains("emerald") || type.contains("diamond")) {
                        if (this.diamondSword.add(entity)) {
                            Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has a " + EnumChatFormatting.AQUA + "Diamond Sword");
                        }
                    } else if (type.contains("stone")) {
                        if (this.stoneSword.add(entity)) {
                            if (this.includeStone.getValue()) {
                                Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has a " + EnumChatFormatting.AQUA + "Stone Sword");
                            }
                        }
                    } else if (type.contains("wood")) {
                        this.stoneSword.remove(entity);
                        this.ironSword.remove(entity);
                        this.diamondSword.remove(entity);
                    }
                }
            }

            if (this.armor.getValue()) {
                for (int slot = 0; slot < 4; slot++) {
                    ItemStack armorPiece = entity.getCurrentArmor(slot);
                    if (armorPiece != null && armorPiece.getItem() instanceof ItemArmor) {
                        ItemArmor armorItem = (ItemArmor) armorPiece.getItem();
                        ItemArmor.ArmorMaterial material = armorItem.getArmorMaterial();

                        if (material == ItemArmor.ArmorMaterial.CHAIN) {
                            if (chainArmor.add(entity)) {
                                Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has " + EnumChatFormatting.LIGHT_PURPLE + "Chain Armor");
                            }
                            break;
                        } else if (material == ItemArmor.ArmorMaterial.IRON) {
                            if (this.ironArmor.add(entity)) {
                                Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has " + EnumChatFormatting.LIGHT_PURPLE + "Iron Armor");
                            }
                            break;
                        } else if (material == ItemArmor.ArmorMaterial.DIAMOND) {
                            if (this.diamondArmor.add(entity)) {
                                Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has " + EnumChatFormatting.LIGHT_PURPLE + "Diamond Armor");
                            }
                            break;
                        } else if (material == ItemArmor.ArmorMaterial.LEATHER) {
                            this.diamondArmor.remove(entity);
                            this.ironArmor.remove(entity);
                            this.chainArmor.remove(entity);
                        }
                    }
                }
            }
        }
    }
}