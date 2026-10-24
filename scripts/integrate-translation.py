#!/usr/bin/env python3
"""Guarded Alpha 0.9 per-chat translation and strict-offline privacy integration.

Patches pinned Mercurygram source only; no message database/transport changes.
"""
import argparse
from pathlib import Path

CHAT = Path("TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java")
DISPATCHER = Path("TMessagesProj/src/main/java/it/belloworld/mercurygram/translate/MgTranslateDispatcher.java")
MODEL = Path("translation/src/com/yooshastan/ultmessenger/translation/UltTranslationPolicy.java")
DEST = Path("TMessagesProj/src/main/java/com/yooshastan/ultmessenger/translation/UltTranslationPolicy.java")

IMPORT = "import org.telegram.ui.ActionBar.ActionBar;\n"
CLASS = "        FactorAnimator.Target\n{\n"
MENU = "            translateItem = headerItem.lazilyAddSubItem(translate, R.drawable.msg_translate, LocaleController.getString(R.string.TranslateMessage));\n"
CLICK = "                } else if (id == view_as_topics) {\n"
OFFLINE_FAILURE = """                if (SharedConfig.mg_translateAutoFallback) {
                    runAlternative(text, fromLng, toLng, aidlFailure, done);
                } else {
                    done.done(null, false, aidlFailure != null
                            ? aidlFailure
                            : MgAidlTranslate.Failure.of(MgAidlTranslate.Reason.UNEXPECTED));
                }
"""
MISSING_PROVIDER = """        // "alternative", or "offline" with no usable provider.
        // The provider-missing downgrade preserves the privacy invariant:
        // offline-mode failure routes through Alternative HTTP, never Telegram cloud.
        // In offline mode the provider itself is what is missing, so keep that
        // as the reported cause if Alternative HTTP also fails.
        final MgAidlTranslate.Failure cause = SharedConfig.MG_TRANSLATE_MODE_OFFLINE.equals(mode)
                ? MgAidlTranslate.Failure.of(MgAidlTranslate.Reason.PROVIDER_UNAVAILABLE)
                : null;
        runAlternative(text, fromLng, toLng, cause, done);
        return Outcome.HANDLED;
"""
CREDIT = "        return MgMozhiClient.engineLabel(SharedConfig.mg_translateAltEngine);\n"
MENU_ID = "ULT_TRANSLATION_MENU_ID"

CHAT_FIELDS = """
    private static final int ULT_TRANSLATION_MENU_ID = 0x554c59;

    private void ultApplyChatTranslation(String language) {
        TranslateController controller = getMessagesController().getTranslateController();
        controller.setDialogTranslateTo(getDialogId(), language);
        if (!controller.toggleTranslatingDialog(getDialogId(), true)) {
            updateTopPanel(true);
        }
    }

    private void showUltTranslationOptions() {
        final android.app.Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        final TranslateController controller = getMessagesController().getTranslateController();
        final String[] languageCodes = {"en", "ko", "ja", "es", "fr", "de", "pt", "ru", "zh", "ar", "uk"};
        final String[] languageNames = {"English", "Korean", "Japanese", "Spanish", "French",
                "German", "Portuguese", "Russian", "Chinese", "Arabic", "Ukrainian"};
        final String[] options = new String[languageNames.length + 1];
        options[0] = controller.isTranslatingDialog(getDialogId())
                ? "Stop translating this chat" : "Keep translation off";
        for (int i = 0; i < languageNames.length; i++) {
            options[i + 1] = "Translate to " + languageNames[i];
        }
        final String mode = SharedConfig.mg_translateMode;
        final boolean offline = UltTranslationPolicy.isOfflineMode(mode);
        final boolean offlineReady = it.belloworld.mercurygram.translate.MgTranslateDispatcher.isOfflineUsable();
        new android.app.AlertDialog.Builder(activity)
                .setTitle("ULT Chat Translation")
                // Android AlertDialog cannot display setMessage and setItems together:
                // keep this alert list-only so language choices are actually visible.
                .setItems(options, (selectionDialog, which) -> {
                    if (which == 0) {
                        controller.toggleTranslatingDialog(getDialogId(), false);
                        updateTopPanel(true);
                        return;
                    }
                    if (!controller.isChatTranslateEnabled()) {
                        new android.app.AlertDialog.Builder(activity)
                                .setMessage("Enable automatic chat translation in Telegram language settings first.")
                                .setPositiveButton(android.R.string.ok, null).show();
                        return;
                    }
                    if (!UltTranslationPolicy.mayTranslate(
                            DialogObject.isEncryptedDialog(getDialogId()), mode, offlineReady)) {
                        new android.app.AlertDialog.Builder(activity)
                                .setTitle("Local translation unavailable")
                                .setMessage("Secret chats require a working offline provider. Offline mode never falls back to HTTP. Select or install a local translation provider in the Mercurygram translation settings.")
                                .setPositiveButton(android.R.string.ok, null).show();
                        return;
                    }
                    final String toLanguage = languageCodes[which - 1];
                    if (UltTranslationPolicy.requiresRemoteConsent(
                            DialogObject.isEncryptedDialog(getDialogId()), mode)) {
                        new android.app.AlertDialog.Builder(activity)
                                .setTitle("Send chat text for translation?")
                                .setMessage("Selected engine: " + UltTranslationPolicy.engineLabel(mode)
                                        + ". Translating this chat sends its message text to a remote translation service. This is not end-to-end local processing. Only enable for chats where you accept that disclosure.")
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton("Allow for this chat", (confirm, clicked) ->
                                        ultApplyChatTranslation(toLanguage))
                                .show();
                    } else {
                        ultApplyChatTranslation(toLanguage);
                    }
                })
                .setNeutralButton("Engine info", (infoDialog, selected) ->
                        new android.app.AlertDialog.Builder(activity)
                                .setTitle("Translation engine")
                                .setMessage("Engine: " + UltTranslationPolicy.engineLabel(mode)
                                        + (offline ? (offlineReady ? " (ready)" : " (provider not installed)") : " (network)")
                                        + "\\nTranslation is selected per chat; stop it to view originals."
                                        + "\\nFor more target languages, use the translation bar.")
                                .setPositiveButton(android.R.string.ok, null)
                                .show())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

"""

