package ddlc.yuri.api.gui.click.atmosphere.config;

import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.utils.misc.Timer;

public class ConfigItem extends ConfigEntry {

    private final Timer lastClick = new Timer();

    public ConfigItem(String name, ConfigTab parent) {
        super(name, parent);
    }

    @Override
    public boolean isActive() {
        return this.parent.getSelectedConfig() == this;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        if (this.lastClick.hasTimeElapsed(200)) {
            ConfigManager.getInstance().loadConfig(this.getName());
        }

        this.parent.setSelectedConfig(this);

        this.lastClick.reset();
    }
}
