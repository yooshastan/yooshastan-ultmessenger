#!/usr/bin/env python3
"""Guarded Alpha 0.8 archive-row display patch for pinned Mercurygram.

Archived conversations, their folder, and server-side state remain unchanged.
"""
import argparse
from pathlib import Path

ACTIVITY = Path("TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java")
ADAPTER = Path("TMessagesProj/src/main/java/org/telegram/ui/Adapters/DialogsAdapter.java")
MODEL = Path("conversation/src/com/yooshastan/ultmessenger/conversation/UltArchiveVisibility.java")
DESTINATION = Path("TMessagesProj/src/main/java/com/yooshastan/ultmessenger/conversation/UltArchiveVisibility.java")

ACTIVITY_IMPORT = "import org.telegram.ui.Adapters.DialogsAdapter;\n"
ADAPTER_IMPORT = "import org.telegram.messenger.AndroidUtilities;\n"
ACTIVITY_CLASS_ANCHOR = "    public boolean hasHiddenArchive() {\n"
ARCHIVE_GUARD = "        return !onlySelect && initialDialogsType == DIALOGS_TYPE_DEFAULT && communityId == 0 && folderId == 0 && getMessagesController().hasHiddenArchive();\n"
PULL_GUARD = "&& getMessagesController().hasHiddenArchive() && viewPage.archivePullViewState == ARCHIVE_ITEM_STATE_HIDDEN;"
MENU_ANCHOR = "        final boolean isCurrentThemeDark;\n"
ADAPTER_CLASS = "public class DialogsAdapter extends RecyclerListView.SelectionAdapter implements DialogCell.DialogCellDelegate {\n"
ARRAY_ANCHOR = "        dialogsCount = array.size();\n        isEmpty = false;\n"
MOVE_ANCHOR = "        int fromIndex = fixPosition(fromPosition);\n        int toIndex = fixPosition(toPosition);\n"
EMPTY_ANCHOR = "            boolean hasArchive = folderId == 0 && dialogsType == 0 && MessagesController.getInstance(currentAccount).dialogs_dict.get(DialogObject.makeFolderDialogId(1)) != null;\n"

ACTIVITY_METHOD = """
    private boolean isUltArchiveRowHidden() {
        return ApplicationLoader.applicationContext.getSharedPreferences(
                UltArchiveVisibility.PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(UltArchiveVisibility.accountKey(currentAccount), false);
    }

"""
MENU = """        // ULT: account-specific local presentation only. No folder mutations.
        if (folderId == 0 && communityId == 0 && !onlySelect
                && initialDialogsType == DIALOGS_TYPE_DEFAULT) {
            final boolean hidden = isUltArchiveRowHidden();
            io.addChecked(hidden, "ULT: Hide Archive row", () -> {
                ApplicationLoader.applicationContext.getSharedPreferences(
                        UltArchiveVisibility.PREFERENCES, Context.MODE_PRIVATE)
                        .edit().putBoolean(UltArchiveVisibility.accountKey(currentAccount), !hidden).apply();
                if (viewPages != null) {
                    for (ViewPage page : viewPages) {
                        if (page != null && page.dialogsAdapter != null) {
                            page.dialogsAdapter.updateList(null);
                            if (page.pullForegroundDrawable != null) {
                                if (isUltArchiveRowHidden()) {
                                    page.pullForegroundDrawable.doNotShow();
                                } else if (hasHiddenArchive()) {
                                    page.pullForegroundDrawable.showHidden();
                                }
                            }
                        }
                    }
                }
            });
            if (hidden) {
                io.add(R.drawable.msg_archive, "ULT: Open Archive", () -> {
                    Bundle args = new Bundle();
                    args.putInt("folderId", 1);
                    presentFragment(new DialogsActivity(args));
                });
            }
            io.addGap();
        }

"""
ADAPTER_METHOD = """
    private boolean shouldHideUltArchiveRow() {
        boolean enabled = mContext.getSharedPreferences(
                UltArchiveVisibility.PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(UltArchiveVisibility.accountKey(currentAccount), false);
        return UltArchiveVisibility.shouldHideArchiveRow(enabled, dialogsType, folderId, isOnlySelect);
    }

"""
ARRAY_FILTER = """        // Copy only the displayed array; do not alter the account's dialogs model.
        if (shouldHideUltArchiveRow()) {
            final long archiveId = DialogObject.makeFolderDialogId(1);
            ArrayList<TLRPC.Dialog> visibleDialogs = new ArrayList<>(array.size());
            for (TLRPC.Dialog dialog : array) {
                if (dialog.id != archiveId) {
                    visibleDialogs.add(dialog);
                }
            }
            array = visibleDialogs;
        }

"""
MOVE_REPLACEMENT = """        int fromIndex;
        int toIndex;
        if (shouldHideUltArchiveRow()) {
            // Resolve displayed positions against original dialogs (minus hidden row).
            Object source = getItem(fromPosition);
            Object target = getItem(toPosition);
            if (!(source instanceof TLRPC.Dialog) || !(target instanceof TLRPC.Dialog)) {
                return;
            }
            fromIndex = dialogs.indexOf(source);
            toIndex = dialogs.indexOf(target);
            if (fromIndex < 0 || toIndex < 0) {
                return;
            }
        } else {
            fromIndex = fixPosition(fromPosition);
            toIndex = fixPosition(toPosition);
        }
"""

