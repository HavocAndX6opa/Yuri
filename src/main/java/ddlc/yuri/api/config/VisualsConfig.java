package ddlc.yuri.api.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ddlc.yuri.Yuri;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.utils.render.DragUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * A named snapshot of the render modules plus the draggable HUD element positions.
 * Mirrors {@link Config} - one JSON file per profile, all of them living in
 * {@link ConfigManager#VISUALS_DIR}.
 */
public final class VisualsConfig implements Serializable {

    private final String name;
    private final File file;

    public VisualsConfig(String name) {
        this.name = name;
        this.file = new File(ConfigManager.VISUALS_DIR, name + ConfigManager.EXTENSION);

        if (!ConfigManager.VISUALS_DIR.exists()) {
            ConfigManager.VISUALS_DIR.mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public boolean saveToFile() {
        JsonObject object = save();
        String contentPrettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson(object);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(contentPrettyPrint);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean loadFromFile() {
        if (!file.exists()) return false;
        try (FileReader reader = new FileReader(file)) {
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(reader);

            if (element == null || !element.isJsonObject()) {
                return false;
            }

            JsonObject object = element.getAsJsonObject();
            load(object);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public JsonObject save() {
        JsonObject jsonObject = new JsonObject();
        JsonObject modulesObject = new JsonObject();
        JsonObject draggingObject = new JsonObject();

        for (Module module : Yuri.INSTANCE.getModuleManager().getModules()) {
            if (module.getCategory() == ModuleCategory.RENDER) {
                modulesObject.add(module.getLabel(), module.save());
            }
        }

        for (String key : DragUtils.components.keySet()) {
            draggingObject.add(key, DragUtils.components.get(key).save());
        }

        jsonObject.add("Modules", modulesObject);
        jsonObject.add("Dragging", draggingObject);

        return jsonObject;
    }

    @Override
    public void load(JsonObject object) {
        if (object.has("Modules")) {
            JsonObject modulesObject = object.getAsJsonObject("Modules");

            for (Module module : Yuri.INSTANCE.getModuleManager().getModules()) {
                if (module.getCategory() == ModuleCategory.RENDER) {
                    if (modulesObject.has(module.getLabel())) {
                        module.load(modulesObject.getAsJsonObject(module.getLabel()));
                    }
                }
            }
        }
        if (object.has("Dragging")) {
            JsonObject draggingObject = object.getAsJsonObject("Dragging");
            for (Map.Entry<String, JsonElement> entry : draggingObject.entrySet()) {
                String key = entry.getKey();
                JsonObject componentData = entry.getValue().getAsJsonObject();
                if (!DragUtils.components.containsKey(key)) {
                    DragUtils.components.put(key, new DragUtils.DraggableComponent(0, 0));
                }
                DragUtils.components.get(key).load(componentData);
            }
        }
    }
}