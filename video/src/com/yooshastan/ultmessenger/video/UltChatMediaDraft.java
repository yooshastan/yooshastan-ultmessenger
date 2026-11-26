package com.yooshastan.ultmessenger.video;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;

/** Captures and revalidates the destination of a confirmed manual media send.
 * Historical class name retained to minimize integration churn; no preview queue. */
public final class UltChatMediaDraft {
    private UltChatMediaDraft() {}
    public static final class Target {
        final int account;
        final long owner, dialog, sender;
        final TLRPC.Message original;
        private Target(BaseFragment fragment, int account, MessageObject message) {
            this.account = account;
            this.owner = UserConfig.getInstance(account).getClientUserId();
            this.original = message.messageOwner;
            this.dialog = MessageObject.getDialogId(original);
            this.sender = activeSender(account, dialog, owner);
        }
    }

    private static long activeSender(int account, long dialog, long owner) {
        if (DialogObject.isUserDialog(dialog)) return owner;
        if (!DialogObject.isChatDialog(dialog)) return 0;
        MessagesController mc = MessagesController.getInstance(account);
        TLRPC.Chat chat = mc.getChat(-dialog);
        return chat == null ? 0 : ChatObject.getSendAsPeerId(chat, mc.getChatFull(-dialog), true);
    }

    public static Target capture(BaseFragment fragment, int account, MessageObject message) {
        if (fragment == null || message == null || message.messageOwner == null
                || message.getId() <= 0 || message.isSponsored() || message.messageOwner.noforwards) return null;
        return new Target(fragment, account, message);
    }

    public static String blockReason(Target t, UltMediaOptions options) {
        if (t == null || t.original == null || t.owner == 0 || t.original.id <= 0
                || t.original.noforwards || t.dialog == 0 || DialogObject.isEncryptedDialog(t.dialog)
                || MessageObject.getDialogId(t.original) != t.dialog) return "Protected or unsupported conversation";
        String policy = UltVideoAutoSave.requestBlockReason(t.account, true, t.owner, null);
        if (policy != null) return policy;
        MessagesController mc = MessagesController.getInstance(t.account);
        if (mc.getSendPaidMessagesStars(t.dialog) > 0) return "Paid messages are not supported";
        if (DialogObject.isUserDialog(t.dialog)) return null;
        if (!DialogObject.isChatDialog(t.dialog)) return "Unsupported conversation";
        TLRPC.Chat chat = mc.getChat(-t.dialog);
        if (chat == null || chat.noforwards || chat.forum || chat.monoforum
                || ChatObject.isChannelAndNotMegaGroup(chat)) return "Protected chat, channel or topic not supported";
        // Manual attachment is by the current user, never by the original link author.
        // Anonymous and alternate send-as identities are excluded in this beta.
        if (t.sender != t.owner || activeSender(t.account, t.dialog, t.owner) != t.sender)
            return "Send-as identity changed or is not your personal account";
        if (options.audio ? !ChatObject.canSendMusic(chat) || !ChatObject.canSendDocument(chat)
                : !ChatObject.canSendVideo(chat)) return "Chat does not allow this media";
        return null;
    }

}
