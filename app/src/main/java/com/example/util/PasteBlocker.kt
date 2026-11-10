package com.example.util

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import java.text.Normalizer

/**
 * Hides the selection toolbar. Clipboard and keyboard protection are also needed;
 * hiding this menu alone does not block paste from an IME or a physical keyboard.
 */
object EmptyTextToolbar : TextToolbar {
    override val status: TextToolbarStatus get() = TextToolbarStatus.Hidden
    override fun hide() {}
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {}
}

/**
 * Allows deletion and edits that insert at most one Unicode code point.
 * Compare the changed region, not the net length: replacing a selection with a
 * shorter pasted sentence must not pass. NFC also permits Portuguese dead keys.
 * This is a bulk-input guard, not proof of the input's source: IMEs do not report
 * whether an individual character came from a key, clipboard, or dictation.
 */
fun isManualTyping(oldText: String, newText: String): Boolean {
    val old = Normalizer.normalize(oldText, Normalizer.Form.NFC)
    val new = Normalizer.normalize(newText, Normalizer.Form.NFC)
    var start = 0
    while (start < old.length && start < new.length && old[start] == new[start]) start++
    var oldEnd = old.length
    var newEnd = new.length
    while (oldEnd > start && newEnd > start && old[oldEnd - 1] == new[newEnd - 1]) {
        oldEnd--
        newEnd--
    }
    return new.codePointCount(start, newEnd) <= 1
}
