package com.yooshastan.ultmessenger.video;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure, testable link discovery policy. Never treats a web page as a video file. */
public final class UltVideoLinkRules {
    private static final Pattern URLS = Pattern.compile("https://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final Pattern META = Pattern.compile("<meta\\b[^>]{0,2048}>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTR = Pattern.compile("([\\w:-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");
    private static final int MAX_LINKS = 3;
    private UltVideoLinkRules() { }

    public static List<String> findUrls(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null) return new ArrayList<>(found);
        Matcher matcher = URLS.matcher(text);
        while (matcher.find() && found.size() < MAX_LINKS) {
            String candidate = matcher.group();
            while (!candidate.isEmpty() && ".,!?:;)]}".indexOf(candidate.charAt(candidate.length() - 1)) >= 0) {
                candidate = candidate.substring(0, candidate.length() - 1);
            }
            if (isAllowedUrl(candidate)) found.add(candidate);
        }
        return new ArrayList<>(found);
    }

    public static boolean isAllowedUrl(String value) {
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo() != null
                    || uri.getRawFragment() != null || uri.getPort() != -1 && uri.getPort() != 443) {
                return false;
            }
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.US);
            if (host.length() > 253 || !host.contains(".") || host.startsWith(".")
                    || host.endsWith(".") || host.contains(":")
                    || host.endsWith(".local") || host.endsWith(".localhost")
                    || host.endsWith(".lan") || host.endsWith(".internal")
                    || host.endsWith(".home") || host.endsWith(".onion")
                    || host.endsWith(".test") || host.endsWith(".invalid")
                    || host.endsWith(".example") || host.endsWith(".arpa")
                    || host.matches("[0-9.]+")) return false;
            for (String label : host.split("\\.")) {
                if (label.isEmpty() || label.length() > 63
                        || !label.matches("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")) return false;
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Only use dynamic site extraction for explicitly recognized platforms. */
    public static boolean isSiteUrl(String value) {
        if (!isAllowedUrl(value)) return false;
        try {
            String host = new URI(value).getHost().toLowerCase(Locale.US);
            String[] allowed = {"youtube.com", "youtu.be", "youtube-nocookie.com",
                    "instagram.com", "vimeo.com", "tiktok.com", "twitter.com", "x.com",
                    "facebook.com", "fb.watch", "threads.net", "threads.com", "reddit.com", "redd.it"};
            for (String domain : allowed) {
                if (host.equals(domain) || host.endsWith("." + domain)) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** Context-menu eligibility is narrower than the automatic extractor host list.
     * Match known media/share routes, not profiles, search pages or homepages. */
    public static boolean isManualMediaUrl(String value) {
        if (!isSiteUrl(value)) return false;
        try {
            URI u = new URI(value);
            String host = u.getHost().toLowerCase(Locale.US);
            String p = u.getRawPath();
            if (p == null) return false;
            String q = u.getRawQuery();
            if (isDomain(host, "youtu.be")) return p.matches("/[A-Za-z0-9_-]+/?");
            if (isDomain(host, "youtube.com") || isDomain(host, "youtube-nocookie.com"))
                return p.matches("/(?:shorts|live|embed)/[A-Za-z0-9_-]+/?")
                        || (p.matches("/watch/?") && q != null && q.matches("(?:.*&)?v=[A-Za-z0-9_-]+(?:&.*)?"));
            if (isDomain(host, "instagram.com")) return p.matches("/(?:[A-Za-z0-9._-]+/)?(?:p|reel|reels|tv)/[A-Za-z0-9_-]+/?");
            if (isDomain(host, "x.com") || isDomain(host, "twitter.com")) return p.matches("/(?:[A-Za-z0-9_]+|i/web)/status/[0-9]+(?:/.*)?");
            if (isDomain(host, "tiktok.com"))
                return p.matches("/@[A-Za-z0-9._-]+/(?:video|photo)/[0-9]+/?")
                        || p.matches("/t/[A-Za-z0-9_-]+/?")
                        || ((host.equals("vm.tiktok.com") || host.equals("vt.tiktok.com")) && p.matches("/[A-Za-z0-9_-]+/?"));
            if (isDomain(host, "fb.watch")) return p.matches("/[A-Za-z0-9_-]+/?");
            if (isDomain(host, "facebook.com")) return p.matches("/(?:reel|share|videos)/[^/].*")
                        || p.matches("/[^/]+/videos/[^/].*")
                        || (p.matches("/(?:watch/?|video[.]php)") && q != null && q.matches("(?:.*&)?v=[0-9]+(?:&.*)?"));
            if (isDomain(host, "vimeo.com")) return p.matches("/(?:.*?/)?[0-9]+(?:/[A-Za-z0-9]+)?/?");
            if (isDomain(host, "threads.com") || isDomain(host, "threads.net")) return p.matches("/@[A-Za-z0-9._-]+/post/[A-Za-z0-9_-]+/?");
            if (isDomain(host, "reddit.com")) return p.matches("/(?:r/[^/]+/)?comments/[A-Za-z0-9]+(?:/.*)?")
                        || p.matches("/r/[^/]+/s/[A-Za-z0-9_-]+/?");
            if (isDomain(host, "redd.it")) return p.matches("/[A-Za-z0-9_-]+/?");
        } catch (Exception ignored) { }
        return false;
    }

    /** Only a category is stored for diagnostics; no URL/path/query is persisted. */
    public static String sourceCategory(String url) {
        if (!isAllowedUrl(url)) return "Invalid/excluded link";
        try {
            String host = new URI(url).getHost().toLowerCase(Locale.US);
            if (isDomain(host, "youtube.com") || isDomain(host, "youtu.be")
                    || isDomain(host, "youtube-nocookie.com")) return "YouTube";
            if (isDomain(host, "instagram.com")) return "Instagram";
            if (isDomain(host, "tiktok.com")) return "TikTok";
            if (isDomain(host, "vimeo.com")) return "Vimeo";
            if (isDomain(host, "facebook.com") || isDomain(host, "fb.watch")) return "Facebook";
            if (isDomain(host, "twitter.com") || isDomain(host, "x.com")) return "X/Twitter";
            if (isDomain(host, "threads.net") || isDomain(host, "threads.com")) return "Threads";
            if (isDomain(host, "reddit.com") || isDomain(host, "redd.it")) return "Reddit";
            if (hasVideoExtension(url)) return "Direct video file";
            return "Other HTTPS webpage (metadata only)";
        } catch (Exception ignored) { return "Invalid/excluded link"; }
    }

    private static boolean isDomain(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }

    /** Audio-only mode is opt-in and uses the original audio stream when possible. */
    public static String audioFormat() { return "bestaudio[ext=m4a]/bestaudio/best"; }

    /** Explicit quality limits apply to site downloads only; default is unchanged. */
    public static String videoFormat(int maxHeight) {
        if (maxHeight != 480 && maxHeight != 720 && maxHeight != 1080) {
            return "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b";
        }
        String h = "[height<=" + maxHeight + "]";
        return "bv*[ext=mp4]" + h + "+ba[ext=m4a]/b[ext=mp4]" + h + "/b" + h;
    }

    /** One offline sanitizer for all supported public site downloads. */
    public static String extractionUrl(String url) {
        if (!isSiteUrl(url)) return url;
        return com.yooshastan.ultmessenger.settings.UltLinkSanitizerPolicy.cleanUrl(url);
    }

    public static boolean hasVideoExtension(String url) {
        try {
            String path = new URI(url).getPath();
            if (path == null) return false;
            path = path.toLowerCase(Locale.US);
            return path.endsWith(".mp4") || path.endsWith(".webm")
                    || path.endsWith(".mov") || path.endsWith(".m4v")
                    || path.endsWith(".3gp") || path.endsWith(".mkv");
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Extract standard HTML social/OG video URLs; not a JavaScript/site-specific extractor. */
    public static String htmlVideoUrl(String html) {
        if (html == null) return null;
        Matcher tags = META.matcher(html.substring(0, Math.min(html.length(), 65536)));
        while (tags.find()) {
            Matcher attributes = ATTR.matcher(tags.group());
            Map<String, String> values = new HashMap<>();
            while (attributes.find()) {
                String value = attributes.group(2) != null ? attributes.group(2)
                        : attributes.group(3) != null ? attributes.group(3) : attributes.group(4);
                values.put(attributes.group(1).toLowerCase(Locale.US), value);
            }
            String property = values.containsKey("property") ? values.get("property") : values.get("name");
            if (property == null) continue;
            property = property.toLowerCase(Locale.US);
            if (!property.equals("og:video") && !property.equals("og:video:url")
                    && !property.equals("og:video:secure_url")
                    && !property.equals("twitter:player:stream")) continue;
            String url = values.get("content");
            if (url == null) continue;
            url = url.replace("&amp;", "&").replace("&#38;", "&").replace("&#x26;", "&");
            if (isAllowedUrl(url)) return url;
        }
        return null;
    }
}
