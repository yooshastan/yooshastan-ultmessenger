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
import com.yooshastan.ultmessenger.appearance.UltAppearancePreset;
import com.yooshastan.ultmessenger.appearance.UltAppearanceThemeAdapter;
import com.yooshastan.ultmessenger.appearance.UltCustomTheme;
import com.yooshastan.ultmessenger.translation.UltTranslationPolicy;
import com.yooshastan.ultmessenger.dns.UltDoqBootstrap;
import com.yooshastan.ultmessenger.dns.UltDoqEndpoint;
import com.yooshastan.ultmessenger.dns.UltDnsTrackerPolicy;
import com.yooshastan.ultmessenger.proxy.UltExternalLinkPolicy;
import com.yooshastan.ultmessenger.proxy.UltStrictProxyPolicy;
import com.yooshastan.ultmessenger.settings.UltScreenPrivacy;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** 
 * Single entry point for Ultgram-owned features. Existing Telegram and
 * Mercurygram settings remain available; preferences are NOT duplicated.
 */
public final class UltSettingsActivity extends BaseFragment {
    private static final String DNS_PREFS = "ult_dns_preview_config";
    private static final String UI_PREFS = "ult_interface_preferences";
    private static final String COMPACT_ROWS = "compact_ult_settings";
    private final ArrayList<LinearLayout> settingsRows = new ArrayList<>();
    private boolean compactRows;
    private static final String DNS_ENDPOINT = "doq_endpoint";
    private static final String DNS_BOOTSTRAP = "doq_bootstrap_ipv4";
    private static final String DNS_BLOCKED = "tracker_blocked_domains";
    private static final String DNS_ALLOWED = "tracker_allowed_domains";

    @Override
    public View createView(Context context) {
        settingsRows.clear();
        compactRows = context.getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE)
                .getBoolean(COMPACT_ROWS, false);
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

        addSection(context, content, "NEW IN THIS BUILD");
        addLink(context, content, "What's New / build information",
                "See Alpha 0.13c+d highlights and the installed Android package version",
                () -> showUltWhatsNew(context));
        addLink(context, content, "Downloads",
                "Active and recent files; long-press a downloaded document for Save As",
                () -> presentFragment(new UltDownloadsActivity()));

