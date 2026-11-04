#!/usr/bin/env python3
"""Guarded experimental strict native MTProto socket mode.

This guards the pinned tgnet ConnectionSocket path. It is not a system-wide
firewall, encrypted DNS implementation, or a proven privacy guarantee.
Never enable it by default or label it leak-proof before device outage tests.
"""
import argparse
from pathlib import Path

JAVA = Path("TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java")
HEADER = Path("TMessagesProj/jni/tgnet/ConnectionsManager.h")
CPP = Path("TMessagesProj/jni/tgnet/ConnectionsManager.cpp")
SOCKET = Path("TMessagesProj/jni/tgnet/ConnectionSocket.cpp")
JNI = Path("TMessagesProj/jni/TgNetWrapper.cpp")
PROXY_UI = Path("TMessagesProj/src/main/java/org/telegram/ui/ProxyListActivity.java")

JAVA_INIT = '        final ProxySettings proxySettings = ProxySettings.fromSharedPreferences(preferences);\n'
JAVA_STARTUP_DONE = '        checkConnection();\n    }\n'
JAVA_SETTER = '    public static void setProxySettings(boolean enabled, ProxySettings settings) {\n'
JAVA_NATIVE = '    public static native void native_setProxySettings(int currentAccount, String address, int port, String username, String password, String secret);\n'
JAVA_IMPORT = 'import org.telegram.utils.proxy.ProxySettings;\n'
HEADER_METHOD = '    void setProxySettings(std::string address, uint16_t port, std::string username, std::string password, std::string secret);\n'
HEADER_FIELD = '    std::string proxyAddress = "";\n'
CPP_METHOD = 'void ConnectionsManager::setProxySettings(std::string address, uint16_t port, std::string username, std::string password, std::string secret) {\n'
CPP_PROXY_FINISH = ('            processRequestQueue(0, 0);\n'
                    '        }\n'
                    '    });\n'
                    '}\n\n'
                    'void ConnectionsManager::setLangCode(std::string langCode) {\n')
SOCKET_ATTACH = '    ConnectionsManager::getInstance(instanceNum).attachConnection(this);\n'
SOCKET_INTERNAL = 'void ConnectionSocket::openConnectionInternal(bool ipv6) {\n'
JNI_METHOD = 'void setProxySettings(JNIEnv *env, jclass c, jint instanceNum, jstring address, jint port, jstring username, jstring password, jstring secret) {\n'
JNI_TABLE = '        {"native_setProxySettings", "(ILjava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void *) setProxySettings},\n'
UI_STATUS = '        details.append("Native strict fail-closed: Not implemented").append(nl);\n'

JAVA_METHODS = """    private static final String ULT_STRICT_NATIVE_PROXY_KEY = "ult_strict_native_proxy";

    public static boolean isUltStrictNativeProxyEnabled() {
        return ApplicationLoader.applicationContext.getSharedPreferences(
                "mainconfig", Activity.MODE_PRIVATE)
                .getBoolean(ULT_STRICT_NATIVE_PROXY_KEY, false);
    }

    /** Configuration guard; does not prove that a selected proxy is reachable. */
    public static boolean canEnableUltStrictNativeProxy() {
        final SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        final ProxySettings selected = ProxySettings.fromSharedPreferences(preferences);
        final boolean validSelection = SharedConfig.currentProxy != null
                && SharedConfig.currentProxy.settings != null
                && SharedConfig.currentProxy.settings.isValid() && selected.isValid()
                && selected.getType() != ProxySettings.Type.WEB
                && (selected.getType() != ProxySettings.Type.MTPROTO
                    || !selected.getSecret().isEmpty());
        return UltStrictProxyPolicy.canEnable(
                preferences.getBoolean("proxy_enabled", false), validSelection, SharedConfig.mg_useTor);
    }

    /** False means enabling was refused; disabling is always allowed. */
    public static boolean setUltStrictNativeProxyEnabled(boolean enabled) {
        if (enabled && !canEnableUltStrictNativeProxy()) {
            return false;
        }
        ApplicationLoader.applicationContext.getSharedPreferences(
                "mainconfig", Activity.MODE_PRIVATE).edit()
                .putBoolean(ULT_STRICT_NATIVE_PROXY_KEY, enabled).apply();
        // Repeat on all accounts even if preference was already OFF; this
        // repairs a stale native guard without weakening an ON preference.
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            native_setStrictProxyMode(account, enabled);
        }
        return true;
    }

"""

