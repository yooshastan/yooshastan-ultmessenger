package com.yooshastan.ultmessenger.stories;

/**
 * UI-only eligibility for the visible Ultgram story Gallery save shortcut.
 * Only user-visible own or public/shareable, unprotected media is locally saved.
 * This changes no Telegram account entitlement or server authorization.
 */
public final class UltStorySavePolicy {
    private UltStorySavePolicy() {}

    public static boolean eligible(boolean hasMedia, boolean skippedOrDeleted,
            boolean unsupported, boolean live, boolean screenshotsAllowed,
            boolean owner, boolean premium, boolean publicShareAllowed,
            boolean peerProtected) {
        return hasMedia && !skippedOrDeleted && !unsupported && !live
                && screenshotsAllowed && !peerProtected
                && (owner || publicShareAllowed);
    }
}
