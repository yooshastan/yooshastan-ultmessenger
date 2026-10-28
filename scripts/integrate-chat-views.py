#!/usr/bin/env python3
"""Guarded Alpha 0.10a chat-view presets for pinned Mercurygram Android.

All filter/sort operations use local copies. No Telegram folders, archives, pins,
messages or server settings are modified.
"""
import argparse
from pathlib import Path

ACTIVITY = Path("TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java")
ADAPTER = Path("TMessagesProj/src/main/java/org/telegram/ui/Adapters/DialogsAdapter.java")
MODEL = Path("conversation/src/com/yooshastan/ultmessenger/conversation/UltChatView.java")
DEST = Path("TMessagesProj/src/main/java/com/yooshastan/ultmessenger/conversation/UltChatView.java")

ACTIVITY_IMPORT = "import org.telegram.ui.Adapters.DialogsAdapter;\n"
ADAPTER_IMPORT = "import org.telegram.messenger.AndroidUtilities;\n"
ACTIVITY_METHOD = "    private void showItemOptions() {\n"
ACTIVITY_MENU = "        final boolean isCurrentThemeDark;\n"
ACTIVITY_RETURN = "        ViewCompat.setOnApplyWindowInsetsListener(fragmentView, this::onApplyWindowInsets);\n        return fragmentView;\n"
ACTIVITY_RESUME = "    public void onResume() {\n        super.onResume();\n"
ADAPTER_CLASS = "public class DialogsAdapter extends RecyclerListView.SelectionAdapter implements DialogCell.DialogCellDelegate {\n"
ADAPTER_ARRAY = "        dialogsCount = array.size();\n        isEmpty = false;\n"
ADAPTER_HINT = "        hasHints = folderId == 0 && dialogsType == DialogsActivity.DIALOGS_TYPE_DEFAULT && !isOnlySelect && !MessagesController.getInstance(currentAccount).hintDialogs.isEmpty();\n"
ADAPTER_MOVE = "        if (shouldHideUltArchiveRow()) {\n            // Resolve displayed positions against original dialogs (minus hidden row).\n"
ADAPTER_ARCHIVE = "            boolean hasArchive = !shouldHideUltArchiveRow() && folderId == 0 && dialogsType == 0 && MessagesController.getInstance(currentAccount).dialogs_dict.get(DialogObject.makeFolderDialogId(1)) != null;\n"

