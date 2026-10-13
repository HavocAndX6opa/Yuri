package ddlc.yuri.api.gui.click.atmosphere.config;

import ddlc.yuri.api.config.Config;
import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.api.config.GithubConfigFetcher;
import ddlc.yuri.api.gui.click.atmosphere.CategoryTab;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class ConfigTab extends CategoryTab {

    private final List<ConfigEntry> configs = new ArrayList<>();

    private ConfigTextField newConfigName;
    private ConfigEntry selectedConfig;

    public ConfigTab() {
        super(null);

        this.refreshConfigs();
    }

    private void add(ConfigEntry entry) {
        this.configs.add(entry);
        this.rows.add(entry);

        if (getLayout() != null) {
            entry.setLayout(getLayout());
        }
    }

    public void refreshConfigs() {
        this.configs.clear();
        this.rows.clear();
        this.selectedConfig = null;

        for (Config config : ConfigManager.getInstance().getElements()) {
            this.add(new ConfigItem(config.getName(), this));
        }

        this.newConfigName = new ConfigTextField("Name", this);
        this.add(this.newConfigName);

        this.add(new ConfigButton("Load", this, configName -> {
            if (configName.isEmpty()) {
                return;
            }
            ConfigManager.getInstance().loadConfig(configName);
        }));

        this.add(new ConfigButton("Save", this, ignored -> {
            String name = this.selectedConfig instanceof ConfigItem
                    ? this.selectedConfig.getName()
                    : this.newConfigName.getValue();
            if (name == null || name.isEmpty()) {
                return;
            }
            ConfigManager.getInstance().saveConfig(name);
            this.refreshConfigs();
        }));

        this.add(new ConfigButton("Delete", this, configName -> {
            if (configName.isEmpty()) {
                return;
            }
            ConfigManager.getInstance().deleteConfig(configName);
            this.refreshConfigs();
        }));

        try {
            List<String> remote = GithubConfigFetcher.fetchConfigList();
            if (remote != null && !remote.isEmpty()) {
                for (String remoteName : remote) {
                    final String configName = remoteName;

                    this.add(new ConfigButton("↳ " + configName, this, ignored -> new Thread(() -> {
                        boolean loaded = GithubConfigFetcher.downloadAndLoadConfig(configName);

                        if (loaded) {
                            Minecraft.getMinecraft().addScheduledTask(this::refreshConfigs);
                        }
                    }, "github-config-download").start()));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public List<ConfigEntry> getConfigs() {
        return new ArrayList<>(this.configs);
    }

    public ConfigEntry getSelectedConfig() {
        return selectedConfig;
    }

    public void setSelectedConfig(ConfigEntry selectedConfig) {
        this.selectedConfig = selectedConfig;
    }
}
