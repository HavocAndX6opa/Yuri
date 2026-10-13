package ddlc.yuri.api.gui.click.atmosphere;

public interface ColumnRow {

    float layoutY();

    void setLayoutY(float layoutY);

    void setLayout(AtmosphereLayout layout);

    float height();

    default boolean isActive() {
        return false;
    }

    default String description() {
        return null;
    }

    default boolean cancelInteraction() {
        return false;
    }

    void render(float panelX, float rowY, float width, boolean hovered, int accent, float alpha);

    void mouseClicked(int mouseX, int mouseY, int mouseButton);

    default void keyTyped(char typedChar, int keyCode) {
    }
}
