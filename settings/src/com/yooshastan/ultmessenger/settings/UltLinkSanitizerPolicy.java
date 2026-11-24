package com.yooshastan.ultmessenger.settings;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline, conservative tracking-parameter removal. Never resolves, fetches or
 * expands links; do not normalize signed or authentication-bearing URLs.
 */
public final class UltLinkSanitizerPolicy {
    private static final Pattern URL = Pattern.compile("https://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final int MAX_TEXT = 65536;
    private static final int MAX_LINKS = 12;
    private static final int MAX_URL = 4096;

    private UltLinkSanitizerPolicy() { }

    public static String cleanText(String text) {
        if (text == null || text.length() > MAX_TEXT || text.isEmpty()) return text;
        Matcher matcher = URL.matcher(text);
        StringBuilder result = new StringBuilder(text.length());
        int last = 0, checked = 0;
        while (matcher.find() && checked++ < MAX_LINKS) {
            result.append(text, last, matcher.start());
            String captured = matcher.group();
            int end = captured.length();
            while (end > 0 && ".,!;:)]}".indexOf(captured.charAt(end - 1)) >= 0) end--;
            String body = captured.substring(0, end);
            result.append(cleanUrl(body)).append(captured.substring(end));
            last = matcher.end();
        }
        result.append(text, last, text.length());
        return result.toString();
    }

    public static String cleanUrl(String raw) {
        if (raw == null || raw.length() > MAX_URL) return raw;
        try {
            URI uri = new URI(raw);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.isOpaque()
                    || uri.getRawUserInfo() != null || uri.getPort() != -1) return raw;
            String hostname = uri.getHost();
            if (hostname == null) return raw;
            String host = hostname.toLowerCase(Locale.US);
            if (!isPublicHostname(host)) return raw;
            String query = uri.getRawQuery();
            if (query == null || query.isEmpty()) return raw;
            String[] params = query.split("&", -1);
            List<String> kept = new ArrayList<>();
            int removed = 0;
            for (String param : params) {
                String rawKey = param.split("=", 2)[0];
                String key = rawKey.toLowerCase(Locale.US);
                if (looksSensitive(key) || (isHost(host, "vimeo.com") && key.equals("h"))) return raw;  // Preserve signed/challenge/private links.
                if (isTrackingKey(host, uri.getRawPath(), key, param)) removed++;
                else kept.add(param);
            }
            if (removed == 0) return raw;
            int question = raw.indexOf('?');
            if (question < 0) return raw;
            int fragment = raw.indexOf('#', question);
            String tail = fragment < 0 ? "" : raw.substring(fragment);
            StringBuilder cleaned = new StringBuilder(raw.substring(0, question));
            if (!kept.isEmpty()) cleaned.append('?').append(String.join("&", kept));
            cleaned.append(tail);
            return cleaned.toString();
        } catch (Exception ignored) {
            return raw;
        }
    }

    private static boolean isPublicHostname(String host) {
        if (!host.contains(".") || host.length() > 253 || host.endsWith(".local")
                || host.endsWith(".localhost") || host.endsWith(".lan")
                || host.endsWith(".internal") || host.endsWith(".onion")
                || host.endsWith(".test") || host.endsWith(".invalid")
                || host.endsWith(".example") || host.endsWith(".arpa")
                || host.matches("[0-9.]+") || host.contains(":")) return false;
        for (String label : host.split("\\.")) {
            if (!label.matches("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")) return false;
        }
        return true;
    }

    private static boolean isHost(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }

