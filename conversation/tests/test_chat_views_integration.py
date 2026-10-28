#!/usr/bin/env python3
"""Alpha 0.10a guarded local chat-view integration tests."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

SCRIPT = Path(__file__).resolve().parents[2] / "scripts/integrate-chat-views.py"
spec = importlib.util.spec_from_file_location("ult_chat_views", SCRIPT)
integration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(integration)

class ChatViewIntegrationTest(TestCase):
    def fixture(self, root):
        activity = root / integration.ACTIVITY
        adapter = root / integration.ADAPTER
        activity.parent.mkdir(parents=True, exist_ok=True)
        adapter.parent.mkdir(parents=True, exist_ok=True)
        activity.write_text("".join((
            integration.ACTIVITY_IMPORT,
            integration.ACTIVITY_METHOD,
            integration.ACTIVITY_MENU,
            integration.ACTIVITY_RETURN,
            integration.ACTIVITY_RESUME,
        )), encoding="utf-8")
        adapter.write_text("".join((
            integration.ADAPTER_IMPORT, integration.ADAPTER_CLASS,
            integration.ADAPTER_HINT, integration.ADAPTER_MOVE,
            '            fromIndex = dialogs.indexOf(source);\n',
            integration.ADAPTER_ARCHIVE, integration.ADAPTER_ARRAY,
        )), encoding="utf-8")
        return activity, adapter

    def test_views_are_local_and_scope_to_all_chats(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            a, b = self.fixture(root)
            integration.integrate(root)
            activity = a.read_text()
            adapter = b.read_text()
            self.assertIn("ULT: Chat Views", activity)
            self.assertIn("ULT: Downloads", activity)
            self.assertIn("new com.yooshastan.ultmessenger.settings.UltDownloadsActivity()", activity)
            self.assertIn("ultSwipeTarget.setOnTouchListener", activity)
            self.assertIn("stepUltCategoryMode(dx < 0 ? 1 : -1)", activity)
            self.assertIn("ultSwipeTarget.setVisibility(enableSwipePad ? View.VISIBLE : View.GONE)", activity)
            self.assertIn("setContentDescription", activity)
            self.assertIn("barFontKey(currentAccount)", activity)
            self.assertIn("Gesture handling is confined to a dedicated, opt-in target", activity)
            self.assertIn("ULT: Next Chat Tab", activity)
            self.assertIn("nextVisibleTab", activity)
            self.assertIn('"ULT View: " + UltChatView.LABELS[UltChatView.modeIndex(next)]', activity)
            self.assertNotIn('"ULT View:  + UltChatView.LABELS', activity)
            self.assertIn("ULT: Show category bar", activity)
            self.assertIn("installUltCategoryBar(context, contentView)", activity)
            self.assertIn("quickBarKey(currentAccount)", activity)
            self.assertIn("refreshUltCategoryBar()", activity)
            self.assertIn("setUltCategoryMode(choice)", activity)
            self.assertIn("initialDialogsType == DIALOGS_TYPE_DEFAULT", activity)
            self.assertNotIn("addDialogFilter", activity)
            self.assertIn("setSingleChoiceItems(UltChatView.LABELS", activity)
            self.assertNotIn("setMessage(", activity)
            self.assertIn("accountKey(currentAccount)", activity)
            self.assertIn("isUltChatViewActive()", adapter)
            self.assertIn("array = ultFilteredDialogs(array, messagesController)", adapter)
            self.assertIn("new ArrayList<>(dialogs.size())", adapter)
            self.assertIn("!isOnlySelect && communityId == 0", adapter)
            self.assertIn("isUltDialogUnread", adapter)
            self.assertIn("compareUnreadFirst", adapter)
            self.assertIn("isUltChatViewActive() && !shouldHideUltArchiveRow()", adapter)
            self.assertIn("dialogs.indexOf(source)", adapter)
            self.assertNotIn("deleteDialog", adapter + activity)
            self.assertNotIn("toggleDialogPin", adapter + activity)
            self.assertTrue((root / integration.DEST).exists())
            with self.assertRaises(ValueError):
                integration.integrate(root)

    def test_anchor_drift_aborts_without_edit(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            a, b = self.fixture(root)
            b.write_text(b.read_text().replace(integration.ADAPTER_MOVE, "changed"))
            old_a, old_b = a.read_bytes(), b.read_bytes()
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertEqual(old_a, a.read_bytes())
            self.assertEqual(old_b, b.read_bytes())
            self.assertFalse((root / integration.DEST).exists())

    def test_duplicate_menu_anchor_rejected(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            a, _ = self.fixture(root)
            a.write_text(a.read_text() + integration.ACTIVITY_MENU)
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertNotIn("showUltChatViewPicker()", a.read_text())

if __name__ == "__main__":
    main()
