package ddlc.yuri.utils.render;

import ddlc.yuri.api.font.CustomFontRenderer;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class FontUtils {
    private static final Map<String, CustomFontRenderer> fontCache = new HashMap<>();
    /** Access ordered so the least recently used scaled font can be evicted. */
    private static final Map<String, CustomFontRenderer> scaledFontCache = new LinkedHashMap<String, CustomFontRenderer>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CustomFontRenderer> eldest) {
            return size() > SCALED_CACHE_LIMIT;
        }
    };

    /**
     * Scaled fonts are keyed by the live GUI scale factor, so the cache can pick up a new entry
     * every time the user changes it. Each entry costs a full glyph atlas, so cap it. The base
     * fonts in {@link #fontCache} are one per fixed size and are never evicted.
     */
    private static final int SCALED_CACHE_LIMIT = 6;

    public static CustomFontRenderer getFont(String name, int size) {
        String cacheKey = name + "|" + size;

        CustomFontRenderer cached = fontCache.get(cacheKey);

        if (cached != null) {
            return cached;
        }

        CustomFontRenderer font = new CustomFontRenderer(name, size, Font.PLAIN, true, false);
        fontCache.put(cacheKey, font);
        return font;
    }

    public static CustomFontRenderer getScaledFont(String name, int size, float scale) {
        String cacheKey = name + "|" + size + "|" + scale;

        synchronized (scaledFontCache) {
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
    }

    public static void clearScaledFontCache() {
        synchronized (scaledFontCache) {
            scaledFontCache.clear();
        }
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