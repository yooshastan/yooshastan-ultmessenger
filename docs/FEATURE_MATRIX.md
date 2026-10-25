# Ultgram — master feature inventory

**Reviewed 2026-10-25.** This is the source of truth for what Yooshastan ULT Messenger ("Ultgram") has delivered, what is incomplete, and which requests still need feasibility or source-license review. The entries below represent **Ultgram implementation status**, not features automatically inherited from Nullgram, Nekogram, Goregram, or Plus Messenger.

**Status legend:** **CI shipped** = source and Android build available, but not necessarily field-validated; **device-tested** = user reported successful device testing; **partial** = foundation exists but requested scope is incomplete; **planned** = not implemented; **research** = feasibility, permission, license or platform limitation needs validation.

## Existing foundations and work needing confirmation

| Feature / inspiration | Status | Shipped scope | Still needed |
| --- | --- | --- | --- |
| Native Telegram login, chats, media, calls (official client / Mercurygram base) | **Device-tested baseline** | Pinned Telegram-compatible Android client; account login and messaging tested | Regressions on Pixel 8a, GrapheneOS and Samsung; upstream security updates |
| Google-free FOSS ARM64 build | **CI shipped + previous device tests** | FOSS flavor, optional-Google architecture, GitHub Actions APK artifact | Release signing, long-running notification/battery tests, distribution |
| Firebase-compatible build | **Partial** | Optional Firebase flavor and CI job supported by scripts | Supply compatible Firebase project config/secret, actually compile and test; current success can mean a **skipped** build |
| Nullgram-inspired appearance | **A36 fixed presets device-tested; Alpha 0.10b.1 manual day/night toggle device-tested** | Midnight, Candy Pink, Neon Stage, Aurora, Obsidian and Minimal; original paired light/dark palettes, optional fixed mode, System restore | All six palettes reported working in day/night; still verify automatic OS following/restart on both phones; cohesive headers, typography, spacing, accessibility and theme previews |
| Original Ultgram launcher icon | **Alpha 0.7 CI shipped** | Violet/silver folded-wing U, Android adaptive and legacy vector paths | Mask/size visual testing, branding consistency, app/notification/shortcut images |
| Chat layout options | **Device-tested foundation, Alpha 0.4** | Compact, Comfortable and Minimal Bubbles settings with original-value restore | Per-chat overrides, more spacing/navigation controls |
| Experimental native socket guard | **Alpha 0.10f.1 draft preview** | Off-by-default guard at native `ConnectionSocket`, with Ultgram Settings toggle and Tor conflict warning | **Not leak-proof or fully fail-closed;** validate with PCAPdroid/root/gateway outage tests. No Java/HTTP/DNS/WebView/media enforcement; DoQ and tracker filtering pending. |
| Ultgram Settings hub | **Alpha 0.10f.0 draft preview** | One ULT entry in main Settings with links to ULT theme presets, account-specific chat-view picker, proxy diagnostics and external-link consent; preserves Mercurygram Settings | Device theme/navigation tests; continue consolidating per-chat controls; no new network security enforcement yet |
| Proxy manager / Nekogram-like controls | **Device-tested Alpha 0.3; Alpha 0.10e draft preview** | Native SOCKS5/MTProto support with named profiles, read-only diagnostics, Alpha 0.10d redacted status, and optional Alpha 0.10e confirmation before ordinary external HTTP(S) link taps | **No native fail-closed or DNS protection**; auto-loaded external content not blocked; device-test prompt, research strict routing/DoQ, Tor and media paths |
| Chat-level tools | **Device-tested foundation, Alpha 0.5** | Copy dialog/chat/user/topic IDs and public links | More optional metadata/context actions; privacy-safe clipboard behavior |
| Message-level tools | **Alpha 0.6 CI shipped** | Copy message ID and sender peer ID from long-press menu | Field regression across imported/secret/forum/channel messages |
| Archive-free Chats view | **Alpha 0.8 CI shipped, device verification pending** | Per-account Hide Archive row and a separate Open Archive menu; no data moves | Verify on-device toggle, restart, account isolation, reorder, search and notification behavior |
| No advertising/tracking added by Ultgram | **Project policy; audit ongoing** | No new Ultgram ad SDK or analytics introduced by these features | Audit dependencies/telemetry and distinguish Telegram-sponsored content from app-added ads |

