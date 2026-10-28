package com.yooshastan.ultmessenger.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
    private SearchDownloadsContainer downloads;
    private boolean selectingSaveAs;
    private File pendingSaveFile;
    private long pendingSaveLength;

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
        fragmentView = downloads;
        return fragmentView;
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
        showSaveAs(item);
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