        addSection(context, content, "APPEARANCE");
        addLink(context, content, "Chat list preview density",
                "Switch Telegram's existing compact two-line / expanded three-line preview",
                () -> showChatListDensity(context));
        addLink(context, content, "Compact Ultgram Settings rows",
                "Reduce spacing in this settings page only (not the chat list)",
                () -> setCompactRows(context, !compactRows));
        addLink(context, content, "ULT theme presets", "Six presets, fixed colors and light/dark pairing",
                () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC)));
        addLink(context, content, "Preview ULT theme palettes",
                "Inspect original light and dark bubble colors without applying a theme",
                () -> showUltThemePreview(context));
        addLink(context, content, "Custom ULT theme colors",
                "Create a fixed theme with your own accent and outgoing message bubble",
                () -> showUltCustomThemeChooser(context));

        addSection(context, content, "CHATS AND TOOLS");
        addLink(context, content, "ULT chat layout presets",
                "In Chat Settings, open the ⋮ menu → ULT Chat Layout",
                () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC)));
        addLink(context, content, "Show chat category bar",
                "Optional All / Unread / Groups / Channels / Bots / People strip on Chats",
                () -> toggleChatCategoryBar(context));
        addLink(context, content, "Customize chat category tabs",
                "Choose visible categories; reorder them locally for this account",
                () -> showTabCustomization(context));
        addLink(context, content, "Category bar appearance",
                "Compact, comfortable or spacious chips for this account",
                () -> showCategoryBarAppearance(context));
        addLink(context, content, "Category typography",
                "Small, normal or large category labels; follows Android font scaling",
                () -> showCategoryTypography(context));
        addLink(context, content, "Swipe category navigator",
                "Optional dedicated gesture surface; never intercepts chat-row swipes",
                () -> showSwipeNavigator(context));
        addLink(context, content, "Offline translation status",
                "Read-only active translation engine and local provider readiness",
                () -> showOfflineTranslationStatus(context));
        addLink(context, content, "Chat views and sorting",
                "All, Unread, Groups, Channels, Bots, People and Unread First",
                () -> showChatViewPicker(context));
        addNote(context, content, "Archive row visibility",
                "Chats → ⋮ → ULT: Hide Archive row (account-specific display only).");
        addNote(context, content, "Chat translation",
                "Use a chat's ⋮ menu → ULT Chat Translation. Language and consent are per chat.");
        addNote(context, content, "Message and chat tools",
                "Long-press a document for an optional readable file summary; nothing downloads automatically.");
        addLink(context, content, "Download queue and recent files",
                "Open Telegram's native downloads view for this account; tap to open/download/cancel",
                () -> presentFragment(new UltDownloadsActivity()));
        addLink(context, content, "Media storage and cache",
                "Open Telegram's native storage manager; deleting anything requires a separate tap",
                () -> presentFragment(new org.telegram.ui.CacheControlActivity()));
        addLink(context, content, "Media auto-download controls",
                "Use Telegram's existing mobile, Wi-Fi and roaming download policies",
                () -> showNativeMediaDownloadSettings(context));

        addSection(context, content, "PRIVACY AND NETWORK");
        addLink(context, content, "Protect Ultgram screenshots",
                "Opt-in Android FLAG_SECURE for main window and Ultgram Settings dialogs",
                () -> showUltScreenPrivacy(context));
        addLink(context, content, "Network test checklist",
                "Mark the manual Wonhee/MTProxy tests you performed; no packet capture",
                () -> showNetworkTestChecklist(context));
        addLink(context, content, "Copy network test report",
                "Read-only VPN hint, native guard and manual test states; no IPs or secrets",
                () -> showNetworkTestReport(context));
        addLink(context, content, "Network privacy dashboard",
                "Current safeguards, unsupported paths and copyable diagnostic summary",
                () -> showPrivacyDashboard(context));
        addLink(context, content, "Identity and privacy limitations",
                "What a proxy, local display settings, and Telegram account identity actually protect",
                () -> showIdentityPrivacyLimits(context));
        addLink(context, content, "Network/DNS source audit matrix",
                "Read-only owned-code audit and missing capture evidence",
                () -> showNetworkSourceMatrix(context));
        addLink(context, content, "Network path audit coverage",
                "Read-only inventory of native, DNS, HTTP, media/CDN and WebView paths",
                () -> showNetworkPathCoverage(context));
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


    private static TextView ultPreviewBubble(Context context, String caption,
                                              int textColor, int backgroundColor) {
        TextView bubble = new TextView(context);
        bubble.setText(caption);
        bubble.setTextColor(textColor);
        bubble.setTextSize(14);
        bubble.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8),
                AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        android.graphics.drawable.GradientDrawable fill =
                new android.graphics.drawable.GradientDrawable();
        fill.setColor(backgroundColor);
        fill.setCornerRadius(AndroidUtilities.dp(13));
        bubble.setBackground(fill);
        return bubble;
    }

    // Alpha 0.13a: a real fixed native Telegram theme, not just a color preview.
    // The existing light/dark paired presets and original themes remain unchanged.
    @Override
    public void onResume() {
        super.onResume();
        final android.app.Activity activity = getParentActivity();
        if (activity == null) return;
        final SharedPreferences prefs = activity.getSharedPreferences(
                UltReleaseNotes.PREFERENCES, Context.MODE_PRIVATE);
        final String key = UltReleaseNotes.seenKey(installedAndroidVersion(activity));
        if (!prefs.getBoolean(key, false)) {
            prefs.edit().putBoolean(key, true).apply();
            showUltWhatsNew(activity);
        }
    }

    private String installedAndroidVersion(Context context) {
        try {
            final android.content.pm.PackageInfo pkg = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            final long code = android.os.Build.VERSION.SDK_INT >= 28
                    ? pkg.getLongVersionCode() : pkg.versionCode;
            return UltReleaseNotes.androidPackageVersion(pkg.versionName, code);
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "Unavailable";
        }
    }

    private void showUltWhatsNew(Context context) {
        new UltSecureDialogBuilder(context)
                .setTitle(UltReleaseNotes.DISPLAY_VERSION + " — What's New")
                .setMessage(UltReleaseNotes.message(installedAndroidVersion(context)))
                .setNeutralButton("Open Downloads", (dialog, which) ->
                        presentFragment(new UltDownloadsActivity()))
                .setPositiveButton("Done", null)
                .show();
    }

    private void showUltCustomThemeChooser(Context context) {
        final UltAppearancePreset[] presets = {
                UltAppearancePreset.MIDNIGHT, UltAppearancePreset.CANDY,
                UltAppearancePreset.STAGE, UltAppearancePreset.MINIMAL,
                UltAppearancePreset.AURORA, UltAppearancePreset.OBSIDIAN
        };
        final String[] labels = new String[presets.length];
        for (int i = 0; i < presets.length; i++) labels[i] = presets[i].displayName;
        new UltSecureDialogBuilder(context)
                .setTitle("Custom ULT theme — choose a base")
                .setItems(labels, (dialog, which) ->
                        showUltCustomThemeEditor(context, presets[which]))
                .setNegativeButton("Cancel", null).show();
    }

    private void showUltCustomThemeEditor(Context context, UltAppearancePreset preset) {
        final UltAppearancePreset.Palette base = preset.forMode(preset.dark);
        final LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8),
                AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        TextView intro = new TextView(context);
        intro.setText("Fixed day/night appearance from " + preset.displayName
                + ". Use #RRGGBB colors. Text contrast is checked before applying. "
                + "System-following paired palettes are unchanged.");
        controls.addView(intro);
        final EditText accent = new EditText(context);
        accent.setSingleLine(true);
        accent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        accent.setHint("Accent e.g. #E86BCB");
        accent.setText(UltCustomTheme.toHex(base.accentArgb));
        controls.addView(accent);
        final EditText outgoing = new EditText(context);
        outgoing.setSingleLine(true);
        outgoing.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        outgoing.setHint("Outgoing bubble e.g. #422C50");
        outgoing.setText(UltCustomTheme.toHex(base.outgoingBubbleArgb));
        controls.addView(outgoing);
        new UltSecureDialogBuilder(context)
                .setTitle(preset.displayName + " custom colors")
                .setView(controls)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply fixed theme", (dialog, ignored) -> {
                    try {
                        if (UltAppearanceThemeAdapter.applyCustom(context, preset,
                                accent.getText().toString(), outgoing.getText().toString())) {
                            if (parentLayout != null) {
                                parentLayout.rebuildAllFragmentViews(true, true);
                            }
                            android.widget.Toast.makeText(context,
                                    "Custom fixed theme applied; original presets retained",
                                    android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            android.widget.Toast.makeText(context,
                                    "Theme import failed; previous theme retained",
                                    android.widget.Toast.LENGTH_LONG).show();
                        }
                    } catch (IllegalArgumentException | java.io.IOException error) {
                        android.widget.Toast.makeText(context,
                                "Theme not applied: " + error.getMessage(),
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void showNativeMediaDownloadSettings(Context context) {
        new UltSecureDialogBuilder(context)
                .setTitle("Telegram media auto-download")
                .setItems(new String[]{"Mobile data", "Wi-Fi", "Roaming"},
                        (dialog, which) -> presentFragment(
                                new org.telegram.ui.DataAutoDownloadActivity(which)))
                .setNegativeButton("Cancel", null).show();
    }

    private void showIdentityPrivacyLimits(Context context) {
        new UltSecureDialogBuilder(context)
                .setTitle("Identity and privacy: real scope")
                .setMessage("Changing your local app appearance or account label does not "
                        + "hide your Telegram account from Telegram or chat recipients. "
                        + "A Telegram proxy can hide your home IP from some Telegram endpoints, "
                        + "but it is not a whole-device VPN or a verified whole-app kill switch. "
                        + "Calls, WebViews, DNS, media/CDN and Java networking need independent "
                        + "testing. No identity spoofing, DoQ transport or tracker enforcement "
                        + "is enabled here. Keep phone-number visibility and account discovery "
                        + "settings configured in Telegram Privacy and Security.")
                .setPositiveButton("Understood", null).show();
    }

    private void showUltThemePreview(Context context) {
        final UltAppearancePreset[] presets = {
                UltAppearancePreset.MIDNIGHT, UltAppearancePreset.CANDY,
                UltAppearancePreset.STAGE, UltAppearancePreset.MINIMAL,
                UltAppearancePreset.AURORA, UltAppearancePreset.OBSIDIAN
        };
        final String[] labels = new String[presets.length];
        for (int i = 0; i < presets.length; i++) labels[i] = presets[i].displayName;
        new UltSecureDialogBuilder(context)
                .setTitle("ULT palette previews")
                .setItems(labels, (dialog, which) -> {
                    final UltAppearancePreset preset = presets[which];
                    final LinearLayout preview = new LinearLayout(context);
                    preview.setOrientation(LinearLayout.VERTICAL);
                    preview.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8),
                            AndroidUtilities.dp(16), AndroidUtilities.dp(8));
                    for (boolean night : new boolean[] {false, true}) {
                        final UltAppearancePreset.Palette colors = preset.forMode(night);
                        final LinearLayout panel = new LinearLayout(context);
                        panel.setOrientation(LinearLayout.VERTICAL);
                        panel.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10),
                                AndroidUtilities.dp(12), AndroidUtilities.dp(10));
                        android.graphics.drawable.GradientDrawable card =
                                new android.graphics.drawable.GradientDrawable();
                        card.setColor(colors.backgroundArgb);
                        card.setCornerRadius(AndroidUtilities.dp(14));
                        panel.setBackground(card);
                        final TextView title = new TextView(context);
                        title.setText(night ? "Night palette" : "Day palette");
                        title.setTextColor(colors.accentArgb);
                        title.setTextSize(14);
                        panel.addView(title);
                        panel.addView(ultPreviewBubble(context, "Incoming message preview",
                                colors.textArgb, colors.incomingBubbleArgb));
                        panel.addView(ultPreviewBubble(context, "Outgoing message preview",
                                colors.textArgb, colors.outgoingBubbleArgb));
                        LinearLayout.LayoutParams box = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        box.bottomMargin = AndroidUtilities.dp(10);
                        preview.addView(panel, box);
                    }
                    new UltSecureDialogBuilder(context)
                            .setTitle(preset.displayName + " (preview only)")
                            .setView(preview)
                            .setPositiveButton("Done", null)
                            .show();
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void showChatListDensity(Context context) {
        final boolean threeLines = SharedConfig.useThreeLinesLayout;
        new UltSecureDialogBuilder(context)
                .setTitle("Telegram chat list preview")
                .setSingleChoiceItems(new String[] {
                        "Two-line preview (compact)",
                        "Three-line preview (more text)" }, threeLines ? 1 : 0,
                        (dialog, selected) -> {
                            SharedConfig.setUseThreeLinesLayout(selected == 1);
                            android.widget.Toast.makeText(context,
                                    "Chat list preview updated",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                .setNegativeButton("Cancel", null).show();
    }

    private void setCompactRows(Context context, boolean enabled) {
        compactRows = enabled;
        context.getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(COMPACT_ROWS, enabled).apply();
        for (LinearLayout row : settingsRows) {
            int vertical = AndroidUtilities.dp(enabled ? 9 : 14);
            row.setPadding(AndroidUtilities.dp(20), vertical,
                    AndroidUtilities.dp(20), vertical);
        }
        android.widget.Toast.makeText(context,
                enabled ? "Compact Ultgram Settings enabled" : "Default spacing restored",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private SharedPreferences testPrefs(Context context) {
        return context.getSharedPreferences(UltNetworkTestReport.PREFERENCES, Context.MODE_PRIVATE);
    }

    private boolean[] manualChecks(Context context) {
        boolean[] values = new boolean[UltNetworkTestReport.CHECKS.length];
        SharedPreferences prefs = testPrefs(context);
        for (int i = 0; i < values.length; i++) {
            values[i] = prefs.getBoolean(UltNetworkTestReport.key(i), false);
        }
        return values;
    }

    private void showNetworkTestChecklist(Context context) {
        final boolean[] values = manualChecks(context);
        new UltSecureDialogBuilder(context)
                .setTitle("ULT manual network checks")
                .setMultiChoiceItems(UltNetworkTestReport.CHECKS, values,
                        (dialog, which, checked) -> values[which] = checked)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save checks", (dialog, which) -> {
                    SharedPreferences.Editor edit = testPrefs(context).edit();
                    for (int i = 0; i < values.length; i++) {
                        edit.putBoolean(UltNetworkTestReport.key(i), values[i]);
                    }
                    edit.apply();
                })
                .show();
    }

    private void showNetworkTestReport(Context context) {
        final String report = UltNetworkTestReport.render(
                UltAndroidDnsInspector.inspectVpn(context),
                ConnectionsManager.isUltStrictNativeProxyEnabled(),
                ConnectionsManager.native_isStrictProxyModeEnabled(UserConfig.selectedAccount),
                android.os.Build.VERSION.SDK_INT, manualChecks(context));
        new UltSecureDialogBuilder(context)
                .setTitle("Ultgram network test report")
                .setMessage(report)
                .setNeutralButton("Copy report", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager)
                            context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(
                                ClipData.newPlainText("Ultgram network test report", report));
                    }
                })
                .setPositiveButton("Done", null)
                .show();
    }

    private void showUltScreenPrivacy(Context context) {
        final boolean enabled = UltScreenPrivacy.isEnabled(context);
        new UltSecureDialogBuilder(context)
                .setTitle("Ultgram screenshot privacy: " + (enabled ? "On" : "Off"))
                .setMessage("Optional Android FLAG_SECURE for the main Ultgram window "
                        + "and Ultgram Settings dialogs. Screenshots, recordings and "
                        + "app recents previews can be blocked for these windows. "
                        + "Other Telegram dialogs and windows, notifications, external "
                        + "apps, compromised devices and photographs are not covered "
                        + "by this change. Account identity is not anonymized. "
                        + "Telegram secure flags and system overrides still apply.")
                .setNeutralButton(enabled ? "Disable" : "Enable", (dialog, which) -> {
                    UltScreenPrivacy.setEnabled(context, !enabled);
                    if (context instanceof org.telegram.ui.LaunchActivity) {
                        ((org.telegram.ui.LaunchActivity) context).refreshUltScreenPrivacy();
                    }
                    android.widget.Toast.makeText(context,
                            !enabled ? "Main-window screenshot privacy enabled"
                                    : "Ultgram screen privacy disabled; Telegram rules still apply",
                            android.widget.Toast.LENGTH_LONG).show();
                })
                .setPositiveButton("Done", null).show();
    }

    private void showNetworkPathCoverage(Context context) {
        final String report = "ULTGRAM NETWORK PATH COVERAGE — STATIC CODE AUDIT\n"
                + "Native tgnet sockets: experimental guard exists; live egress not proven\n"
                + "Java HTTP and hostname resolution: NOT FULLY AUDITED\n"
                + "Android DNS, DoT, DoQ: external routing NOT VERIFIED; DoQ backend NOT INSTALLED\n"
                + "Media downloads and CDN: NOT ROUTE-VERIFIED\n"
                + "WebViews, previews and external links: NOT ROUTE-VERIFIED\n"
                + "Calls / real-time media (UDP): NOT ROUTE-VERIFIED\n\n"
                + "Capture required: phone-edge/gateway + tunnel-inner + proxy-host; "
                + "a proxy-host capture alone cannot rule out direct traffic. "
                + "Android VPN indicator does not identify the app route. "
                + "No new packet capture, DNS filtering, or traffic monitoring is active.";
        new UltSecureDialogBuilder(context)
                .setTitle("Ultgram network path coverage")
                .setMessage(report)
                .setNeutralButton("Copy coverage", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager)
                            context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("ULT network coverage", report));
                    }
                })
                .setPositiveButton("Done", null)
                .show();
    }

    private SharedPreferences dnsPrefs(Context context) {
        return context.getSharedPreferences(DNS_PREFS, Context.MODE_PRIVATE);
    }

    private void showPrivacyDashboard(Context context) {
        final SharedPreferences prefs = dnsPrefs(context);
        final boolean strictNative = ConnectionsManager.isUltStrictNativeProxyEnabled();
        final boolean observedNative = ConnectionsManager.native_isStrictProxyModeEnabled(UserConfig.selectedAccount);
        final String endpoint = prefs.getString(DNS_ENDPOINT, "");
        final String report = "ULTGRAM NETWORK PRIVACY — LOCAL STATUS ONLY\n"
                + "Native Telegram socket guard: " + UltStrictProxyPolicy.nativeStatus(strictNative, observedNative) + "\n"
                + "DoQ server configured: " + (!endpoint.isEmpty() ? "Yes" : "No") + "\n"
                + "DoQ QUIC backend: NOT INSTALLED\n"
                + "DNS tracker rules: SAVED DRAFT — NOT ACTIVE\n"
                + "Android active-network VPN: " + UltAndroidDnsInspector.inspectVpn(context) + "\n"
                + "Android network DNS: " + UltAndroidDnsInspector.inspect(context) + "\n\n"
                + "UNPROTECTED/UNAUDITED: Java/HTTP, DNS, media, CDN and WebViews. "
                + "A native proxy guard is not a whole-app VPN or a proven kill switch. "
                + "No DNS encryption or tracker filtering is enabled by this preview. "
                + "Do not treat this summary as proof against traffic leaks.";
        new UltSecureDialogBuilder(context)
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
        new UltSecureDialogBuilder(context)
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
        new UltSecureDialogBuilder(context)
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

    private void toggleChatCategoryBar(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE);
        String key = UltChatView.quickBarKey(UserConfig.selectedAccount);
        boolean enabled = !preferences.getBoolean(key, false);
        preferences.edit().putBoolean(key, enabled).apply();
        android.widget.Toast.makeText(context,
                enabled ? "Category bar enabled in Chats" : "Category bar hidden",
                android.widget.Toast.LENGTH_SHORT).show();
    }


    private void showCategoryBarAppearance(Context context) {
        final int account = UserConfig.selectedAccount;
        final SharedPreferences prefs = context.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE);
        final String key = UltChatView.barStyleKey(account);
        final int selected = UltChatView.barStyleIndex(
                prefs.getString(key, UltChatView.BAR_COMFORTABLE));
        new UltSecureDialogBuilder(context)
                .setTitle("ULT category bar appearance")
                .setSingleChoiceItems(UltChatView.BAR_STYLE_LABELS, selected, (dialog, which) -> {
                    prefs.edit().putString(key, UltChatView.BAR_STYLES[which]).apply();
                    android.widget.Toast.makeText(context,
                            "Return to Chats to refresh category chips",
                            android.widget.Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNeutralButton("Restore default", (dialog, which) -> {
                    prefs.edit().remove(key).apply();
                    android.widget.Toast.makeText(context,
                            "Comfortable category chips restored",
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show();
    }


    private void showCategoryTypography(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE);
        final String key = UltChatView.barFontKey(UserConfig.selectedAccount);
        final int selected = UltChatView.barFontIndex(
                prefs.getString(key, UltChatView.FONT_NORMAL));
        new UltSecureDialogBuilder(context)
                .setTitle("Category bar typography")
                .setSingleChoiceItems(UltChatView.FONT_STYLE_LABELS, selected, (dialog, which) -> {
                    prefs.edit().putString(key, UltChatView.FONT_STYLES[which]).apply();
                    android.widget.Toast.makeText(context,
                            "Return to Chats to refresh category label size",
                            android.widget.Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNeutralButton("Restore default", (dialog, which) ->
                        prefs.edit().remove(key).apply())
                .setNegativeButton("Cancel", null).show();
    }

    private void showSwipeNavigator(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE);
        final String key = UltChatView.swipePadKey(UserConfig.selectedAccount);
        final int selected = prefs.getBoolean(key, false) ? 1 : 0;
        new UltSecureDialogBuilder(context)
                .setTitle("Dedicated swipe category navigator")
                .setMessage("Off by default. When enabled, a narrow ↔ control appears next to the optional category strip. Swipe on that control to change only local visible categories, or tap for next. Normal chat rows, Telegram folders and Android back gestures are never intercepted.")
                .setSingleChoiceItems(new String[] {"Disabled", "Enabled"}, selected,
                        (dialog, which) -> {
                            prefs.edit().putBoolean(key, which == 1).apply();
                            android.widget.Toast.makeText(context,
                                    "Return to Chats to update the category navigator",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                .setNegativeButton("Cancel", null).show();
    }

    private void showTabCustomization(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE);
        final int account = UserConfig.selectedAccount;
        final String key = UltChatView.tabConfigKey(account);
        final String current = prefs.getString(key, UltChatView.defaultTabConfig());
        final String[] modes = {UltChatView.UNREAD, UltChatView.GROUPS,
                UltChatView.CHANNELS, UltChatView.BOTS, UltChatView.PEOPLE};
        final String[] labels = {"Unread", "Groups", "Channels", "Bots", "People"};
        final boolean[] checked = new boolean[modes.length];
        for (int i = 0; i < modes.length; i++) {
            checked[i] = UltChatView.isTabVisible(current, modes[i]);
        }
        new UltSecureDialogBuilder(context)
                .setTitle("Visible category tabs (All always shown)")
                .setMultiChoiceItems(labels, checked, (dialog, index, enabled) -> checked[index] = enabled)
                .setPositiveButton("Save", (dialog, which) -> {
                    String saved = current;
                    for (int i = 0; i < modes.length; i++) {
                        if (!checked[i]) saved = UltChatView.changeTabVisibility(saved, modes[i], false);
                    }
                    for (int i = 0; i < modes.length; i++) {
                        if (checked[i] && !UltChatView.isTabVisible(saved, modes[i])) {
                            saved = UltChatView.changeTabVisibility(saved, modes[i], true);
                        }
                    }
                    SharedPreferences.Editor editor = prefs.edit().putString(key, saved);
                    String active = prefs.getString(UltChatView.accountKey(account), UltChatView.ALL);
                    if (UltChatView.isQuickTab(active) && !UltChatView.isTabVisible(saved, active)) {
                        editor.putString(UltChatView.accountKey(account), UltChatView.ALL);
                    }
                    editor.apply();
                    NotificationCenter.getInstance(account).postNotificationName(
                            NotificationCenter.dialogsNeedReload);
                    showTabOrder(context, account, prefs, key);
                })
                .setNeutralButton("Reorder existing", (dialog, which) ->
                        showTabOrder(context, account, prefs, key))
                .setNegativeButton("Cancel", null).show();
    }

    private void showTabOrder(Context context, int account,
                              SharedPreferences prefs, String key) {
        final String current = prefs.getString(key, UltChatView.defaultTabConfig());
        final String[] tabs = UltChatView.visibleTabs(current);
        final String[] labels = new String[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            labels[i] = (i == 0 ? "All (fixed)" : UltChatView.quickTabLabel(tabs[i]));
        }
        new UltSecureDialogBuilder(context)
                .setTitle("Category order: select a tab to move")
                .setItems(labels, (dialog, which) -> {
                    if (which == 0) {
                        android.widget.Toast.makeText(context, "All stays first",
                                android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new UltSecureDialogBuilder(context)
                            .setTitle(UltChatView.quickTabLabel(tabs[which]))
                            .setItems(new String[] {"Move earlier", "Move later"}, (moveDialog, direction) -> {
                                String next = UltChatView.moveTab(current, tabs[which],
                                        direction == 0 ? -1 : 1);
                                prefs.edit().putString(key, next).apply();
                                android.widget.Toast.makeText(context,
                                        "Return to Chats to refresh category order",
                                        android.widget.Toast.LENGTH_SHORT).show();
                                showTabOrder(context, account, prefs, key);
                            })
                            .setNegativeButton("Cancel", null).show();
                })
                .setNeutralButton("Reset categories", (dialog, which) -> {
                    prefs.edit().remove(key).apply();
                    android.widget.Toast.makeText(context,
                            "Default tabs restored; reopen Chats",
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("Done", null).show();
    }

    private void showOfflineTranslationStatus(Context context) {
        final String mode = SharedConfig.mg_translateMode;
        final boolean offline = UltTranslationPolicy.isOfflineMode(mode);
        final boolean ready =
                it.belloworld.mercurygram.translate.MgTranslateDispatcher.isOfflineUsable();
        final String report = "ULTGRAM TRANSLATION STATUS (READ ONLY)\n"
                + "Selected engine: " + UltTranslationPolicy.engineLabel(mode) + "\n"
                + "Offline provider: " + (ready ? "available (capability reported)"
                        : "NOT INSTALLED / NOT AVAILABLE") + "\n"
                + "Offline mode selected: " + (offline ? "YES" : "NO") + "\n\n"
                + (offline ? (ready ? "The local provider reports ready; verify a translation on-device."
                                    : "Fail closed: no remote fallback is permitted while offline mode is selected.")
                           : "Remote translation may send message text outside the device with consent.")
                + "\nSecret chats: network translation prohibited by Ultgram policy."
                + "\nNo language model is bundled by Ultgram.";
        new UltSecureDialogBuilder(context)
                .setTitle("Translation engine availability")
                .setMessage(report)
                .setPositiveButton("Done", null).show();
    }

    private void showNetworkSourceMatrix(Context context) {
        final String report = UltNetworkSourceMatrix.render();
        new UltSecureDialogBuilder(context)
                .setTitle("Network/DNS audit: source review only")
                .setMessage(report)
                .setNeutralButton("Copy audit", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager)
                            context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("ULT network matrix", report));
                    }
                })
                .setPositiveButton("Done", null).show();
    }

    private void showChatViewPicker(Context context) {
        final SharedPreferences preferences = context.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE);
        final int account = UserConfig.selectedAccount;
        final String key = UltChatView.accountKey(account);
        final int selected = UltChatView.modeIndex(
                preferences.getString(key, UltChatView.ALL));
        new UltSecureDialogBuilder(context)
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
        new UltSecureDialogBuilder(context)
                .setTitle("External link confirmations: " + (enabled ? "On" : "Off"))
                .setMessage("Ask before opening HTTP(S) links from Ultgram. "
                        + "This does not block automatic web content, DNS or native Telegram traffic.")
                .setNeutralButton(enabled ? "Disable" : "Enable", (dialog, which) ->
                        UltExternalLinkPolicy.setEnabled(context, !enabled))
                .setPositiveButton("Done", null)
                .show();
    }

    private void showNativeStrictProxy(Context context) {
        final boolean requested = ConnectionsManager.isUltStrictNativeProxyEnabled();
        final boolean observed = ConnectionsManager.native_isStrictProxyModeEnabled(UserConfig.selectedAccount);
        // Keep OFF accessible even when saved settings and native state disagree,
        // the configured proxy was removed, or Tor was turned on later.
        new UltSecureDialogBuilder(context)
                .setTitle("Experimental native proxy-only: " + (requested ? "On" : "Off"))
                .setMessage("Saved strict switch: " + (requested ? "ON" : "OFF")
                        + ". Native state for this account: " + (observed ? "ON" : "OFF")
                        + ". Disabling always requests native reconnection, even if "
                        + "the saved switch is already OFF. Enabling requires a valid "
                        + "selected proxy; proxy failure may interrupt messages. "
                        + "This is NOT a full firewall or proof of DNS/media/Java routing. "
                        + "Keep Wonhee connected and independently verify network paths "
                        + "before relying on privacy claims.")
                .setNeutralButton("Disable and reconnect", (dialog, which) ->
                        ConnectionsManager.setUltStrictNativeProxyEnabled(false))
                .setNegativeButton(requested || observed ? null : "Enable (experimental)",
                        (dialog, which) -> {
                            if (SharedConfig.mg_useTor) {
                                new UltSecureDialogBuilder(context)
                                        .setTitle("Native strict mode unavailable with Tor")
                                        .setMessage("Mercurygram Tor owns the native proxy slot. "
                                                + "Turn Tor off before testing the native guard.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            } else if (!ConnectionsManager.canEnableUltStrictNativeProxy()) {
                                new UltSecureDialogBuilder(context)
                                        .setTitle("Select a proxy before strict mode")
                                        .setMessage("Strict mode cannot be enabled while Use Proxy is OFF "
                                                + "or the selected proxy is invalid. Open ULT Proxy Manager, "
                                                + "select a valid MTProto or SOCKS5 profile, and enable Use Proxy. "
                                                + "This checks configuration only, not proxy reachability.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            } else {
                                new UltSecureDialogBuilder(context)
                                        .setTitle("Confirm experimental strict mode")
                                        .setMessage("Telegram native sockets will be restricted. A proxy "
                                                + "failure can interrupt messages. This does not enforce "
                                                + "DNS, Java, media, or device-wide routing.")
                                        .setNegativeButton("Cancel", null)
                                        .setPositiveButton("Enable", (confirm, button) -> {
                                            if (!ConnectionsManager.setUltStrictNativeProxyEnabled(true)) {
                                                new UltSecureDialogBuilder(context)
                                                        .setTitle("Strict mode was not enabled")
                                                        .setMessage("Proxy configuration changed. "
                                                                + "Select and enable a valid proxy first.")
                                                        .setPositiveButton("OK", null)
                                                        .show();
                                            }
                                        })
                                        .show();
                            }
                        })
                .setPositiveButton("Done", null)
                .show();
    }

    private void showAndroidPrivateDns(Context context) {
        new UltSecureDialogBuilder(context)
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

    /**
     * Ultgram Settings dialogs have separate windows from LaunchActivity.
     * Apply screenshot privacy to the dialog window before showing it.
     * Do not override Telegram passcode restrictions or the global bypass.
     */
    private static final class UltSecureDialogBuilder extends android.app.AlertDialog.Builder {
        UltSecureDialogBuilder(Context context) {
            super(context);
        }

        @Override
        public android.app.AlertDialog show() {
            final android.app.AlertDialog dialog = create();
            final boolean telegramPasscodeSecure = SharedConfig.passcodeHash.length() > 0
                    && !SharedConfig.allowScreenCapture;
            if ((telegramPasscodeSecure || UltScreenPrivacy.isEnabled(getContext()))
                    && !SharedConfig.disableSecureFlags && dialog.getWindow() != null) {
                dialog.getWindow().addFlags(
                        android.view.WindowManager.LayoutParams.FLAG_SECURE);
            }
            dialog.show();
            return dialog;
        }
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

    private LinearLayout createRow(Context context, LinearLayout parent,
                                           String title, String detail, boolean clickable) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        int rowSpacing = AndroidUtilities.dp(compactRows ? 9 : 14);
        row.setPadding(AndroidUtilities.dp(20), rowSpacing,
                AndroidUtilities.dp(20), rowSpacing);
        settingsRows.add(row);
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

    private void addNote(Context context, LinearLayout parent, String title, String detail) {
        createRow(context, parent, title, detail, false);
    }

    private void addLink(Context context, LinearLayout parent,
                                 String title, String detail, Runnable action) {
        createRow(context, parent, title, detail, true).setOnClickListener(view -> action.run());
    }
}
