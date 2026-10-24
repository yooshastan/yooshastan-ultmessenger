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

CONSTANTS = """    public final static int OPTION_ULT_COPY_MESSAGE_ID = 0x554c56;
    public final static int OPTION_ULT_COPY_SENDER_ID = 0x554c57;
"""
MENU = """        // ULT: local read-only metadata; never copy private message content.
        if (selectedObject != null && selectedObject.messageOwner != null
                && selectedObject.getId() != 0 && !selectedObject.isSponsored()) {
            items.add("ULT: Copy message ID");
            options.add(OPTION_ULT_COPY_MESSAGE_ID);
            icons.add(R.drawable.msg_copy);
            if (selectedObject.getFromChatId() != 0) {
                items.add("ULT: Copy sender peer ID");
                options.add(OPTION_ULT_COPY_SENDER_ID);
                icons.add(R.drawable.msg_copy);
            }
        }

"""
HANDLERS = """            case OPTION_ULT_COPY_MESSAGE_ID: {
                AndroidUtilities.addToClipboard(UltChatTools.numericId(selectedObject.getId()));
                android.widget.Toast.makeText(getParentActivity(), "Message ID copied",
                        android.widget.Toast.LENGTH_SHORT).show();
                break;
            }
            case OPTION_ULT_COPY_SENDER_ID: {
                final long senderPeerId = selectedObject.getFromChatId();
                if (senderPeerId != 0) {
                    AndroidUtilities.addToClipboard(UltChatTools.numericId(senderPeerId));
                    android.widget.Toast.makeText(getParentActivity(), "Sender peer ID copied",
                            android.widget.Toast.LENGTH_SHORT).show();
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