## Remaining feature backlog

| Priority | Feature | Influence / desired result | Scope and acceptance criteria | Proposed milestone |
| --- | --- | --- | --- | --- |
| **P0** | Per-chat automatic translation | Nekogram-like convenience | **Alpha 0.9.1 core flow device-tested:** ULT per-chat language/start/stop menu, remote translation consent, strict offline provider failure with no HTTP fallback, secret-chat gate. Still verify Pixel/GrapheneOS behavior, installable offline provider, inherited translation controls, other languages and caching | **Alpha 0.9.1 core flow tested; offline/secret-chat QA pending** |
| **P0** | Tabs, chat categories and advanced sorting | Plus Messenger / Nullgram | Sort unread/pinned/recent, folders, separate groups/channels/bots, optional hidden tabs; never reorder authoritative Telegram account data merely to change presentation | **Alpha 0.10a presets preview; swipe tabs still pending** |
| **P0** | Proxy diagnostics and connection controls | Nekogram / privacy focus | Alpha 0.10c read-only active-profile/connection/probe diagnostics; later native proxy failover policy, trust explanation, opt-in strict switch and persistent state | **Alpha 0.10** |
| **P0** | DNS privacy / DoQ audit | Requested QUIC DNS | Inventory every DNS path (app Java/native MTProto, proxy and media); evaluate DoQ (RFC 9250), DoH3 and strict no-plaintext-fallback mode only when routing can actually be enforced | **Alpha 0.10 / research** |
| **P0** | Local identity masking | Privacy goal | Mask account name/number/avatar in screen/UI/screenshot modes, redact sensitive logs, optionally strip safe outgoing media metadata; do not claim to spoof Telegram sender identities | **Alpha 0.11** |
| **P1** | Rich creator/channel tools | Premium-style convenience | Channel post/caption drafts, templates, scheduling shortcuts, media formatting and cross-post preview; respect account roles and Telegram-side limits | **Alpha 0.12** |
| **P1** | Improved story composer | Requested stories benefits | Drafts, templates, formatting/presets, client-side preview, permission/status notices; honor stories.canSendStory and server/Premium limits | **Alpha 0.12** |
| **P1** | More local Premium-like features | Goregram-inspired conveniences | Custom icon switcher, enhanced navigation, editor tools, local download-management UX, theme options; explicitly separate local UI from server entitlements | **Alpha 0.12+** |
| **P1** | Expanded multi-account tools | Goregram-inspired | Convenient switching, local identity aliases, account-order/layout controls; research resource/security impact before raising account limits | **Later** |
| **P1** | Message and media utilities | Nekogram / Goregram inspiration | Additional context actions, save-as/export for content user may save, link handling, player/file viewer choices, display options; review content protection and attribution rules before copying fork patches | **Later** |
| **P1** | More Nullgram visual polish | Nullgram design inspiration | **Alpha 0.10b.1 device-tested:** all six ULT day/night palettes. Still needed: chat list density, emoji/typography, navigation, accessibility, theme previews and editable colors; use original assets | **Iterative** |
| **P2** | Built-in SMS/MMS manager | Unified-messenger ambition | Opt-in SMS/MMS UI; request **Android default SMS role** before write/receive integration; consent, contact handling, privacy separation from Telegram | **Research / separate companion module** |
| **P2** | RCS companion (hybrid approach) | Preserve native RCS conversations | Use supported Android handoff/intents and optional companion entry points; do not promise access to Google Messages private RCS database or root-based entitlement bypass | **Research** |
| **P2** | Group join verification assistance | Faster onboarding | Show verification instructions, supported standard Android Autofill, accessible prompts; **do not automatically solve CAPTCHAs or bypass anti-spam challenges** | **Research** |
| **P2** | Privacy-oriented media sending | Identity masking/privacy | Safe EXIF/location removal before optional uploads, inspect metadata preview, preserve source files; make opt-in and document quality loss | **Later** |
| **P2** | Optional microG compatibility | GrapheneOS/Google-free | Work if microG is installed but **never require it** for FOSS variant; Firebase/push-provider behavior must be tested separately | **QA track** |
| **P0 for distribution** | Signed releases and app stores | Yooshastan Studios | Stable release key, versioned GitHub Releases, privacy disclosures, build reproducibility, F-Droid feasibility/source inclusion, public download page | **Beta readiness** |
| **P0 for safety** | Security/quality regression | All forks | Upstream merge cadence, native build and device smoke tests, transport/proxy tests, accessibility, safe secret-chat translation, no logging of private content, license/attribution audit | **Every milestone** |

