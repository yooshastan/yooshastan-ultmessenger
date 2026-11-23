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
                    "facebook.com", "fb.watch", "threads.net"};
            for (String domain : allowed) {
                if (host.equals(domain) || host.endsWith("." + domain)) return true;
            }
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
            if (isDomain(host, "threads.net")) return "Threads";
            if (hasVideoExtension(url)) return "Direct video file";
            return "Other HTTPS webpage (metadata only)";
        } catch (Exception ignored) { return "Invalid/excluded link"; }
    }

    private static boolean isDomain(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
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
