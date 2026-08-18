package ddlc.yuri.api.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GithubConfigFetcher {

    private static final String API_TREE_URL = "https://api.github.com/repos/unleg1t/yuri-configs/git/trees/main?recursive=1";
    private static final String RAW_BASE_URL = "https://raw.githubusercontent.com/unleg1t/yuri-configs/main/";

    private GithubConfigFetcher() {
    }

    public static List<String> listRemoteConfigs() {
        List<String> out = new ArrayList<>();
        try {
            URL url = new URL(API_TREE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                JsonParser parser = new JsonParser();
                JsonObject root = (JsonObject) parser.parse(reader);
                JsonArray tree = root.getAsJsonArray("tree");
                if (tree != null) {
                    for (JsonElement el : tree) {
                        JsonObject obj = el.getAsJsonObject();
                        String path = obj.get("path").getAsString();
                        if (path.toLowerCase().endsWith(".json")) {
                            out.add(path);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static boolean downloadRemoteConfig(String repoPath) {
        // legacy synchronous download
        return downloadRemoteConfigWithProgress(repoPath, null);
    }

    public static boolean downloadRemoteConfigWithProgress(String repoPath, ddlc.yuri.utils.render.progress.ProgressBarEntry entry) {
        if (repoPath == null || repoPath.isEmpty()) return false;
        try {
            URL url = new URL(RAW_BASE_URL + repoPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int contentLength = conn.getContentLength();

            try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream())) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int read;
                int total = 0;
                while ((read = in.read(buf)) != -1) {
                    bout.write(buf, 0, read);
                    total += read;
                    if (entry != null && contentLength > 0) {
                        entry.setProgress(Math.max(0f, Math.min(1f, (float) total / (float) contentLength)));
                    }
                }
                byte[] content = bout.toByteArray();
                File outFile = new File(ConfigManager.CONFIGS_DIR, new File(repoPath).getName());
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(content);
                }
                if (entry != null) entry.setProgress(1f);
                return true;
            }
        } catch (Exception ignored) {
            if (entry != null) entry.setProgress(0f);
            return false;
        }
    }
}
