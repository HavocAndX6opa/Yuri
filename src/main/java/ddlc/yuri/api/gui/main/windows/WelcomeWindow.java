package ddlc.yuri.api.gui.main.windows;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class WelcomeWindow extends Window {

    private final String title;
    private final String description;
    private final List<String> wrappedDescription = new ArrayList<>();

    public WelcomeWindow(float x, float y, float width, float height, String title, String description) {
        super(x, y, width, height, 20f); // 20f for header height
        this.title = title;
        this.description = description;
    }

    @Override
    public void render(float mouseX, float mouseY) {
        // Render base blurred window and shadow frame
        super.render(mouseX, mouseY);

        float currentX = getX();
        float currentY = getY();
        float width = getWidth();

        // 1. Render Window Header Title
        CustomFontRenderer headerFont = FontUtils.getScaledFont("sf", 18, 1.0f);
        if (headerFont != null) {
            headerFont.drawString(
                    title,
                    currentX + 5,
                    currentY + 5,
                    ColorManager.getColor().getRGB()
            );
        }

        // 2. Render Description / Content Body
        CustomFontRenderer bodyFont = FontUtils.getScaledFont("sf", 18, 1.0f);
        if (bodyFont != null) {
            float textMargin = 10f;
            float textWidthLimit = width - (textMargin * 2f);

            // Wrap text lazily if it hasn't been wrapped yet
            if (wrappedDescription.isEmpty() && description != null && !description.isEmpty()) {
                wrapText(bodyFont, description, textWidthLimit);
            }

            float contentY = currentY + getHeader() + 5f;
            float lineSpacing = bodyFont.getHeight() + 4f;

            for (String line : wrappedDescription) {
                bodyFont.drawString(
                        line,
                        currentX + textMargin,
                        contentY,
                        new Color(220, 220, 220, 230).getRGB()
                );
                contentY += lineSpacing;
            }
        }
    }

    private void wrapText(CustomFontRenderer font, String text, float maxWidth) {
        wrappedDescription.clear();

        // Replace literal "\n" strings with actual newline characters
        String normalizedText = text.replace("\\n", "\n");

        // Split by explicit line breaks (\n)
        String[] explicitLines = normalizedText.split("\n", -1);

        for (String line : explicitLines) {
            if (line.isEmpty()) {
                wrappedDescription.add(""); // Empty line for spacing
                continue;
            }

            String[] words = line.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (font.getStringWidth(currentLine + word + " ") <= maxWidth) {
                    currentLine.append(word).append(" ");
                } else {
                    wrappedDescription.add(currentLine.toString().trim());
                    currentLine = new StringBuilder(word).append(" ");
                }
            }

            if (currentLine.length() > 0) {
                wrappedDescription.add(currentLine.toString().trim());
            }
        }
    }
}