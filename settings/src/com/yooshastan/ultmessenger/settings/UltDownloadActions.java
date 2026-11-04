package com.yooshastan.ultmessenger.settings;

import java.util.ArrayList;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

/**
 * UI-level actions on Telegram's existing per-account downloads store.
 * No custom transfer engine, disk deletion, or changes to proxy/DNS paths.
 */
public final class UltDownloadActions {
    private UltDownloadActions() {}

    private static boolean same(MessageObject a, MessageObject b) {
        return a != null && b != null && a.getDocument() != null
                && b.getDocument() != null && a.getId() == b.getId()
                && a.getDialogId() == b.getDialogId()
                && a.getDocument().id == b.getDocument().id;
    }

    private static ArrayList<MessageObject> queue(int account) {
        return new ArrayList<>(DownloadController.getInstance(account).downloadingFiles);
    }

    public static boolean isQueued(int account, MessageObject message) {
        for (MessageObject entry : queue(account)) {
            if (same(entry, message)) return true;
        }
        return false;
    }

    public static boolean isRunning(int account, MessageObject message) {
        return isQueued(account, message) && FileLoader.getInstance(account)
                .isLoadingFile(message.getFileName());
    }

    public static int pauseAll(int account) {
        final FileLoader loader = FileLoader.getInstance(account);
        int paused = 0;
        for (MessageObject message : queue(account)) {
            final TLRPC.Document document = message.getDocument();
            if (document != null && loader.isLoadingFile(message.getFileName())) {
                loader.cancelLoadFile(document); // Same native pause as Telegram's list
                paused++;
            }
        }
        return paused;
    }

    public static int resumeAll(int account) {
        final FileLoader loader = FileLoader.getInstance(account);
        int resumed = 0;
        for (MessageObject message : queue(account)) {
            final TLRPC.Document document = message.getDocument();
            if (document != null && !loader.isLoadingFile(message.getFileName())) {
                message.putInDownloadsStore = true;
                loader.loadFile(document, message, FileLoader.PRIORITY_LOW, 0);
                resumed++;
            }
        }
        if (resumed > 0) DownloadController.getInstance(account).updateFilesLoadingPriority();
        return resumed;
    }

    /** Resume only the first paused queued document, leaving the rest paused. */
    public static boolean resumeNext(int account) {
        final FileLoader loader = FileLoader.getInstance(account);
        for (MessageObject message : queue(account)) {
            final TLRPC.Document document = message.getDocument();
            if (document != null && !loader.isLoadingFile(message.getFileName())) {
                message.putInDownloadsStore = true;
                loader.loadFile(document, message, FileLoader.PRIORITY_LOW, 0);
                DownloadController.getInstance(account).updateFilesLoadingPriority();
                return true;
            }
        }
        return false;
    }

    /** Snapshot of the native queue; no media content or file names exposed. */
    public static int[] queueCounts(int account) {
        final FileLoader loader = FileLoader.getInstance(account);
        int running = 0;
        int paused = 0;
        for (MessageObject message : queue(account)) {
            if (message.getDocument() == null) continue;
            if (loader.isLoadingFile(message.getFileName())) running++;
            else paused++;
        }
        return new int[]{running, paused, recentCount(account)};
    }

    public static boolean pauseOne(int account, MessageObject message) {
        if (!isRunning(account, message)) return false;
        FileLoader.getInstance(account).cancelLoadFile(message.getDocument());
        return true;
    }

    public static boolean resumeOne(int account, MessageObject message) {
        if (!isQueued(account, message) || isRunning(account, message)) return false;
        message.putInDownloadsStore = true;
        FileLoader.getInstance(account).loadFile(message.getDocument(), message,
                FileLoader.PRIORITY_LOW, 0);
        DownloadController.getInstance(account).updateFilesLoadingPriority();
        return true;
    }

    public static boolean moveToTop(int account, MessageObject message) {
        final DownloadController controller = DownloadController.getInstance(account);
        final ArrayList<MessageObject> entries = queue(account);
        int position = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (same(entries.get(i), message)) {
                position = i;
                break;
            }
        }
        if (position <= 0) return false;
        for (int i = position; i > 0; i--) {
            controller.swapLoadingPriority(entries.get(i), entries.get(i - 1));
            final MessageObject previous = entries.get(i - 1);
            entries.set(i - 1, entries.get(i));
            entries.set(i, previous);
        }
        return true;
    }

    public static int recentCount(int account) {
        return DownloadController.getInstance(account).recentDownloadingFiles.size();
    }

    public static void clearHistory(int account) {
        // The upstream clearRecentDownloadedFiles() only deletes history DB
        // entries. NEVER call deleteRecentFiles(), which also erases files.
        DownloadController.getInstance(account).clearRecentDownloadedFiles();
    }
}
