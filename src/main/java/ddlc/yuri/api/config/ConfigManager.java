package ddlc.yuri.api.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ddlc.yuri.Yuri;
import ddlc.yuri.utils.misc.Manager;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class ConfigManager extends Manager<Config> {

    @Getter
    private static ConfigManager instance;

    @Getter
    private final BindsConfig bindsConfig;

    @Getter
    private final List<VisualsConfig> visualsConfigs = new ArrayList<>();

    public ConfigManager() {
        super(loadConfigs());
        instance = this;

        if (!CONFIGS_DIR.exists()) {
            boolean ignored = CONFIGS_DIR.mkdirs();
        }

        if (!VISUALS_DIR.exists()) {
            boolean ignored = VISUALS_DIR.mkdirs();
        }

        migrateLegacyVisuals();

        this.bindsConfig = new BindsConfig();
        loadVisualsProfiles();
    }

    public static final File CONFIGS_DIR = new File(Yuri.NAME, "configs");
    public static final File VISUALS_DIR = new File(Yuri.NAME, "visuals");
    public static final String EXTENSION = ".json";

    /** Profile auto-saved on shutdown and auto-loaded on startup. */
    public static final String DEFAULT_PROFILE = "default";

    /**
     * Visuals used to live in a single Yuri/visuals.json. Move it to the profile folder so
     * existing HUD layouts survive the switch to named profiles.
     */
    private static void migrateLegacyVisuals() {
        File legacy = new File(Yuri.NAME, "visuals.json");
        File migrated = new File(VISUALS_DIR, DEFAULT_PROFILE + EXTENSION);

        if (!legacy.exists() || migrated.exists()) {
            return;
        }

        try {
            Files.move(legacy.toPath(), migrated.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
        }
    }

    private void loadVisualsProfiles() {
        File[] files = VISUALS_DIR.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (FilenameUtils.getExtension(file.getName()).equals("json")) {
                visualsConfigs.add(new VisualsConfig(FilenameUtils.removeExtension(file.getName())));
            }
        }
    }

    public VisualsConfig findVisuals(String visualsName) {
        if (visualsName == null) return null;

        for (VisualsConfig visualsConfig : visualsConfigs) {
            if (visualsConfig.getName().equalsIgnoreCase(visualsName)) {
                return visualsConfig;
            }
        }

        if (new File(VISUALS_DIR, visualsName + EXTENSION).exists()) {
            VisualsConfig visualsConfig = new VisualsConfig(visualsName);
            visualsConfigs.add(visualsConfig);
            return visualsConfig;
        }

        return null;
    }

    public boolean loadVisuals(String visualsName) {
        VisualsConfig visualsConfig = findVisuals(visualsName);
        if (visualsConfig == null) return false;
        return visualsConfig.loadFromFile();
    }

    public boolean saveVisuals(String visualsName) {
        if (visualsName == null) return false;
        VisualsConfig visualsConfig = findVisuals(visualsName);

        if (visualsConfig == null) {
            visualsConfig = new VisualsConfig(visualsName);
            visualsConfigs.add(visualsConfig);
        }

        return visualsConfig.saveToFile();
    }

    public boolean deleteVisuals(String visualsName) {
        VisualsConfig visualsConfig = findVisuals(visualsName);
        if (visualsConfig == null) return false;

        File file = visualsConfig.getFile();
        visualsConfigs.remove(visualsConfig);
        return file.exists() && file.delete();
    }

    public boolean loadConfig(String configName) {
        if (configName == null) return false;
        Config config = findConfig(configName);

        if (config == null) return false;
        try (FileReader reader = new FileReader(config.getFile())) {
            JsonParser parser = new JsonParser();
            JsonObject object = (JsonObject) parser.parse(reader);
            config.load(object);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean saveConfig(String configName) {
        if (configName == null) return false;
        Config config;
        if ((config = findConfig(configName)) == null) {
            Config newConfig = (config = new Config(configName));
            getElements().add(newConfig);
        }

        String contentPrettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson(config.save());
        try (FileWriter writer = new FileWriter(config.getFile())) {
            writer.write(contentPrettyPrint);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Config findConfig(String configName) {
        if (configName == null) return null;
        for (Config config : getElements()) {
            if (config.getName().equalsIgnoreCase(configName))
                return config;
        }

        if (new File(CONFIGS_DIR, configName + EXTENSION).exists())
            return new Config(configName);

        return null;
    }

    public boolean deleteConfig(String configName) {
        if (configName == null) return false;
        Config config;
        if ((config = findConfig(configName)) != null) {
            final File f = config.getFile();
            getElements().remove(config);
            return f.exists() && f.delete();
        }
        return false;
    }

    private static ArrayList<Config> loadConfigs() {
        final ArrayList<Config> loadedConfigs = new ArrayList<>();
        File[] files = CONFIGS_DIR.listFiles();
        if (files != null) {
            for (File file : files) {
                if (FilenameUtils.getExtension(file.getName()).equals("json"))
                    loadedConfigs.add(new Config(FilenameUtils.removeExtension(file.getName())));
            }
        }
        return loadedConfigs;
    }
}