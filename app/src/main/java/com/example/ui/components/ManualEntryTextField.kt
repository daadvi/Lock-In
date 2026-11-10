package com.example.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.example.util.EmptyTextToolbar
import com.example.util.isManualTyping

// Scoped to challenge fields; never read, erase, or overwrite the user's clipboard.
private object BlockedClipboard : ClipboardManager {
    override fun getText(): AnnotatedString? = null
    override fun setText(annotatedString: AnnotatedString) = Unit
}

/** Common input for all unlock passwords, phrases and confirmation codes. */
@Composable
fun ManualEntryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    var inputRejected by remember(value) { mutableStateOf(false) }
    TextInputProtection(manualEntry = true) {
        CompositionLocalProvider(
            LocalClipboardManager provides BlockedClipboard,
            LocalTextToolbar provides EmptyTextToolbar
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { candidate ->
                    inputRejected = !isManualTyping(value, candidate)
                    if (!inputRejected) onValueChange(candidate)
                },
                modifier = modifier.onPreviewKeyEvent { event ->
                    isClipboardShortcut(event.nativeKeyEvent).also { blocked ->
                        if (blocked) inputRejected = true
                    }
                },
                enabled = enabled,
                isError = isError,
                textStyle = textStyle,
                label = label,
                placeholder = placeholder,
                trailingIcon = trailingIcon,
                supportingText = if (inputRejected) {
                    { Text("Digite caractere por caractere. Colagem e inserção em bloco estão bloqueadas.") }
                } else null,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions.copy(
                    autoCorrectEnabled = false,
                    showKeyboardOnFocus = keyboardOptions.showKeyboardOnFocus,
                    hintLocales = keyboardOptions.hintLocales
                ),
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                shape = shape,
                colors = colors
            )
        }
    }
}
