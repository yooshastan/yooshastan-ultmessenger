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
import com.yooshastan.ultmessenger.video.UltExtractorMaintenance;

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
    private Button cancelButton, modeButton, metadataButton, retryButton, audioFormatButton;
    private LinearLayout[] panels;
    private Button[] tabs;
    private ScrollView settingsScroll;
    private int selectedPanel;
    private final android.os.Handler statusHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable statusTick = new Runnable() {
        @Override public void run() {
            refresh();
            statusHandler.postDelayed(this, 1000);
        }
    };
    private TextView status;
    private TextView limitation;
    private TextView maintenanceStatus;
    private Button channelButton, autoCheckButton, updateNetworkButton, checkButton, updateButton, rollbackButton, bundledButton, verifyButton;
    private Context maintenanceContext;

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
        settingsScroll = scroll;
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout nav = new LinearLayout(context);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(nav);
        String[] names = {"Manager", "Preferences", "Automation", "Extractor", "History"};
        LinearLayout secondNav = new LinearLayout(context);
        secondNav.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(secondNav);
        tabs = new Button[names.length];
        panels = new LinearLayout[names.length];
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            tabs[i] = addButton(i < 3 ? nav : secondNav, context, names[i], () -> showPanel(index));
            tabs[i].setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            panels[i] = new LinearLayout(context);
            panels[i].setOrientation(LinearLayout.VERTICAL);
            content.addView(panels[i]);
        }
        LinearLayout manager = panels[0], downloads = panels[1], automation = panels[2], extractor = panels[3], history = panels[4];

        addHeading(automation, context, "Automatic media downloads");
        autoSaveButton = addButton(automation, context, "", () -> {
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

        addHeading(automation, context, "Send downloaded videos into my chats");
        addText(automation, context,
                "When YOU post a public video link, a successfully downloaded MP4 "
                        + "is sent as a SECOND Telegram message in an ordinary one-to-one "
                        + "chat or Saved Messages. Groups, channels and anonymous posts "
                        + "are allowed only when the sender identity is unchanged. "
                + "Forum topics and broadcast channels remain excluded. OFF by default.", 14);
        postToChatButton = addButton(automation, context, "", () -> {
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

        addHeading(downloads, context, "Network preference");
        unmeteredButton = addButton(downloads, context, "", () -> {
            UltVideoAutoSave.setWifiOnly(currentAccount,
                    !UltVideoAutoSave.isWifiOnly(currentAccount));
            refresh();
        });

        addHeading(downloads, context, "Media format");
        modeButton = addButton(downloads, context, "", () -> new UltSecureDialogBuilder(context)
                .setTitle("Download format for supported sites")
                .setItems(new String[]{"Video (default)", "Audio only (default audio format, local save)"}, (dialog, which) -> {
                    UltVideoAutoSave.setAudioOnly(currentAccount, which == 1);
                    refresh();
                }).show());
        addText(downloads, context, "Audio mode saves locally only and ignores direct video links. Changing mode turns automatic chat posting off.", 13);
        metadataButton = addButton(downloads, context, "", () -> {
            if (UltVideoAutoSave.metadataProbeEnabled(currentAccount)) {
                UltVideoAutoSave.setMetadataProbeEnabled(currentAccount, false); refresh(); return;
            }
            new UltSecureDialogBuilder(context).setTitle("Inspect ordinary webpages?")
                    .setMessage("When enabled, Ultgram may contact other HTTPS websites to look for video metadata. These requests are outside MTProxy. OFF is recommended for privacy.")
                    .setNegativeButton("Cancel", null).setPositiveButton("Enable", (dialog, which) -> {
                        UltVideoAutoSave.setMetadataProbeEnabled(currentAccount, true); refresh();
                    }).show();
        });
        audioFormatButton = addButton(downloads, context, "", () -> new UltSecureDialogBuilder(context)
                .setTitle("Default audio format")
                .setItems(new String[]{"M4A", "MP3", "Opus"}, (dialog, which) -> {
                    UltVideoAutoSave.setDefaultAudioFormat(currentAccount, new String[]{"m4a","mp3","opus"}[which]); refresh();
                }).show());
        addHeading(downloads, context, "Video quality");
        qualityButton = addButton(downloads, context, "", () -> {
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
        addText(downloads, context, "Applies to new site downloads only.", 13);
        sizeButton = addButton(downloads, context, "", () -> {
            final int[] sizes = {100, 250, 500, 1024};
            new UltSecureDialogBuilder(context)
                    .setTitle("Site download workspace limit")
                    .setItems(new String[]{"100 MiB (default)", "250 MiB", "500 MiB", "1 GiB"},
                            (dialog, which) -> {
                                UltVideoAutoSave.setSiteLimitMiB(currentAccount, sizes[which]);
                                refresh();
                            }).show();
        });
        addText(downloads, context, "Limits total temporary files for new site downloads, including audio and merge files. Direct file downloads retain their 100 MiB limit.", 13);

        addHeading(manager, context, "Download manager");
        addText(manager, context, "Long-press a supported media link to add video or audio to its chat. Review before sending, or select Save to device. Automatic downloads are optional.", 14);
        status = addText(manager, context, "", 14);
        cancelButton = addButton(manager, context, "Cancel active and queued site downloads", () -> {
            UltVideoAutoSave.cancelSiteJobs(currentAccount);
            refresh();
        });
        retryButton = addButton(manager, context, "Retry last failed site download", () -> {
            UltVideoAutoSave.retryLastSite(currentAccount); refresh();
        });
        addText(manager, context, "Retry uses the last failed supported site link in this session (up to 15 minutes), saves locally, and never sends a chat message.", 13);
        addButton(manager, context, "Refresh status", this::refresh);

        addHeading(extractor, context, "Extractor maintenance");
        maintenanceContext = context.getApplicationContext();
        maintenanceStatus = addText(extractor, context, "", 14);
        addText(extractor, context, "Shared across accounts. Official yt-dlp releases only; checksum and local Python version test before atomic replacement. Update traffic is outside MTProxy. Checks never install automatically.", 13);
        channelButton = addButton(extractor, context, "", () -> new UltSecureDialogBuilder(context)
                .setTitle("Extractor release channel")
                .setItems(new String[]{"Stable (recommended)", "Nightly"}, (dialog, which) -> {
                    UltExtractorMaintenance.setNightly(maintenanceContext, which == 1); refresh();
                }).show());
        autoCheckButton = addButton(extractor, context, "", () -> {
            UltExtractorMaintenance.setAutomatic(maintenanceContext, !UltExtractorMaintenance.automatic(maintenanceContext));
            UltExtractorMaintenance.maybeCheck(maintenanceContext); refresh();
        });
        updateNetworkButton = addButton(extractor, context, "", () -> {
            UltExtractorMaintenance.setUnmetered(maintenanceContext, !UltExtractorMaintenance.unmetered(maintenanceContext)); refresh();
        });
        addButton(extractor, context, "Read installed version (offline)", () -> {
            UltExtractorMaintenance.start(maintenanceContext, "inspect"); refresh();
        });
        checkButton = addButton(extractor, context, "Check for extractor updates", () -> {
            UltExtractorMaintenance.start(maintenanceContext, "check"); refresh();
        });
        updateButton = addButton(extractor, context, "Install latest verified extractor", () -> new UltSecureDialogBuilder(context)
                .setTitle("Install selected channel release?")
                .setMessage("Contacts official GitHub release servers outside Telegram MTProxy. Switching channels may install an older version. No update runs with native strict proxy enabled. The current extractor will be backed up.")
                .setNegativeButton("Cancel", null).setPositiveButton("Install", (dialog, which) -> {
                    UltExtractorMaintenance.start(maintenanceContext, "update"); refresh();
                }).show());
        rollbackButton = addButton(extractor, context, "Restore previous extractor", () -> new UltSecureDialogBuilder(context)
                .setTitle("Restore previous extractor?")
                .setMessage("Offline. The current extractor becomes the next rollback backup.")
                .setNegativeButton("Cancel", null).setPositiveButton("Restore", (dialog, which) -> {
                    UltExtractorMaintenance.start(maintenanceContext, "rollback"); refresh();
                }).show());
        bundledButton = addButton(extractor, context, "Restore bundled extractor", () -> new UltSecureDialogBuilder(context)
                .setTitle("Restore APK-bundled extractor?")
                .setMessage("Offline. This may install an older extractor; your current one is backed up.")
                .setNegativeButton("Cancel", null).setPositiveButton("Restore", (dialog, which) -> {
                    UltExtractorMaintenance.start(maintenanceContext, "bundled"); refresh();
                }).show());
        verifyButton = addButton(extractor, context, "Verify local extractor SHA-256", () -> {
            UltExtractorMaintenance.start(maintenanceContext, "verify"); refresh();
        });

        addHeading(history, context, "Saved site media");
        addButton(history, context, "Download history", () -> showHistory(context));
        addButton(history, context, "Clear history (keep files)", () ->
                new UltSecureDialogBuilder(context)
                        .setTitle("Clear site download history?")
                        .setMessage("This removes local history entries only. Downloaded files stay on your device.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Clear", (dialog, which) -> {
                            UltVideoAutoSave.clearSiteHistory(currentAccount);
                            refresh();
                        }).show());

        addHeading(history, context, "Privacy and limits");
        limitation = addText(history, context, "", 14);
        fragmentView = scroll;
        showPanel(selectedPanel);
        refresh();
        return fragmentView;
    }

    private void showPanel(int index) {
        selectedPanel = index;
        for (int i = 0; i < panels.length; i++) {
            panels[i].setVisibility(i == index ? View.VISIBLE : View.GONE);
            tabs[i].setEnabled(i != index);
        }
        if (settingsScroll != null) settingsScroll.scrollTo(0, 0);
    }

    private void showHistory(Context context) {
        org.json.JSONArray history = UltVideoAutoSave.siteHistory(currentAccount);
        if (history.length() == 0) {
            new UltSecureDialogBuilder(context).setTitle("Download history")
                    .setMessage("No site saves recorded since this feature was added. Older files remain in Downloads/Ultgram.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String[] rows = new String[history.length()];
        for (int i = 0; i < rows.length; i++) {
            org.json.JSONObject item = history.optJSONObject(i);
            if (item == null) { rows[i] = "Unavailable entry"; continue; }
            rows[i] = item.optString("source", "Site") + " · "
                    + android.text.format.Formatter.formatFileSize(context, item.optLong("bytes"))
                    + "\n" + item.optString("name", "Video");
        }
        new UltSecureDialogBuilder(context).setTitle("Saved site videos")
                .setItems(rows, (dialog, which) -> {
                    org.json.JSONObject item = history.optJSONObject(which);
                    if (item == null) return;
                    android.net.Uri uri = android.net.Uri.parse(item.optString("uri"));
                    if (!"content".equals(uri.getScheme()) || !"media".equals(uri.getAuthority())) return;
                    try {
                        android.content.Intent view = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                        view.setDataAndType(uri, item.optString("name").startsWith("ult_audio_") ? "audio/*" : "video/*");
                        view.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(view);
                    } catch (Exception ignored) {
                        android.widget.Toast.makeText(context, "File unavailable or no video player installed", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Close", null).show();
    }

    private void refresh() {
        if (autoSaveButton == null) return;
        boolean enabled = UltVideoAutoSave.isEnabled(currentAccount);
        boolean post = UltVideoAutoSave.isPostToChat(currentAccount);
        boolean strict = UltVideoAutoSave.strictModeRequested();
        autoSaveButton.setText("Auto-save media links: " + (enabled ? "ON" : "OFF"));
        postToChatButton.setText("Automatically send video to chat: "
                + (post ? "ON" : "OFF"));
        boolean audio = UltVideoAutoSave.audioOnly(currentAccount);
        postToChatButton.setEnabled(!audio && (enabled || post));
        audioFormatButton.setText("Default audio: " + UltVideoAutoSave.defaultAudioFormat(currentAccount).toUpperCase(java.util.Locale.US));
        modeButton.setText("Download format: " + (audio ? "Audio only" : "Video"));
        metadataButton.setText("Inspect ordinary webpages: " + (UltVideoAutoSave.metadataProbeEnabled(currentAccount) ? "ON" : "OFF"));
        qualityButton.setEnabled(true);
        retryButton.setEnabled(UltVideoAutoSave.canRetry(currentAccount));
        unmeteredButton.setText("Wi-Fi / unmetered only: "
                + (UltVideoAutoSave.isWifiOnly(currentAccount) ? "ON" : "OFF"));
        int height = UltVideoAutoSave.maxVideoHeight(currentAccount);
        qualityButton.setText("Video quality: " + (height == 0 ? "Automatic" : "Up to " + height + "p"));
        sizeButton.setText("Site size limit: " + UltVideoAutoSave.siteLimitMiB(currentAccount) + " MiB");
        cancelButton.setEnabled(UltVideoAutoSave.hasSiteJobs(currentAccount));
        final long attemptedAt = UltVideoAutoSave.lastAttemptMillis(currentAccount);
        final String attemptTime = attemptedAt > 0
                ? java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
                    .format(new java.util.Date(attemptedAt))
                : "Not yet recorded";
        status.setText(UltVideoAutoSave.siteQueueSummary(currentAccount) + "\nReady chat attachments: " + com.yooshastan.ultmessenger.video.UltChatMediaDraft.readyCount(currentAccount)
                + "\nReview ready attachments by long-pressing the original link.\n\nyt-dlp: " + UltVideoSiteDownloader.extractorVersion()
                + "\nLast inspected: " + UltVideoAutoSave.lastSourceCategory(currentAccount)
                + " (" + attemptTime + ")"
                + "\nResult: " + UltVideoAutoSave.lastResult(currentAccount)
                + "\nConfirmed site saves: "
                + UltVideoAutoSave.completedSiteSaves(currentAccount)
                + "\nHistorical queued/saved (unverified): "
                + UltVideoAutoSave.queuedCount(currentAccount)
                + "\nNative strict proxy: " + (strict ? "ON - downloader PAUSED" : "OFF"));
        boolean busy = UltExtractorMaintenance.busy();
        maintenanceStatus.setText("Installed: " + UltVideoSiteDownloader.extractorVersion()
                + "\nRuntime: " + (UltExtractorMaintenance.runtimeInitialized() ? "Initialized locally" : "Not initialized")
                + "\nSelected release: " + (UltExtractorMaintenance.available().isEmpty() ? "Not checked" : UltExtractorMaintenance.available())
                + "\n" + UltExtractorMaintenance.status());
        channelButton.setText("Channel: " + (UltExtractorMaintenance.nightly(maintenanceContext) ? "Nightly" : "Stable"));
        autoCheckButton.setText("Daily check when opening this screen: " + (UltExtractorMaintenance.automatic(maintenanceContext) ? "ON" : "OFF"));
        updateNetworkButton.setText("Unmetered updates only: " + (UltExtractorMaintenance.unmetered(maintenanceContext) ? "ON" : "OFF"));
        channelButton.setEnabled(!busy);
        checkButton.setEnabled(!busy && !strict);
        updateButton.setEnabled(!busy && !strict);
        rollbackButton.setEnabled(!busy && UltExtractorMaintenance.hasBackup(maintenanceContext));
        bundledButton.setEnabled(!busy);
        verifyButton.setEnabled(!busy);
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
        if (maintenanceContext != null) UltExtractorMaintenance.maybeCheck(maintenanceContext);
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
