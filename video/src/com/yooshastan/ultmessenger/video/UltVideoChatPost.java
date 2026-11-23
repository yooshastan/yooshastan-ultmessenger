package com.yooshastan.ultmessenger.video;

import android.content.Context;

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
        if (DialogObject.isUserDialog(dialogId)) return true;
        if (!DialogObject.isChatDialog(dialogId)) return false;

        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.Chat chat = mc.getChat(-dialogId);
        if (chat == null || chat.forum || chat.monoforum
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
                                  boolean outgoingLink) {
        if (!outgoingLink || !UltVideoAutoSave.canAutoPost(account)
                || !safeDestination(account, dialogId, original)
                || completedVideo == null || !completedVideo.isFile()
                || !completedVideo.getName().toLowerCase(Locale.US).endsWith(".mp4")
                || completedVideo.length() <= 0 || completedVideo.length() > MAX_BYTES) {
            return false;
        }
        File folder = new File(context.getFilesDir(), SUBDIR);
        if (!folder.isDirectory() && !folder.mkdirs()) return false;
        cleanupOldFiles(folder);
        File staged = new File(folder, "ult_" + UUID.randomUUID() + ".mp4");
        boolean stagedOk = false;
        try (FileInputStream in = new FileInputStream(completedVideo);
             FileOutputStream out = new FileOutputStream(staged)) {
            byte[] buffer = new byte[65536];
            long copied = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (!UltVideoAutoSave.canAutoPost(account)
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
            return false;
        }

        AndroidUtilities.runOnUIThread(() -> {
            if (!UltVideoAutoSave.canAutoPost(account)
                    || !safeDestination(account, dialogId, original)) {
                staged.delete();
                UltVideoAutoSave.noteResult(account,
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
                UltVideoAutoSave.noteResult(account,
                        "Video queued as a reply in the originating chat; check delivery");
            } catch (RuntimeException ignored) {
                UltVideoAutoSave.noteResult(account,
                        "Video saved locally; Telegram rejected media send");
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
