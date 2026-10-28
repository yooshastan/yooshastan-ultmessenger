package com.yooshastan.ultmessenger.settings;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.SearchDownloadsContainer;
import org.telegram.ui.FilteredSearchView;

/**
 * Alpha 0.13b: reuse Telegram's exact active/recent file download view.
 * All transfers/opens/cancels stay in Telegram FileLoader and existing
 * protected-media behavior; no new storage, request or alternate downloader.
 */
public final class UltDownloadsActivity extends BaseFragment
        implements FilteredSearchView.UiCallback {

    private SearchDownloadsContainer downloads;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("ULT Downloads");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        downloads = new SearchDownloadsContainer(this, currentAccount);
        downloads.setUiCallback(this);
        downloads.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        fragmentView = downloads;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (downloads != null) downloads.update(false);
    }

    // This slim viewer deliberately does not pretend to support Telegram's
    // multi-select action mode. Native tap-to-open/download/cancel still works.
    @Override
    public void goToMessage(MessageObject message) {
        if (getParentActivity() != null) {
            Toast.makeText(getParentActivity(),
                    "Open the chat from Telegram Search to view the source message",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean actionModeShowing() {
        return false;
    }

    @Override
    public void toggleItemSelection(MessageObject item, View view, int a) {
        // Deliberately disabled; never bulk-export or alter messages.
    }

    @Override
    public boolean isSelected(FilteredSearchView.MessageHashId id) {
        return false;
    }

    @Override
    public void showActionMode() {
        if (getParentActivity() != null) {
            Toast.makeText(getParentActivity(),
                    "Multi-select is available in Telegram's original Downloads search",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getFolderId() {
        return 0;
    }
}
