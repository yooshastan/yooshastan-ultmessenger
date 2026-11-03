package com.yooshastan.ultmessenger.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.telegram.messenger.AndroidUtilities;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLRPC;
import com.yooshastan.ultmessenger.chat.UltChatTools;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.SearchDownloadsContainer;
import org.telegram.ui.FilteredSearchView;

/**
 * Alpha 0.13b: reuse Telegram's exact active/recent file download view.
 * All transfers/opens/cancels stay in Telegram FileLoader and existing
 * protected-media behavior; no new storage, request or alternate downloader.
 */
public final class UltDownloadsActivity extends BaseFragment
        implements FilteredSearchView.UiCallback {

    private static final int CREATE_DOCUMENT_REQUEST = 0x13CD;
    private static final int VERIFY_DOCUMENT_REQUEST = 0x14DE;
    private SearchDownloadsContainer downloads;
    private boolean selectingSaveAs;
    private File pendingSaveFile;
    private long pendingSaveLength;
    private MessageObject pendingVerifyMessage;
    private File pendingVerifySource;
    private long pendingVerifyLength;
    private int pendingVerifyAccount = -1;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("ULT Downloads");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        downloads = new SearchDownloadsContainer(this, currentAccount);
        downloads.setUiCallback(this);
        downloads.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        // Native SearchDownloadsContainer continues to own progress, history,
        // drag-to-reorder and message taps. These visible shortcuts call only
        // Telegram's own FileLoader/DownloadController for this account.
        final LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        final HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        final LinearLayout toolbar = new LinearLayout(context);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        addToolbarButton(context, toolbar, "Pause all", () -> {
            final int changed = UltDownloadActions.pauseAll(currentAccount);
            downloads.update(true);
            showDownloadToast(changed == 0 ? "No active downloads to pause"
                    : "Paused " + changed + " downloads");
        });
        addToolbarButton(context, toolbar, "Resume all", () -> {
            final int changed = UltDownloadActions.resumeAll(currentAccount);
            downloads.update(true);
            showDownloadToast(changed == 0 ? "No queued downloads to resume"
                    : "Resumed " + changed + " downloads");
        });
        addToolbarButton(context, toolbar, "Resume next", () -> {
            final boolean started = UltDownloadActions.resumeNext(currentAccount);
            downloads.update(true);
            showDownloadToast(started ? "Resumed the next queued download"
                    : "No paused document downloads in queue");
        });
        addToolbarButton(context, toolbar, "Queue status", () -> {
            final int[] counts = UltDownloadActions.queueCounts(currentAccount);
            final Activity activity = getParentActivity();
            if (activity == null) return;
            showSecureDialog(activity, new android.app.AlertDialog.Builder(activity)
                    .setTitle("Download queue status")
                    .setMessage("Active: " + counts[0] + "\\nPaused: " + counts[1]
                            + "\\nRecent history: " + counts[2]
                            + "\\n\\nCounts are for this Telegram account. "
                            + "The native download queue manages transfer scheduling.")
                    .setPositiveButton("OK", null));
        });
        addToolbarButton(context, toolbar, "Network rules", this::showAutoDownloadRules);
        addToolbarButton(context, toolbar, "Clear history", this::confirmClearHistory);
        scroll.addView(toolbar);
        column.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        column.addView(downloads, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        fragmentView = column;
        return fragmentView;
    }

    private void addToolbarButton(Context context, LinearLayout bar,
                                  String text, Runnable action) {
        final Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(12f);
        button.setMinHeight(AndroidUtilities.dp(44));
        button.setOnClickListener(view -> action.run());
        bar.addView(button, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, AndroidUtilities.dp(48)));
    }

    private void showDownloadToast(String message) {
        final Activity activity = getParentActivity();
        if (activity != null) Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    private void showAutoDownloadRules() {
        final Activity activity = getParentActivity();
        if (activity == null) return;
        showSecureDialog(activity, new android.app.AlertDialog.Builder(activity)
                .setTitle("Automatic media downloads")
                .setMessage("Control photos, videos, stories and documents separately "
                        + "on mobile data, Wi-Fi or roaming. Telegram's existing "
                        + "per-network rules determine automatic transfers.")
                .setItems(new String[]{"Mobile data", "Wi-Fi", "Roaming"},
                        (dialog, choice) -> presentFragment(
                                new org.telegram.ui.DataAutoDownloadActivity(choice)))
                .setNegativeButton("Cancel", null));
    }

    private void confirmClearHistory() {
        final Activity activity = getParentActivity();
        if (activity == null) return;
        final int count = UltDownloadActions.recentCount(currentAccount);
        if (count == 0) {
            showDownloadToast("Recent download history is already empty");
            return;
        }
        showSecureDialog(activity, new android.app.AlertDialog.Builder(activity)
                .setTitle("Clear recent download history?")
                .setMessage("Remove " + count + " entries from Recent Downloads? "
                        + "This only removes history entries. It does NOT delete "
                        + "downloaded files or cancel active transfers.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear history", (dialog, ignored) -> {
                    UltDownloadActions.clearHistory(currentAccount);
                    if (downloads != null) downloads.update(true);
                    showDownloadToast("Recent download history cleared");
                }));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (downloads != null) downloads.update(false);
    }

    // The native download UI still owns all transfers. Long-press is a
    // user-initiated single-file export only, not unsupported bulk selection.
    @Override
    public void goToMessage(MessageObject message) {
        if (getParentActivity() != null) {
            Toast.makeText(getParentActivity(),
                    "Open the chat from Telegram Search to view the source message",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean actionModeShowing() {
        return selectingSaveAs;
    }

    @Override
    public void toggleItemSelection(MessageObject item, View view, int a) {
        selectingSaveAs = false;
        showDocumentActions(item);
    }

    @Override
    public boolean isSelected(FilteredSearchView.MessageHashId id) {
        return false;
    }

    @Override
    public void showActionMode() {
        selectingSaveAs = true;
    }

    private static boolean isExportAllowed(MessageObject message, int account) {
        if (message == null || message.messageOwner == null
                || message.getDocument() == null || message.isSponsored()
                || message.messageOwner.noforwards
                || DialogObject.isEncryptedDialog(message.getDialogId())) {
            return false;
        }
        final TLRPC.Peer peer = message.messageOwner.peer_id;
        if (peer != null) {
            final long chatId = peer.channel_id != 0 ? peer.channel_id : peer.chat_id;
            if (chatId != 0) {
                final TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatId);
                // Fail closed if channel/group restrictions cannot be resolved.
                if (chat == null || chat.noforwards) return false;
            }
        }
        return true;
    }

    /**
     * Local secure-dialog helper. UltSettingsActivity.UltSecureDialogBuilder is
     * private to the settings screen, so Downloads must protect its own windows.
     * Preserve the same Telegram passcode and opt-in FLAG_SECURE policy used
     * for the existing Save As prompt.
     */
    private static void showSecureDialog(Activity activity,
                                         android.app.AlertDialog.Builder builder) {
        final android.app.AlertDialog dialog = builder.create();
        final boolean telegramSecure = SharedConfig.passcodeHash.length() > 0
                && !SharedConfig.allowScreenCapture;
        if ((telegramSecure || UltScreenPrivacy.isEnabled(activity))
                && !SharedConfig.disableSecureFlags && dialog.getWindow() != null) {
            dialog.getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }
        dialog.show();
    }

    /** One file at a time. Pause/priority controls do not export its content. */
    private void showDocumentActions(MessageObject message) {
        final Activity activity = getParentActivity();
        if (activity == null || message == null || message.getDocument() == null) return;
        final boolean queued = UltDownloadActions.isQueued(currentAccount, message);
        final boolean export = isExportAllowed(message, currentAccount);
        if (!queued && !export) {
            showDownloadToast("This document is protected or unavailable");
            return;
        }
        final ArrayList<String> labels = new ArrayList<>();
        final ArrayList<Runnable> actions = new ArrayList<>();
        if (queued) {
            final boolean running = UltDownloadActions.isRunning(currentAccount, message);
            labels.add(running ? "Pause this download" : "Resume this download");
            actions.add(() -> {
                final boolean succeeded = running
                        ? UltDownloadActions.pauseOne(currentAccount, message)
                        : UltDownloadActions.resumeOne(currentAccount, message);
                if (downloads != null) downloads.update(true);
                showDownloadToast(succeeded
                        ? (running ? "Download paused" : "Download resumed")
                        : "This download is no longer queued");
            });
            labels.add("Move to top of queue");
            actions.add(() -> {
                final boolean succeeded = UltDownloadActions.moveToTop(currentAccount, message);
                if (downloads != null) downloads.update(true);
                showDownloadToast(succeeded
                        ? "Moved to top of download queue" : "Already first or no longer queued");
            });
        }
        if (export) {
            labels.add("Inspect file and SHA-256");
            actions.add(() -> inspectDocument(message));
            labels.add("Save As…");
            actions.add(() -> showSaveAs(message));
            labels.add("Verify saved copy…");
            actions.add(() -> chooseFileToVerify(message));
        }
        showSecureDialog(activity, new android.app.AlertDialog.Builder(activity)
                .setTitle("ULT download controls")
                .setItems(labels.toArray(new String[0]), (dialog, option) -> {
                    if (option >= 0 && option < actions.size()) actions.get(option).run();
                })
                .setNegativeButton("Cancel", null));
    }

    /** Hashes only an already-downloaded local file on a worker thread.
     * Never reads the source chat or sends anything to the network. */
    private void inspectDocument(MessageObject message) {
        final Activity activity = getParentActivity();
        if (activity == null || !isExportAllowed(message, currentAccount)) return;
        final File source = FileLoader.getInstance(currentAccount)
                .getPathToMessage(message.messageOwner);
        if (source == null || !source.isFile() || !source.canRead()) {
            Toast.makeText(activity, "Download the document before inspecting",
                    Toast.LENGTH_LONG).show();
            return;
        }
        final long expectedSize = source.length();
        if (expectedSize > UltFileInspection.MAX_INSPECT_BYTES) {
            Toast.makeText(activity, "File is too large for in-app checksum inspection",
                    Toast.LENGTH_LONG).show();
            return;
        }
        final String fileName = UltChatTools.safeFileName(
                FileLoader.getDocumentFileName(message.getDocument()));
        final String mime = message.getDocument().mime_type;
        Toast.makeText(activity, "Calculating local SHA-256…",
                Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                final UltFileInspection.Result result;
                try (InputStream input = new FileInputStream(source)) {
                    result = UltFileInspection.digest(input);
                }
                if (result.bytesRead != expectedSize || source.length() != expectedSize) {
                    throw new java.io.IOException("Document changed while inspecting; retry");
                }
                activity.runOnUiThread(() -> {
                    if (getParentActivity() != activity) return;
                    showSecureDialog(activity, new android.app.AlertDialog.Builder(activity)
                            .setTitle("Downloaded document inspector")
                            .setMessage(UltFileInspection.describe(fileName, mime, result))
                            .setNegativeButton("Close", null)
                            .setPositiveButton("Copy SHA-256", (dialog, ignored) -> {
                                android.content.ClipboardManager clipboard =
                                        (android.content.ClipboardManager) activity.getSystemService(
                                                Context.CLIPBOARD_SERVICE);
                                if (clipboard != null) {
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                                            "ULT SHA-256", result.sha256));
                                    Toast.makeText(activity,
                                            "Checksum copied; clipboard is visible to Android",
                                            Toast.LENGTH_LONG).show();
                                }
                            }));
                });
            } catch (java.io.IOException error) {
                activity.runOnUiThread(() -> {
                    if (getParentActivity() == activity) {
                        Toast.makeText(activity, "Inspection failed: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }, "ULT-Media-Inspect").start();
    }

    /** Compare a user-selected SAF copy to the permitted local source.
     * Never writes to, uploads or automatically selects a cloud destination. */
    private void chooseFileToVerify(MessageObject message) {
        final Activity activity = getParentActivity();
        if (activity == null || !isExportAllowed(message, currentAccount)) return;
        final File source = FileLoader.getInstance(currentAccount)
                .getPathToMessage(message.messageOwner);
        if (source == null || !source.isFile() || !source.canRead()
                || source.length() > UltFileInspection.MAX_INSPECT_BYTES) {
            Toast.makeText(activity, "Download a file under 512 MiB before verifying",
                    Toast.LENGTH_LONG).show();
            return;
        }
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pendingVerifyMessage = message;
        pendingVerifySource = source;
        pendingVerifyLength = source.length();
        pendingVerifyAccount = currentAccount;
        try {
            startActivityForResult(intent, VERIFY_DOCUMENT_REQUEST);
        } catch (android.content.ActivityNotFoundException error) {
            clearPendingVerify();
            Toast.makeText(activity, "No Android file picker available", Toast.LENGTH_LONG).show();
        }
    }

    private void clearPendingVerify() {
        pendingVerifyMessage = null;
        pendingVerifySource = null;
        pendingVerifyLength = 0;
        pendingVerifyAccount = -1;
    }

    private void verifySelectedCopy(int resultCode, Intent data) {
        final MessageObject message = pendingVerifyMessage;
        final File source = pendingVerifySource;
        final long expected = pendingVerifyLength;
        final int account = pendingVerifyAccount;
        clearPendingVerify();
        final Activity activity = getParentActivity();
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null
                || activity == null || source == null || account != currentAccount
                || !isExportAllowed(message, account)) return;
        final Uri selectedUri = data.getData();
        Toast.makeText(activity, "Comparing local download with selected file…",
                Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                if (!source.isFile() || !source.canRead() || source.length() != expected
                        || expected > UltFileInspection.MAX_INSPECT_BYTES) {
                    throw new java.io.IOException("Original file changed; retry comparison");
                }
                final UltFileInspection.Result original;
                try (InputStream sourceStream = new FileInputStream(source)) {
                    original = UltFileInspection.digest(sourceStream);
                }
                final UltFileInspection.Result copy;
                try (InputStream copyStream = activity.getContentResolver()
                        .openInputStream(selectedUri)) {
                    if (copyStream == null) {
                        throw new java.io.IOException("Selected file could not be opened");
                    }
                    copy = UltFileInspection.digest(copyStream);
                }
                if (source.length() != expected || original.bytesRead != expected) {
                    throw new java.io.IOException("Original file changed during verification");
                }
                final boolean identical = UltFileInspection.sameContent(original, copy);
                activity.runOnUiThread(() -> {
                    // A user might switch accounts while the file is being hashed.
                    // Never show a private file-comparison result in another account.
                    if (getParentActivity() != activity || currentAccount != account
                            || !isExportAllowed(message, account)) return;
                    showSecureDialog(activity, new android.app.AlertDialog.Builder(activity)
                            .setTitle(identical ? "Verified: identical" : "Verification failed: files differ")
                            .setMessage(identical
                                    ? "The selected file has the same size and SHA-256 as the "
                                    + "downloaded source. This checks bytes, not the file's origin."
                                    : "The selected copy has different bytes or size. "
                                    + "Check the destination and retry; no files were modified.")
                            .setPositiveButton("Done", null));
                });
            } catch (Exception error) {
                final String details = error.getMessage() != null
                        ? error.getMessage() : "Provider unavailable";
                activity.runOnUiThread(() -> {
                    if (getParentActivity() == activity && currentAccount == account
                            && isExportAllowed(message, account)) {
                        Toast.makeText(activity, "Verify failed: " + details,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }, "ULT-SAF-Verify").start();
    }

    private void showSaveAs(MessageObject message) {
        final Activity activity = getParentActivity();
        if (activity == null) return;
        if (!isExportAllowed(message, currentAccount)) {
            Toast.makeText(activity, "This media cannot be exported", Toast.LENGTH_LONG).show();
            return;
        }
        final File source = FileLoader.getInstance(currentAccount).getPathToMessage(message.messageOwner);
        if (source == null || !source.isFile() || !source.canRead()) {
            Toast.makeText(activity, "Download the document completely before Save As",
                    Toast.LENGTH_LONG).show();
            return;
        }
        final String name = UltChatTools.safeFileName(
                FileLoader.getDocumentFileName(message.getDocument()));
        final String mime = message.getDocument().mime_type;
        final String type = mime != null && mime.matches(
                "[A-Za-z0-9][A-Za-z0-9.+_-]*/[A-Za-z0-9][A-Za-z0-9.+_-]*")
                ? mime : "application/octet-stream";
        final android.app.AlertDialog prompt = new android.app.AlertDialog.Builder(activity)
                .setTitle("Save document as…")
                .setMessage("Export a copy of this already-downloaded file to a location "
                        + "you choose. Do not use for sensitive files on shared storage.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Choose location", (dialog, which) -> {
                    final Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(type);
                    intent.putExtra(Intent.EXTRA_TITLE, name);
                    pendingSaveFile = source;
                    pendingSaveLength = source.length();
                    try {
                        startActivityForResult(intent, CREATE_DOCUMENT_REQUEST);
                    } catch (android.content.ActivityNotFoundException e) {
                        pendingSaveFile = null;
                        Toast.makeText(activity, "No system document picker available",
                                Toast.LENGTH_LONG).show();
                    }
                }).create();
        final boolean telegramSecure = SharedConfig.passcodeHash.length() > 0
                && !SharedConfig.allowScreenCapture;
        if ((telegramSecure || UltScreenPrivacy.isEnabled(activity))
                && !SharedConfig.disableSecureFlags && prompt.getWindow() != null) {
            prompt.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }
        prompt.show();
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (requestCode == VERIFY_DOCUMENT_REQUEST) {
            verifySelectedCopy(resultCode, data);
            return;
        }
        if (requestCode != CREATE_DOCUMENT_REQUEST) return;
        final File source = pendingSaveFile;
        final long expectedLength = pendingSaveLength;
        pendingSaveFile = null;
        pendingSaveLength = 0L;
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null
                || source == null) return;
        final Activity activity = getParentActivity();
        if (activity == null) return;
        final Uri destination = data.getData();
        new Thread(() -> {
            boolean success = false;
            String error = "Unable to save file";
            try {
                if (!source.isFile() || source.length() != expectedLength) {
                    throw new java.io.IOException("Downloaded file changed; retry export");
                }
                try (InputStream input = new FileInputStream(source);
                     OutputStream output = activity.getContentResolver()
                             .openOutputStream(destination, "w")) {
                    if (output == null) {
                        throw new java.io.IOException("Destination refused write access");
                    }
                    byte[] buffer = new byte[32768];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                }
                success = true;
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : error;
            }
            final boolean finished = success;
            final String result = error;
            activity.runOnUiThread(() -> Toast.makeText(activity,
                    finished ? "Document saved" : "Save As failed: " + result,
                    Toast.LENGTH_LONG).show());
        }, "ULT-SAF-SaveAs").start();
    }

    @Override
    public int getFolderId() {
        return 0;
    }
}
