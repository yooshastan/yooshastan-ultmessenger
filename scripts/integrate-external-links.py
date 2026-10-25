#!/usr/bin/env python3
"""Guarded Alpha 0.10e opt-in external-link confirmation integration.

This is deliberately NOT a VPN, DNS firewall, automatic-media blocker or
native MTProto fail-closed implementation. Only Browser.openUrl dispatch is
intercepted, before the upstream web/Custom Tabs dispatch runs.
"""
import argparse
from pathlib import Path

BROWSER = Path("TMessagesProj/src/main/java/org/telegram/messenger/browser/Browser.java")
PROXY_UI = Path("TMessagesProj/src/main/java/org/telegram/ui/ProxyListActivity.java")
ANDROID_PACKAGE = Path("TMessagesProj/src/main/java/com/yooshastan/ultmessenger/proxy")
MODELS = (
    "UltExternalLinkRules.java",
    "UltExternalLinkPolicy.java",
)

BROWSER_IMPORT_ANCHOR = "import org.telegram.messenger.AccountInstance;\n"
BROWSER_METHOD = (
    "    public static void openUrl(final Context context, Uri uri, boolean _allowCustom, "
    "boolean tryTelegraph, boolean forceNotInternalForApps, Progress inCaseLoading, "
    "String browser, boolean allowIntent, boolean allowInAppBrowser, boolean forceRequest) {\n"
)
PROXY_IMPORT_ANCHOR = "import com.yooshastan.ultmessenger.proxy.UltProxyLabels;\n"
PROXY_PRIVACY_METHOD_ANCHOR = "    private void showUltProxyPrivacyInfo() {\n"
PROXY_PRIVACY_TITLE_ANCHOR = '                .setTitle("ULT Proxy Privacy")\n'
PROXY_STATUS_ANCHOR = "        details.append(nl).append(UltProxyDiagnostics.dnsAuditWarning());\n"

BROWSER_WRAPPER = BROWSER_METHOD + """        // Guard only explicit HTTP(S) link dispatch. Native MTProto and media
        // request paths are outside this scope. No approval is persisted.
        if (context != null && uri != null && UltExternalLinkPolicy.shouldConfirm(
                context, uri, isInternalUri(uri, new boolean[]{false}))) {
            // No user-visible consent is possible without an Activity, so do
            // not silently open the external URL from a background context.
            if (!(context instanceof Activity)) {
                return;
            }
            final Uri requestedUrl = uri;
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Open external website?")
                    .setMessage("This HTTP(S) link opens outside the Telegram proxy. "
                            + "The destination may learn your network IP and other metadata. "
                            + "Opening once does not change your proxy settings.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Open once", (dialog, which) ->
                            openUltApprovedUrl(context, requestedUrl, _allowCustom,
                                    tryTelegraph, forceNotInternalForApps, inCaseLoading,
                                    browser, allowIntent, allowInAppBrowser, forceRequest))
                    .show();
            return;
        }
        openUltApprovedUrl(context, uri, _allowCustom, tryTelegraph,
                forceNotInternalForApps, inCaseLoading, browser, allowIntent,
                allowInAppBrowser, forceRequest);
    }

    // Original pinned upstream dispatcher; open-once approval bypasses only the
    // one selected request and leaves the guard enabled for future link taps.
    private static void openUltApprovedUrl(final Context context, Uri uri,
            boolean _allowCustom, boolean tryTelegraph, boolean forceNotInternalForApps,
            Progress inCaseLoading, String browser, boolean allowIntent,
            boolean allowInAppBrowser, boolean forceRequest) {
"""

PROXY_SETTINGS_METHOD = """    private void showUltExternalLinkSettings() {
        final android.app.Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        final boolean enabled = UltExternalLinkPolicy.isEnabled(activity);
        new android.app.AlertDialog.Builder(activity)
                .setTitle("External link confirmations: " + (enabled ? "On" : "Off"))
                .setMessage("When on, URLs opened through Ultgram's regular Browser.openUrl "
                        + "flow ask permission before opening external HTTP(S) websites. "
                        + "Telegram internal links are exempt. This does NOT block embedded "
                        + "media, previews, automatic web requests, DNS or direct Telegram "
                        + "connections, and it is NOT a VPN or a fail-closed firewall.")
                .setNeutralButton(enabled ? "Disable" : "Enable", (dialog, which) -> {
                    UltExternalLinkPolicy.setEnabled(activity, !enabled);
                    Toast.makeText(activity, "External link confirmations "
                            + (!enabled ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("Done", null)
                .show();
    }

"""


def replace_once(source: str, anchor: str, replacement: str) -> str:
    count = source.count(anchor)
    if count != 1:
        raise ValueError(f"Expected one pinned upstream anchor, found {count}: {anchor[:90]!r}")
    return source.replace(anchor, replacement, 1)


def integrate(root: Path) -> tuple[Path, Path]:
    browser_path = root / BROWSER
    proxy_path = root / PROXY_UI
    browser = browser_path.read_text(encoding="utf-8")
    proxy = proxy_path.read_text(encoding="utf-8")
    if "openUltApprovedUrl" in browser or "showUltExternalLinkSettings" in proxy:
        raise ValueError("ULT external-link guard integration already applied")

    browser = replace_once(browser, BROWSER_IMPORT_ANCHOR,
                           BROWSER_IMPORT_ANCHOR
                           + "import com.yooshastan.ultmessenger.proxy.UltExternalLinkPolicy;\n")
    browser = replace_once(browser, BROWSER_METHOD, BROWSER_WRAPPER)
    proxy = replace_once(proxy, PROXY_IMPORT_ANCHOR,
                         PROXY_IMPORT_ANCHOR
                         + "import com.yooshastan.ultmessenger.proxy.UltExternalLinkPolicy;\n")
    proxy = replace_once(proxy, PROXY_PRIVACY_METHOD_ANCHOR,
                         PROXY_SETTINGS_METHOD + PROXY_PRIVACY_METHOD_ANCHOR)
    proxy = replace_once(proxy, PROXY_PRIVACY_TITLE_ANCHOR,
                         PROXY_PRIVACY_TITLE_ANCHOR
                         + '                .setNeutralButton("Link guard settings", '
                         + '(dialog, which) -> showUltExternalLinkSettings())\n')
    proxy = replace_once(proxy, PROXY_STATUS_ANCHOR,
                         '        details.append("External link confirmations: ")\n'
                         '                .append(UltExternalLinkPolicy.isEnabled(activity) '
                         '? "On" : "Off").append(" (link taps only)").append(nl);\n'
                         '        details.append("Native strict fail-closed: Not implemented")'
                         '.append(nl);\n'
                         + PROXY_STATUS_ANCHOR)

    model_root = Path(__file__).resolve().parents[1] / "proxy/src/com/yooshastan/ultmessenger/proxy"
    model_contents = {}
    for name in MODELS:
        # Reject missing model inputs before modifying either upstream file.
        model_contents[name] = (model_root / name).read_bytes()
    browser_path.write_text(browser, encoding="utf-8")
    proxy_path.write_text(proxy, encoding="utf-8")
    dest = root / ANDROID_PACKAGE
    dest.mkdir(parents=True, exist_ok=True)
    for name, content in model_contents.items():
        (dest / name).write_bytes(content)
    return browser_path, proxy_path


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    args = parser.parse_args()
    print("Patched external link dispatch and ULT proxy privacy UI:", *integrate(args.source_root))
