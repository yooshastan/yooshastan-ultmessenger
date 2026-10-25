package com.yooshastan.ultmessenger.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
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
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
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
        addLink(context, content, "ULT chat layout presets",
                "In Chat Settings, open the ⋮ menu → ULT Chat Layout",
                () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC)));
        addLink(context, content, "Chat views and sorting",
                "All, Unread, Groups, Channels, Bots and Unread First",
                () -> showChatViewPicker(context));
        addNote(context, content, "Archive row visibility",
                "Chats → ⋮ → ULT: Hide Archive row (account-specific display only).");
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
        addLink(context, content, "Experimental native proxy-only mode",
                "Opt-in guard for native Telegram sockets; not a full traffic firewall",
                () -> showNativeStrictProxy(context));
        addLink(context, content, "Android Private DNS (DoT) status",
                "Inspect the current system resolver and open Android DNS settings",
                () -> showAndroidPrivateDns(context));
        addNote(context, content, "Dedicated DNS-over-QUIC and tracker protection",
                "Not implemented. No app-enforced DoQ, encrypted-only DNS or tracker filtering.");

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

    private void showNativeStrictProxy(Context context) {
        final boolean enabled = ConnectionsManager.isUltStrictNativeProxyEnabled();
        if (!enabled && SharedConfig.mg_useTor) {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Native strict mode unavailable with Tor")
                    .setMessage("Mercurygram Tor owns the native proxy slot. "
                            + "Turn Tor off before testing Ultgram's experimental native guard.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        new android.app.AlertDialog.Builder(context)
                .setTitle("Experimental native proxy-only: " + (enabled ? "On" : "Off"))
                .setMessage("When on, Ultgram refuses to open a direct native Telegram "
                        + "socket without an active proxy address. An unavailable or disabled "
                        + "proxy may stop Telegram networking until a working proxy is "
                        + "selected or this mode is turned off. This does not cover Java "
                        + "HTTP, DNS, WebViews, external media or all traffic paths. "
                        + "It is NOT yet verified as fail-closed. Test with PCAPdroid "
                        + "before relying on it for privacy.")
                .setNeutralButton(enabled ? "Disable" : "Enable (experimental)", (dialog, which) ->
                        ConnectionsManager.setUltStrictNativeProxyEnabled(!enabled))
                .setPositiveButton("Cancel", null)
                .show();
    }

    private void showAndroidPrivateDns(Context context) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Android Private DNS (DoT)")
                .setMessage(UltAndroidDnsInspector.inspect(context)
                        + "\n\nThis is Android's current network DNS status, not an "
                        + "Ultgram-controlled resolver. Private DNS uses DoT, not DoQ. "
                        + "Native proxy, Java, media and WebView resolution paths are "
                        + "not independently leak-verified. A VPN such as PCAPdroid "
                        + "may affect the network state shown here.")
                .setNeutralButton("Open network settings", (dialog, which) -> {
                    try {
                        context.startActivity(new Intent("android.settings.PRIVATE_DNS_SETTINGS"));
                    } catch (RuntimeException missingPrivateDnsPage) {
                        try {
                            context.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                        } catch (RuntimeException missingNetworkSettings) {
                            android.widget.Toast.makeText(context,
                                    "Search Android Settings for Private DNS",
                                    android.widget.Toast.LENGTH_LONG).show();
                        }
                    }
                })
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
