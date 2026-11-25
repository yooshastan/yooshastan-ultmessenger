package com.yooshastan.ultmessenger.video;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.SystemClock;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Toast;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import com.yooshastan.ultmessenger.settings.UltScreenPrivacy;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Private, bounded chat attachments. Only the explicit preview Send action
 * hands a local file to Telegram's native media upload path. No automatic send. */
public final class UltChatMediaDraft {
    private static final long MAX_BYTES = 100L * 1024 * 1024;
    private static final long MAX_CACHE = 512L * 1024 * 1024;
    private static final long TTL = 15L * 60 * 1000;
    private static final int MAX_PENDING = 4;
    private static final ConcurrentHashMap<String, Draft> READY = new ConcurrentHashMap<>();
    private static boolean initialized;
    private UltChatMediaDraft() {}

    public static final class Target {
        final int account;
        final long owner, dialog, sender;
        final TLRPC.Message original;
        final String key;
        final WeakReference<BaseFragment> fragment;
        private Target(BaseFragment fragment, int account, MessageObject message) {
            this.fragment = new WeakReference<>(fragment);
            this.account = account;
            this.owner = UserConfig.getInstance(account).getClientUserId();
            this.original = message.messageOwner;
            this.dialog = MessageObject.getDialogId(original);
            this.sender = activeSender(account, dialog, owner);
            this.key = account + ":" + owner + ":" + dialog + ":" + original.id;
        }
    }
    private static final class Draft {
        final Target target;
        final UltMediaOptions options;
        final File file;
        final long created = SystemClock.elapsedRealtime();
        final AtomicBoolean reviewing = new AtomicBoolean();
        Draft(Target target, UltMediaOptions options, File file) {
            this.target = target; this.options = options; this.file = file;
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

    private static String key(int account, MessageObject message) {
        if (message == null || message.messageOwner == null) return "";
        return account + ":" + UserConfig.getInstance(account).getClientUserId() + ":"
                + MessageObject.getDialogId(message.messageOwner) + ":" + message.getId();
    }

    public static boolean hasReady(int account, MessageObject message) {
        return READY.containsKey(key(account, message));
    }

    public static int readyCount(int account) {
        int count = 0;
        for (Draft d : READY.values()) if (d.target.account == account) count++;
        return count;
    }

    private static boolean visible(BaseFragment fragment) {
        return fragment != null && !fragment.isFinished && !fragment.isPaused()
                && fragment.getParentLayout() != null
                && fragment.getParentLayout().getLastFragment() == fragment
                && fragment.getParentActivity() != null
                && !fragment.getParentActivity().isFinishing()
                && !fragment.getParentActivity().isDestroyed();
    }

    private static long size(File folder) {
        File[] files = folder.listFiles();
        long n = 0;
        if (files != null) for (File f : files) if (f.isFile()) n += f.length();
        return n;
    }

    private static synchronized File pendingDir(Context c) throws IOException {
        File dir = new File(c.getCacheDir(), "ult_chat_media_pending");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("No draft directory");
        if (!initialized) {
            // Unsent drafts are session-only. Native upload sources are in a
            // separate cache directory and are never removed by this cleanup.
            File[] leftovers = dir.listFiles();
            if (leftovers != null) for (File f : leftovers) if (f.isFile()) f.delete();
            initialized = true;
        }
        return dir;
    }

    private static File uploadDir(Context c) throws IOException {
        File dir = new File(c.getCacheDir(), "ult_chat_media_upload");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("No upload cache");
        return dir;
    }

    public static boolean stage(Context c, Target t, UltMediaOptions options, File completed,
                                AtomicBoolean cancelled) {
        if (!options.toChat || t == null || cancelled.get() || blockReason(t, options) != null) return false;
        String extension = options.audio ? options.audioFormat : "mp4";
        if (completed == null || !completed.isFile() || completed.length() <= 0
                || completed.length() > MAX_BYTES
                || !completed.getName().endsWith("." + extension)) {
            UltVideoAutoSave.noteResult(t.account, "Chat attachment requires supported media up to 100 MiB; use Save to device for other files");
            return false;
        }
        if (READY.size() >= MAX_PENDING || READY.containsKey(t.key)) {
            UltVideoAutoSave.noteResult(t.account, "Review or discard ready attachments first");
            return false;
        }
        File staged = null;
        boolean registered = false;
        try {
            File pending = pendingDir(c), upload = uploadDir(c);
            if (size(pending) + size(upload) + completed.length() > MAX_CACHE) {
                UltVideoAutoSave.noteResult(t.account, "Private attachment cache is full; use Telegram cache management after uploads finish");
                return false;
            }
            staged = new File(pending, "ult_" + UUID.randomUUID() + "." + extension);
            long copied = 0;
            try (InputStream in = new FileInputStream(completed); FileOutputStream out = new FileOutputStream(staged)) {
                byte[] buf = new byte[65536];
                for (int n; (n = in.read(buf)) != -1;) {
                    if (cancelled.get() || blockReason(t, options) != null || copied + n > MAX_BYTES)
                        throw new IOException("Cancelled");
                    out.write(buf, 0, n);
                    copied += n;
                }
                out.getFD().sync();
            }
            if (copied != completed.length() || copied <= 0 || cancelled.get()
                    || blockReason(t, options) != null) return false;
            Draft draft = new Draft(t, options, staged);
            if (READY.putIfAbsent(t.key, draft) != null) return false;
            registered = true;
            UltVideoAutoSave.noteResult(t.account, "Attachment ready; review and press Send in the original chat");
            AndroidUtilities.runOnUIThread(() -> {
                BaseFragment fragment = t.fragment.get();
                if (visible(fragment) && fragment.getParentActivity().hasWindowFocus()) review(fragment, draft);
            });
            AndroidUtilities.runOnUIThread(() -> expire(draft), TTL);
            return true;
        } catch (Exception ignored) {
            UltVideoAutoSave.noteResult(t.account, "Could not prepare private chat attachment");
            return false;
        } finally {
            if (!registered && staged != null) staged.delete();
        }
    }

    private static void expire(Draft draft) {
        if (draft.reviewing.get()) {
            AndroidUtilities.runOnUIThread(() -> expire(draft), 60000);
        } else if (SystemClock.elapsedRealtime() - draft.created >= TTL) {
            discard(draft);
        }
    }

    private static void discard(Draft draft) {
        if (READY.remove(draft.target.key, draft)) draft.file.delete();
    }

    public static void reviewReady(BaseFragment fragment, int account, MessageObject message) {
        Draft draft = READY.get(key(account, message));
        if (draft != null) review(fragment, draft);
    }

    private static void review(BaseFragment fragment, Draft draft) {
        if (!visible(fragment) || READY.get(draft.target.key) != draft
                || !draft.reviewing.compareAndSet(false, true)) return;
        Target t = draft.target;
        Activity a = fragment.getParentActivity();
        if (!draft.file.isFile()) {
            discard(draft); draft.reviewing.set(false);
            Toast.makeText(a, "Attachment expired; download again", Toast.LENGTH_SHORT).show(); return;
        }
        if (blockReason(t, draft.options) != null) {
            draft.reviewing.set(false);
            Toast.makeText(a, "Attachment blocked by chat or network policy", Toast.LENGTH_SHORT).show(); return;
        }
        LinearLayout body = new LinearLayout(a);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(16);
        body.setPadding(pad, pad, pad, pad);
        TextView info = new TextView(a);
        info.setText("Reply to the original link in this conversation. Nothing is uploaded until you press Send.\n"
                + android.text.format.Formatter.formatFileSize(a, draft.file.length()));
        body.addView(info);
        VideoView player = new VideoView(a);
        body.addView(player, new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(draft.options.audio ? 1 : 200)));
        Button play = new Button(a);
        play.setText("Play preview");
        play.setEnabled(false);
        body.addView(play);
        player.setOnPreparedListener(mp -> play.setEnabled(true));
        player.setOnErrorListener((mp, what, extra) -> {
            play.setEnabled(false); play.setText("Preview unavailable on this device"); return true;
        });
        player.setVideoPath(draft.file.getAbsolutePath());
        play.setOnClickListener(v -> {
            if (player.isPlaying()) { player.pause(); play.setText("Play preview"); }
            else { player.start(); play.setText("Pause preview"); }
        });
        AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle(draft.options.audio ? "Add audio to chat" : "Add video to chat")
                .setView(body).setPositiveButton("Send", null)
                .setNegativeButton("Discard", (d,w) -> {
                    discard(draft); UltVideoAutoSave.noteResult(t.account, "Attachment discarded");
                }).setNeutralButton("Later", null).create();
        if (dialog.getWindow() != null && !org.telegram.messenger.SharedConfig.disableSecureFlags
                && (UltScreenPrivacy.isEnabled(a) || (a.getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_SECURE) != 0))
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        dialog.setOnDismissListener(d -> {
            player.stopPlayback(); draft.reviewing.set(false);
            if (SystemClock.elapsedRealtime() - draft.created >= TTL) discard(draft);
        });
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String blocked = blockReason(t, draft.options);
            if (!visible(fragment) || blocked != null || READY.get(t.key) != draft
                    || SystemClock.elapsedRealtime() - draft.created >= TTL) {
                Toast.makeText(a, blocked == null ? "Original chat or attachment no longer available" : blocked,
                        Toast.LENGTH_SHORT).show(); return;
            }
            File upload;
            try {
                upload = new File(uploadDir(a), draft.file.getName());
                // Same private cache filesystem. No public Downloads copy.
                if (!draft.file.renameTo(upload)) throw new IOException("Could not stage upload");
            } catch (IOException ignored) {
                Toast.makeText(a, "Could not stage upload", Toast.LENGTH_SHORT).show(); return;
            }
            READY.remove(t.key, draft);
            try {
                MessageObject reply = new MessageObject(t.account, t.original, false, false);
                AccountInstance account = AccountInstance.getInstance(t.account);
                String path = upload.getAbsolutePath();
                if (draft.options.audio) {
                    // Pinned Telegram document preparer extracts music metadata for
                    // MP3/M4A/Opus and creates native audio attachments when readable.
                    SendMessagesHelper.prepareSendingDocument(account, path, path, null, "",
                            draft.options.mime(), t.dialog, reply, null, null, null, null,
                            true, 0, null, null, false);
                } else {
                    SendMessagesHelper.prepareSendingVideo(account, path, null, null, null, t.dialog,
                            reply, null, null, null, null, 0, null,
                            true, 0, 0, false, false, "", null, 0, 0);
                }
                // Keep handed-off paths for Telegram's async upload. Do not
                // delete based solely on enqueue success or elapsed time.
                UltVideoAutoSave.noteResult(t.account, "Attachment handed to Telegram; check message delivery");
            } catch (RuntimeException ignored) {
                UltVideoAutoSave.noteResult(t.account, "Telegram rejected attachment handoff; check chat");
            }
            dialog.dismiss();
        });
    }
}
