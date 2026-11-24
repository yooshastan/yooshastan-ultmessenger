package com.yooshastan.ultmessenger.settings;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.yooshastan.ultmessenger.video.UltVideoAutoSave;
import com.yooshastan.ultmessenger.video.UltVideoSiteDownloader;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

/**
 * Direct entry from Ultgram Settings > Downloads and Storage.
 * Controls are visible without opening nested dialogs or horizontal toolbars.
 */
public final class UltVideoLinksSettingsActivity extends BaseFragment {
    private Button autoSaveButton;
    private Button postToChatButton;
    private Button unmeteredButton;
    private Button qualityButton;
    private Button sizeButton;
    private Button cancelButton;
    private final android.os.Handler statusHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable statusTick = new Runnable() {
        @Override public void run() {
            refresh();
            statusHandler.postDelayed(this, 1000);
        }
    };
    private TextView status;
    private TextView limitation;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Video links & auto-post");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addHeading(content, context, "Automatic video downloads");
        autoSaveButton = addButton(content, context, "", () -> {
            final boolean next = !UltVideoAutoSave.isEnabled(currentAccount);
            if (!next) {
                UltVideoAutoSave.setPostToChat(currentAccount, false);
                UltVideoAutoSave.setEnabled(currentAccount, false);
                refresh();
                return;
            }
            new UltSecureDialogBuilder(context)
                    .setTitle("Enable external video downloads?")
                    .setMessage("Ultgram will contact websites in newly posted chats. "
                            + "These connections are outside Telegram's MTProxy and "
                            + "may expose your IP address. Strict proxy mode blocks "
                            + "the external extractor. Only download media you may save.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Enable", (dialog, which) -> {
                        UltVideoAutoSave.setEnabled(currentAccount, true);
                        refresh();
                    })
                    .show();
        });

        addHeading(content, context, "Send downloaded videos into my chats");
        addText(content, context,
                "When YOU post a public video link, a successfully downloaded MP4 "
                        + "is sent as a SECOND Telegram message in an ordinary one-to-one "
                        + "chat or Saved Messages. Groups, channels and anonymous posts "
                        + "are allowed only when the sender identity is unchanged. "
                + "Forum topics and broadcast channels remain excluded. OFF by default.", 14);
        postToChatButton = addButton(content, context, "", () -> {
            if (UltVideoAutoSave.isPostToChat(currentAccount)) {
                UltVideoAutoSave.setPostToChat(currentAccount, false);
                refresh();
                return;
            }
            if (!UltVideoAutoSave.isEnabled(currentAccount)) {
                new UltSecureDialogBuilder(context)
                        .setTitle("Enable downloads first")
                        .setMessage("Enable automatic video downloads before enabling chat posting.")
                        .setPositiveButton("OK", null).show();
                return;
            }
            new UltSecureDialogBuilder(context)
                    .setTitle("Automatically send videos to chat?")
                    .setMessage("After YOU post a supported video URL, Ultgram will download "
                            + "the video then upload and SEND it to the SAME conversation "
                            + "as another message. Every participant may see it. "
                            + "Videos may consume mobile data and storage. "
                            + "Only completed MP4s are eligible. "
                            + "Ordinary groups are supported only when the video "
                            + "can be sent as the SAME identity as your link. "
                            + "Forum topics, broadcasts, paid/restricted groups are "
                            + "excluded for safety. This does not post incoming links "
                            + "or protected media.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Enable auto-post", (dialog, which) -> {
                        UltVideoAutoSave.setPostToChat(currentAccount, true);
                        refresh();
                    })
                    .show();
        });

        addHeading(content, context, "Network preference");
        unmeteredButton = addButton(content, context, "", () -> {
            UltVideoAutoSave.setWifiOnly(currentAccount,
                    !UltVideoAutoSave.isWifiOnly(currentAccount));
            refresh();
        });

        addHeading(content, context, "Video quality");
        qualityButton = addButton(content, context, "", () -> {
            final int[] choices = {0, 480, 720, 1080};
            new UltSecureDialogBuilder(context)
                    .setTitle("Maximum site video resolution")
                    .setItems(new String[]{"Automatic (original behavior)", "480p", "720p", "1080p"},
                            (dialog, which) -> {
                                UltVideoAutoSave.setMaxVideoHeight(currentAccount, choices[which]);
                                refresh();
                            })
                    .show();
        });
        addText(content, context, "Applies to new site downloads only.", 13);
        sizeButton = addButton(content, context, "", () -> {
            final int[] sizes = {100, 250, 500, 1024};
            new UltSecureDialogBuilder(context)
                    .setTitle("Site download workspace limit")
                    .setItems(new String[]{"100 MiB (default)", "250 MiB", "500 MiB", "1 GiB"},
                            (dialog, which) -> {
                                UltVideoAutoSave.setSiteLimitMiB(currentAccount, sizes[which]);
                                refresh();
                            }).show();
        });
        addText(content, context, "Limits total temporary files for new site downloads, including audio and merge files. Direct file downloads retain their 100 MiB limit.", 13);