NATIVE_METHOD = """void ConnectionsManager::setStrictProxyMode(bool enabled) {
    scheduleTask([&, enabled] {
        if (strictProxyMode.load() == enabled) {
            // A repeated ON request after startup is a safe reconnect trigger
            // only after the selected native proxy has been configured.
            // A repeated OFF request retains the recovery behavior from 0.10h.2.
            if (networkAvailable && !networkPaused && (!enabled || !proxyAddress.empty())) {
                Datacenter *active = getDatacenterWithId(DEFAULT_DATACENTER_ID);
                if (active != nullptr) active->createGenericConnection()->connect();
                processRequestQueue(0, 0);
            }
            return;
        }
        strictProxyMode.store(enabled);
        for (auto & datacenter : datacenters) {
            datacenter.second->suspendConnections(true);
        }
        Datacenter *primary = getDatacenterWithId(DEFAULT_DATACENTER_ID);
        if (primary != nullptr && primary->isHandshakingAny()) {
            primary->beginHandshake(HandshakeTypeCurrent, true);
        }
        // A saved strict ON switch can be restored before Java has applied the
        // selected native proxy. Do not start a blocked/direct attempt in that gap.
        // The native proxy setter and the post-init handshake retry resume it.
        const bool awaitingProxy = enabled && proxyAddress.empty();
        if (!awaitingProxy && networkAvailable && !networkPaused && primary != nullptr) {
            primary->createGenericConnection()->connect();
        }
        if (!awaitingProxy) {
            processRequestQueue(0, 0);
        }
    });
}

bool ConnectionsManager::isStrictProxyModeEnabled() const {
    return strictProxyMode.load();
}

"""

SOCKET_GUARD = """    // ULT experimental native strict mode: never open a direct tgnet
    // destination socket when there is no configured native proxy.
    // Explicit candidate-proxy checks may still connect to their proxy target.
    ConnectionsManager &ultManager = ConnectionsManager::getInstance(instanceNum);
    if (ultManager.strictProxyMode && ultManager.proxyAddress.empty()
            && overrideProxyAddress.empty()) {
        if (LOGS_ENABLED) DEBUG_D("strict proxy mode blocked direct tgnet socket");
        closeSocket(1, -1);
        return;
    }
"""

SOCKET_INTERNAL_GUARD = """    // Recheck at socket creation: asynchronous proxy host resolution may
    // finish after strict mode or the selected proxy has changed.
    ConnectionsManager &ultManager = ConnectionsManager::getInstance(instanceNum);
    if (ultManager.strictProxyMode && ultManager.proxyAddress.empty()
            && overrideProxyAddress.empty()) {
        if (LOGS_ENABLED) DEBUG_D("strict proxy mode blocked late direct tgnet socket");
        closeSocket(1, -1);
        return;
    }
"""


def once(source: str, anchor: str, value: str) -> str:
    count = source.count(anchor)
    if count != 1:
        raise ValueError(f"Pinned network anchor changed: {count} copies of {anchor[:88]!r}")
    return source.replace(anchor, value, 1)


