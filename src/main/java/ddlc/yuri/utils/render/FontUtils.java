package ddlc.yuri.utils.render;

import ddlc.yuri.api.font.CustomFontRenderer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FontUtils {
    private static final Map<String, CustomFontRenderer> fontCache = new HashMap<>();
    private static final Map<String, CustomFontRenderer> scaledFontCache = new HashMap<>();

    public static CustomFontRenderer getFont(String name, int size) {
        String cacheKey = name + "|" + size;

        if (fontCache.containsKey(cacheKey)) {
            return fontCache.get(cacheKey);
        }

        CustomFontRenderer font = new CustomFontRenderer(name, size, Font.PLAIN, true, false);
        fontCache.put(cacheKey, font);
        return font;
    }

    public static CustomFontRenderer getScaledFont(String name, int size, float scale) {
        String cacheKey = name + "|" + size + "|" + scale;

        if (scaledFontCache.containsKey(cacheKey)) {
            return scaledFontCache.get(cacheKey);
        }

        CustomFontRenderer original = getFont(name, size);
        if (original == null) return null;

        String fontName = original.getNameFontTTF();
        float originalSize = original.getFont().getSize();
        float newSize = originalSize * scale;

        CustomFontRenderer scaledFont = new CustomFontRenderer(fontName, newSize, Font.PLAIN, true, false);
        scaledFontCache.put(cacheKey, scaledFont);

        return scaledFont;
    }

    public static void clearScaledFontCache() {
        scaledFontCache.clear();
    }

    private static CustomFontRenderer createFont(String name, int size) {
        return new CustomFontRenderer(name, size, Font.PLAIN, true, false);
    }

    public static String getIconString(IconStrings icon) {
        return icon.getString();
    }

    public enum IconStrings {

        // HUD / System
        COMPUTER('A'),
        CLOUD('B'),
        SEARCH('C'),
        TAG('D'),
        USER('E'),
        TARGET('F'),
        BUG('G'),
        ACTION('H'),
        EDIT('I'),
        CHECK('J'),
        SETTINGS('K'),
        EDIT_BOX('L'),
        GLOBE('M'),
        FEATHER('N'),
        LOCATION('O'),
        NETWORK('P'),
        AIRPLANE('Q'),

        // Directional
        DOWN_LEFT('R'),
        DOWN_RIGHT('S'),
        LEFT('T'),
        UP_LEFT('U'),
        RIGHT('V'),
        DOWN('W'),
        Y('X'),
        UP_RIGHT('Y');

        private final char character;

        IconStrings(char character) {
            this.character = character;
        }

        public char getCharacter() {
            return character;
        }

        public String getString() {
            return String.valueOf(character);
        }

        @Override
        public String toString() {
            return getString();
        }
    }

}