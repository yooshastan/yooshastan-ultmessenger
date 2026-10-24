# Phase 2 — Nullgram-inspired ULT appearance

This is a staged feature for Yooshastan ULT Messenger Alpha 0.1.

## Built on the appearance branch
- Five immutable palette presets with safe fallback to SYSTEM.
- Stable enum names saved in Android SharedPreferences.
- A guarded source integration script that adds **ULT Presets** to Chat Settings' overflow menu in the pinned Mercurygram Android source.
- A native single-choice dialog displaying the five presets and persisting selection.
- Pure-Java preset tests and Python integration smoke tests.

## Integration steps after the Android baseline builds
1. Fetch the pinned Mercurygram Android tree using the bootstrap branch helper.
2. From the combined repository checkout, run `python3 scripts/integrate-appearance.py .work/mercurygram` (replace the destination with the actual checked-out Android source path).
3. Compile the Android debug APK and verify that **Settings → Chat Settings → ⋮ → ULT Presets** displays the choices.
4. Connect saved selection to Telegram's theme engine for actual app-wide color application.
5. Test switching and persistence across restart, system light/dark transitions, OLED contrast, RTL, emoji, chat media, and calls.

## Presets
| Preset | Accent | Background | Mode |
|---|---|---|---|
| System | #1976D2 | #F5F6FA | Follow system |
| ULT Midnight | #E86BCB | #171327 | Dark |
| Candy Pink | #E3499B | #FFF0FA | Light |
| Neon Stage | #5DF1E6 | #11182A | Dark |
| Minimal | #727B8A | #F7F8FA | Light |

**Current limitation:** The dialog saves a preference but does not yet recolor Telegram. It is not included in the Alpha APK workflow until the appearance and bootstrap branches are integrated and tested. The injection script fails if upstream anchors change, rather than silently patching an incompatible version. Preserve upstream GPL license and attribution.
