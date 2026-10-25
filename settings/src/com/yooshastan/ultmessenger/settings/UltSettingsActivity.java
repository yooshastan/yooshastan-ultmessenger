package com.yooshastan.ultmessenger.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.yooshastan.ultmessenger.conversation.UltChatView;
import com.yooshastan.ultmessenger.proxy.UltExternalLinkPolicy;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ProxyListActivity;
import org.telegram.ui.ThemeActivity;

/** 
 * Single entry point for Ultgram-owned features. Existing Telegram and
 * Mercurygram settings remain available; preferences are NOT duplicated.
 */
public final class UltSettingsActivity extends BaseFragment {

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Ultgram Settings");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addSection(context, content, "APPEARANCE");
        addLink(context, content, "ULT theme presets", "Six presets, fixed colors and light/dark pairing",
                () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC)));

        addSection(context, content, "CHATS AND TOOLS");
        addLink(context, content, "Chat views and sorting",
                "All, Unread, Groups, Channels, Bots and Unread First",
                () -> showChatViewPicker(context));
        addNote(context, content, "Chat translation",
                "Use a chat's ⋮ menu → ULT Chat Translation. Language and consent are per chat.");
        addNote(context, content, "Message and chat tools",
                "Copy chat IDs from the chat menu or message IDs from the long-press menu.");

        addSection(context, content, "PRIVACY AND NETWORK");
        addLink(context, content, "ULT Proxy Manager",
                "Named proxies, connection diagnostics and external link confirmations",
                () -> presentFragment(new ProxyListActivity()));
        addLink(context, content, "Confirm external links",
                "Ask before opening HTTP(S) links outside the Telegram proxy",
                () -> showExternalLinkGuard(context));
        addNote(context, content, "Strict proxy, encrypted DNS and tracker protection",
                "Under development in Alpha 0.10f. No strict, DNS or tracker protection is active yet.");

        addSection(context, content, "ABOUT");
        addNote(context, content, "Ultgram by Yooshastan Studios",
                "Independent Telegram-compatible client. Existing Mercurygram settings remain separate.");
        fragmentView = scroll;
        return fragmentView;
    }

    private void showChatViewPicker(Context context) {
        final SharedPreferences preferences = context.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE);
        final int account = UserConfig.selectedAccount;
        final String key = UltChatView.accountKey(account);
        final int selected = UltChatView.modeIndex(
                preferences.getString(key, UltChatView.ALL));
        new android.app.AlertDialog.Builder(context)
                .setTitle("ULT Chat Views")
                .setSingleChoiceItems(UltChatView.LABELS, selected, (dialog, choice) -> {
                    preferences.edit().putString(key, UltChatView.MODES[choice]).apply();
                    NotificationCenter.getInstance(account).postNotificationName(
                            NotificationCenter.dialogsNeedReload);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showExternalLinkGuard(Context context) {
        final boolean enabled = UltExternalLinkPolicy.isEnabled(context);
        new android.app.AlertDialog.Builder(context)
                .setTitle("External link confirmations: " + (enabled ? "On" : "Off"))
                .setMessage("Ask before opening HTTP(S) links from Ultgram. "
                        + "This does not block automatic web content, DNS or native Telegram traffic.")
                .setNeutralButton(enabled ? "Disable" : "Enable", (dialog, which) ->
                        UltExternalLinkPolicy.setEnabled(context, !enabled))
                .setPositiveButton("Done", null)
                .show();
    }

    private static void addSection(Context context, LinearLayout parent, String title) {
        TextView label = new TextView(context);
        label.setText(title);
        label.setTextSize(13);
        label.setLetterSpacing(0.05f);
        label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        label.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20),
                AndroidUtilities.dp(20), AndroidUtilities.dp(10));
        parent.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private static LinearLayout createRow(Context context, LinearLayout parent,
                                           String title, String detail, boolean clickable) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(14),
                AndroidUtilities.dp(20), AndroidUtilities.dp(14));
        row.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        row.addView(titleView);
        TextView detailView = new TextView(context);
        detailView.setText(detail);
        detailView.setTextSize(13);
        detailView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = AndroidUtilities.dp(4);
        row.addView(detailView, detailParams);
        row.setClickable(clickable);
        row.setFocusable(clickable);
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private static void addNote(Context context, LinearLayout parent, String title, String detail) {
        createRow(context, parent, title, detail, false);
    }

    private static void addLink(Context context, LinearLayout parent,
                                 String title, String detail, Runnable action) {
        createRow(context, parent, title, detail, true).setOnClickListener(view -> action.run());
    }
}
