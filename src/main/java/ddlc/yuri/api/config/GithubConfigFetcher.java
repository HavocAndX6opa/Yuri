package ddlc.yuri.api.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import ddlc.yuri.Yuri;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class GithubConfigFetcher {
    private static final String REPO_API_URL = "https://api.github.com/repos/unleg1t/yuri-configs/contents";
    private static final String RAW_URL = "https://raw.githubusercontent.com/unleg1t/yuri-configs/main/";
    private static final int TIMEOUT = 5000;

    private static final List<String> cache = new ArrayList<>();
    private static final List<Consumer<List<String>>> waiting = new CopyOnWriteArrayList<>();

    private static volatile boolean loaded;
    private static volatile boolean loading;

    public static boolean isLoaded() {
        return loaded;
    }

    public static List<String> getCachedConfigList() {
        return loaded ? copyOfCache() : null;
    }

    public static List<String> fetchConfigList() {
        if (loaded) return copyOfCache();

        load();

        return copyOfCache();
    }

    public static void fetchConfigListAsync(Consumer<List<String>> callback) {
        if (loaded) {
            callback.accept(copyOfCache());
            return;
        }

        waiting.add(callback);

        if (loading) return;

        loading = true;

        Thread thread = new Thread(() -> {
            load();

            loading = false;

            for (Consumer<List<String>> pending : waiting) {
                pending.accept(copyOfCache());
            }

            waiting.clear();
        }, "github-config-list");

        thread.setDaemon(true);
        thread.start();
    }

    public static void invalidate() {
        loaded = false;
    }

    private static List<String> load() {
        List<String> configs = new ArrayList<>();

        try {
            URL url = new URL(REPO_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();

            JsonArray files = new JsonParser().parse(response.toString()).getAsJsonArray();
            for (JsonElement element : files) {
                String name = element.getAsJsonObject().get("name").getAsString();
                if (name.endsWith(".json")) {
                    configs.add(name.replace(".json", ""));
                }
            }

            synchronized (GithubConfigFetcher.class) {
                cache.clear();
                cache.addAll(configs);
                loaded = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return configs;
    }

    private static List<String> copyOfCache() {
        synchronized (GithubConfigFetcher.class) {
            return new ArrayList<>(cache);
        }
    }

    public static boolean downloadAndLoadConfig(String configName) {
        try {
            String configUrl = RAW_URL + configName + ".json";
            URL url = new URL(configUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            conn.disconnect();

            java.io.File configFile = new java.io.File(ConfigManager.CONFIGS_DIR, configName + ".json");
            Files.write(Paths.get(configFile.getPath()), content.toString().getBytes());

            return Yuri.INSTANCE.getConfigManager().loadConfig(configName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}