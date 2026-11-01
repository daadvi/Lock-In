package com.example.util

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * Empty TextToolbar that suppresses the context menu (Copy, Paste, Cut, Select All)
 * to strictly prevent bypassing lock rituals through copy-paste.
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
 * Validates text input changes to detect clipboard paste attempts.
 * Returns true if the change looks like normal manual typing (1 or 0 character delta).
 */
fun isManualTyping(oldText: String, newText: String, maxBurst: Int = 1): Boolean {
    val delta = newText.length - oldText.length
    return delta <= maxBurst
}
