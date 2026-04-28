package org.tkit.onecx.onecxsvcgen.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class GitHubReleaseService {

    private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"");

    public String latestReleaseTag(String owner, String repo, String fallback) {
        // First try the human-facing redirect URL which points to the latest release.
        // GitHub redirects /releases/latest to /releases/tag/{tag} so we can extract the tag from
        // the Location header without hitting the API (works without auth and has less strict rate-limits).
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://github.com/" + owner + "/" + repo + "/releases/latest"))
                    .header("User-Agent", "onecx-svc-generator")
                    .GET()
                    .build();
            HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            int sc = resp.statusCode();
            if (sc == 302 || sc == 301 || sc == 307 || sc == 308) {
                String loc = resp.headers().firstValue("location").orElse(null);
                if (loc != null) {
                    // location typically ends with /releases/tag/vX.Y.Z or /tag/vX.Y.Z
                    int idx = loc.lastIndexOf("/tag/");
                    String tag = null;
                    if (idx >= 0) {
                        tag = loc.substring(idx + 5);
                    } else {
                        // fallback: last path segment
                        int s = loc.lastIndexOf('/');
                        if (s >= 0 && s + 1 < loc.length()) {
                            tag = loc.substring(s + 1);
                        }
                    }
                    if (tag != null && !tag.isBlank()) {
                        // strip leading v if present
                        if (tag.startsWith("v") || tag.startsWith("V")) {
                            tag = tag.substring(1);
                        }
                        System.out.println("[onecx-svc-generator] Resolved latest release via redirect for " + owner + "/" + repo + ": " + tag);
                        return tag;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[onecx-svc-generator] Redirect resolution failed for " + owner + "/" + repo + ": " + e.getMessage());
        }

        // Fallback to GitHub API releases/latest endpoint if the redirect approach didn't work
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest"))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "onecx-svc-generator")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Matcher matcher = TAG_NAME.matcher(response.body());
                if (matcher.find()) {
                    String tag = matcher.group(1);
                    System.out.println("[onecx-svc-generator] Resolved latest release via API for " + owner + "/" + repo + ": " + tag);
                    return tag;
                }
            } else {
                System.out.println("[onecx-svc-generator] API request returned status " + response.statusCode() + " for " + owner + "/" + repo);
            }
        } catch (Exception ignored) {
            System.out.println("[onecx-svc-generator] API resolution failed for " + owner + "/" + repo + ": " + ignored.getMessage());
        }
        System.out.println("[onecx-svc-generator] Falling back to version: " + fallback + " for " + owner + "/" + repo);
        return fallback;
    }

}
