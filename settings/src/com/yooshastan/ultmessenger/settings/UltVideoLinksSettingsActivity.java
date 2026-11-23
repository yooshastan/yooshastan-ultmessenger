package com.yooshastan.ultmessenger.settings;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.yooshastan.ultmessenger.video.UltVideoAutoSave;

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
                        + "is sent as a SECOND Telegram message to that same chat. "
                        + "Others in the chat will see and receive it. OFF by default.", 14);
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
                            + "This does not post incoming links or protected media.")
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

        addHeading(content, context, "Download status");
        status = addText(content, context, "", 15);
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
        status.setText("Last attempt: " + UltVideoAutoSave.lastResult(currentAccount)
                + "\nCompleted/queued: " + UltVideoAutoSave.queuedCount(currentAccount)
                + "\nNative strict proxy: " + (strict ? "ON - downloader PAUSED" : "OFF"));
        limitation.setText("New outgoing text links only for chat auto-post; "
                + "other users' links are never auto-posted. Site extraction is experimental; "
                + "YouTube/Instagram may fail. Videos are also saved under Downloads/Ultgram. "
                + "No cookies or authentication bypass. An active Android VPN may have "
                + "different routing rules; MTProxy does not cover this extractor.");
    }

    @Override public void onResume() {
        super.onResume();
        refresh();
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
