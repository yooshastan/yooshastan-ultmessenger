package com.yooshastan.ultmessenger.settings;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import com.yooshastan.ultmessenger.chat.UltChatTools;

/**
 * Media Studio: on-device index of Telegram's recent downloaded document files.
 * No directory scan, network requests, bulk exports or protected-media bypass.
 */
public final class UltMediaStudioActivity extends BaseFragment {
    private static final int MAX_ITEMS = 200;
    private static final class Row {
        final UltMediaIndex.Entry entry;
        final MessageObject message;
        Row(UltMediaIndex.Entry entry, MessageObject message) {
            this.entry = entry;
            this.message = message;
        }
    }
    private final ArrayList<Row> available = new ArrayList<>();
    private EditText search;
    private LinearLayout results;
    private TextView summary;
    private Button sortButton;
    private UltMediaIndex.Kind category;
    private UltMediaIndex.Order order = UltMediaIndex.Order.NEWEST;

    @Override public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Media Studio");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        root.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8),
                AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        TextView description = label(context,
                "Search and sort files already present in Telegram's Recent Downloads. "
                + "Only eligible document-backed media with a readable local copy appear. "
                + "Photo-only messages and older files outside Recent Downloads are not indexed.", 13);
        root.addView(description);
        search = new EditText(context);
        search.setSingleLine(true);
        search.setHint("Search downloaded file names");
        root.addView(search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                render();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        HorizontalScrollView filters = new HorizontalScrollView(context);
        LinearLayout chips = new LinearLayout(context);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        addChip(context, chips, "All", null);
        addChip(context, chips, "Photos", UltMediaIndex.Kind.PHOTO);
        addChip(context, chips, "Videos", UltMediaIndex.Kind.VIDEO);
        addChip(context, chips, "Audio", UltMediaIndex.Kind.AUDIO);
        addChip(context, chips, "Documents", UltMediaIndex.Kind.DOCUMENT);
        filters.addView(chips);
        root.addView(filters);
        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.VERTICAL);
        sortButton = button(context, "Sort: newest", () -> {
            if (order == UltMediaIndex.Order.NEWEST) order = UltMediaIndex.Order.LARGEST;
            else if (order == UltMediaIndex.Order.LARGEST) order = UltMediaIndex.Order.NAME;
            else order = UltMediaIndex.Order.NEWEST;
            sortButton.setText("Sort: " + order.name().toLowerCase(Locale.ROOT));
            render();
        });
        controls.addView(sortButton);
        controls.addView(button(context, "Refresh local index", this::refresh));
        controls.addView(button(context, "Open Downloads and queue controls",
                () -> presentFragment(new UltDownloadsActivity())));
        controls.addView(button(context, "Mobile / Wi-Fi / roaming rules", () -> showNetworkRules()));
        controls.addView(button(context, "Storage and cache manager",
                () -> presentFragment(new org.telegram.ui.CacheControlActivity())));
        root.addView(controls);
        summary = label(context, "Loading local recent downloads…", 14);
        root.addView(summary);
        ScrollView scroll = new ScrollView(context);
        results = new LinearLayout(context);
        results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        fragmentView = root;
        refresh();
        return fragmentView;
    }

    private static TextView label(Context context, String text, int size) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        view.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(8),
                AndroidUtilities.dp(6), AndroidUtilities.dp(8));
        return view;
    }
    private static Button button(Context context, String text, Runnable action) {
        Button result = new Button(context);
        result.setAllCaps(false);
        result.setText(text);
        result.setTextSize(13);
        result.setOnClickListener(v -> action.run());
        return result;
    }
    private void addChip(Context context, LinearLayout bar, String text, UltMediaIndex.Kind kind) {
        Button chip = button(context, text, () -> {
            category = kind;
            render();
        });
        bar.addView(chip);
    }

    private boolean eligible(MessageObject message) {
        if (message == null || message.messageOwner == null
                || message.getDocument() == null || message.isSponsored()
                || message.messageOwner.noforwards
                || DialogObject.isEncryptedDialog(message.getDialogId())) return false;
        TLRPC.Peer peer = message.messageOwner.peer_id;
        if (peer != null) {
            long chatId = peer.channel_id != 0 ? peer.channel_id : peer.chat_id;
            if (chatId != 0) {
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
                if (chat == null || chat.noforwards) return false;
            }
        }
        return true;
    }

    private void refresh() {
        if (results == null) return;
        available.clear();
        // Snapshot only the active account's native Recent Downloads history.
        ArrayList<MessageObject> messages = new ArrayList<>(
                DownloadController.getInstance(currentAccount).recentDownloadingFiles);
        for (MessageObject message : messages) {
            if (!eligible(message)) continue;
            File file = FileLoader.getInstance(currentAccount)
                    .getPathToMessage(message.messageOwner);
            if (file == null || !file.isFile() || !file.canRead()) continue;
            String fileName = UltChatTools.safeFileName(
                    FileLoader.getDocumentFileName(message.getDocument()));
            if (fileName == null || fileName.trim().isEmpty()) fileName = "Unnamed document";
            available.add(new Row(new UltMediaIndex.Entry(fileName,
                    message.getDocument().mime_type, file.length(),
                    message.messageOwner.date), message));
            if (available.size() >= MAX_ITEMS) break;
        }
        render();
    }

    private void render() {
        if (results == null || summary == null) return;
        results.removeAllViews();
        ArrayList<UltMediaIndex.Entry> models = new ArrayList<>();
        for (Row row : available) models.add(row.entry);
        String term = search == null ? "" : search.getText().toString();
        List<UltMediaIndex.Entry> filtered = UltMediaIndex.filter(
                models, category, term, order);
        summary.setText(filtered.size() + " matching cached files / "
                + available.size() + " eligible recent files (account only)");
        if (filtered.isEmpty()) {
            results.addView(label(results.getContext(),
                    "No matching cached files. Try All or check Telegram Downloads. "
                    + "This screen never scans private folders or exports protected media.", 14));
            return;
        }
        for (UltMediaIndex.Entry item : filtered) {
            TextView row = label(results.getContext(), item.name + "\n"
                    + item.kind + " • " + humanBytes(item.bytes)
                    + " • " + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
                            new Date(item.messageEpochSeconds * 1000L)), 14);
            row.setMinHeight(AndroidUtilities.dp(60));
            row.setOnClickListener(v -> showDetails(item));
            results.addView(row);
            View divider = new View(results.getContext());
            divider.setBackgroundColor(0xFF555555);
            results.addView(divider, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(1)));
        }
    }

    private static String humanBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) return String.format(Locale.US,
                "%.1f GiB", bytes / (1024.0 * 1024 * 1024));
        if (bytes >= 1024L * 1024) return String.format(Locale.US,
                "%.1f MiB", bytes / (1024.0 * 1024));
        if (bytes >= 1024L) return String.format(Locale.US, "%.1f KiB", bytes / 1024.0);
        return bytes + " B";
    }

    private void showDetails(UltMediaIndex.Entry item) {
        Activity activity = getParentActivity();
        if (activity == null) return;
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(activity)
                .setTitle(item.name)
                .setMessage("Type: " + item.kind + "\nLocal size: " + humanBytes(item.bytes)
                        + "\nMessage date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                                .format(new Date(item.messageEpochSeconds * 1000L))
                        + "\n\nTo inspect SHA-256, verify a copy or Save As, long-press "
                        + "the same eligible file in ULT Downloads. Files are never "
                        + "sent anywhere by this screen.")
                .setNeutralButton("Open Downloads",
                        (dialogInterface, which) -> presentFragment(new UltDownloadsActivity()))
                .setPositiveButton("Close", null).create();
        if ((UltScreenPrivacy.isEnabled(activity)
                || (SharedConfig.passcodeHash.length() > 0 && !SharedConfig.allowScreenCapture))
                && !SharedConfig.disableSecureFlags && dialog.getWindow() != null) {
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }
        dialog.show();
    }

    private void showNetworkRules() {
        Activity activity = getParentActivity();
        if (activity == null) return;
        new android.app.AlertDialog.Builder(activity)
                .setTitle("Automatic download rules")
                .setItems(new String[]{"Mobile data", "Wi-Fi", "Roaming"},
                        (dialog, choice) -> presentFragment(
                                new org.telegram.ui.DataAutoDownloadActivity(choice)))
                .setNegativeButton("Cancel", null).show();
    }

    @Override public void onResume() {
        super.onResume();
        if (results != null) refresh();
    }
}
