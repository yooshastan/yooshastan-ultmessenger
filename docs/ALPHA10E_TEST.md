# Ultgram Alpha 0.10e — external link confirmation preview

Alpha 0.10e is an **opt-in link-tap guard**, not a full privacy firewall.
It does not change Telegram's MTProto transport, proxy settings, credential
storage, DNS, automatic CDN/media requests, or in-app WebView resource loads.

## Enable and verify

1. Install the **FOSS ARM64 Alpha 0.10e preview** from the successful Actions
   artifact. Keep the working MTProto proxy configured as before.
2. Open **ULT Proxy Manager → info icon → Link guard settings**. Verify the
   initial state is **Off**. Tap **Enable**.
3. Tap an ordinary HTTPS link in a chat. A dialog headed **Open external
   website?** should appear before the browser or Custom Tab. Tap **Cancel**
   and verify nothing opens.
4. Tap the same link again and select **Open once**. It should open normally,
   without changing the MTProto proxy profile. On the next tap, the prompt
   should appear again.
5. Open a native Telegram chat/invite link. It should use the normal app
   behavior, without an HTTP(S) external-site warning.
6. Reopen **Link guard settings**, tap **Disable**, then verify external links
   open through the original upstream path.
7. Inspect **ULT Connection Diagnostics**. External-link confirmation state
   should be visible separately from native Telegram proxy reachability,
   strict fail-closed, and unverified DNS routing.

## Optional isolated observation

- With PCAPdroid's **app filter set to only Ultgram**, leave the MTProto
  proxy enabled throughout a capture. Test channel loading, downloads, and
  messages, noting precisely what actions you took.
- External-site traffic may still use direct network connections when you
  explicitly approve links. Automatic media and preview requests are not
  covered by this link-tap feature.
- Verify the proxy is actually used by inspecting **sustained application
  data**, not a single TCP SYN or a UI connection indicator. PCAPdroid's
  local VPN representation can contain synthetic TCP headers.
- The absence of direct Telegram application payload in one capture does
  **not** demonstrate fail-closed behavior under a proxy outage. Never call
  this feature a VPN, DNS leak protection, general-purpose ad blocker, or
  proof of anonymity.

## Release gates for eventual strict proxy mode

- Native Android/C++ connection-path audit for DNS, proxy outage,
  reconnection, alternative transports, startup, media, and file downloads.
- Confirm intentional proxy selection cannot silently become direct Telegram
  transport, including across process death and settings changes.
- Automated end-to-end checks with a **forced proxy outage** and independent
  gateway-side observation. Security claims only after those checks pass.
- Separate consideration of embedded web content and Android VPN/Tailscale/
  PCAPdroid compatibility; a basic MTProto proxy is not a web proxy.

Do not include actual proxy secrets, full source IP addresses, PCAPs, account
identifiers or unrelated browsing destinations in public bug reports.