def replace_once(source: str, anchor: str, replacement: str) -> str:
    count = source.count(anchor)
    if count != 1:
        raise ValueError(f"Pinned source changed: expected one anchor, found {count}: {anchor[:90]!r}")
    return source.replace(anchor, replacement, 1)

def integrate(root: Path) -> tuple[Path, Path]:
    activity = root / ACTIVITY
    adapter = root / ADAPTER
    main_source = activity.read_text(encoding="utf-8")
    list_source = adapter.read_text(encoding="utf-8")
    if "ULT: Hide Archive row" in main_source or "shouldHideUltArchiveRow" in list_source:
        raise ValueError("ULT archive visibility already integrated")
    # Check both upstream files and the model before any writes.
    for source, anchors in (
        (main_source, (ACTIVITY_IMPORT, ACTIVITY_CLASS_ANCHOR, ARCHIVE_GUARD, PULL_GUARD, MENU_ANCHOR)),
        (list_source, (ADAPTER_IMPORT, ADAPTER_CLASS, ARRAY_ANCHOR, MOVE_ANCHOR, EMPTY_ANCHOR)),
    ):
        for anchor in anchors:
            if source.count(anchor) != 1:
                raise ValueError(f"Pinned source changed: {anchor[:90]!r}")
    model_bytes = (Path(__file__).resolve().parents[1] / MODEL).read_bytes()

    main_source = replace_once(main_source, ACTIVITY_IMPORT,
        ACTIVITY_IMPORT + "import com.yooshastan.ultmessenger.conversation.UltArchiveVisibility;\n")
    main_source = replace_once(main_source, ARCHIVE_GUARD,
        ARCHIVE_GUARD.replace("return !onlySelect", "return !isUltArchiveRowHidden() && !onlySelect"))
    main_source = replace_once(main_source, PULL_GUARD,
        PULL_GUARD.replace("getMessagesController().hasHiddenArchive()", "hasHiddenArchive()"))
    main_source = replace_once(main_source, MENU_ANCHOR, MENU + MENU_ANCHOR)
    main_source = replace_once(main_source, ACTIVITY_CLASS_ANCHOR, ACTIVITY_METHOD + ACTIVITY_CLASS_ANCHOR)

    list_source = replace_once(list_source, ADAPTER_IMPORT,
        ADAPTER_IMPORT + "import com.yooshastan.ultmessenger.conversation.UltArchiveVisibility;\n")
    list_source = replace_once(list_source, ADAPTER_CLASS, ADAPTER_CLASS + ADAPTER_METHOD)
    list_source = replace_once(list_source, ARRAY_ANCHOR, ARRAY_FILTER + ARRAY_ANCHOR)
    list_source = replace_once(list_source, MOVE_ANCHOR, MOVE_REPLACEMENT)
    list_source = replace_once(list_source, EMPTY_ANCHOR,
        EMPTY_ANCHOR.replace("= folderId == 0", "= !shouldHideUltArchiveRow() && folderId == 0"))

    destination = root / DESTINATION
    destination.parent.mkdir(parents=True, exist_ok=True)
    activity.write_text(main_source, encoding="utf-8")
    adapter.write_text(list_source, encoding="utf-8")
    destination.write_bytes(model_bytes)
    return activity, adapter

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    args = parser.parse_args()
    for path in integrate(args.source_root):
        print(f"Patched {path}")
