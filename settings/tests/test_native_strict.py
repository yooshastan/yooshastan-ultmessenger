#!/usr/bin/env python3
"""Strict native-socket patch tests with synthetic pinned-source anchors."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

MODULE = Path(__file__).resolve().parents[2] / "scripts/integrate-native-strict.py"
spec = importlib.util.spec_from_file_location("ult_native_strict", MODULE)
integration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(integration)

ANCHORS = {
    integration.JAVA: integration.JAVA_IMPORT + integration.JAVA_INIT + integration.JAVA_STARTUP_DONE + integration.JAVA_SETTER + integration.JAVA_NATIVE,
    integration.HEADER: integration.HEADER_METHOD + integration.HEADER_FIELD,
    integration.CPP: integration.CPP_METHOD + integration.CPP_PROXY_FINISH,
    integration.SOCKET: integration.SOCKET_ATTACH + integration.SOCKET_INTERNAL,
    integration.JNI: integration.JNI_METHOD + integration.JNI_TABLE,
    integration.PROXY_UI: integration.UI_STATUS + '        listView.setOnItemClickListener((view, position) -> {\n            if (position == useProxyRow) {\n',
}


def setup_sources(root):
    for path, content in ANCHORS.items():
        target = root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")


class NativeStrictTest(TestCase):
    def test_native_guard_wired_through_jni_and_startup(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            setup_sources(root)
            paths = integration.integrate(root)
            self.assertEqual(len(paths), 6)
            self.assertIn("native_setStrictProxyMode(currentAccount", (root / integration.JAVA).read_text())
            self.assertIn("checkConnection();\n        if (preferences.getBoolean", (root / integration.JAVA).read_text())
            self.assertIn("const bool awaitingProxy = enabled && proxyAddress.empty();", (root / integration.CPP).read_text())
            self.assertIn("if (strictProxyMode.load() && !proxyAddress.empty()", (root / integration.CPP).read_text())
            self.assertIn("ult_strict_native_proxy", (root / integration.JAVA).read_text())
            self.assertIn("setStrictProxyMode(bool enabled)", (root / integration.HEADER).read_text())
            self.assertIn("suspendConnections(true)", (root / integration.CPP).read_text())
            self.assertIn("primary->createGenericConnection()->connect()", (root / integration.CPP).read_text())
            self.assertIn("strictProxyMode.load()", (root / integration.CPP).read_text())
            self.assertIn("bool isStrictProxyModeEnabled() const", (root / integration.HEADER).read_text())
            self.assertIn("std::atomic<bool> strictProxyMode", (root / integration.HEADER).read_text())
            self.assertIn("canEnableUltStrictNativeProxy()", (root / integration.JAVA).read_text())
            self.assertIn("UltStrictProxyPolicy.canEnable", (root / integration.JAVA).read_text())
            self.assertIn("Disable native strict mode first", (root / integration.PROXY_UI).read_text())
            native_socket = (root / integration.SOCKET).read_text()
            self.assertIn("ultManager.strictProxyMode && ultManager.proxyAddress.empty()", native_socket)
            self.assertIn("overrideProxyAddress.empty()", native_socket)
            self.assertIn("closeSocket(1, -1);", native_socket)
            self.assertIn("blocked late direct tgnet socket", native_socket)
            self.assertIn('"native_setStrictProxyMode", "(IZ)V"', (root / integration.JNI).read_text())
            self.assertIn('"native_isStrictProxyModeEnabled", "(I)Z"', (root / integration.JNI).read_text())
            self.assertIn("routing NOT leak-verified", (root / integration.PROXY_UI).read_text())
            with self.assertRaises(ValueError):
                integration.integrate(root)

    def test_drift_refuses_all_writes(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            setup_sources(root)
            target = root / integration.PROXY_UI
            target.write_text("unknown new UI", encoding="utf-8")
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertEqual((root / integration.JAVA).read_text(), ANCHORS[integration.JAVA])
            self.assertEqual(target.read_text(), "unknown new UI")


if __name__ == "__main__":
    main()
