# Ultgram Alpha 0.10i.2 — message utility QA

Base: Pixel 8a-confirmed strict startup recovery from Alpha 0.10i.1a.

## Scope
Two **user-initiated clipboard-only** actions in the existing message long-press menu:

- `ULT: Copy message time (ISO UTC)` when a message has a positive Unix timestamp. Example: `1970-01-01T00:00:01Z`.
- `ULT: Copy public message link` only for positive message IDs in channels or supergroups with an actual public username, outside encrypted chats. This produces `https://t.me/<public_name>/<message_id>`, never a private invite link.

No message database mutations, shares, downloads, network transport changes, or bulk clipboard operations. The existing message ID, sender ID, Unix timestamp and document detail actions are preserved.

## Device QA
1. Leave Wonhee VPN connected, configured native proxy enabled, native strict proxy ON. Force-stop/open, verify chats connect **without toggling strict**.
2. Long-press an ordinary incoming message with timestamp, copy ISO UTC, inspect clipboard, compare to Unix timestamp.
3. Open a **public channel or supergroup**, long-press a regular message, confirm Copy public message link appears and yields a valid public `t.me/name/id` URL.
4. Private group, one-to-one, and secret chats **must not** offer this public-link action. For user and chat ID actions, confirm no unexpected regression.
5. Verify long-press on sponsored entries and service messages doesn't offer ULT metadata incorrectly.
6. Verify user-initiated copy does not send data to an external host; Android clipboard access is an OS-level privacy exposure.
7. Test installed APK on Pixel 8a, GrapheneOS/Android 17, with day/night themes and force-stop/relaunch.

**Security boundary:** Native strict only covers known tgnet socket paths. DNS/media/CDN/WebViews still require independent egress audit.