def once(source: str, old: str, new: str) -> str:
    count = source.count(old)
    if count != 1:
        raise ValueError(f"Pinned source drift, expected one anchor (got {count}): {old[:85]!r}")
    return source.replace(old, new, 1)

def integrate(root: Path) -> tuple[Path, Path]:
    chat_file = root / CHAT
    dispatch_file = root / DISPATCHER
    chat = chat_file.read_text(encoding="utf-8")
    dispatch = dispatch_file.read_text(encoding="utf-8")
    if MENU_ID in chat or "ULT strict offline" in dispatch:
        raise ValueError("ULT translation already integrated")
    for src, anchors in (
        (chat, (IMPORT, CLASS, MENU, CLICK)),
        (dispatch, (OFFLINE_FAILURE, MISSING_PROVIDER, CREDIT)),
    ):
        for anchor in anchors:
            if src.count(anchor) != 1:
                raise ValueError(f"Pinned source drift: {anchor[:85]!r}")
    model = (Path(__file__).resolve().parents[1] / MODEL).read_bytes()

    chat = once(chat, IMPORT, IMPORT +
            "import com.yooshastan.ultmessenger.translation.UltTranslationPolicy;\n")
    chat = once(chat, CLASS, CLASS + CHAT_FIELDS)
    chat = once(chat, MENU, MENU +
            '            headerItem.lazilyAddSubItem(ULT_TRANSLATION_MENU_ID, R.drawable.msg_translate, "ULT Chat Translation");\n')
    chat = once(chat, CLICK,
            "                } else if (id == ULT_TRANSLATION_MENU_ID) {\n"
            "                    showUltTranslationOptions();\n" + CLICK)

    dispatch = once(dispatch, OFFLINE_FAILURE, """                // ULT strict offline: no HTTP or cloud downgrade on AIDL failure.
                done.done(null, false, aidlFailure != null
                        ? aidlFailure
                        : MgAidlTranslate.Failure.of(MgAidlTranslate.Reason.UNEXPECTED));
""")
    dispatch = once(dispatch, MISSING_PROVIDER, """        // ULT strict offline: a missing local provider fails closed.
        if (SharedConfig.MG_TRANSLATE_MODE_OFFLINE.equals(mode)) {
            done.done(null, false, MgAidlTranslate.Failure.of(
                    MgAidlTranslate.Reason.PROVIDER_UNAVAILABLE));
            return Outcome.HANDLED;
        }
        // Alternative HTTP is only used when explicitly selected.
        runAlternative(text, fromLng, toLng, null, done);
        return Outcome.HANDLED;
""")
    dispatch = once(dispatch, CREDIT, """        if (SharedConfig.MG_TRANSLATE_MODE_OFFLINE.equals(mode)) {
            return "Offline provider unavailable";
        }
""" + CREDIT)

    # Keep upstream documentation aligned with the strict local-only behavior.
    old_explanation = """ * <p>Privacy invariant: the {@code "offline"} mode NEVER silently
 * falls back to Telegram cloud. When the AIDL provider is missing or
 * fails, the fallback is Alternative HTTP (gated by
 * {@link SharedConfig#mg_translateAutoFallback}) — a user who picked
 * offline did not pick Telegram-mediated translation."""
    new_explanation = """ * <p>ULT strict offline: offline AIDL failures never fall back to any
 * network service. Alternative HTTP remains an explicit, separate mode."""
    if old_explanation in dispatch:
        dispatch = once(dispatch, old_explanation, new_explanation)

    target = root / DEST
    target.parent.mkdir(parents=True, exist_ok=True)
    chat_file.write_text(chat, encoding="utf-8")
    dispatch_file.write_text(dispatch, encoding="utf-8")
    target.write_bytes(model)
    return chat_file, dispatch_file

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    for updated in integrate(parser.parse_args().source_root):
        print(f"Patched {updated}")
