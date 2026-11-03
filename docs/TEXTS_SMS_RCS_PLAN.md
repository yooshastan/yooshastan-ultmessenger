# Ultgram Texts — local SMS/MMS folder and RCS handoff

**Status:** engineering foundation only. Not in the installable beta. No SMS
permissions, default-handler registration, SMS provider access, RCS parsing,
or newly visible folder is enabled by this branch.

## The product we are building

- **Texts (SMS/MMS)** will be a distinct local device folder in Ultgram's
  chat navigation, separate from Telegram dialogs, Telegram cloud folders,
  Telegram accounts, and proxy routing.
- It will show device phone-number threads ordered by most recent message,
  unread status, search and conversation view; support normal SMS send,
  receive and delivery feedback; provide robust MMS receive/send, notifications,
  multi-SIM selection, attachments and drafts.
- SMS/MMS threads must remain local; never mirror their content into Telegram,
  translated messages, proxy reports, analytics, or cloud storage.
- **RCS** cannot be read/sent through the ordinary Android SMS/MMS provider.
  On Android 16, a normal third-party Ultgram build has no general RCS
  conversation API. The near-term user-facing action will explicitly hand off
  to the currently installed RCS-capable messaging app; not claim that it is
  Ultgram's RCS folder, nor attempt accessibility/screen-scraping interception.
- Android API 37's AlternativeMessageTransportService (AMTS) is a transport
  delegation hook, not an RCS backend. It does not provide a general bridge
  to Google Messages. Investigate only after official support and testing.

## Hard requirements before enabling the folder

1. Implement Android default SMS role eligibility: SENDTO for SMS/MMS,
   SMS_DELIVER broadcast receiver, WAP_PUSH_DELIVER receiver, and
   RESPOND_VIA_MESSAGE service. All expected components, providers, and
   Android lifecycle behaviors must work, not placeholders.
2. Implement full SMS/MMS provider write/read paths, sending, delivery receipts,
   incoming notifications, attachment handling, outgoing drafts and retries.
3. Only after this capability works, request `RoleManager.ROLE_SMS` through
   user action; then, and only then, request the required SMS/MMS permissions.
   The user can keep their existing default SMS app. A decline leaves Ultgram
   as an ordinary Telegram client, with no SMS access.
4. Gate every SMS-provider read, broadcast-processing action, or send on the
   active/default role plus the actual permissions. Immediately stop accessing
   provider data when the role is lost; clear volatile views.
5. Check phone SIM/subscription handling and restore existing SMS/MMS client
   when Ultgram is not ready. No automatic role switching or permission prompts.
6. Design a separate local Texts folder. Don't add SMS rows to TLRPC.Dialog,
   invent Telegram dialog IDs, or sync SMS to Telegram cloud storage.
7. Add a visible RCS handoff and honest indication of which app manages RCS.
   Do not imply Telegram strict proxy protects SMS/MMS or external RCS traffic.
8. Audit SMS permissions against store policies, include an accurate privacy
   statement and prominent opt-in UI, and review GrapheneOS/Android 16 behavior.

## Staging / acceptance gates

- **Foundation (this branch):** local grouping model, role/permission readiness
  policy, unit tests and architecture. Nothing installed in the APK; no new
  dangerous permissions.
- **SMS/MMS developer preview:** Android default-handler-compliant component,
  native inbox and conversation navigation, sending/receiving/MMS/notifications
  and role-loss handling; tests on spare test device or SIM.
- **Private beta:** explicit opt-in, full regression on Pixel 8a GrapheneOS and
  Samsung A36, permissions and multi-SIM tests, verified SMS/MMS delivery,
  end-to-end fallback to the user's old messages app, no Telegram regression.
- **RCS:** external-app handoff on those Android builds; not direct management
  unless a truly supported, auditable third-party API emerges.

## Test cases

- Zero telephony, declined opt-in, role absent, permission absent, and incomplete
  MMS support must all return non-ready.
- A "ready" status must require actual default role, all SMS/MMS provider
  conditions, and opt-in.
- Duplicate provider messages, chronological sort, unread counts, draft flows,
  and separate personal/work SIM handling require tests in the next phase.
- No SMS/MMS text in debug logs, clipboard exports without user action,
  translator pipelines, Telegram message-store queries, or proxy reports.
- Message receive/send when Ultgram is default, and no interception or read
  after role revocation.
- SMS messages sent over the carrier are not end-to-end encrypted by Ultgram;
  RCS encryption depends on the separate RCS provider, not Ultgram.
- No installable public artifact until a working, policy-compliant default
  handler exists. A button that merely opens an external app is not a completed
  in-app inbox.
