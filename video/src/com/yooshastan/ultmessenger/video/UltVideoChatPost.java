package com.yooshastan.ultmessenger.video;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * Opt-in native Telegram media send after downloading a video link authored
 * by this account. Group auto-post is permitted only if the currently active
 * "send as" identity STILL matches the one used by the original message.
 *
 * No anonymous/admin identity is replaced by the personal account. Channels,
 * forum topics, paid/group-restricted chats are withheld until supported
 * with the correct author, topic and payment context.
 */
public final class UltVideoChatPost {
    private static final long MAX_BYTES = 100L * 1024L * 1024L;
    private static final long MAX_STAGING_AGE_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final String SUBDIR = "ult_video_outgoing_pending";

    private UltVideoChatPost() { }

    // Same reservation for automatic and explicit video posting. Do not store URLs.
    private static final java.util.Set<String> PENDING = new java.util.HashSet<>();
    private static final java.util.LinkedHashSet<String> HANDED_OFF = new java.util.LinkedHashSet<>();
    public static synchronized boolean alreadyRequested(String key) {
        return PENDING.contains(key) || HANDED_OFF.contains(key);
    }
    private static synchronized boolean reserve(String key) {
        if (alreadyRequested(key)) return false;
        return PENDING.add(key);
    }
    private static synchronized void release(String key, boolean handedOff) {
        PENDING.remove(key);
        if (handedOff) {
            HANDED_OFF.add(key);
            if (HANDED_OFF.size() > 1000) HANDED_OFF.remove(HANDED_OFF.iterator().next());
        }
    }
    public static String handoffKey(int account, long ownerId, long dialogId, int messageId, String url, UltMediaOptions options) {
        String input = account + ":" + ownerId + ":" + dialogId + ":" + messageId + ":" + UltVideoLinkRules.extractionUrl(url) + ":" + options.handoffVariant();
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
    private static boolean permitted(int account, long dialogId, TLRPC.Message original,
            boolean outgoingLink, long ownerId, UltChatMediaDraft.Target manualTarget,
            UltMediaOptions options, java.util.concurrent.atomic.AtomicBoolean cancelled) {
        if (cancelled.get() || ownerId != org.telegram.messenger.UserConfig.getInstance(account).getClientUserId()) return false;
        if (manualTarget != null) {
            return options.directChatPost() && manualTarget.account == account
                    && manualTarget.owner == ownerId && manualTarget.dialog == dialogId
                    && manualTarget.original == original
                    && UltChatMediaDraft.blockReason(manualTarget, options) == null;
        }
        return !options.audio && outgoingLink && UltVideoAutoSave.canAutoPost(account)
                && safeDestination(account, dialogId, original);
    }

    /**
     * Validate the destination on the UI thread again immediately before
     * creating Telegram's send request, not only before downloading.
     */
    public static boolean safeDestination(int account, long dialogId, TLRPC.Message original) {
        if (original == null || !original.out || original.id <= 0
                || original.noforwards || original.peer_id == null || dialogId == 0
                || DialogObject.isEncryptedDialog(dialogId)
                || MessageObject.getDialogId(original) != dialogId) {
            return false;
        }
        if (MessagesController.getInstance(account).getSendPaidMessagesStars(dialogId) > 0) return false;
        if (DialogObject.isUserDialog(dialogId)) return true;
        if (!DialogObject.isChatDialog(dialogId)) return false;

        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.Chat chat = mc.getChat(-dialogId);
        if (chat == null || chat.noforwards || chat.forum || chat.monoforum
                || ChatObject.isChannelAndNotMegaGroup(chat)
                || !ChatObject.canSendVideo(chat)
                || mc.getSendPaidMessagesStars(dialogId) > 0) {
            return false;
        }

        // For an anonymous group admin this is the group's/channel's peer,
        // NOT the user's personal ID. The regular native media send path gets
        // its sender from ChatObject.getSendAsPeerId just before dispatch.
        // Only auto-post while the same identity is selected.
        final long originalSender = MessageObject.getPeerId(original.from_id);
        final long activeSender = ChatObject.getSendAsPeerId(
                chat, mc.getChatFull(-dialogId), true);
        return originalSender != 0 && originalSender == activeSender;
    }

    public static boolean enqueue(Context context, int account, long dialogId,
                                  TLRPC.Message original, File completedVideo,
                                  boolean outgoingLink, long ownerId, String url,
                                  UltChatMediaDraft.Target manualTarget, UltMediaOptions options,
                                  java.util.concurrent.atomic.AtomicBoolean cancelled, Uri published) {
        if (!permitted(account, dialogId, original, outgoingLink, ownerId, manualTarget, options, cancelled)) {
            UltVideoAutoSave.noteChatResult(account, "Saved locally; chat posting policy blocked the send");
            return false;
        }
        if (completedVideo == null || !completedVideo.isFile()
                || !completedVideo.getName().toLowerCase(Locale.US).endsWith("." + options.extension())
                || completedVideo.length() <= 0 || completedVideo.length() > MAX_BYTES) {
            UltVideoAutoSave.noteChatResult(account, "Saved locally; unsupported attachment or over 100 MiB");
            return false;
        }
        String key = handoffKey(account, ownerId, dialogId, original.id, url, options);
        if (!reserve(key)) {
            UltVideoAutoSave.noteChatResult(account, "Media already queued for this link and format; check chat delivery");
            return false;
        }
        // Telegram rejects private internal paths for document/audio sends.
        // Only use the exact MediaStore row just published by this worker;
        // never accept an arbitrary external URI from a message or user input.
        if (options.audio) {
            if (published == null || !"content".equals(published.getScheme())
                    || !MediaStore.AUTHORITY.equals(published.getAuthority())) {
                release(key, false);
                UltVideoAutoSave.noteChatResult(account, "Saved locally; missing published audio URI");
                return false;
            }
            AndroidUtilities.runOnUIThread(() -> {
                if (!permitted(account, dialogId, original, outgoingLink, ownerId, manualTarget, options, cancelled)) {
                    release(key, false);
                    UltVideoAutoSave.noteChatResult(account, "Saved locally; audio send policy changed");
                    return;
                }
                try {
                    MessageObject replyToLink = DialogObject.isChatDialog(dialogId)
                            ? new MessageObject(account, original, false, false) : null;
                    // Null MIME lets native document preparation infer the exact
                    // extension from MediaStore's display name (m4a/mp3/opus).
                    // Passing audio/mp4 could otherwise select an mp4 extension.
                    SendMessagesHelper.prepareSendingDocument(AccountInstance.getInstance(account),
                            null, null, published, "", null, dialogId, replyToLink,
                            null, null, null, null, true, 0, null, null, false);
                    release(key, true);
                    UltVideoAutoSave.noteChatResult(account, options.audioFormat.toUpperCase(Locale.US)
                            + " audio submitted to Telegram; delivery not yet confirmed");
                } catch (RuntimeException ignored) {
                    release(key, false);
                    UltVideoAutoSave.noteChatResult(account, "Saved locally; Telegram rejected audio handoff");
                }
            });
            return true;
        }
        File folder = new File(context.getFilesDir(), SUBDIR);
        if (!folder.isDirectory() && !folder.mkdirs()) {
            release(key, false);
            UltVideoAutoSave.noteChatResult(account, "Saved locally; unable to open private upload directory");
            return false;
        }
        cleanupOldFiles(folder);
        File staged = new File(folder, "ult_" + UUID.randomUUID() + "." + options.extension());
        boolean stagedOk = false;
        try (FileInputStream in = new FileInputStream(completedVideo);
             FileOutputStream out = new FileOutputStream(staged)) {
            byte[] buffer = new byte[65536];
            long copied = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (!permitted(account, dialogId, original, outgoingLink, ownerId, manualTarget, options, cancelled)
                        || copied + n > MAX_BYTES) throw new IOException("Cancelled or oversized");
                out.write(buffer, 0, n);
                copied += n;
            }
            out.getFD().sync();
            stagedOk = copied > 0 && copied == completedVideo.length();
        } catch (IOException ignored) {
            // Never log a private URL or a video file path.
        }
        if (!stagedOk) {
            staged.delete();
            release(key, false);
            UltVideoAutoSave.noteChatResult(account, "Saved locally; private upload staging failed or was cancelled");
            return false;
        }

        AndroidUtilities.runOnUIThread(() -> {
            if (!permitted(account, dialogId, original, outgoingLink, ownerId, manualTarget, options, cancelled)) {
                staged.delete();
                release(key, false);
                UltVideoAutoSave.noteChatResult(account,
                        "Saved locally; group sender, rights or network policy changed");
                return;
            }
            try {
                final MessageObject replyToLink = DialogObject.isChatDialog(dialogId)
                        ? new MessageObject(account, original, false, false) : null;

                // Telegram's own upload queue uses the current send-as peer,
                // validated above. Replying to the original link anchors this
                // second message to the right conversation.
                SendMessagesHelper.prepareSendingVideo(
                        AccountInstance.getInstance(account),
                        staged.getAbsolutePath(),
                        null, null, null, dialogId,
                        replyToLink, null, null, null, null, 0, null,
                        true, 0, 0, false, false,
                        "", null, 0, 0);
                release(key, true);
                UltVideoAutoSave.noteChatResult(account,
                        "Video submitted to Telegram in originating chat; delivery not yet confirmed");
            } catch (RuntimeException ignored) {
                release(key, false);
                UltVideoAutoSave.noteChatResult(account,
                        "Media saved locally; Telegram rejected media send");
            }
        });
        return true;
    }

    private static void cleanupOldFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - MAX_STAGING_AGE_MS;
        for (File f : files) {
            if (f.isFile() && f.getName().startsWith("ult_")
                    && f.getName().endsWith(".mp4") && f.lastModified() < cutoff) {
                f.delete();
            }
        }
    }
}
