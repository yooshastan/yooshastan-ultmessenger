package com.yooshastan.ultmessenger.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ProxyListActivity;

/**
 * Live, read-only privacy diagnostics. A screen-scoped sampler of UI/platform
 * state, NOT a packet sniffer, network firewall or proxy-routing guarantee.
 */
public final class UltPrivacyLabActivity extends BaseFragment {
    private final UltPrivacySession session = new UltPrivacySession();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status;
    private TextView timeline;
    private boolean visible;
    private final Runnable sampler = new Runnable() {
        @Override public void run() {
            if (!visible) return;
            sample();
            handler.postDelayed(this, 4000L);
        }
    };

    @Override public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Privacy Lab");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        ScrollView scroll = new ScrollView(context);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addText(content, context, "Observed app and Android states (refreshes every 4 seconds). "
                + "No packets are captured and this screen does not verify any traffic route.", 14);
        status = addText(content, context, "Waiting for snapshot…", 16);
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.VERTICAL);
        button(bar, context, "Refresh now", this::sample);
        button(bar, context, "Copy redacted session report", () -> {
            ClipboardManager clipboard = (ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText(
                        "ULT Privacy Lab", session.redactedReport()));
                Toast.makeText(context, "Report copied; clipboard may be visible to Android",
                        Toast.LENGTH_LONG).show();
            }
        });
        button(bar, context, "Clear this session's observations", () -> {
            session.clear();
            sample();
        });
        button(bar, context, "Open Telegram proxy manager",
                () -> presentFragment(new ProxyListActivity()));
        content.addView(bar);
        addText(content, context, "Session timeline (memory only, last 40 changes)", 17);
        timeline = addText(content, context, "No observations yet", 13);
        addText(content, context, "How to test: check strict guard ON, temporarily select "
                + "an unreachable test proxy and verify chats stop, restore your real proxy, "
                + "then switch Wi-Fi/cellular and check recovery. Do NOT treat a successful "
                + "test as proof that DNS, link previews, HTTP, media or calls are routed "
                + "through the proxy. For leak assurance use independent device-edge capture.", 13);
        fragmentView = scroll;
        return fragmentView;
    }

    private static TextView addText(LinearLayout root, Context context,
                                    String value, int sizeSp) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        text.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        root.addView(text);
        return text;
    }

    private static void button(LinearLayout root, Context context, String label, Runnable action) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(label);
        button.setOnClickListener(v -> action.run());
        root.addView(button);
    }

    private static String transport(Context context) {
        try {
            ConnectivityManager manager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) return "Unknown";
            Network active = manager.getActiveNetwork();
            if (active == null) return "Offline";
            NetworkCapabilities caps = manager.getNetworkCapabilities(active);
            if (caps == null) return "Unknown";
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return "VPN";
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Cellular";
            return "Other";
        } catch (RuntimeException denied) {
            return "Unknown";
        }
    }

    private void sample() {
        Context context = getParentActivity();
        if (context == null || status == null || timeline == null) return;
        // A fragment can outlive an account switch; never display its old session.
        if (UserConfig.selectedAccount != currentAccount) {
            session.clear();
            status.setText("Account changed. Reopen Privacy Lab for the active account.");
            timeline.setText("Session cleared after account switch.");
            return;
        }
        final boolean requested = ConnectionsManager.isUltStrictNativeProxyEnabled();
        final boolean observed = ConnectionsManager.native_isStrictProxyModeEnabled(currentAccount);
        final int connection = ConnectionsManager.getInstance(currentAccount).getConnectionState();
        final String network = transport(context);
        session.observe(System.currentTimeMillis(), network, requested, observed, connection);
        status.setText("Android active transport: " + network
                + "\nNative proxy-only requested: " + (requested ? "ON" : "OFF")
                + "\nNative socket guard reported: " + (observed ? "ON" : "OFF")
                + "\nTelegram connection state code: " + connection
                + "\nAndroid Private DNS: " + UltAndroidDnsInspector.inspect(context)
                + "\n\nRoute proof: NOT VERIFIED. This is state observation, not egress capture.");
        StringBuilder seen = new StringBuilder();
        for (String row : session.events()) seen.append("• ").append(row).append('\n');
        timeline.setText(seen.toString());
    }

    @Override public void onResume() {
        super.onResume();
        visible = true;
        handler.removeCallbacks(sampler);
        handler.post(sampler);
    }

    @Override public void onPause() {
        visible = false;
        handler.removeCallbacks(sampler);
        super.onPause();
    }

    @Override public void onFragmentDestroy() {
        visible = false;
        handler.removeCallbacks(sampler);
        session.clear();
        super.onFragmentDestroy();
    }
}
