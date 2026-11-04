package com.yooshastan.ultmessenger.settings;

/** Local Settings card only. Does not alter Telegram identity or network state. */
public final class UltIdentityCover {
    public static final String PREFS = "ult_identity_cover";
    private UltIdentityCover() {}
    public static String key(int account) {
        if (account < 0) throw new IllegalArgumentException("account");
        return "cover_account_" + account;
    }
    public static boolean isCovered(boolean enabled) {
        return enabled;
    }
    public static String summary(String username, String phone, boolean covered) {
        if (covered) return "Username: ••••\nPhone: ••••";
        final String user = username == null || username.trim().isEmpty()
                ? "not set" : "@" + username.trim();
        final String number = phone == null || phone.trim().isEmpty()
                ? "not set" : phone.trim();
        return "Username: " + user + "\nPhone: " + number;
    }
}
