package com.yooshastan.ultmessenger.video;

import java.util.Locale;

/** Conservative categorization: NEVER display/log original URLs or credentials. */
public final class UltVideoFailure {
    private UltVideoFailure() { }

    public static String publicReason(Throwable thrown, String stage) {
        if (thrown == null) return "Video processing failed";
        String name = thrown.getClass().getSimpleName().toLowerCase(Locale.US);
        String message = thrown.getMessage() == null ? "" : thrown.getMessage().toLowerCase(Locale.US);
        if ("initialization".equals(stage)) {
            return "Embedded downloader initialization failed (runtime or Python package)";
        }
        if (message.contains("sign in") || message.contains("login")
                || message.contains("cookies") || message.contains("private video")
                || message.contains("http error 401") || message.contains("authentication required")
                || message.contains("authentication is required")) {
            return "Site requires login; no cookies or account credentials are used";
        }
        if (message.contains("http error 403") || message.contains("forbidden")
                || message.contains("access denied")) {
            return "Site denied access (403 or forbidden); public extraction may be unavailable";
        }
        if (message.contains("http error 429") || message.contains("rate limit")
                || message.contains("too many requests")) {
            return "Video site rate-limited the download";
        }
        if (message.contains("requested format") || message.contains("format is not available")
                || message.contains("no video formats")) {
            return "No usable video/audio format on this site";
        }
        if (message.contains("unsupported url") || message.contains("unsupported site")) {
            return "Site extractor does not support this video";
        }
        if (message.contains("ffmpeg") || message.contains("merge failed")) {
            return "Could not combine video and audio streams";
        }
        if (message.contains("unable to extract") || message.contains("signature extraction")
                || message.contains("javascript")) {
            return "Site changed its player/extraction requirements";
        }
        if (message.contains("timed out") || message.contains("timeout")
                || message.contains("network is unreachable")
                || message.contains("unable to download webpage")
                || message.contains("connection refused") || message.contains("ssl:")) {
            return "Network or TLS error during extraction";
        }
        if (message.contains("no space left") || message.contains("enospc")) {
            return "Not enough storage for the video";
        }
        if (name.contains("interrupt") || name.contains("cancel")) {
            return "Extractor interrupted or cancelled";
        }
        return "Extractor failed (unclassified); no private URL logged";
    }
}
