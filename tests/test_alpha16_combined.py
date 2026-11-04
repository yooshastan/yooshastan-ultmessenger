"""Alpha 0.16 opt-in UI/security source contracts.
Fast checks only; the pinned Android integration and device QA are separate gates.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


class Alpha16Contracts(unittest.TestCase):
    def test_chat_sort_is_local_and_stable(self):
        model = read("conversation/src/com/yooshastan/ultmessenger/conversation/UltChatView.java")
        patch = read("scripts/integrate-chat-views.py")
        self.assertIn("MENTIONS_FIRST", model)
        self.assertIn("compareMentionsFirst", model)
        self.assertIn("a.unread_mentions_count > 0", patch)
        self.assertIn("displayed.sort(", patch)
        self.assertNotIn("messagesController.set", patch)

    def test_extra_fonts_preserve_old_keys(self):
        model = read("conversation/src/com/yooshastan/ultmessenger/conversation/UltChatView.java")
        for name in ('FONT_TINY', 'FONT_SMALL', 'FONT_NORMAL', 'FONT_MEDIUM',
                     'FONT_LARGE', 'FONT_XL'):
            self.assertIn(name, model)

    def test_metadata_copy_is_opt_in(self):
        tools = read("chat/src/com/yooshastan/ultmessenger/chat/UltChatTools.java")
        patch = read("scripts/integrate-message-tools.py")
        self.assertIn('messageMetadataJson', tools)
        self.assertIn('ULT: Copy reference', patch)
        self.assertIn('UltChatTools.messageReference(', patch)
        self.assertNotIn('ULT: Copy metadata (JSON)', patch)
        self.assertIn('!selectedObject.isSponsored()', patch)
        self.assertIn('currentEncryptedChat == null', patch)
        self.assertNotIn('sendMessage(', tools)

    def test_identity_reveal_not_persisted(self):
        source = read("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        part = source.split("private void showLocalIdentityCover(Context context, boolean revealOnce)", 1)[1].split(
            "private void showNetworkEgressEvidence", 1
        )[0]
        self.assertIn('showLocalIdentityCover(context, !revealOnce)', part)
        self.assertIn('getBoolean(UltIdentityCover.key(currentAccount), true)', part)
        self.assertNotIn('putBoolean(UltIdentityCover.key(currentAccount), false)', part)

    def test_save_location_is_saf_hint_not_auto_export(self):
        source = read("settings/src/com/yooshastan/ultmessenger/settings/UltDownloadsActivity.java")
        for required in ('Intent.ACTION_CREATE_DOCUMENT',
                         'DocumentsContract.EXTRA_INITIAL_URI',
                         'lastSaveKey(currentAccount)', 'lastSaveKey(saveAccount)',
                         '"Forget save location"',
                         'isExportAllowed(message, currentAccount)'):
            self.assertIn(required, source)
        self.assertNotIn('ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION', source)
        self.assertIn('if (saveAccount >= 0)', source)


if __name__ == "__main__":
    unittest.main()
