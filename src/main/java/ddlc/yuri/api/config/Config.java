package ddlc.yuri.api.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ddlc.yuri.Yuri;
import ddlc.yuri.modules.Module;
import ddlc.yuri.utils.render.DragUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class Config implements Serializable {

    private final String name;
    private final File file;

    public Config(String name) {
        this.name = name;
        this.file = new File(ConfigManager.CONFIGS_DIR, name + ConfigManager.EXTENSION);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    @Override
    public JsonObject save() {
        JsonObject jsonObject = new JsonObject();
        JsonObject modulesObject = new JsonObject();
        JsonObject draggingObject = new JsonObject();

        for (Module module : Yuri.INSTANCE.getModuleManager().getModules())
            modulesObject.add(module.getLabel(), module.save(false));

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
                if (modulesObject.has(module.getLabel()))
                    module.load(modulesObject.getAsJsonObject(module.getLabel()));
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
