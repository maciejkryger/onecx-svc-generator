///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class onecx_svc_generator {

    private static final String REPO_OWNER = "maciejkryger";
    private static final String REPO_NAME = "onecx-svc-generator";
    private static final String FALLBACK_VERSION = "3.1.0";

    public static void main(String... args) throws Exception {
        String version = System.getProperty("onecx.svc.generator.version");
        if (version == null || version.isBlank()) {
            version = latestReleaseTag(REPO_OWNER, REPO_NAME, FALLBACK_VERSION);
        }
        String url = String.format(
                "https://github.com/%s/%s/releases/download/v%s/onecx-svc-generator-%s-runner.jar",
                REPO_OWNER, REPO_NAME, version, version);

        Path cacheDir = Path.of(System.getProperty("user.home"), ".cache", "onecx-svc-generator");
        Files.createDirectories(cacheDir);
        Path jar = cacheDir.resolve("onecx-svc-generator-" + version + "-runner.jar");

        if (Files.notExists(jar)) {
            try (InputStream in = URI.create(url).toURL().openStream()) {
                Files.copy(in, jar, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        cmd.add("-jar");
        cmd.add(jar.toString());
        cmd.addAll(Arrays.asList(args));

        Process p = new ProcessBuilder(cmd).inheritIO().start();
        System.exit(p.waitFor());
    }

    static String latestReleaseTag(String owner, String repo, String fallback) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest"))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "onecx-svc-generator-launcher")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Matcher m = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"").matcher(response.body());
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
