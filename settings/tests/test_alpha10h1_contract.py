#!/usr/bin/env python3
"""Alpha 0.10h.1: opt-in Ultgram tools, no new network authority."""
from pathlib import Path
from unittest import TestCase

ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java"
REPORT = ROOT / "settings/src/com/yooshastan/ultmessenger/settings/UltNetworkTestReport.java"
CHAT = ROOT / "conversation/src/com/yooshastan/ultmessenger/conversation/UltChatView.java"
MSG = ROOT / "scripts/integrate-message-tools.py"


class Alpha10hToolsContract(TestCase):
    def test_manual_report_is_not_a_vpn_or_security_proof(self):
        ui = UI.read_text()
        report = REPORT.read_text()
        self.assertIn("UltAndroidDnsInspector.inspectVpn(context)", ui)
        self.assertIn("showNetworkTestReport", ui)
        self.assertIn("USER-REPORTED", report)
        self.assertIn("route NOT verified", report)
        self.assertIn("NO PER-APP ROUTE", report)
        self.assertNotIn("HttpURLConnection", report)
        self.assertNotIn("Socket(", report)
        self.assertNotIn("getConnectionOwnerUid", ui)

    def test_presentation_and_clipboard_are_explicit_opt_in(self):
        ui = UI.read_text()
        chat = CHAT.read_text()
        msg = MSG.read_text()
        self.assertIn("compact_ult_settings", ui)
        self.assertIn("AndroidUtilities.dp(compactRows ? 9 : 14)", ui)
        self.assertIn("nextQuickTab", chat)
        self.assertIn("People / private chats", chat)
        self.assertIn("ULT: Copy message time", msg)
        self.assertIn("ULT: Copy document details", msg)
        self.assertIn("if (selectedObject.getDocument() != null)", msg)
        self.assertNotIn("sendMessage(", msg)
        self.assertNotIn("downloadFile(", msg)
