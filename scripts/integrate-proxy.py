#!/usr/bin/env python3
"""Guarded Alpha 0.3 integration with Mercurygram's native proxy UI.

No proxy transport, routing state, credentials or Tor controls are replaced.
Only local alias labels and an informational privacy action are injected.
"""
import argparse
from pathlib import Path

TARGET = Path("TMessagesProj/src/main/java/org/telegram/ui/ProxyListActivity.java")
MODEL = Path("proxy/src/com/yooshastan/ultmessenger/proxy/UltProxyLabels.java")
DIAGNOSTICS_MODEL = Path("proxy/src/com/yooshastan/ultmessenger/proxy/UltProxyDiagnostics.java")
STRICT_POLICY_MODEL = Path("proxy/src/com/yooshastan/ultmessenger/proxy/UltStrictProxyPolicy.java")
IMPORT_ANCHOR = "import org.telegram.ui.Components.SlideChooseView;\n"
CLASS_ANCHOR = "public class ProxyListActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {\n"
EDIT_ANCHOR = "            checkImageView.setOnClickListener(v -> presentFragment(new ProxySettingsActivity(currentInfo)));\n"
DISPLAY_ANCHOR = (
    "                textView.setText(proxyInfo.settings.getType() == ProxySettings.Type.WEB\n"
    '                        ? proxyInfo.settings.getAddress() + " (WEB)"\n'
    '                        : proxyInfo.settings.getAddress() + ":" + proxyInfo.settings.getPort());\n'
)
MENU_ANCHOR = "        actionBar.setAllowOverlayTitle(false);\n"
ADAPTIVE_ANCHOR = "        actionBar.setAdaptiveBackground(listView);\n"
THEME_ICON_ANCHOR = ("        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));\n")
THEME_TITLE_ANCHOR = ("        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));\n")
CLICK_ANCHOR = "                switch (id) {\n                    case -1:\n"

FIELDS = """
    private static final int ULT_PROXY_PRIVACY_MENU_ID = 0x554c50;

    private String getUltProxyLabel(SharedConfig.ProxyInfo info) {
        if (info == null || info.mgInternal || getParentActivity() == null) {
            return "";
        }
        String key = UltProxyLabels.preferenceKey(
                info.settings.getType().name(), info.settings.getAddress(), info.settings.getPort());
        return getParentActivity().getSharedPreferences(UltProxyLabels.PREFERENCES, Context.MODE_PRIVATE)
                .getString(key, "");
    }

    private void showUltProxyActions(SharedConfig.ProxyInfo info) {
        if (info == null || info.mgInternal || getParentActivity() == null) {
            return;
        }
        new android.app.AlertDialog.Builder(getParentActivity())
                .setTitle("ULT Proxy Profiles")
                .setItems(new String[]{"Edit connection", "Name this profile", "Connection diagnostics"}, (dialog, selected) -> {
                    if (selected == 0) {
                        presentFragment(new ProxySettingsActivity(info));
                    } else if (selected == 1) {
                        showUltRenameProxy(info);
                    } else {
                        showUltProxyDiagnostics(info);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showUltRenameProxy(SharedConfig.ProxyInfo info) {
        final android.app.Activity activity = getParentActivity();
        if (info == null || info.mgInternal || activity == null) {
            return;
        }
        final String key = UltProxyLabels.preferenceKey(
                info.settings.getType().name(), info.settings.getAddress(), info.settings.getPort());
        final SharedPreferences preferences = activity.getSharedPreferences(
                UltProxyLabels.PREFERENCES, Context.MODE_PRIVATE);
        final android.widget.EditText input = new android.widget.EditText(activity);
        input.setSingleLine(true);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(64)});
        input.setHint("Local name, e.g. Home SOCKS5");
        input.setText(preferences.getString(key, ""));
        input.setSelection(input.length());
        new android.app.AlertDialog.Builder(activity)
                .setTitle("Name ULT proxy profile")
                .setMessage("Names stay on this device. Leave blank to remove a name; never include passwords or secrets.")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        String label = UltProxyLabels.normalizeLabel(input.getText().toString());
                        SharedPreferences.Editor editor = preferences.edit();
                        if (label.isEmpty()) {
                            editor.remove(key);
                        } else {
                            editor.putString(key, label);
                        }
                        editor.apply();
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                    } catch (IllegalArgumentException invalid) {
                        Toast.makeText(activity, invalid.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    private void showUltProxyDiagnostics(SharedConfig.ProxyInfo info) {
        final android.app.Activity activity = getParentActivity();
        if (info == null || info.mgInternal || activity == null) {
            return;
        }
        boolean selected = SharedConfig.currentProxy == info;
        // This describes the proxy UI switch; Tor has separate upstream controls.
        boolean enabled = useProxySettings && !SharedConfig.mg_useTor;
        boolean connected = currentConnectionState == ConnectionsManager.ConnectionStateConnected;
        boolean updating = currentConnectionState == ConnectionsManager.ConnectionStateUpdating;
        String nl = System.lineSeparator();
        StringBuilder details = new StringBuilder();
        details.append("Transport: ").append(info.settings.getType().name()).append(nl);
        details.append("Global Use Proxy switch: ").append(enabled ? "On" : "Off").append(nl);
        if (SharedConfig.mg_useTor) {
            details.append("Mercurygram Tor mode is active separately.").append(nl);
        }
        details.append("Selection/session: ").append(UltProxyDiagnostics.routeStatus(
                enabled, selected, connected, updating)).append(nl);
        details.append("Cached reachability: ").append(UltProxyDiagnostics.nativeProbeStatus(
                info.checking, info.availableCheckTime, info.available, info.ping)).append(nl);
        details.append(nl).append(UltProxyDiagnostics.dnsAuditWarning());
        new android.app.AlertDialog.Builder(activity)
                .setTitle("ULT Connection Diagnostics")
                .setMessage(details.toString())
                .setNeutralButton("Copy redacted status", (dialog, which) -> {
                    String snapshot = UltProxyDiagnostics.redactedAuditSnapshot(
                            info.settings.getType().name(), enabled, selected, connected, updating,
                            info.checking, info.availableCheckTime, info.available, info.ping);
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                            activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                                "ULT proxy status", snapshot));
                        Toast.makeText(activity, "Redacted status copied to clipboard",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showUltProxyPrivacyInfo() {
        if (getParentActivity() == null) {
            return;
        }
        new android.app.AlertDialog.Builder(getParentActivity())
                .setTitle("ULT Proxy Privacy")
                .setMessage("ULT uses the existing Telegram SOCKS5/MTProto proxy transports. "
                        + "A proxy can obscure your network IP from Telegram's servers, "
                        + "but the proxy operator may see your IP, connection times and other metadata. "
                        + "Telegram can still associate your phone number and account. "
                        + "A proxy is not a VPN or a guarantee of anonymity. "
                        + "Turning off Use Proxy can allow direct connections. "
                        + "Mercurygram's Tor mode has separate protections and controls.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

"""