def integrate(root: Path) -> list[Path]:
    paths = [root / p for p in (JAVA, HEADER, CPP, SOCKET, JNI, PROXY_UI)]
    sources = [p.read_text(encoding="utf-8") for p in paths]
    if any("native_setStrictProxyMode" in x or "strictProxyMode" in x for x in sources):
        raise ValueError("ULT native strict integration already applied")
    java, header, cpp, socket, jni, proxy_ui = sources
    java = once(java, JAVA_IMPORT, JAVA_IMPORT + 'import com.yooshastan.ultmessenger.proxy.UltStrictProxyPolicy;\n')
    java = once(java, JAVA_INIT, JAVA_INIT +
                '        native_setStrictProxyMode(currentAccount, preferences.getBoolean("ult_strict_native_proxy", false));\n')
    # Keep the strict guard installed before the first native proxy operation,
    # then replay ON after proxy and network initialisation for cold-start recovery.
    java = once(java, JAVA_STARTUP_DONE,
                '        checkConnection();\n'
                '        if (preferences.getBoolean("ult_strict_native_proxy", false)) {\n'
                '            native_setStrictProxyMode(currentAccount, true);\n'
                '        }\n'
                '    }\n')
    java = once(java, JAVA_SETTER, JAVA_METHODS + JAVA_SETTER)
    java = once(java, JAVA_NATIVE,
                JAVA_NATIVE + '    public static native void native_setStrictProxyMode(int currentAccount, boolean enabled);\n'
                             + '    public static native boolean native_isStrictProxyModeEnabled(int currentAccount);\n')
    header = once(header, HEADER_METHOD, HEADER_METHOD + '    void setStrictProxyMode(bool enabled);\n'
                  + '    bool isStrictProxyModeEnabled() const;\n')
    header = once(header, HEADER_FIELD, HEADER_FIELD + '    std::atomic<bool> strictProxyMode{false};\n')
    cpp = once(cpp, CPP_METHOD, NATIVE_METHOD + CPP_METHOD)
    cpp = once(cpp, CPP_PROXY_FINISH,
               '            // Startup strict mode can precede proxy restoration.\n'
               '            // Once a valid address is installed, explicitly wake Telegram.\n'
               '            if (strictProxyMode.load() && !proxyAddress.empty()\n'
               '                    && networkAvailable && !networkPaused && datacenter != nullptr) {\n'
               '                datacenter->createGenericConnection()->connect();\n'
               '            }\n'
               + CPP_PROXY_FINISH)
    socket = once(socket, SOCKET_ATTACH, SOCKET_ATTACH + SOCKET_GUARD)
    socket = once(socket, SOCKET_INTERNAL, SOCKET_INTERNAL + SOCKET_INTERNAL_GUARD)
    jni = once(jni, JNI_METHOD,
               'void setStrictProxyMode(JNIEnv *env, jclass c, jint instanceNum, jboolean enabled) {\n'
               '    ConnectionsManager::getInstance(instanceNum).setStrictProxyMode(enabled == JNI_TRUE);\n'
               '}\n\n'
               'jboolean isStrictProxyModeEnabled(JNIEnv *env, jclass c, jint instanceNum) {\n'
               '    return ConnectionsManager::getInstance(instanceNum).isStrictProxyModeEnabled() ? JNI_TRUE : JNI_FALSE;\n'
               '}\n\n' + JNI_METHOD)
    jni = once(jni, JNI_TABLE,
               JNI_TABLE + '        {"native_setStrictProxyMode", "(IZ)V", (void *) setStrictProxyMode},\n'
               + '        {"native_isStrictProxyModeEnabled", "(I)Z", (void *) isStrictProxyModeEnabled},\n')
    proxy_ui = once(proxy_ui, '        listView.setOnItemClickListener((view, position) -> {\n            if (position == useProxyRow) {\n',
                    '        listView.setOnItemClickListener((view, position) -> {\n            if (position == useProxyRow) {\n'
                    '                if (useProxySettings && ConnectionsManager.isUltStrictNativeProxyEnabled()) {\n'
                    '                    new android.app.AlertDialog.Builder(getParentActivity())\n'
                    '                            .setTitle("Disable native strict mode first")\n'
                    '                            .setMessage("Strict mode is still ON. Turn it off in Ultgram Settings before disabling the selected proxy. No direct fallback was enabled.")\n'
                    '                            .setPositiveButton("OK", null).show();\n'
                    '                    return;\n'
                    '                }\n')
    proxy_ui = once(proxy_ui, UI_STATUS,
                    '        details.append("Experimental native socket guard requested: ")\n'
                    '                .append(ConnectionsManager.isUltStrictNativeProxyEnabled() ? "On" : "Off")\n'
                    '                .append(nl);\n'
                    '        details.append("Native socket guard observed (this account): ")\n'
                    '                .append(ConnectionsManager.native_isStrictProxyModeEnabled(currentAccount) ? "On" : "Off")\n'
                    '                .append(" (routing NOT leak-verified)").append(nl);\n')
    # All anchors are checked before any file is written.
    for target, updated in zip(paths, (java, header, cpp, socket, jni, proxy_ui)):
        target.write_text(updated, encoding="utf-8")
    return paths


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    args = parser.parse_args()
    print("Patched native strict proxy paths:", *integrate(args.source_root))
