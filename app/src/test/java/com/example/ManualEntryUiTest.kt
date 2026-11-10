package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.ManualEntryTextField
import com.example.ui.components.EmergencyUnlockScreen
import com.example.data.local.UserPreferencesRepository
import com.example.service.EmergencyUnlockManager
import com.example.ui.theme.LockInTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class ManualEntryUiTest {
    @get:Rule val compose = createComposeRule()
    private var value by mutableStateOf("")
    private lateinit var editorView: View
    private val clipboard: ClipboardManager
        get() = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private fun showField(initialValue: String = "", inDialog: Boolean = false, password: Boolean = false) {
        value = initialValue
        compose.setContent {
            LockInTheme {
                val field: @androidx.compose.runtime.Composable () -> Unit = {
                    editorView = LocalView.current
                    ManualEntryTextField(value, { value = it }, Modifier.testTag("field"),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ))
                }
                if (inDialog) {
                    Dialog(onDismissRequest = {}) {
                        Box(Modifier.size(320.dp, 160.dp)) { field() }
                    }
                } else field()
            }
        }
        compose.onNodeWithTag("field").performClick()
    }

    private fun inputConnection(info: EditorInfo = EditorInfo()): InputConnection {
        lateinit var connection: InputConnection
        compose.runOnIdle {
            connection = checkNotNull(editorView.onCreateInputConnection(info))
        }
        return connection
    }

    @Test fun rejectsPastedAndDictatedPhrasesIncludingReplacementOfSelection() {
        showField("texto anterior longo")
        val field = compose.onNodeWithTag("field")
        field.performTextInputSelection(TextRange(0, value.length))
        field.performTextInput("colar")
        compose.runOnIdle { assertEquals("texto anterior longo", value) }
        field.performTextReplacement("outra frase inteira")
        compose.runOnIdle { assertEquals("texto anterior longo", value) }
        field.performTextClearance()
        field.performTextInput("frase por ditado")
        compose.runOnIdle { assertEquals("", value) }
    }

    @Test fun keyboardShortcutsCannotReadWriteOrCutEvenOneClipboardCharacter() {
        showField("segredo")
        compose.runOnIdle { clipboard.setPrimaryClip(ClipData.newPlainText("test", "x")) }
        val field = compose.onNodeWithTag("field")
        field.performTextInputSelection(TextRange(0, value.length))
        for (key in listOf(Key.C, Key.X, Key.V, Key.Insert)) {
            field.performKeyInput {
                keyDown(Key.CtrlLeft)
                pressKey(key)
                keyUp(Key.CtrlLeft)
            }
            compose.runOnIdle {
                assertEquals("segredo", value)
                assertEquals("x", clipboard.primaryClip!!.getItemAt(0).text.toString())
            }
        }
        field.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Insert)
            pressKey(Key.Delete)
            keyUp(Key.ShiftLeft)
        }
        compose.runOnIdle { assertEquals("segredo", value) }
    }

    @Test fun imeContextMenuCannotCopyCutOrPasteAndConnectionStillTypes() {
        showField("segredo", inDialog = true)
        compose.onNodeWithTag("field").performTextInputSelection(TextRange(0, value.length))
        compose.runOnIdle { clipboard.setPrimaryClip(ClipData.newPlainText("test", "x")) }
        val connection = inputConnection()
        compose.runOnIdle {
            for (id in listOf(android.R.id.copy, android.R.id.cut, android.R.id.paste, android.R.id.pasteAsPlainText)) {
                assertFalse(connection.performContextMenuAction(id))
            }
            assertEquals("x", clipboard.primaryClip!!.getItemAt(0).text.toString())
            assertEquals("segredo", value)
            connection.commitText("a", 1)
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals("a", value) }
    }

    @Test fun normalComposingTextAccentsAndDoneStillWork() {
        var done = false
        compose.setContent {
            LockInTheme {
                editorView = LocalView.current
                ManualEntryTextField(value, { value = it }, Modifier.testTag("field"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { done = true }))
            }
        }
        compose.onNodeWithTag("field").performClick()
        val connection = inputConnection()
        for (text in listOf("n", "na", "nã", "não")) {
            compose.runOnIdle { connection.setComposingText(text, 1) }
            compose.waitForIdle()
        }
        compose.runOnIdle { connection.commitText("não", 1) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals("não", value) }
        compose.onNodeWithTag("field").performImeAction()
        compose.runOnIdle { assertTrue(done) }
        compose.onNodeWithTag("field").performKeyInput { pressKey(Key.Backspace) }
        compose.runOnIdle { assertEquals("nã", value) }
    }

    @Test fun dialogPasswordKeepsPasswordTypeAndDisablesSuggestionsAndVoiceHints() {
        showField(inDialog = true, password = true)
        val info = EditorInfo()
        inputConnection(info)
        assertEquals(InputType.TYPE_TEXT_VARIATION_PASSWORD, info.inputType and InputType.TYPE_MASK_VARIATION)
        assertTrue(info.inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0)
        assertEquals(EditorInfo.IME_ACTION_DONE, info.imeOptions and EditorInfo.IME_MASK_ACTION)
        assertTrue(info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0)
        assertTrue(info.privateImeOptions.contains("com.google.android.inputmethod.latin.noMicrophoneKey"))
    }

    @Test fun ordinaryEmailFieldAlsoRequestsNoVoiceWithoutChangingEmailLayoutOrPaste() {
        compose.setContent {
            LockInTheme {
                editorView = LocalView.current
                OutlinedTextField(value, { value = it }, Modifier.testTag("field"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            }
        }
        compose.onNodeWithTag("field").performClick()
        val info = EditorInfo()
        inputConnection(info)
        assertEquals(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, info.inputType and InputType.TYPE_MASK_VARIATION)
        assertTrue(info.privateImeOptions.contains("com.google.android.inputmethod.latin.noMicrophoneKey"))
        compose.onNodeWithTag("field").performTextInput("teste@example.com")
        compose.runOnIdle { assertEquals("teste@example.com", value) }
    }

    @Test fun emergencyUnlockRejectsBulkInputButAllFiveManualPasswordStepsStillComplete() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        UserPreferencesRepository(context).updatePassword("Ab1!")
        UserPreferencesRepository(context).setLockLevel(com.example.data.model.LockLevel.DEV_DREAM)
        compose.setContent {
            LockInTheme { EmergencyUnlockScreen(onDismiss = {}, onUnlockConfirmed = {}) }
        }
        val password = compose.onNodeWithTag("emergency_unlock_password_input")
        password.performScrollTo().performTextInput("Ab1!")
        password.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        repeat(5) {
            password.performScrollTo()
            "Ab1!".forEach { character -> password.performTextInput(character.toString()) }
            password.performImeAction()
        }
        compose.onNodeWithText("Senha confirmada 5 vezes com sucesso!").assertExists()
        val phrase = compose.onNodeWithTag("emergency_unlock_phrase_input")
        phrase.performScrollTo().performTextInput(EmergencyUnlockManager.CONFIRMATION_PHRASE)
        phrase.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        EmergencyUnlockManager.CONFIRMATION_PHRASE.forEach { character ->
            phrase.performTextInput(character.toString())
        }
        phrase.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,
            AnnotatedString(EmergencyUnlockManager.CONFIRMATION_PHRASE)))
    }
}
