package com.example.ui.components

import android.os.Build
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest

/** Requests no voice key from LatinIME/Gboard. These are IME hints, not an OS ban. */
internal fun EditorInfo.requestNoVoiceInput() {
    val options = privateImeOptions.orEmpty().split(',').filter { it.isNotBlank() }.toMutableSet()
    options.addAll(listOf(
        "nm",
        "com.google.android.inputmethod.latin.noMicrophoneKey",
        "com.android.inputmethod.latin.noMicrophoneKey"
    ))
    privateImeOptions = options.joinToString(",")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
    }
}

internal fun isClipboardShortcut(event: KeyEvent): Boolean = when (event.keyCode) {
    KeyEvent.KEYCODE_COPY, KeyEvent.KEYCODE_CUT, KeyEvent.KEYCODE_PASTE -> true
    KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_V -> event.isCtrlPressed || event.isMetaPressed
    KeyEvent.KEYCODE_INSERT -> event.isCtrlPressed || event.isMetaPressed || event.isShiftPressed
    KeyEvent.KEYCODE_FORWARD_DEL -> event.isShiftPressed
    else -> false
}

/** Blocks explicit transfer commands even when the IME sends them directly. */
internal class ManualEntryInputConnection(target: InputConnection) : InputConnectionWrapper(target, false) {
    override fun performContextMenuAction(id: Int): Boolean = when (id) {
        android.R.id.copy, android.R.id.cut, android.R.id.paste, android.R.id.pasteAsPlainText -> false
        else -> super.performContextMenuAction(id)
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean =
        if (isClipboardShortcut(event)) true else super.sendKeyEvent(event)

    override fun commitCompletion(text: CompletionInfo?): Boolean = false
    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean = false
}

/** Applies to every Compose text input, including fields in dialog windows. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TextInputProtection(
    manualEntry: Boolean = false,
    content: @Composable () -> Unit
) {
    val interceptor = remember(manualEntry) {
        PlatformTextInputInterceptor { request, next ->
            next.startInputMethod(PlatformTextInputMethodRequest { info ->
                val connection = request.createInputConnection(info)
                info.requestNoVoiceInput()
                if (manualEntry) {
                    if (info.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT) {
                        info.inputType = (info.inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT.inv()) or
                            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    }
                    ManualEntryInputConnection(connection)
                } else {
                    connection
                }
            })
        }
    }
    InterceptPlatformTextInput(interceptor, content)
}
