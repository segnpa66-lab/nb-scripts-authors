package gg.nulls.library;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class UpdateManager {
    private static final String LATEST = "https://api.github.com/repos/segnpa66-lab/nb-scripts-authors/releases/latest";
    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor();
    private static final Handler UI = new Handler(Looper.getMainLooper());

    static final class Release {
        final String version, url, sha256;
        final long size;
        Release(String version, String url, String sha256, long size) {
            this.version = version; this.url = url; this.sha256 = sha256; this.size = size;
        }
    }

    static int compareVersions(String first, String second) {
        String[] a = first.replaceFirst("^[vV]", "").split("\\.");
        String[] b = second.replaceFirst("^[vV]", "").split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? part(a[i]) : 0, y = i < b.length ? part(b[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    private static int part(String value) {
        try { return Integer.parseInt(value.replaceFirst("[^0-9].*$", "")); }
        catch (NumberFormatException ignored) { return 0; }
    }

    static Release parse(JSONObject release) throws Exception {
        if (release.optBoolean("draft") || release.optBoolean("prerelease")) return null;
        String version = release.getString("tag_name");
        JSONArray assets = release.getJSONArray("assets");
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String url = asset.optString("browser_download_url");
            String digest = asset.optString("digest");
            long size = asset.optLong("size");
            if ("Script-Library.apk".equals(asset.optString("name"))
                    && url.startsWith("https://github.com/segnpa66-lab/nb-scripts-authors/releases/download/")
                    && digest.matches("sha256:[0-9a-fA-F]{64}") && size > 0 && size <= 100L * 1024 * 1024)
                return new Release(version, url, digest.substring(7).toLowerCase(Locale.ROOT), size);
        }
        return null;
    }

    static void check(String installedVersion, Consumer<Release> result) {
        NETWORK.execute(() -> {
            ExecutorService probe = Executors.newSingleThreadExecutor();
            Future<Release> task = probe.submit(() -> {
                HttpURLConnection connection = (HttpURLConnection) new URL(LATEST).openConnection();
                connection.setConnectTimeout(4500); connection.setReadTimeout(4500);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                try {
                    if (connection.getResponseCode() != 200) return null;
                    return parse(new JSONObject(Api.read(connection.getInputStream(), 512 * 1024)));
                } finally { connection.disconnect(); }
            });
            try {
                Release release = task.get(5, TimeUnit.SECONDS);
                if (release != null && compareVersions(release.version, installedVersion) > 0)
                    UI.post(() -> result.accept(release));
            } catch (Exception ignored) { task.cancel(true); }
            finally { probe.shutdownNow(); }
        });
    }

    static void download(Activity activity, Release release, Consumer<String> error) {
        NETWORK.execute(() -> {
            HttpURLConnection connection = null;
            File target = null;
            try {
                File directory = new File(activity.getCacheDir(), "updates");
                if (!directory.exists() && !directory.mkdirs()) throw new IOException("Не удалось подготовить загрузку");
                target = new File(directory, "Script-Library.apk");
                connection = (HttpURLConnection) new URL(release.url).openConnection();
                connection.setConnectTimeout(15000); connection.setReadTimeout(30000);
                if (connection.getResponseCode() != 200) throw new IOException("Не удалось скачать APK");
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long length = 0;
                try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(target)) {
                    byte[] bytes = new byte[32768]; int count;
                    while ((count = input.read(bytes)) != -1) {
                        length += count;
                        if (length > 100L * 1024 * 1024) throw new IOException("APK слишком большой");
                        digest.update(bytes, 0, count); output.write(bytes, 0, count);
                    }
                }
                StringBuilder checksum = new StringBuilder();
                for (byte value : digest.digest()) checksum.append(String.format(Locale.ROOT, "%02x", value & 255));
                if (length != release.size || !checksum.toString().equals(release.sha256)) throw new IOException("Контрольная сумма APK не совпадает");
                File apk = target;
                UI.post(() -> install(activity, apk, error));
            } catch (Exception exception) {
                if (target != null) target.delete();
                UI.post(() -> error.accept(exception.getMessage() == null ? "Не удалось загрузить обновление" : exception.getMessage()));
            } finally { if (connection != null) connection.disconnect(); }
        });
    }

    private static void install(Activity activity, File apk, Consumer<String> error) {
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName()));
            try { activity.startActivity(settings); error.accept("Разрешите установку из этого приложения и нажмите «Обновить» ещё раз."); }
            catch (Exception ignored) { error.accept("Разрешите установку приложений из этого источника в настройках Android."); }
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".updates", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Exception exception) { error.accept("Не удалось открыть установщик Android."); }
    }
}
