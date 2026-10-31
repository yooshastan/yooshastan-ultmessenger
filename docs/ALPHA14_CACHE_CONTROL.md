# Ultgram fine-grained cache size — Alpha 0.14 candidate

## Requested behavior
The pinned Mercurygram Storage Usage screen offers coarse limits (2, 5, 16, 32 GB depending on device size) or No Limit. Users wanting a size between 5 and 16 GB should be able to select **8, 10, 12 GB or any whole gigabyte**.

**Ultgram Settings → Storage and Cache → Custom maximum cache size** provides a native Android SeekBar with **2–32 GB in 1 GB increments**, plus a separate **No Limit** action and Cancel. The dialog shows the current saved limit and the proposed limit, defaulting the slider thumb to 10 GB when the saved value is Unlimited; that does **not** change the saved setting until Apply.

The reserved upstream preference `cache_limit=1` means **300 MB** in the original UI, not 1 GB, so the safe custom range begins at 2 GB.

The implementation writes only the existing Telegram `SharedConfig.getPreferences().putInt("cache_limit", GB)` value. It does not request storage permissions, scan new paths, immediately erase cached files or install another cleanup engine. **Automatic cleanup runs when Telegram's own background cache maintenance executes; it is not instantaneous or a hard real-time storage quota.**

The stock Telegram Storage Usage slider still shows coarse choices but, when a custom integer preference is selected, it also displays that precise value instead of incorrectly presenting No Limit. Legacy 300 MB selection remains an upstream option.

## Tests
- Plain-Java exact-range policy tests: 2..32 accepted, 1 sentinel excluded, Unlimited accepted, default selection not persisted.
- Source contract test for explicit Apply and no immediate deletion, dual preference continuity, pinned exact-match native patch and duplicate/changed-anchor refusal.
- Full Android build integration and real Pixel 8a / Samsung A36 QA: pick 8/10/12 GB, reopen both Ultgram Settings and Telegram Storage Usage, reboot app, test No Limit/Cancel, verify old cache not deleted just by changing slider, check that automatic cleanup follows saved setting on future runs, and confirm strict proxy/story save regressions.

**Build/test limitation:** This is not a promise of instantaneous disk-cap enforcement or protection for non-Telegram files.
