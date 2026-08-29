package ddlc.yuri.api.font;


import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class CustomFont
{

    private float imgSize = 2048;
    protected CharData[] charData = new CharData[65536];
    protected Font font;
    protected boolean antiAlias;
    protected boolean fractionalMetrics;
    protected int fontHeight = -1;
    protected int charOffset = 0;
    protected DynamicTexture tex;
    private static final int PADDING = 2;
    // advance (atlas pixels) used for chars that have no rasterized glyph, so a
    // missing glyph pushes the rest of the line right instead of pulling it left
    protected int missingCharAdvance = 8;
    // only the game font itself is rasterized past this point (keeps the atlas
    // from being flooded by low-priority fallback ranges)
    private static final int FALLBACK_CHAR_LIMIT = 0x2C00;
    private static final String[] FALLBACK_FONT_NAMES = {
        // symbols (heart/star/gear etc. that servers use in chat and scoreboard)
        "Segoe UI Symbol", "Segoe UI Emoji", "Noto Sans Symbols", "Noto Sans Symbols 2",
        "Apple Symbols", "DejaVu Sans", "Symbola", "Arial Unicode MS"
        // note: chinese/japanese/hangul are NOT rasterized here - a 2048x2048 atlas
        // fits only a fraction of the ~21000 cjk ideographs. the renderer delegates
        // wide east-asian chars to the vanilla font renderer, whose unicode font
        // pages have complete cjk coverage (same as how vanilla displays them)
    };

    public CustomFont(Font font, boolean antiAlias, boolean fractionalMetrics)
    {
        this.font = font;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        tex = setupTexture(font, antiAlias, fractionalMetrics, this.charData);
    }

    public DynamicTexture setupTexture(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars)
    {
        BufferedImage img = generateFontImage(font, antiAlias, fractionalMetrics, chars);

        try
        {
            DynamicTexture texture = new DynamicTexture(img);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            return texture;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public int getCharOffset()
    {
        return this.charOffset;
    }

    public void setCharOffset(int charOffset)
    {
        this.charOffset = charOffset;
    }

    protected BufferedImage generateFontImage(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars)
    {
        int imgSize = (int) this.imgSize;
        BufferedImage bufferedImage = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        g.setFont(font);
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, imgSize, imgSize);
        g.setColor(Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        for (int i = 0; i < chars.length; i++)
        {
            chars[i] = new CharData();
        }

        List<Font> fallbackFonts = loadFallbackFonts(font);
        FontMetrics baseMetrics = g.getFontMetrics(font);

        // every glyph shares one baseline inside its cell so mixed fonts (game
        // font + symbol/CJK fallbacks) stay vertically aligned
        int maxAscent = Math.max(baseMetrics.getMaxAscent(), 1);
        int maxDescent = Math.max(baseMetrics.getMaxDescent(), 1);

        for (Font fallback : fallbackFonts)
        {
            FontMetrics metrics = g.getFontMetrics(fallback);
            maxAscent = Math.max(maxAscent, metrics.getMaxAscent());
            maxDescent = Math.max(maxDescent, metrics.getMaxDescent());
        }

        int cellHeight = maxAscent + maxDescent;

        Rectangle2D spaceBounds = baseMetrics.getStringBounds(" ", g);
        this.missingCharAdvance = Math.max((int) spaceBounds.getWidth(), 4);

        // reference ink of the game font itself ('H' = plain capital, no
        // descender/overshoot): small symbols get vertically centered on it
        int baseInkCenter;
        int baseInkHeight;
        BufferedImage scratch = new BufferedImage(128, cellHeight + 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D scratchG = (Graphics2D) scratch.getGraphics();
        scratchG.setColor(Color.WHITE);
        scratchG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        scratchG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        scratchG.setFont(font);
        scratchG.drawString("H", 2, maxAscent);
        scratchG.dispose();
        int hWidth = Math.min((int) baseMetrics.getStringBounds("H", g).getWidth() + 6, 126);
        int[] baseInk = measureInk(scratch, 2, 0, hWidth, cellHeight);

        if (baseInk != null)
        {
            baseInkCenter = (baseInk[0] + baseInk[1]) / 2;
            baseInkHeight = baseInk[1] - baseInk[0] + 1;
        }
        else
        {
            baseInkCenter = maxAscent - baseMetrics.getMaxAscent() / 2;
            baseInkHeight = (int) (baseMetrics.getMaxAscent() * 0.75D);
        }

        int baseGlyphTop = maxAscent - baseMetrics.getMaxAscent();
        int symbolMaxInkHeight = (int) (baseInkHeight * 1.4D);

        int[] cursor = {0, 1};

        // order matters: the game font's own glyphs win, then unicode symbols the
        // game font is missing (servers use those in chat and scoreboard), then
        // latin-ext/greek/cyrillic with whatever room is left. wide east-asian
        // chars are delegated to the vanilla renderer by CustomFontRenderer
        rasterizeRange(g, chars, bufferedImage, font, baseMetrics, font, 0x0000, 0x10000, cursor, imgSize, cellHeight, maxAscent, baseGlyphTop, baseInkCenter, symbolMaxInkHeight);

        // separator bars (box drawing / block elements, e.g. hypixel's chat separator):
        // the dedicated symbol fonts draw these as fat blocks, while the logical
        // font's thin bars match the vanilla look people expect
        Font logicalFallback = fallbackFonts.get(fallbackFonts.size() - 1);
        rasterizeRange(g, chars, bufferedImage, logicalFallback, g.getFontMetrics(logicalFallback), font, 0x2500, 0x25B0, cursor, imgSize, cellHeight, maxAscent, baseGlyphTop, baseInkCenter, symbolMaxInkHeight);

        for (Font fallback : fallbackFonts)
        {
            rasterizeRange(g, chars, bufferedImage, fallback, g.getFontMetrics(fallback), font, 0x2190, FALLBACK_CHAR_LIMIT, cursor, imgSize, cellHeight, maxAscent, baseGlyphTop, baseInkCenter, symbolMaxInkHeight);
        }

        for (Font fallback : fallbackFonts)
        {
            rasterizeRange(g, chars, bufferedImage, fallback, g.getFontMetrics(fallback), font, 0x0000, 0x2190, cursor, imgSize, cellHeight, maxAscent, baseGlyphTop, baseInkCenter, symbolMaxInkHeight);
        }

        // line spacing must keep matching the original renderer: base font metrics
        // only, fallback fonts are not allowed to inflate getHeight()
        this.fontHeight = baseMetrics.getHeight();
        return bufferedImage;
    }

    private void rasterizeRange(Graphics2D g, CharData[] chars, BufferedImage atlas, Font renderFont, FontMetrics metrics, Font baseFont, int from, int to, int[] cursor, int imgSize, int cellHeight, int maxAscent, int baseGlyphTop, int baseInkCenter, int symbolMaxInkHeight)
    {
        int positionX = cursor[0];
        int positionY = cursor[1];
        boolean isBase = renderFont == baseFont;

        for (int i = from; i < to; i++)
        {
            char ch = (char) i;

            if (chars[i].valid || Character.isISOControl(ch) || isZeroWidth(ch) || !renderFont.canDisplay(ch))
            {
                continue;
            }

            int advance = (int) metrics.getStringBounds(String.valueOf(ch), g).getWidth();

            if (positionX + advance + 8 + PADDING >= imgSize)
            {
                positionX = 0;
                positionY += cellHeight + PADDING;
            }

            if (positionY + cellHeight + PADDING >= imgSize)
            {
                break; // atlas is full, remaining chars fall back to missingCharAdvance
            }

            CharData charData = chars[i];
            charData.width = advance + 8;
            charData.height = cellHeight;
            charData.storedX = positionX;
            charData.storedY = positionY;
            charData.valid = true;

            g.setFont(renderFont);
            g.drawString(String.valueOf(ch), positionX + 2, positionY + maxAscent);

            // measure the actual ink: invisible glyphs (servers hide zero-width
            // obfuscation chars inside words) must not eat space, and small
            // symbol glyphs get centered on the text line instead of trusting
            // the fallback font's metrics
            int[] ink = measureInk(atlas, positionX + 2, positionY, advance + 4, cellHeight);

            if (ink == null)
            {
                if (ch != ' ' && ch != '\u00A0')
                {
                    charData.width = 8; // fully blank cell -> zero visible width
                }
            }
            else if (!isBase && ink[1] - ink[0] + 1 <= symbolMaxInkHeight)
            {
                // small standalone symbol: center its ink on the game font's line
                charData.glyphTop = (ink[0] + ink[1]) / 2 - baseInkCenter;
            }
            else
            {
                // letters, CJK and tall glyphs sit on the baseline like the game font
                charData.glyphTop = baseGlyphTop;
            }

            positionX += charData.width + PADDING;
        }

        cursor[0] = positionX;
        cursor[1] = positionY;
    }

    // returns {inkTop, inkBottom} relative to the scanned region, or null when blank
    private int[] measureInk(BufferedImage image, int x, int y, int width, int height)
    {
        width = Math.min(width, image.getWidth() - x);
        height = Math.min(height, image.getHeight() - y);

        if (width <= 0 || height <= 0)
        {
            return null;
        }

        int[] pixels = new int[width * height];
        image.getRGB(x, y, width, height, pixels, 0, width);
        int top = -1;
        int bottom = -1;

        for (int row = 0; row < height; row++)
        {
            for (int col = 0; col < width; col++)
            {
                if ((pixels[row * width + col] >>> 24) != 0)
                {
                    if (top < 0)
                    {
                        top = row;
                    }

                    bottom = row;
                }
            }
        }

        return top < 0 ? null : new int[]{top, bottom};
    }

    private List<Font> loadFallbackFonts(Font base)
    {
        List<Font> fallbacks = new ArrayList<Font>();
        float size = base.getSize2D();

        for (String name : FALLBACK_FONT_NAMES)
        {
            Font candidate = new Font(name, Font.PLAIN, 1).deriveFont(size);

            // java silently maps unknown family names to Dialog
            if (!candidate.getFamily().equalsIgnoreCase(name))
            {
                continue;
            }

            fallbacks.add(candidate);
        }

        // logical font, always present: covers greek/cyrillic/etc. via platform mapping
        fallbacks.add(new Font(Font.DIALOG, Font.PLAIN, 1).deriveFont(size));

        return fallbacks;
    }

    public void drawChar(CharData[] chars, char c, float x, float y) throws ArrayIndexOutOfBoundsException
    {
        try
        {
            if (chars[c] == null || !chars[c].valid) return;
            // shift the quad up by glyphTop so every glyph lands where the layout
            // decided (baseline-aligned letters, ink-centered symbols)
            drawQuad(x, y - chars[c].glyphTop, chars[c].width, chars[c].height, chars[c].storedX, chars[c].storedY, chars[c].width, chars[c].height);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected void drawQuad(float x, float y, float width, float height, float srcX, float srcY, float srcWidth, float srcHeight)
    {
        float renderSRCX = srcX / imgSize;
        float renderSRCY = srcY / imgSize;
        float renderSRCWidth = srcWidth / imgSize;
        float renderSRCHeight = srcHeight / imgSize;
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
        GL11.glVertex2d(x + width, y);
        GL11.glTexCoord2f(renderSRCX, renderSRCY);
        GL11.glVertex2d(x, y);
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x + width, y + height);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
        GL11.glVertex2d(x + width, y);
    }

    public int getStringHeight(String text)
    {
        return getHeight();
    }

    public int getHeight()
    {
        return (this.fontHeight - 8) / 2;
    }

    public int getStringWidth(String text)
    {
        if (text == null)
        {
            return 0;
        }

        int width = 0;

        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);

            if (c >= 0 && c < this.charData.length && this.charData[c] != null && this.charData[c].valid)
            {
                width += this.charData[c].width - 8 + this.charOffset;
            }
            else
            {
                width += advanceFor(c);
            }
        }

        return width / 2;
    }

    protected int advanceFor(char c)
    {
        if (Character.isISOControl(c) || isZeroWidth(c))
        {
            return 0;
        }

        int type = Character.getType(c);

        // unassigned / private-use / lone surrogate codepoints no font can show:
        // servers use these as invisible padding, giving them space width only
        // punches visible holes in words
        if (type == Character.UNASSIGNED || type == Character.PRIVATE_USE || type == Character.SURROGATE)
        {
            return 0;
        }

        // wide east-asian glyphs that did not fit in the atlas still need their
        // full-width slot so the rest of the line keeps its rhythm
        if (isWideEastAsian(c))
        {
            return this.missingCharAdvance * 2;
        }

        return this.missingCharAdvance;
    }

    protected static boolean isWideEastAsian(char c)
    {
        return (c >= 0x2E80 && c <= 0x9FFF)     // cjk radicals, kana, unified ideographs, yijing
            || (c >= 0xAC00 && c <= 0xD7AF)     // hangul syllables
            || (c >= 0xF900 && c <= 0xFAFF)     // cjk compatibility ideographs
            || (c >= 0xFF00 && c <= 0xFF60)     // fullwidth forms
            || (c >= 0xFFE0 && c <= 0xFFE6);    // fullwidth signs
    }

    // zero-width/combining chars (ZWSP, ZWNJ, soft hyphen, combining marks,
    // variation selectors...) - servers hide these inside text, they must
    // never take up visible space
    protected static boolean isZeroWidth(char c)
    {
        int type = Character.getType(c);

        return type == Character.FORMAT
            || type == Character.NON_SPACING_MARK
            || type == Character.COMBINING_SPACING_MARK;
    }

    public boolean isAntiAlias()
    {
        return this.antiAlias;
    }

    public void setAntiAlias(boolean antiAlias)
    {
        if (this.antiAlias != antiAlias)
        {
            this.antiAlias = antiAlias;
            tex = setupTexture(this.font, antiAlias, this.fractionalMetrics, this.charData);
        }
    }

    public boolean isFractionalMetrics()
    {
        return this.fractionalMetrics;
    }

    public void setFractionalMetrics(boolean fractionalMetrics)
    {
        if (this.fractionalMetrics != fractionalMetrics)
        {
            this.fractionalMetrics = fractionalMetrics;
            tex = setupTexture(this.font, this.antiAlias, fractionalMetrics, this.charData);
        }
    }

    public Font getFont()
    {
        return this.font;
    }

    public void setFont(Font font)
    {
        this.font = font;
        tex = setupTexture(font, this.antiAlias, this.fractionalMetrics, this.charData);
    }

    protected class CharData
    {
        public int width;
        public int height;
        public int storedX;
        public int storedY;
        public int glyphTop;
        public boolean valid;

        protected CharData()
        {
        }
    }
}