## Explicit product boundaries

- **Premium / stories / channels:** Client features such as custom icons and editor layouts can be free. Telegram account Premium flags, some account/channel limits, story eligibility and certain posting operations are checked by Telegram servers; no client can legitimately promise to unlock all of them. References: https://core.telegram.org/api/premium and https://core.telegram.org/api/stories.
- **Ads:** Ultgram will not insert ads. Removing Telegram-sponsored messages is a different policy/technical question, not an implemented guarantee.
- **Identity / anonymity:** A proxy hides some network endpoints from each other but does not remove the account identity Telegram/peers see. "Mask" will mean local UI/screenshot protection unless explicitly shown otherwise.
- **QUIC DNS:** DoQ is a real protocol (https://www.rfc-editor.org/rfc/rfc9250), but setting a DoQ resolver cannot claim leak protection until native Telegram DNS and proxy resolution paths are audited.
- **SMS/RCS:** Android grants SMS-provider write and incoming SMS/MMS routing to the default SMS app (https://developer.android.com/reference/android/provider/Telephony). Google's published RCS for Business APIs serve business agents, not arbitrary access to personal Google Messages histories (https://developers.google.com/business-communications/rcs-business-messaging/guides/get-started/how-it-works).
- **Content protection:** Some Goregram features bypass media copy restrictions. Do not silently import them; explicitly review permission, privacy, trust and Telegram terms before a decision.
- **Verification:** Supporting native Autofill and help is in scope; automatic CAPTCHA solving/anti-bot bypass is not.
- **Translation:** Offline/on-device is preferred, but do not assume a Google SDK works on Google-free devices or that an offline model is bundled. Opt-in remote engines must disclose which content would leave the device.
- **Distribution:** Review GPL-2.0+ source obligations and licenses of all third-party assets/patches. F-Droid review/buildability may require separate work.

## Inspiration and license review queue

These are candidate source references for **selective, audited feature design**, not code already imported:

1. Nullgram: https://github.com/qwq233/Nullgram (appearance/navigation).
2. Nekogram: https://github.com/Nekogram/Nekogram (message tools and UX).
3. Goregram: https://github.com/AmanoTeam/Goregram (client-side conveniences; review protected-content behavior).
4. Plus Messenger: https://github.com/cchamm/Plus-Messenger (historical code reference; verify revision/maintenance and licensing before porting).
5. Upstream Telegram Android: https://github.com/DrKLO/Telegram (keep API/security behavior current).

For an actual port, record **exact immutable revision, files, license, modified code, build result, and device verification**. Reimplement UX when copying creates unsafe forks or merge conflicts.

## Release gates

- **Appearance update:** Fixed ULT themes were reported working on Samsung A36 (theme switching, persistence and dark-mode interaction); Pixel 8a also improved. Initial Alpha 0.10b paired build ran on-device but its in-app Night Mode button fell back to stock themes. Alpha 0.10b.1 updates/restores native sun/moon targets and the user confirmed all six ULT palettes display in day and night mode. OS-following and original-setting restoration still need independent QA.

- **Current review stack:** Alpha 0.6 and 0.7 still need clear device sign-off; Alpha 0.8 FOSS CI produced an APK, but archive-specific field tests have not been explicitly confirmed.
- **Alpha 0.9 progress (device reported working):** Guarded patch includes per-chat controls and strict offline-only dispatch. FOSS APK and English per-chat translation on a device were reported successful; local offline provider and secret-chat QA remain release gates; provider availability must be disclosed. The inherited Mercurygram translation bar still has its own cloud settings and entry points.
- **Beta goal:** Signed, repeatably buildable FOSS release with a feature matrix matching actual behavior, permission review, and a tested download link.

### Alpha 0.10c proxy/DNS gate

The first preview adds **read-only diagnostics**, not a new resolver or transport. Existing native proxy check state is available but does not prove any DNS/media path is tunneled. Before adding DoQ, a kill switch, an active network checker, or anonymity claims, complete the [transport/DNS audit](PROXY_DNS_AUDIT.md) and real-device leak/failover tests.
