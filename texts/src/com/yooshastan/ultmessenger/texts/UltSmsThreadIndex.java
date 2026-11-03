package com.yooshastan.ultmessenger.texts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Presentation-only SMS/MMS thread grouping.
 *
 * An eventual Android Telephony provider adapter supplies input after
 * UltTextsAccessPolicy permits it. No provider access, network, Telegram
 * account lookup, persistent cache or message export takes place here.
 */
public final class UltSmsThreadIndex {
    public enum Kind { SMS, MMS }

    public static final class Message {
        public final long threadId;
        public final long messageId;
        public final long timestampMs;
        public final String participant;
        public final String preview;
        public final boolean unread;
        public final Kind kind;

        public Message(long threadId, long messageId, long timestampMs,
                       String participant, String preview,
                       boolean unread, Kind kind) {
            if (threadId <= 0 || messageId <= 0 || timestampMs < 0 || kind == null) {
                throw new IllegalArgumentException("Invalid SMS/MMS row");
            }
            this.threadId = threadId;
            this.messageId = messageId;
            this.timestampMs = timestampMs;
            this.participant = participant == null ? "" : participant;
            this.preview = preview == null ? "" : preview;
            this.unread = unread;
            this.kind = kind;
        }
    }

    public static final class Thread {
        public final long threadId;
        public final long mostRecentMessageId;
        public final long updatedMs;
        public final int unreadMessages;
        public final String participant;
        public final String preview;
        public final Kind lastKind;

        private Thread(long threadId, long mostRecentMessageId, long updatedMs,
                       int unreadMessages, String participant,
                       String preview, Kind lastKind) {
            this.threadId = threadId;
            this.mostRecentMessageId = mostRecentMessageId;
            this.updatedMs = updatedMs;
            this.unreadMessages = unreadMessages;
            this.participant = participant;
            this.preview = preview;
            this.lastKind = lastKind;
        }
    }

    private UltSmsThreadIndex() {}

    public static List<Thread> group(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return Collections.emptyList();
        Map<Long, ArrayList<Message>> perThread = new HashMap<>();
        for (Message message : messages) {
            if (message != null) {
                perThread.computeIfAbsent(message.threadId, unused -> new ArrayList<>())
                        .add(message);
            }
        }
        ArrayList<Thread> threads = new ArrayList<>();
        for (Map.Entry<Long, ArrayList<Message>> bucket : perThread.entrySet()) {
            Message latest = null;
            int unread = 0;
            for (Message candidate : bucket.getValue()) {
                if (candidate.unread) unread++;
                if (latest == null || candidate.timestampMs > latest.timestampMs
                        || (candidate.timestampMs == latest.timestampMs
                        && candidate.messageId > latest.messageId)) {
                    latest = candidate;
                }
            }
            if (latest != null) {
                threads.add(new Thread(bucket.getKey(), latest.messageId, latest.timestampMs,
                        unread, latest.participant, latest.preview, latest.kind));
            }
        }
        threads.sort(Comparator.comparingLong((Thread item) -> item.updatedMs)
                .reversed().thenComparingLong(item -> item.threadId));
        return Collections.unmodifiableList(threads);
    }
}
