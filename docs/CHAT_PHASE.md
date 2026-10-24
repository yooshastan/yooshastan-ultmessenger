# ULT chat enhancements — draft design

## First model
The pure-Java `UltChatLayoutPreset` defines local-only presentation choices:
- Telegram Default (no changes)
- Compact (denser message bubbles)
- Comfortable (larger spacing)
- Media Focused (media emphasis)

The enum safely loads stored values and requires accessible labels in all modes.

## Next Android tasks
1. Locate the current pinned Telegram chat layout adapter and UI settings.
2. Add ULT Chat Layout to settings, opt-in with Telegram Default as fallback.
3. Apply spacing and media emphasis via UI code, not by altering message contents or delivery behavior.
4. Test dynamic font scale, RTL, group chats, stickers, long messages, and Android TalkBack.
5. Review Nekogram/Goregram/Plus Messenger features individually, preserving their GPL attribution and avoiding a wholesale fork merge.

No Telegram runtime code is modified by this branch yet. No paid/server-side Premium entitlement is unlocked.