ACTIVITY_FIELDS = """
    // Local, account-specific display preset. The server's folders and dialog order are untouched.
    private void showUltChatViewPicker() {
        final Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE);
        final String key = UltChatView.accountKey(currentAccount);
        final int selected = UltChatView.modeIndex(preferences.getString(key, UltChatView.ALL));
        new android.app.AlertDialog.Builder(activity)
                .setTitle("ULT Chat Views")
                .setSingleChoiceItems(UltChatView.LABELS, selected, (dialog, choice) -> {
                    preferences.edit().putString(key, UltChatView.MODES[choice]).apply();
                    if (viewPages != null) {
                        for (ViewPage page : viewPages) {
                            if (page != null && page.dialogsAdapter != null) {
                                page.dialogsAdapter.updateList(null);
                            }
                        }
                    }
                    refreshUltCategoryBar();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    // Quick tab cycle uses the same local per-account preferences as the picker.
    // No Telegram folder, read-state or backend ordering is changed.
    private void advanceUltChatView() {
        final Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE);
        final String key = UltChatView.accountKey(currentAccount);
        final String next = UltChatView.nextVisibleTab(
                preferences.getString(key, UltChatView.ALL),
                preferences.getString(UltChatView.tabConfigKey(currentAccount),
                        UltChatView.defaultTabConfig()));
        preferences.edit().putString(key, next).apply();
        if (viewPages != null) {
            for (ViewPage page : viewPages) {
                if (page != null && page.dialogsAdapter != null) {
                    page.dialogsAdapter.updateList(null);
                }
            }
        }
        refreshUltCategoryBar();
        android.widget.Toast.makeText(activity,
                "ULT View: " + UltChatView.LABELS[UltChatView.modeIndex(next)],
                android.widget.Toast.LENGTH_SHORT).show();
    }
    private android.widget.HorizontalScrollView ultCategoryBar;
    private android.widget.LinearLayout ultCategoryRow;

    private boolean isUltCategoryBarEnabled() {
        return ApplicationLoader.applicationContext.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(UltChatView.quickBarKey(currentAccount), false);
    }

    private void setUltCategoryMode(String mode) {
        ApplicationLoader.applicationContext.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE)
                .edit().putString(UltChatView.accountKey(currentAccount), UltChatView.sanitize(mode)).apply();
        if (viewPages != null) {
            for (ViewPage page : viewPages) {
                if (page != null && page.dialogsAdapter != null) {
                    page.dialogsAdapter.updateList(null);
                }
            }
        }
        refreshUltCategoryBar();
    }

    private void refreshUltCategoryBar() {
        if (ultCategoryBar == null || ultCategoryRow == null) return;
        boolean mainChats = folderId == 0 && communityId == 0 && !onlySelect
                && initialDialogsType == DIALOGS_TYPE_DEFAULT && TextUtils.isEmpty(searchString);
        ultCategoryBar.setVisibility(mainChats && isUltCategoryBarEnabled()
                ? View.VISIBLE : View.GONE);
        final SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE);
        final String mode = prefs.getString(UltChatView.accountKey(currentAccount), UltChatView.ALL);
        final String barStyle = UltChatView.sanitizeBarStyle(prefs.getString(
                UltChatView.barStyleKey(currentAccount), UltChatView.BAR_COMFORTABLE));
        final android.view.ViewGroup.LayoutParams barParams = ultCategoryBar.getLayoutParams();
        if (barParams != null) {
            barParams.height = AndroidUtilities.dp(UltChatView.barHeightDp(barStyle));
            ultCategoryBar.setLayoutParams(barParams);
        }
        ultCategoryBar.setPadding(AndroidUtilities.dp(5),
                AndroidUtilities.dp(UltChatView.BAR_COMPACT.equals(barStyle) ? 2 : 4),
                AndroidUtilities.dp(5),
                AndroidUtilities.dp(UltChatView.BAR_COMPACT.equals(barStyle) ? 2 : 4));
        final String[] visible = UltChatView.visibleTabs(prefs.getString(
                UltChatView.tabConfigKey(currentAccount), UltChatView.defaultTabConfig()));
        ultCategoryRow.removeAllViews();
        for (String choice : visible) {
            android.widget.TextView chip = new android.widget.TextView(ultCategoryRow.getContext());
            chip.setText(UltChatView.quickTabLabel(choice));
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(13);
            chip.setMinHeight(AndroidUtilities.dp(UltChatView.chipHeightDp(barStyle)));
            chip.setPadding(AndroidUtilities.dp(UltChatView.chipPaddingDp(barStyle)), 0,
                    AndroidUtilities.dp(UltChatView.chipPaddingDp(barStyle)), 0);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> setUltCategoryMode(choice));
            boolean selected = choice.equals(mode);
            chip.setTextColor(getThemedColor(selected
                    ? Theme.key_windowBackgroundWhiteBlueText
                    : Theme.key_windowBackgroundWhiteBlackText));
            android.graphics.drawable.GradientDrawable fill = new android.graphics.drawable.GradientDrawable();
            fill.setColor(selected ? getThemedColor(Theme.key_windowBackgroundGray) : android.graphics.Color.TRANSPARENT);
            fill.setCornerRadius(AndroidUtilities.dp(16));
            chip.setBackground(fill);
            chip.setSelected(selected);
            ultCategoryRow.addView(chip, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT));
        }
    }

    private void installUltCategoryBar(Context context, android.widget.FrameLayout container) {
        if (folderId != 0 || communityId != 0 || onlySelect
                || initialDialogsType != DIALOGS_TYPE_DEFAULT || !TextUtils.isEmpty(searchString)) {
            ultCategoryBar = null;
            ultCategoryRow = null;
            return;
        }
        ultCategoryBar = new android.widget.HorizontalScrollView(context);
        ultCategoryBar.setHorizontalScrollBarEnabled(false);
        ultCategoryBar.setFillViewport(false);
        ultCategoryBar.setPadding(AndroidUtilities.dp(5), AndroidUtilities.dp(4),
                AndroidUtilities.dp(5), AndroidUtilities.dp(4));
        android.graphics.drawable.GradientDrawable panel = new android.graphics.drawable.GradientDrawable();
        panel.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
        panel.setCornerRadius(AndroidUtilities.dp(18));
        ultCategoryBar.setBackground(panel);
        ultCategoryBar.setElevation(AndroidUtilities.dp(3));
        ultCategoryRow = new android.widget.LinearLayout(context);
        ultCategoryRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        ultCategoryBar.addView(ultCategoryRow, new android.widget.HorizontalScrollView.LayoutParams(
                android.widget.HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                android.widget.HorizontalScrollView.LayoutParams.MATCH_PARENT));
        // Floating, opt-in strip above the bottom message composer/primary action area.
        // It does not replace or mutate Telegram folder tabs.
        container.addView(ultCategoryBar, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, UltChatView.barHeightDp(UltChatView.BAR_COMFORTABLE),
                Gravity.BOTTOM, 12, 0, 12, 92));
        refreshUltCategoryBar();
    }

"""
ACTIVITY_MENU_ITEMS = """        if (folderId == 0 && communityId == 0 && !onlySelect
                && initialDialogsType == DIALOGS_TYPE_DEFAULT) {
            io.add(R.drawable.msg_customize, "ULT: Chat Views", this::showUltChatViewPicker);
            io.add(R.drawable.msg_customize, "ULT: Next Chat Tab", this::advanceUltChatView);
            io.addChecked(isUltCategoryBarEnabled(), "ULT: Show category bar", () -> {
                ApplicationLoader.applicationContext.getSharedPreferences(
                        UltChatView.PREFERENCES, Context.MODE_PRIVATE)
                        .edit().putBoolean(UltChatView.quickBarKey(currentAccount),
                                !isUltCategoryBarEnabled()).apply();
                refreshUltCategoryBar();
            });
            io.addGap();
        }

"""
ADAPTER_METHODS = """
    private String ultChatViewMode() {
        return UltChatView.sanitize(mContext.getSharedPreferences(
                UltChatView.PREFERENCES, Context.MODE_PRIVATE)
                .getString(UltChatView.accountKey(currentAccount), UltChatView.ALL));
    }

    private boolean isUltChatViewActive() {
        return folderId == 0 && dialogsType == DialogsActivity.DIALOGS_TYPE_DEFAULT
                && !isOnlySelect && communityId == 0
                && !UltChatView.ALL.equals(ultChatViewMode());
    }

    private static boolean isUltDialogUnread(TLRPC.Dialog dialog) {
        return dialog.unread_count > 0 || dialog.unread_mark;
    }

    private ArrayList<TLRPC.Dialog> ultFilteredDialogs(ArrayList<TLRPC.Dialog> dialogs,
                                                       MessagesController controller) {
        if (!isUltChatViewActive()) {
            return dialogs;
        }
        final String mode = ultChatViewMode();
        final ArrayList<TLRPC.Dialog> displayed = new ArrayList<>(dialogs.size());
        final long archiveId = DialogObject.makeFolderDialogId(1);
        for (TLRPC.Dialog dialog : dialogs) {
            if (dialog.id == archiveId) {
                if (UltChatView.UNREAD_FIRST.equals(mode)) {
                    displayed.add(dialog);
                }
                continue;
            }
            boolean group = false, channel = false, bot = false;
            if (DialogObject.isChatDialog(dialog.id)) {
                TLRPC.Chat chat = controller.getChat(-dialog.id);
                if (chat != null) {
                    channel = ChatObject.isChannel(chat) && !chat.megagroup;
                    group = !channel;
                }
            } else if (DialogObject.isUserDialog(dialog.id)) {
                TLRPC.User user = controller.getUser(dialog.id);
                bot = user != null && user.bot;
            }
            if (UltChatView.accepts(mode, isUltDialogUnread(dialog), group, channel, bot)) {
                displayed.add(dialog);
            }
        }
        if (UltChatView.UNREAD_FIRST.equals(mode)) {
            // TimSort is stable: preserve normal order among same-priority dialogs.
            displayed.sort((a, b) -> UltChatView.compareUnreadFirst(
                    a.id == archiveId, b.id == archiveId,
                    a.pinned, b.pinned,
                    isUltDialogUnread(a), isUltDialogUnread(b)));
        }
        return displayed;
    }

"""

