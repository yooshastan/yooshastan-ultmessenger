package com.yooshastan.ultmessenger.settings;

import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import com.yooshastan.ultmessenger.conversation.UltChatView;
import com.yooshastan.ultmessenger.dns.UltDoqBootstrap;
import com.yooshastan.ultmessenger.dns.UltDoqEndpoint;
import com.yooshastan.ultmessenger.dns.UltDnsTrackerPolicy;
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
import java.util.HashSet;
import java.util.Set;

/** 
 * Single entry point for Ultgram-owned features. Existing Telegram and
 * Mercurygram settings remain available; preferences are NOT duplicated.
 */
public final class UltSettingsActivity extends BaseFragment {
    private static final String DNS_PREFS = "ult_dns_preview_config";
    private static final String DNS_ENDPOINT = "doq_endpoint";
    private static final String DNS_BOOTSTRAP = "doq_bootstrap_ipv4";
    private static final String DNS_BLOCKED = "tracker_blocked_domains";
    private static final String DNS_ALLOWED = "tracker_allowed_domains";

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
        addLink(context, content, "Network privacy dashboard",
                "Current safeguards, unsupported paths and copyable diagnostic summary",
                () -> showPrivacyDashboard(context));
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
        addLink(context, content, "DNS-over-QUIC configuration (preview)",
                "Save resolver and literal bootstrap IP; native QUIC transport is NOT installed",
                () -> showDoqConfiguration(context));
        addLink(context, content, "Tracker domain rules (preview)",
                "Edit local block/allow rules; filtering is NOT active in this APK",
                () -> showTrackerRules(context));

