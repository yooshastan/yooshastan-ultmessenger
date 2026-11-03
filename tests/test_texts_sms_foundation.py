"""Texts foundation must not masquerade as an enabled SMS/RCS inbox."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

class TextsFoundationContracts(unittest.TestCase):
    def test_no_sensitive_platform_access_in_foundation(self):
        policy = read("texts/src/com/yooshastan/ultmessenger/texts/UltTextsAccessPolicy.java")
        index = read("texts/src/com/yooshastan/ultmessenger/texts/UltSmsThreadIndex.java")
        combined = policy + index
        for forbidden in ("READ_SMS", "RECEIVE_SMS", "SEND_SMS",
                          "SmsManager", "Telephony.Sms.CONTENT_URI",
                          "ContentResolver", "PermissionChecker", "org.telegram",
                          "RcsUceAdapter", "RcsMessageStore"):
            self.assertNotIn(forbidden, combined)
        self.assertIn("DEFAULT_SMS_ROLE_REQUIRED", policy)
        self.assertIn("SMS_IMPLEMENTATION_INCOMPLETE", policy)
        self.assertIn("return false;", policy.split("mayReadRcsProvider()", 1)[1])
        self.assertIn('"Texts (SMS/MMS)"', policy)

    def test_no_unfinished_folder_in_public_chat_categories(self):
        chat = read("conversation/src/com/yooshastan/ultmessenger/conversation/UltChatView.java")
        settings = read("settings/src/com/yooshastan/ultmessenger/settings/UltSettingsActivity.java")
        self.assertNotIn('"RCS"', chat)
        self.assertNotIn('"SMS"', chat)
        self.assertNotIn("new UltTextsActivity()", settings)
        self.assertNotIn("ROLE_SMS", settings)

    def test_no_manifest_or_build_activation_without_real_default_handler(self):
        integration = read("scripts/integrate-ult-settings.py")
        beta = read(".github/workflows/android-beta.yml")
        self.assertNotIn("UltTextsAccessPolicy.java", integration)
        self.assertNotIn("UltSmsThreadIndex.java", integration)
        self.assertNotIn("integrate-sms", beta)
        self.assertIn("integrate-native-strict.py", beta)
        self.assertIn("integrate-translation.py", beta)

    def test_contracts_state_reliable_feature_scope(self):
        doc = read("docs/TEXTS_SMS_RCS_PLAN.md")
        for phrase in ("SMS_DELIVER", "WAP_PUSH_DELIVER", "RESPOND_VIA_MESSAGE",
                       "RoleManager.ROLE_SMS", "multi-SIM", "Android 16",
                       "RCS", "provider data", "No SMS/MMS text"):
            self.assertIn(phrase, doc)

if __name__ == "__main__":
    unittest.main()
