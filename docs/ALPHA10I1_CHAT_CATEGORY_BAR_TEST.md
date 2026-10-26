# Ultgram Alpha 0.10i.1 — optional category bar QA

Base: device-tested Alpha 0.10h.2 native strict-proxy recovery. This increment changes **local chat navigation only**.

## New feature
- In Chats → ⋮, enable **ULT: Show category bar**, or open Ultgram Settings → **Show chat category bar**.
- A compact scrollable strip provides All, Unread, Groups, Channels, Bots, People.
- Each selection updates the existing local per-account list filter. Categories do not create Telegram folders or change server ordering, pinning, read status, or archived data.
- Disable the strip from either entry point. The existing chat-view picker still includes **Unread First** sorting.
- The strip is hidden by default and only offered in the primary Chats screen (not Archive, selection or search screens).

## Device checks
1. Install over 0.10h.2; leave your VPN and working MTProto profile configured.
2. Confirm the strip is initially hidden. Enable it from Chats → ⋮.
3. Tap each of the six category labels; check list content, empty states, and scrolling.
4. Switch accounts (if available): category and visibility choices must be isolated.
5. Force-stop and reopen: selected category and bar visibility persist.
6. Open an individual chat, media, Telegram folders, and Archive. Verify normal behavior.
7. Disable the bar; verify native Telegram navigation resumes unchanged.
8. Check Candy Pink day/night and standard appearance; the chips should remain legible.
9. With Wonhee connected, send messages and download media. Strict-proxy on/off behavior must match 0.10h.2.
10. If any UI overlap, ANR, or lost message occurs, restore the stable 0.10h.2 APK and report the steps.

## Privacy boundaries
No new network access, routing, DNS, MTProxy, or native strict-guard changes. This is not a leak test. 
