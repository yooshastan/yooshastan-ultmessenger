# ULT Proxy manager — implementation phases

## Scope
- Support existing Telegram SOCKS5 and MTProto transports, without a custom interception proxy or bypassing Telegram identity controls.
- Store multiple named profiles and allow explicit opt-in selection.
- Later: connection health checks, per-profile timeouts, optional failover, and one-tap restore.
- Keep MTProto secrets and SOCKS5 passwords in encrypted app-owned storage, not in exported preference files or logs.
- Proxy connections may expose metadata to the proxy operator. A proxy does not anonymize a Telegram account from Telegram itself.

## First code increment
`ProxyProfile.java` is an immutable validated endpoint model. It does **not** make connections, change Telegram routing, or persist credentials. Its test covers malformed hosts and ports.

## Planned app integration
1. Discover Telegram's existing proxy configuration model and settings activity at the pinned revision.
2. Add an opt-in **ULT Proxy Profiles** settings panel using existing transport hooks.
3. Persist non-secret metadata in app preferences and secrets in encrypted storage.
4. Add test-connection with explicit indicators; never silently route traffic to a new proxy.
5. Test on GrapheneOS, without and with Google Play/microG.
