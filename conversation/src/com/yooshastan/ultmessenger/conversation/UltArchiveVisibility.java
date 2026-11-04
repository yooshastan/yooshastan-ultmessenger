package com.yooshastan.ultmessenger.conversation;

/** Pure local display logic; no archive actions or transport changes. */
public final class UltArchiveVisibility {
    public static final String PREFERENCES = "ult_conversation";

    private UltArchiveVisibility() { }

    public static String accountKey(int account) {
        if (account < 0) {
            throw new IllegalArgumentException("Account must be non-negative");
        }
        return "hide_archive_row_account_" + account;
    }

    public static boolean shouldHideArchiveRow(boolean enabled, int dialogsType, int folderId,
                                               boolean isSelectionMode) {
        return enabled && dialogsType == 0 && folderId == 0 && !isSelectionMode;
    }
}
