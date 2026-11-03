package com.yooshastan.ultmessenger.settings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Local metadata-only index of permitted, already-downloaded document-backed media. */
public final class UltMediaIndex {
    public enum Kind { PHOTO, VIDEO, AUDIO, DOCUMENT }
    public enum Order { NEWEST, LARGEST, NAME }
    public static final class Entry {
        public final String name;
        public final Kind kind;
        public final long bytes;
        public final long messageEpochSeconds;
        public Entry(String name, String mime, long bytes, long seconds) {
            this.name = name == null ? "" : name;
            this.kind = kind(mime, this.name);
            this.bytes = Math.max(0L, bytes);
            this.messageEpochSeconds = Math.max(0L, seconds);
        }
    }
    private UltMediaIndex() {}

    public static Kind kind(String mime, String name) {
        final String m = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        final String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (m.startsWith("image/") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                || n.endsWith(".png") || n.endsWith(".webp")) return Kind.PHOTO;
        if (m.startsWith("video/") || n.endsWith(".mp4") || n.endsWith(".mkv")
                || n.endsWith(".webm")) return Kind.VIDEO;
        if (m.startsWith("audio/") || n.endsWith(".mp3") || n.endsWith(".flac")
                || n.endsWith(".opus") || n.endsWith(".ogg") || n.endsWith(".m4a"))
            return Kind.AUDIO;
        return Kind.DOCUMENT;
    }

    public static List<Entry> filter(List<Entry> all, Kind selected, String query, Order order) {
        ArrayList<Entry> result = new ArrayList<>();
        if (all == null) return result;
        final String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (Entry item : all) {
            if (item == null || (selected != null && item.kind != selected)) continue;
            if (!item.name.toLowerCase(Locale.ROOT).contains(needle)) continue;
            result.add(item);
        }
        final Order choice = order == null ? Order.NEWEST : order;
        if (choice == Order.LARGEST)
            result.sort(Comparator.comparingLong((Entry x) -> x.bytes).reversed()
                    .thenComparing(x -> x.name.toLowerCase(Locale.ROOT)));
        else if (choice == Order.NAME)
            result.sort(Comparator.comparing(x -> x.name.toLowerCase(Locale.ROOT)));
        else
            result.sort(Comparator.comparingLong((Entry x) -> x.messageEpochSeconds)
                    .reversed().thenComparing(x -> x.name.toLowerCase(Locale.ROOT)));
        return result;
    }
}
