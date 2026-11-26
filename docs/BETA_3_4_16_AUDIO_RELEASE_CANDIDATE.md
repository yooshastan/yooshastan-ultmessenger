# Beta 3.4.16 audio release candidate

VersionCode 7183032. Replaces manual audio preview with the same confirmed
download, local save and native Telegram handoff used by manual video.
M4A/MP3/Opus use Telegram's document/audio preparation API. Duplicate keys
distinguish video and each audio format; same-format duplicate handoffs
are suppressed for the current process. Automation remains video-only
for chat posting; automatic audio mode still saves locally. Existing
sender, account, network, permissions and strict proxy gates remain.
The 100 MiB upload limit remains. Private upload file lifetime and
confirmed delivery tracking are not solved by this change. The download
counter is a local-save count, not a delivery count.

Retest audio with fresh links and automation OFF, video regression,
same-format duplicate prevention, group permissions and strict proxy.
Do not publish a debug or unsigned APK; follow the prior release checklist
in BETA_3_4_15_RELEASE_CANDIDATE.md with the updated version metadata.
