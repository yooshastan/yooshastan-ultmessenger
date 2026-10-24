#!/usr/bin/env python3
"""Guarded Alpha 0.5 ULT chat-tools menu integration for pinned Mercurygram.

Read-only ID/link actions; no message database or transport modifications.
"""
import argparse
from pathlib import Path

TARGET = Path("TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java")
MODEL = Path("chat/src/com/yooshastan/ultmessenger/chat/UltChatTools.java")
IMPORT_ANCHOR = "import org.telegram.ui.ActionBar.ActionBar;\n"
CLASS_ANCHOR = "public class ChatActivity extends BaseFragment implements\n"
MENU_ANCHOR = (
    "            translateItem = headerItem.lazilyAddSubItem(translate, R.drawable.msg_translate, "
    "LocaleController.getString(R.string.TranslateMessage));\n"
)
CLICK_ANCHOR = "                } else if (id == view_as_topics) {\n"

FIELDS = """
    private static final int ULT_CHAT_TOOLS_MENU_ID = 0x554c55;

    private void showUltChatTools() {
        final android.app.Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        final ArrayList<String> labels = new ArrayList<>();
        final ArrayList<String> values = new ArrayList<>();
        labels.add("Copy dialog ID: " + dialog_id);
        values.add(UltChatTools.numericId(dialog_id));

        if (currentChat != null) {
            labels.add("Copy chat ID: " + currentChat.id);
            values.add(UltChatTools.numericId(currentChat.id));
        } else if (currentUser != null) {
            labels.add("Copy user ID: " + currentUser.id);
            values.add(UltChatTools.numericId(currentUser.id));
        }
        long topicId = getTopicId();
        if (UltChatTools.hasTopic(topicId)) {
            labels.add("Copy topic ID: " + topicId);
            values.add(UltChatTools.numericId(topicId));
        }
        if (currentEncryptedChat == null) {
            String publicName = currentChat != null ? currentChat.username
                    : (currentUser != null ? currentUser.username : null);
            String publicLink = UltChatTools.publicLink(publicName);
            if (publicLink != null) {
                labels.add("Copy public t.me link");
                values.add(publicLink);
            }
        }
        new android.app.AlertDialog.Builder(activity)
                .setTitle("ULT Chat Tools")
                .setItems(labels.toArray(new String[0]), (dialog, selection) -> {
                    AndroidUtilities.addToClipboard(values.get(selection));
                    android.widget.Toast.makeText(activity, "Copied to clipboard",
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

"""

def replace_once(source: str, anchor: str, replacement: str) -> str:
    count = source.count(anchor)
    if count != 1:
        raise ValueError(f"Expected one pinned ChatActivity anchor, found {count}: {anchor[:72]!r}")
    return source.replace(anchor, replacement, 1)

def integrate(root: Path) -> Path:
    target = root / TARGET
    source = target.read_text(encoding="utf-8")
    if "ULT_CHAT_TOOLS_MENU_ID" in source:
        raise ValueError("ULT Chat Tools integration already applied")
    source = replace_once(source, IMPORT_ANCHOR,
                          IMPORT_ANCHOR + "import com.yooshastan.ultmessenger.chat.UltChatTools;\n")
    source = replace_once(source, CLASS_ANCHOR, CLASS_ANCHOR + FIELDS)
    source = replace_once(source, MENU_ANCHOR,
                          MENU_ANCHOR + '            headerItem.lazilyAddSubItem(ULT_CHAT_TOOLS_MENU_ID, R.drawable.msg_info, "ULT Chat Tools");\n')
    source = replace_once(source, CLICK_ANCHOR,
                          "                } else if (id == ULT_CHAT_TOOLS_MENU_ID) {\n"
                          "                    showUltChatTools();\n" + CLICK_ANCHOR)

    model_src = Path(__file__).resolve().parents[1] / MODEL
    if not model_src.is_file():
        raise FileNotFoundError(model_src)
    model_data = model_src.read_bytes()
    target.write_text(source, encoding="utf-8")
    model_dst = root / "TMessagesProj/src/main/java/com/yooshastan/ultmessenger/chat/UltChatTools.java"
    model_dst.parent.mkdir(parents=True, exist_ok=True)
    model_dst.write_bytes(model_data)
    return target

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    args = parser.parse_args()
    print(f"Patched {integrate(args.source_root)}")
