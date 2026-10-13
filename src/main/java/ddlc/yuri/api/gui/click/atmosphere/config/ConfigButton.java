package ddlc.yuri.api.gui.click.atmosphere.config;

import java.util.function.Consumer;

public class ConfigButton extends ConfigEntry {

    private final Consumer<String> actionPerformed;

    public ConfigButton(String name, ConfigTab parent, Consumer<String> actionPerformed) {
        super(name, parent);

        this.actionPerformed = actionPerformed;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        ConfigEntry selected = this.parent.getSelectedConfig();

        this.actionPerformed.accept(selected instanceof ConfigItem ? selected.getName() : "");
    }
}
