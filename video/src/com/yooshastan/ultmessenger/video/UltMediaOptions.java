package com.yooshastan.ultmessenger.video;

/** Immutable per-request options; no global setting is changed by a manual download. */
public final class UltMediaOptions {
    public final boolean manual, audio, toChat;
    public final int height;
    public final String audioFormat;
    public UltMediaOptions(boolean manual, boolean audio, int height, String audioFormat) {
        this(manual, audio, height, audioFormat, false);
    }
    public UltMediaOptions(boolean manual, boolean audio, int height, String audioFormat, boolean toChat) {
        this.manual = manual;
        this.toChat = manual && toChat;
        this.audio = audio;
        this.height = height == 480 || height == 720 || height == 1080 ? height : 0;
        this.audioFormat = "mp3".equals(audioFormat) || "opus".equals(audioFormat) ? audioFormat : "m4a";
    }
    public boolean directVideoPost() { return toChat && !audio; }
    public String variant() { return (audio ? audioFormat : "video-" + height) + (toChat && audio ? "-chat" : ""); }
    public String mime() {
        if (!audio) return "video/mp4";
        return "mp3".equals(audioFormat) ? "audio/mpeg" : "opus".equals(audioFormat) ? "audio/ogg" : "audio/mp4";
    }
}
