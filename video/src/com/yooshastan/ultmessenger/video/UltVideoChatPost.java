package com.yooshastan.ultmessenger.video;

import android.content.Context;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SendMessagesHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Explicitly opt-in: send ONLY a successfully downloaded MP4 from an OWN,
 * server-confirmed text link to the same normal Telegram dialog.
 *
 * Do not delete the staged file until Telegram can finish its asynchronous
 * upload. A bounded age-based cleanup runs on subsequent successful staging.
 */
public final class UltVideoChatPost {
    private static final long MAX_BYTES = 100L * 1024L * 1024L;
    private static final long MAX_STAGING_AGE_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final String SUBDIR = "ult_video_outgoing_pending";

    private UltVideoChatPost() { }

    public static boolean enqueue(Context context, int account, long dialogId,
                                  int originalMessageId, File completedVideo,
                                  boolean outgoingLink) {
        if (!outgoingLink || originalMessageId == 0 || dialogId == 0
                || !UltVideoAutoSave.canAutoPost(account)
                || completedVideo == null || !completedVideo.isFile()
                || !completedVideo.getName().toLowerCase(java.util.Locale.US).endsWith(".mp4")
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
            // File contents and chat URLs are not logged.
        }
        if (!stagedOk) {
            staged.delete();
            return false;
        }

        AndroidUtilities.runOnUIThread(() -> {
            if (!UltVideoAutoSave.canAutoPost(account)) {
                staged.delete();
                UltVideoAutoSave.noteResult(account, "Auto-post stopped: option or network protection changed");
                return;
            }
            try {
                // This uses Telegram's native media upload/send queue.
                // It does not call any external posting API and does not bypass MTProxy.
                SendMessagesHelper.prepareSendingVideo(
                        AccountInstance.getInstance(account),
                        staged.getAbsolutePath(),
                        null, null, null, dialogId,
                        null, null, null, null, null, 0, null,
                        true, 0, 0, false, false,
                        "", null, 0, 0);
                UltVideoAutoSave.noteResult(account,
                        "Video handed to Telegram upload queue; verify delivery in chat");
            } catch (RuntimeException ignored) {
                UltVideoAutoSave.noteResult(account,
                        "Video saved locally; Telegram rejected auto-post preparation");
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
