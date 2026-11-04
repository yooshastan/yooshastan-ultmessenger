#!/usr/bin/env python3
"""Guarded Alpha 0.6 read-only message-context actions for pinned Mercurygram.

Runs after integrate-chat-tools.py; does not mutate or transmit messages.
"""
import argparse
from pathlib import Path

TARGET = Path("TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java")
CONSTANT_ANCHOR = "    public final static int OPTION_COPY = 3;\n"
MENU_ANCHOR = "        if (showWelcomeMessageRevertOption(primaryMessage)) {\n"
HANDLER_ANCHOR = "            case OPTION_COPY: {\n"

CONSTANTS = """    // Two focused actions instead of a long row of near-duplicate copy items.
    public final static int OPTION_ULT_COPY_MESSAGE_REFERENCE = 0x554c5d;
    public final static int OPTION_ULT_COPY_FILE_SUMMARY = 0x554c5c;
"""
MENU = """        // One readable reference for IDs, UTC and eligible public permalink.
        // Telegram's own Message Details and Copy actions remain above.
        if (selectedObject != null && selectedObject.messageOwner != null
                && selectedObject.getId() > 0 && !selectedObject.isSponsored()) {
            items.add("ULT: Copy reference");
            options.add(OPTION_ULT_COPY_MESSAGE_REFERENCE);
            icons.add(R.drawable.msg_copy);
            if (selectedObject.getDocument() != null
                    && !selectedObject.messageOwner.noforwards) {
                items.add("ULT: Copy file info");
                options.add(OPTION_ULT_COPY_FILE_SUMMARY);
                icons.add(R.drawable.msg_copy);
            }
        }

"""
HANDLERS = """            case OPTION_ULT_COPY_MESSAGE_REFERENCE: {
                // No private/secret t.me links; values copied only on explicit tap.
                if (selectedObject != null && selectedObject.messageOwner != null
                        && selectedObject.getId() > 0 && !selectedObject.isSponsored()) {
                    final String publicName = currentEncryptedChat == null
                            && currentChat != null && ChatObject.isChannel(currentChat)
                            ? ChatObject.getPublicUsername(currentChat) : null;
                    final String reference = UltChatTools.messageReference(
                            selectedObject.getId(), selectedObject.getFromChatId(),
                            selectedObject.messageOwner.date, publicName);
                    if (reference != null) {
                        AndroidUtilities.addToClipboard(reference);
                        android.widget.Toast.makeText(getParentActivity(),
                                "Reference copied", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            }
            case OPTION_ULT_COPY_FILE_SUMMARY: {
                if (selectedObject != null && selectedObject.messageOwner != null
                        && selectedObject.getDocument() != null
                        && !selectedObject.isSponsored()
                        && !selectedObject.messageOwner.noforwards) {
                    final org.telegram.tgnet.TLRPC.Document file = selectedObject.getDocument();
                    final String summary = UltChatTools.mediaSummary(
                            org.telegram.messenger.FileLoader.getDocumentFileName(file),
                            file.mime_type, file.size);
                    AndroidUtilities.addToClipboard(summary);
                    android.widget.Toast.makeText(getParentActivity(),
                            "File info copied", android.widget.Toast.LENGTH_SHORT).show();
                }
                break;
            }
"""


def replace_once(source: str, anchor: str, insertion: str) -> str:
    count = source.count(anchor)
    if count != 1:
        raise ValueError(f"Expected one pinned message-menu anchor, found {count}: {anchor[:70]!r}")
    return source.replace(anchor, insertion + anchor, 1)


def integrate(root: Path) -> Path:
    target = root / TARGET
    source = target.read_text(encoding="utf-8")
    if "ULT_CHAT_TOOLS_MENU_ID" not in source or "UltChatTools.numericId" not in source:
        raise ValueError("Apply Alpha 0.5 chat tools before Alpha 0.6 message tools")
    if "OPTION_ULT_COPY_MESSAGE_ID" in source:
        raise ValueError("ULT Message Tools integration already applied")
    # Validate all anchors before writing anything to disk.
    for anchor in (CONSTANT_ANCHOR, MENU_ANCHOR, HANDLER_ANCHOR):
        if source.count(anchor) != 1:
            raise ValueError(f"Pinned upstream changed; missing/duplicated anchor: {anchor[:70]!r}")
    source = replace_once(source, CONSTANT_ANCHOR, CONSTANTS)
    source = replace_once(source, MENU_ANCHOR, MENU)
    source = replace_once(source, HANDLER_ANCHOR, HANDLERS)
    target.write_text(source, encoding="utf-8")
    return target


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    args = parser.parse_args()
    print(f"Patched {integrate(args.source_root)}")
