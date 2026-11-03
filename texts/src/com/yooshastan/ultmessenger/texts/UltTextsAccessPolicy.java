package com.yooshastan.ultmessenger.texts;

/**
 * Local-only eligibility policy for a future SMS/MMS folder.
 *
 * Android's SMS-provider data must never be requested merely because a
 * Telegram account is logged in. This class grants no permissions and does
 * not make Ultgram a default SMS handler by itself.
 */
public final class UltTextsAccessPolicy {
    public enum Status {
        DISABLED,
        TELEPHONY_UNAVAILABLE,
        DEFAULT_SMS_ROLE_REQUIRED,
        SMS_IMPLEMENTATION_INCOMPLETE,
        READY_FOR_SMS_MMS
    }

    private UltTextsAccessPolicy() {}

    public static Status evaluate(boolean userOptedIn,
                                  boolean smsCapableDevice,
                                  boolean isDefaultSmsApp,
                                  boolean canReadSms,
                                  boolean canSendSms,
                                  boolean canReceiveSms,
                                  boolean canReceiveMms,
                                  boolean providerHandlersReady) {
        if (!userOptedIn) return Status.DISABLED;
        if (!smsCapableDevice) return Status.TELEPHONY_UNAVAILABLE;
        if (!isDefaultSmsApp) return Status.DEFAULT_SMS_ROLE_REQUIRED;
        if (!canReadSms || !canSendSms || !canReceiveSms || !canReceiveMms
                || !providerHandlersReady) {
            return Status.SMS_IMPLEMENTATION_INCOMPLETE;
        }
        return Status.READY_FOR_SMS_MMS;
    }

    public static boolean mayAccessSmsProvider(Status status) {
        return status == Status.READY_FOR_SMS_MMS;
    }

    /** Android 16 provides no general third-party RCS inbox API. */
    public static boolean mayReadRcsProvider() {
        return false;
    }

    public static String folderLabel() {
        return "Texts (SMS/MMS)";
    }
}