    private static boolean isTrackingKey(String host, String path, String key, String param) {
        if (key.startsWith("utm_")) return true;
        if (key.equals("fbclid") || key.equals("gclid") || key.equals("dclid")
                || key.equals("msclkid") || key.equals("mc_cid") || key.equals("mc_eid")
                || key.equals("_hsenc") || key.equals("_hsmi")
                || key.equals("ttclid") || key.equals("twclid") || key.equals("rdt_cid")
                || key.equals("gbraid") || key.equals("wbraid")) return true;
        // Instagram rotates its share-attribution token names. Keep this
        // exact, site-scoped allowlist rather than stripping all four-letter
        // query keys: e.g. img_index and story_media_id are functional.
        if (isHost(host, "instagram.com")
                && (key.equals("igshid") || key.equals("igsh")
                    || key.equals("igsi") || key.equals("ig_rid")
                    || key.equals("stkn") || key.equals("avpt")
                    || key.equals("bqrl") || key.equals("cnvx")
                    || key.equals("cplk") || key.equals("dlrf")
                    || key.equals("dmpr") || key.equals("evlk")
                    || key.equals("exln") || key.equals("fqnt")
                    || key.equals("gwpx") || key.equals("hvrq")
                    || key.equals("jplx") || key.equals("knvt")
                    || key.equals("lqvp") || key.equals("mdxt")
                    || key.equals("obrf") || key.equals("psln")
                    || key.equals("rpxt") || key.equals("srtk")
                    || key.equals("vrfl") || key.equals("xtok"))) return true;
        // X share URLs commonly append s (share source) and t (share token).
        // Restrict these short keys to canonical public status paths on X/Twitter.
        if ((host.equals("x.com") || host.equals("www.x.com") || host.equals("mobile.x.com")
                || host.equals("twitter.com") || host.equals("www.twitter.com") || host.equals("mobile.twitter.com"))
                && path != null && path.matches("/(?:[A-Za-z0-9_]+/status|i/web/status)/[0-9]+/?")
                && (key.equals("s") || key.equals("t"))) return true;
        if ((isHost(host, "youtu.be") || isHost(host, "youtube.com")
                || isHost(host, "youtube-nocookie.com"))
                && key.equals("si")) return true;

        // Do not expand opaque short links or remove their destination path.
        if (isHost(host, "tiktok.com") && path != null
                && (path.matches("/@[A-Za-z0-9._-]+/(?:video|photo)/[0-9]+/?")
                    || path.matches("/(?:t|v)/[A-Za-z0-9._-]+/?")
                    || ((host.equals("vm.tiktok.com") || host.equals("vt.tiktok.com"))
                        && path.matches("/[A-Za-z0-9_-]+/?")))) {
            return oneOf(key, "_r", "_t", "tt_medium", "tt_from", "is_from_webapp",
                    "sender_device", "sender_web_id", "is_copy_url", "share_link_id",
                    "share_app_id", "share_item_id", "share_iid", "u_code");
        }
        if ((isHost(host, "facebook.com") || host.equals("fb.watch") || host.equals("fb.com"))
                && path != null && (path.matches("/(?:reel|videos|posts|share|watch|story.php|permalink.php)(?:/.*)?")
                    || path.matches("/[A-Za-z0-9._-]+/(?:videos|posts|reels)/[^/]+/?")
                    || (host.equals("fb.watch") && path.matches("/[A-Za-z0-9_-]+/?")))) {
            return oneOf(key, "mibextid", "rdid", "shbid", "shbv", "sfnsn",
                    "__cft__", "__tn__", "comment_tracking", "referral_code");
        }
        if ((isHost(host, "reddit.com") || host.equals("redd.it"))
                && path != null && (path.matches("/(?:r/[A-Za-z0-9_]+/)?comments/[A-Za-z0-9]+(?:/.*)?")
                    || path.matches("/r/[A-Za-z0-9_]+/s/[A-Za-z0-9_-]+/?")
                    || (host.equals("redd.it") && path.matches("/[A-Za-z0-9]+/?")))) {
            return oneOf(key, "share_id", "rdt");
        }
        if ((isHost(host, "threads.net") || isHost(host, "threads.com"))
                && path != null && path.matches("/@[A-Za-z0-9._-]+/post/[A-Za-z0-9_-]+/?")) {
            return oneOf(key, "xmt", "igsh", "igshid");
        }
        // Only remove Vimeo's explicit copy attribution, never unlisted IDs or
        // playback settings. The h parameter is protected in the caller.
        if (isHost(host, "vimeo.com") && path != null
                && path.matches("/(?:video/)?[0-9]+/?")) {
            return key.equals("share") && param.equalsIgnoreCase("share=copy");
        }
        return false;
    }

    private static boolean oneOf(String key, String... values) {
        for (String value : values) if (value.equals(key)) return true;
        return false;
    }

    private static boolean looksSensitive(String key) {
        // When any parameter suggests a signed, auth or short-lived request,
        // preserve the entire URL, not just that one parameter.
        return key.contains("%") || key.contains(";") || key.startsWith("x-amz-")
                || key.startsWith("x-goog-") || key.startsWith("x-oss-")
                || key.startsWith("x-ms-") || key.startsWith("policy")
                || key.startsWith("signature") || key.equals("sig")
                || key.equals("token") || key.equals("access_token")
                || key.equals("refresh_token") || key.equals("auth")
                || key.equals("authorization") || key.equals("key")
                || key.equals("api_key") || key.equals("apikey")
                || key.equals("expires") || key.equals("expiration")
                || key.equals("expire") || key.equals("exp")
                || key.equals("code") || key.equals("state")
                || key.equals("session") || key.equals("sessionid")
                || key.equals("awsaccesskeyid") || key.equals("credential")
                || key.equals("download_token") || key.equals("hash")
                || key.equals("key-pair-id") || key.equals("hdnea") || key.equals("hdnts")
                || key.equals("solution") || key.equals("js_challenge") || key.startsWith("jsc_")
                || key.equals("cf_clearance") || key.startsWith("__cf_chl")
                || key.equals("access_key") || key.equals("accesskey") || key.equals("authkey");
    }
}