        addSection(context, content, "ABOUT");
        addNote(context, content, "Ultgram by Yooshastan Studios",
                "Independent Telegram-compatible client. Existing Mercurygram settings remain separate.");
        fragmentView = scroll;
        return fragmentView;
    }

    private SharedPreferences dnsPrefs(Context context) {
        return context.getSharedPreferences(DNS_PREFS, Context.MODE_PRIVATE);
    }

    private void showPrivacyDashboard(Context context) {
        final SharedPreferences prefs = dnsPrefs(context);
        final boolean strictNative = ConnectionsManager.isUltStrictNativeProxyEnabled();
        final String endpoint = prefs.getString(DNS_ENDPOINT, "");
        final String report = "ULTGRAM NETWORK PRIVACY — LOCAL STATUS ONLY\n"
                + "Native Telegram socket guard: " + (strictNative ? "ON (experimental)" : "OFF") + "\n"
                + "DoQ server configured: " + (!endpoint.isEmpty() ? "Yes" : "No") + "\n"
                + "DoQ QUIC backend: NOT INSTALLED\n"
                + "DNS tracker rules: SAVED DRAFT — NOT ACTIVE\n"
                + "Android network DNS: " + UltAndroidDnsInspector.inspect(context) + "\n\n"
                + "UNPROTECTED/UNAUDITED: Java/HTTP, DNS, media, CDN and WebViews. "
                + "A native proxy guard is not a whole-app VPN or a proven kill switch. "
                + "No DNS encryption or tracker filtering is enabled by this preview. "
                + "Do not treat this summary as proof against traffic leaks.";
        new android.app.AlertDialog.Builder(context)
                .setTitle("Ultgram network privacy")
                .setMessage(report)
                .setNeutralButton("Copy status", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager)
                            context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("Ultgram privacy status", report));
                    }
                })
                .setPositiveButton("Done", null)
                .show();
    }

    private void showDoqConfiguration(Context context) {
        final SharedPreferences prefs = dnsPrefs(context);
        LinearLayout fields = new LinearLayout(context);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8),
                AndroidUtilities.dp(20), AndroidUtilities.dp(8));
        EditText endpoint = new EditText(context);
        endpoint.setSingleLine(true);
        endpoint.setHint("quic://dns.example.org:853");
        endpoint.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        endpoint.setText(prefs.getString(DNS_ENDPOINT, ""));
        fields.addView(endpoint);
        EditText bootstrap = new EditText(context);
        bootstrap.setSingleLine(true);
        bootstrap.setHint("Trusted numeric resolver IP (required)");
        bootstrap.setInputType(InputType.TYPE_CLASS_TEXT);
        bootstrap.setText(prefs.getString(DNS_BOOTSTRAP, ""));
        fields.addView(bootstrap);
        new android.app.AlertDialog.Builder(context)
                .setTitle("DoQ resolver draft — NOT ACTIVE")
                .setMessage("Save a DoQ server hostname and its trusted numeric bootstrap "
                        + "address. These settings do not send queries; the native QUIC backend "
                        + "and Telegram resolver integration are not installed. No OS DNS "
                        + "bootstrap or plaintext fallback will be allowed in strict mode.")
                .setView(fields)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save draft", (dialog, which) -> {
                    String address = endpoint.getText().toString().trim();
                    String ip = bootstrap.getText().toString().trim();
                    try {
                        if (!address.isEmpty()) {
                            UltDoqEndpoint.parse(address);
                            if (ip.isEmpty()) {
                                throw new IllegalArgumentException("Bootstrap IP is required");
                            }
                            UltDoqBootstrap.ipv4(ip);
                        } else if (!ip.isEmpty()) {
                            throw new IllegalArgumentException("Enter a DoQ server first");
                        }
                        prefs.edit().putString(DNS_ENDPOINT, address)
                                .putString(DNS_BOOTSTRAP, ip).apply();
                        android.widget.Toast.makeText(context, "Draft saved; DoQ remains inactive",
                                android.widget.Toast.LENGTH_LONG).show();
                    } catch (IllegalArgumentException invalid) {
                        android.widget.Toast.makeText(context, invalid.getMessage(),
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private static Set<String> parseDomains(String text) {
        Set<String> result = new HashSet<>();
        if (text.length() > 16384) {
            throw new IllegalArgumentException("Domain list is too long");
        }
        for (String line : text.split("\\r?\\n", -1)) {
            String rule = line.trim();
            if (!rule.isEmpty() && !rule.startsWith("#")) {
                result.add(UltDnsTrackerPolicy.normalizeDomain(rule));
            }
        }
        if (result.size() > 1024) {
            throw new IllegalArgumentException("Limit rules to 1024 domains per list");
        }
        return result;
    }

    private void showTrackerRules(Context context) {
        final SharedPreferences prefs = dnsPrefs(context);
        LinearLayout fields = new LinearLayout(context);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8),
                AndroidUtilities.dp(20), AndroidUtilities.dp(8));
        EditText blocked = new EditText(context);
        blocked.setHint("Blocked domains, one per line");
        blocked.setMinLines(3);
        blocked.setMaxLines(6);
        blocked.setText(prefs.getString(DNS_BLOCKED, ""));
        fields.addView(blocked);
        EditText allowed = new EditText(context);
        allowed.setHint("Allowlisted domains, one per line");
        allowed.setMinLines(2);
        allowed.setMaxLines(4);
        allowed.setText(prefs.getString(DNS_ALLOWED, ""));
        fields.addView(allowed);
        new android.app.AlertDialog.Builder(context)
                .setTitle("Tracker DNS rules — NOT ACTIVE")
                .setMessage("Save candidate block and allow rules. Suffix matching uses DNS "
                        + "label boundaries; allow rules take precedence. These rules are not "
                        + "enforced until the DoQ resolver is connected to app-owned lookups. "
                        + "No browsing-history or domain logging is added.")
                .setView(fields)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save draft", (dialog, which) -> {
                    String blocking = blocked.getText().toString();
                    String allowing = allowed.getText().toString();
                    try {
                        Set<String> blockRules = parseDomains(blocking);
                        Set<String> allowRules = parseDomains(allowing);
                        new UltDnsTrackerPolicy(blockRules, allowRules);
                        prefs.edit().putString(DNS_BLOCKED, blocking)
                                .putString(DNS_ALLOWED, allowing).apply();
                        android.widget.Toast.makeText(context,
                                "Saved " + blockRules.size() + " block / " + allowRules.size()
                                        + " allow rules (inactive)",
                                android.widget.Toast.LENGTH_LONG).show();
                    } catch (IllegalArgumentException invalid) {
                        android.widget.Toast.makeText(context, invalid.getMessage(),
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                })
                .show();
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
