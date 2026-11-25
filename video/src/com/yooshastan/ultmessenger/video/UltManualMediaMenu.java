package com.yooshastan.ultmessenger.video;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;
import android.view.WindowManager;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import com.yooshastan.ultmessenger.settings.UltLinkSanitizerPolicy;
import com.yooshastan.ultmessenger.settings.UltScreenPrivacy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Explicit long-press actions. Never sends media or changes automatic download preferences. */
public final class UltManualMediaMenu {
    private UltManualMediaMenu() {}
    public static List<String> links(MessageObject message, boolean protectedChat) {
        List<String> found = new ArrayList<>();
        if (protectedChat || message == null || message.messageOwner == null || message.getId() <= 0
                || message.messageOwner.noforwards || message.isSponsored()
                || DialogObject.isEncryptedDialog(MessageObject.getDialogId(message.messageOwner))) return found;
        for (String url : UltVideoLinkRules.findUrls(message.messageOwner.message))
            if (UltVideoLinkRules.isManualMediaUrl(url) && !found.contains(url)) found.add(url);
        if (message.messageOwner.entities != null) for (TLRPC.MessageEntity entity : message.messageOwner.entities) {
            if (found.size() >= 3) break;
            if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                String url = ((TLRPC.TL_messageEntityTextUrl) entity).url;
                if (UltVideoLinkRules.isManualMediaUrl(url) && !found.contains(url)) found.add(url);
            }
        }
        return found;
    }
    private static boolean protectedNow(int account, MessageObject message, boolean protectedChat) {
        if (protectedChat || message == null || message.messageOwner == null) return true;
        long dialogId = MessageObject.getDialogId(message.messageOwner);
        if (DialogObject.isEncryptedDialog(dialogId) || dialogId == 0) return true;
        if (DialogObject.isChatDialog(dialogId)) {
            TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-dialogId);
            return chat == null || chat.noforwards;
        }
        return !DialogObject.isUserDialog(dialogId);
    }
    public static boolean eligible(MessageObject message, boolean protectedChat) {
        return !links(message, protectedChat).isEmpty();
    }
    private static void show(Activity activity, AlertDialog.Builder builder) {
        if (activity == null || activity.isFinishing()) return;
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null && !org.telegram.messenger.SharedConfig.disableSecureFlags
                && (UltScreenPrivacy.isEnabled(activity) || (activity.getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_SECURE) != 0)) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        dialog.show();
    }
    private static void toast(Activity a, String text) { Toast.makeText(a, text, Toast.LENGTH_SHORT).show(); }
    private static void choose(Activity a, String title, String[] items, Consumer<Integer> selected) {
        show(a, new AlertDialog.Builder(a).setTitle(title).setItems(items, (d, i) -> selected.accept(i))
                .setNegativeButton("Cancel", null));
    }
    public static void open(Activity a, int account, MessageObject message, boolean protectedChat, int action) {
        if (a == null || a.isFinishing()) return;
        List<String> urls = links(message, protectedNow(account, message, protectedChat));
        if (urls.isEmpty()) return;
        final long owner = UserConfig.getInstance(account).getClientUserId();
        Consumer<Integer> chooseLink = index -> {
            if (owner == 0 || owner != UserConfig.getInstance(account).getClientUserId()
                    || !links(message, protectedNow(account, message, protectedChat)).contains(urls.get(index))) return;
            String url = urls.get(index);
            if (action == 3) {
                AndroidUtilities.addToClipboard(UltLinkSanitizerPolicy.cleanUrl(url));
                toast(a, "Link copied; protected parameters preserved");
                return;
            }
            String block = UltVideoAutoSave.requestBlockReason(account, true, owner, null);
            if (block != null) { toast(a, "Download blocked: " + block); return; }
            Consumer<UltMediaOptions> submit = options -> {
                if (!links(message, protectedNow(account, message, protectedChat)).contains(url)) return;
                show(a, new AlertDialog.Builder(a).setTitle("Save media locally?")
                        .setMessage("Contacts the selected website outside Telegram MTProxy. Only download media you may save. No chat message will be sent.")
                        .setNegativeButton("Cancel", null).setPositiveButton("Download", (d, w) -> {
                            if (!links(message, protectedNow(account, message, protectedChat)).contains(url)) return;
                            toast(a, UltVideoAutoSave.queueManualSite(account, MessageObject.getDialogId(message.messageOwner),
                                    message.getId(), url, options, owner));
                        }));
            };
            int height = UltVideoAutoSave.maxVideoHeight(account);
            String format = UltVideoAutoSave.defaultAudioFormat(account);
            if (action == 0) submit.accept(new UltMediaOptions(true, false, height, format));
            else if (action == 1) submit.accept(new UltMediaOptions(true, true, height, format));
            else choose(a, "Download with options", new String[]{"Video", "Audio only"}, kind -> {
                if (kind == 0) {
                    int[] heights = {0,1080,720,480};
                    choose(a, "Maximum resolution", new String[]{"Best available","1080p","720p","480p"},
                            i -> submit.accept(new UltMediaOptions(true, false, heights[i], format)));
                } else {
                    String[] formats = {"m4a","mp3","opus"};
                    choose(a, "Audio format", new String[]{"M4A","MP3","Opus"},
                            i -> submit.accept(new UltMediaOptions(true, true, height, formats[i])));
                }
            });
        };
        if (urls.size() == 1) chooseLink.accept(0);
        else {
            String[] labels = new String[urls.size()];
            for (int i = 0; i < labels.length; i++) labels[i] = "Link " + (i+1) + " · " + UltVideoLinkRules.sourceCategory(urls.get(i));
            choose(a, "Select media link", labels, chooseLink);
        }
    }
}