def replace_once(source: str, anchor: str, replacement: str) -> str:
    count = source.count(anchor)
    if count != 1:
        raise ValueError(f"Expected exactly one upstream proxy anchor; found {count}: {anchor[:72]!r}")
    return source.replace(anchor, replacement, 1)

def integrate(root: Path) -> Path:
    target = root / TARGET
    source = target.read_text(encoding="utf-8")
    if "ULT_PROXY_PRIVACY_MENU_ID" in source:
        raise ValueError("ULT proxy integration already applied")
    source = replace_once(source, IMPORT_ANCHOR,
                          IMPORT_ANCHOR + "import com.yooshastan.ultmessenger.proxy.UltProxyLabels;\n"
                          + "import com.yooshastan.ultmessenger.proxy.UltProxyDiagnostics;\n")
    source = replace_once(source, CLASS_ANCHOR, CLASS_ANCHOR + FIELDS)
    source = replace_once(source, EDIT_ANCHOR,
                          "            checkImageView.setOnClickListener(v -> showUltProxyActions(currentInfo));\n")
    source = replace_once(source, DISPLAY_ANCHOR,
                          "                String endpoint = proxyInfo.settings.getType() == ProxySettings.Type.WEB\n"
                          '                        ? proxyInfo.settings.getAddress() + " (WEB)"\n'
                          '                        : proxyInfo.settings.getAddress() + ":" + proxyInfo.settings.getPort();\n'
                          "                textView.setText(UltProxyLabels.displayName(getUltProxyLabel(proxyInfo), endpoint));\n")
    source = replace_once(source, MENU_ANCHOR,
                          MENU_ANCHOR + "        actionBar.setTitle(\"ULT Proxy Manager\");\n"
                          + "        actionBar.createMenu().addItem(ULT_PROXY_PRIVACY_MENU_ID, R.drawable.msg_info)\n"
                          + '                .setContentDescription("ULT proxy privacy");\n')
    # Mercurygram makes this action bar adaptive/translucent, so color the
    # title and back/info icons against the visible page instead of its
    # nominal actionBarDefault accent background.
    source = replace_once(source, ADAPTIVE_ANCHOR,
                          ADAPTIVE_ANCHOR
                          + "        actionBar.setTitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));\n"
                          + "        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), false);\n")
    source = replace_once(source, THEME_ICON_ANCHOR,
                          THEME_ICON_ANCHOR.replace("Theme.key_actionBarDefaultIcon",
                                                    "Theme.key_windowBackgroundWhiteBlackText"))
    source = replace_once(source, THEME_TITLE_ANCHOR,
                          THEME_TITLE_ANCHOR.replace("Theme.key_actionBarDefaultTitle",
                                                    "Theme.key_windowBackgroundWhiteBlackText"))
    source = replace_once(source, CLICK_ANCHOR,
                          "                switch (id) {\n"
                          + "                    case ULT_PROXY_PRIVACY_MENU_ID:\n"
                          + "                        showUltProxyPrivacyInfo();\n"
                          + "                        break;\n"
                          + "                    case -1:\n")
    model_src = Path(__file__).resolve().parents[1] / MODEL
    model_dst = root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/proxy/UltProxyLabels.java"
    if not model_src.is_file():
        raise FileNotFoundError(model_src)
    # Validate all source anchors and prerequisites before writing anything.
    model_data = model_src.read_bytes()
    diagnostics_src = Path(__file__).resolve().parents[1] / DIAGNOSTICS_MODEL
    diagnostics_dst = root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/proxy/UltProxyDiagnostics.java"
    if not diagnostics_src.is_file():
        raise FileNotFoundError(diagnostics_src)
    diagnostics_data = diagnostics_src.read_bytes()
    strict_policy_src = Path(__file__).resolve().parents[1] / STRICT_POLICY_MODEL
    strict_policy_dst = root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/proxy/UltStrictProxyPolicy.java"
    if not strict_policy_src.is_file():
        raise FileNotFoundError(strict_policy_src)
    strict_policy_data = strict_policy_src.read_bytes()
    target.write_text(source, encoding="utf-8")
    model_dst.parent.mkdir(parents=True, exist_ok=True)
    model_dst.write_bytes(model_data)
    diagnostics_dst.write_bytes(diagnostics_data)
    strict_policy_dst.write_bytes(strict_policy_data)
    return target

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    args = parser.parse_args()
    print(f"Patched {integrate(args.source_root)}")