def once(source: str, old: str, new: str) -> str:
    count = source.count(old)
    if count != 1:
        raise ValueError(f"Pinned/stacked source drift, expected one anchor (got {count}): {old[:100]!r}")
    return source.replace(old, new, 1)

def integrate(root: Path) -> tuple[Path, Path]:
    activity_file = root / ACTIVITY
    adapter_file = root / ADAPTER
    activity = activity_file.read_text(encoding="utf-8")
    adapter = adapter_file.read_text(encoding="utf-8")
    if "showUltChatViewPicker()" in activity or "ultFilteredDialogs" in adapter:
        raise ValueError("ULT chat views already integrated")
    for src, anchors in (
        (activity, (ACTIVITY_IMPORT, ACTIVITY_METHOD, ACTIVITY_MENU, ACTIVITY_RETURN, ACTIVITY_RESUME)),
        (adapter, (ADAPTER_IMPORT, ADAPTER_CLASS, ADAPTER_ARRAY, ADAPTER_HINT,
                   ADAPTER_MOVE, ADAPTER_ARCHIVE)),
    ):
        for anchor in anchors:
            if src.count(anchor) != 1:
                raise ValueError(f"Pinned/stacked source drift: {anchor[:100]!r}")
    model_bytes = (Path(__file__).resolve().parents[1] / MODEL).read_bytes()

    activity = once(activity, ACTIVITY_IMPORT, ACTIVITY_IMPORT +
                    "import com.yooshastan.ultmessenger.conversation.UltChatView;\n")
    activity = once(activity, ACTIVITY_METHOD, ACTIVITY_FIELDS + ACTIVITY_METHOD)
    activity = once(activity, ACTIVITY_MENU, ACTIVITY_MENU_ITEMS + ACTIVITY_MENU)
    activity = once(activity, ACTIVITY_RETURN,
                    ACTIVITY_RETURN.replace("        return fragmentView;",
                                            "        installUltCategoryBar(context, contentView);\\n        return fragmentView;".replace("\\n", "\n")))
    activity = once(activity, ACTIVITY_RESUME,
                    ACTIVITY_RESUME + "        refreshUltCategoryBar();\n")

    adapter = once(adapter, ADAPTER_IMPORT, ADAPTER_IMPORT +
                   "import com.yooshastan.ultmessenger.conversation.UltChatView;\n")
    adapter = once(adapter, ADAPTER_CLASS, ADAPTER_CLASS + ADAPTER_METHODS)
    adapter = once(adapter, ADAPTER_ARRAY,
                   "        array = ultFilteredDialogs(array, messagesController);\n" + ADAPTER_ARRAY)
    adapter = once(adapter, ADAPTER_HINT,
                   ADAPTER_HINT.replace("hasHints = folderId", "hasHints = !isUltChatViewActive() && folderId"))
    adapter = once(adapter, ADAPTER_MOVE,
                   ADAPTER_MOVE.replace("if (shouldHideUltArchiveRow())", "if (shouldHideUltArchiveRow() || isUltChatViewActive())"))
    adapter = once(adapter, ADAPTER_ARCHIVE,
                   ADAPTER_ARCHIVE.replace("= !shouldHideUltArchiveRow()", "= !isUltChatViewActive() && !shouldHideUltArchiveRow()"))

    destination = root / DEST
    destination.parent.mkdir(parents=True, exist_ok=True)
    activity_file.write_text(activity, encoding="utf-8")
    adapter_file.write_text(adapter, encoding="utf-8")
    destination.write_bytes(model_bytes)
    return activity_file, adapter_file

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    for patched in integrate(parser.parse_args().source_root):
        print(f"Patched {patched}")