        addHeading(content, context, "Download status");
        status = addText(content, context, "", 14);
        cancelButton = addButton(content, context, "Cancel active site download", () -> {
            UltVideoSiteDownloader.cancelAccount(currentAccount);
            refresh();
        });
        addButton(content, context, "Refresh status", this::refresh);

        addHeading(content, context, "Privacy and limits");
        limitation = addText(content, context, "", 14);
        fragmentView = scroll;
        refresh();
        return fragmentView;
    }

    private void refresh() {
        if (autoSaveButton == null) return;
        boolean enabled = UltVideoAutoSave.isEnabled(currentAccount);
        boolean post = UltVideoAutoSave.isPostToChat(currentAccount);
        boolean strict = UltVideoAutoSave.strictModeRequested();
        autoSaveButton.setText("Auto-save video links: " + (enabled ? "ON" : "OFF"));
        postToChatButton.setText("Automatically send video to chat: "
                + (post ? "ON" : "OFF"));
        postToChatButton.setEnabled(enabled || post);
        unmeteredButton.setText("Wi-Fi / unmetered only: "
                + (UltVideoAutoSave.isWifiOnly(currentAccount) ? "ON" : "OFF"));
        int height = UltVideoAutoSave.maxVideoHeight(currentAccount);
        qualityButton.setText("Video quality: " + (height == 0 ? "Automatic" : "Up to " + height + "p"));
        sizeButton.setText("Site size limit: " + UltVideoAutoSave.siteLimitMiB(currentAccount) + " MiB");
        cancelButton.setEnabled(UltVideoSiteDownloader.hasActive(currentAccount));
        final long attemptedAt = UltVideoAutoSave.lastAttemptMillis(currentAccount);
        final String attemptTime = attemptedAt > 0
                ? java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
                    .format(new java.util.Date(attemptedAt))
                : "Not yet recorded";
        status.setText("yt-dlp: " + UltVideoSiteDownloader.extractorVersion()
                + "\nLast inspected: " + UltVideoAutoSave.lastSourceCategory(currentAccount)
                + " (" + attemptTime + ")"
                + "\nResult: " + UltVideoAutoSave.lastResult(currentAccount)
                + "\nConfirmed site saves: "
                + UltVideoAutoSave.completedSiteSaves(currentAccount)
                + "\nHistorical queued/saved (unverified): "
                + UltVideoAutoSave.queuedCount(currentAccount)
                + "\nNative strict proxy: " + (strict ? "ON - downloader PAUSED" : "OFF"));
        limitation.setText("Automatic chat posting supports one-to-one chats, "
                + "Saved Messages, and regular groups with a verified matching "
                + "send-as identity (including anonymous group identity when it "
                + "has not changed). Forum topics and broadcast channels are paused. "
                + "New outgoing text links only; other members' links never auto-post. "
                + "Site extraction is experimental; "
                + "Site access and available formats vary. Videos are saved under Downloads/Ultgram. "
                + "No cookies or authentication bypass. An active Android VPN may have "
                + "different routing rules; MTProxy does not cover this extractor.");
    }

    @Override public void onResume() {
        super.onResume();
        statusHandler.removeCallbacks(statusTick);
        statusHandler.post(statusTick);
    }

    @Override public void onPause() {
        statusHandler.removeCallbacks(statusTick);
        super.onPause();
    }

    @Override public void onFragmentDestroy() {
        statusHandler.removeCallbacks(statusTick);
        super.onFragmentDestroy();
    }

    /**
     * Preserve the same FLAG_SECURE safeguards as Ultgram Settings dialogs.
     * UltSettingsActivity's similarly named builder is a PRIVATE nested class,
     * so this screen needs its own accessible builder implementation.
     */
    private static final class UltSecureDialogBuilder extends android.app.AlertDialog.Builder {
        UltSecureDialogBuilder(Context context) {
            super(context);
        }

        @Override
        public android.app.AlertDialog show() {
            final android.app.AlertDialog dialog = create();
            final boolean telegramPasscodeSecure =
                    org.telegram.messenger.SharedConfig.passcodeHash.length() > 0
                    && !org.telegram.messenger.SharedConfig.allowScreenCapture;
            if ((telegramPasscodeSecure || UltScreenPrivacy.isEnabled(getContext()))
                    && !org.telegram.messenger.SharedConfig.disableSecureFlags
                    && dialog.getWindow() != null) {
                dialog.getWindow().addFlags(
                        android.view.WindowManager.LayoutParams.FLAG_SECURE);
            }
            dialog.show();
            return dialog;
        }
    }

    private static void addHeading(LinearLayout root, Context context, String label) {
        TextView view = addText(root, context, label, 18);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private static TextView addText(LinearLayout root, Context context, String body, int sp) {
        TextView view = new TextView(context);
        view.setText(body);
        view.setTextSize(sp);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        view.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        root.addView(view);
        return view;
    }

    private static Button addButton(LinearLayout root, Context context,
                                    String title, Runnable action) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(title);
        button.setOnClickListener(v -> action.run());
        root.addView(button);
        return button;
    }
}
