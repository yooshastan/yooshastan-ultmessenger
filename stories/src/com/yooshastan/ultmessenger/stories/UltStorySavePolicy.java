package com.yooshastan.ultmessenger.stories;

/**
 * UI-only eligibility for the visible Ultgram story Gallery save shortcut.
 * Server Premium/account rules and content restrictions remain authoritative.
 */
public final class UltStorySavePolicy {
    private UltStorySavePolicy() {}

    public static boolean eligible(boolean hasMedia, boolean skippedOrDeleted,
            boolean unsupported, boolean live, boolean screenshotsAllowed,
            boolean owner, boolean premium, boolean publicShareAllowed,
            boolean peerProtected) {
        return hasMedia && !skippedOrDeleted && !unsupported && !live
                && screenshotsAllowed && !peerProtected
                && (owner || (premium && publicShareAllowed));
    }
}
