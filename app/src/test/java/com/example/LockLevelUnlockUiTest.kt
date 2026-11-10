package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.LockLevel
import com.example.service.EmergencyUnlockManager
import com.example.ui.components.AbsoluteInstallBlockDialog
import com.example.ui.components.EmergencyUnlockScreen
import com.example.ui.components.HighFrictionChallengeType
import com.example.ui.components.HighFrictionSecurityChallengeFlow
import com.example.ui.screens.TrustUnlockScreen
import com.example.ui.theme.LockInTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercises the actual fields and submit actions for every saved lock level. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LockLevelUnlockUiTest(private val level: LockLevel, private val repetitions: Int) {
    @get:Rule val compose = createComposeRule()
    private val password = "Ab1!"
    private val prefs get() = LockInApplication.instance.userPreferences

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}: {1} passwords")
        fun levels() = listOf(
            arrayOf<Any>(LockLevel.BASIC, 1),
            arrayOf<Any>(LockLevel.DETERMINED, 3),
            arrayOf<Any>(LockLevel.DEV_DREAM, 5)
        )
    }

    @Before fun saveLevelAndPassword() {
        prefs.setLockLevel(level)
        prefs.updatePassword(password)
    }

    private fun typeManually(field: SemanticsNodeInteraction, text: String) {
        if (hasAnyAncestor(hasScrollAction()).matches(field.fetchSemanticsNode())) field.performScrollTo()
        text.forEach { field.performTextInput(it.toString()) }
    }

    /** A failed attempt after partial progress must require the entire sequence again. */
    private fun passwordSequence(
        field: SemanticsNodeInteraction,
        submit: () -> Unit = { field.performImeAction() },
        assertIncomplete: () -> Unit
    ) {
        repeat(repetitions - 1) {
            typeManually(field, password)
            submit()
            assertIncomplete()
        }
        typeManually(field, "Wrong!")
        submit()
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        assertIncomplete()
        repeat(repetitions) { index ->
            typeManually(field, password)
            // Exercise both IME submission and the visible button where provided.
            submit()
            if (index < repetitions - 1) assertIncomplete()
        }
    }

    @Test fun savedLevelKeepsItsRepetitionCountAfterRepositoryRecreation() {
        val reloaded = UserPreferencesRepository(LockInApplication.instance)
        assertEquals(level, reloaded.getLockLevel())
        assertEquals(repetitions, reloaded.getLockLevel().passwordRepeatCount)
    }

    @Test fun emergencyUnlockRequiresExactlyTheSavedLevelCount() {
        var unlocked = false
        compose.setContent {
            LockInTheme { EmergencyUnlockScreen({}, { unlocked = true }) }
        }
        val confirm = compose.onNodeWithTag("emergency_unlock_confirm_button")
        typeManually(compose.onNodeWithTag("emergency_unlock_phrase_input"), EmergencyUnlockManager.CONFIRMATION_PHRASE.uppercase(java.util.Locale.ROOT))
        confirm.assertIsNotEnabled()
        val field = compose.onNodeWithTag("emergency_unlock_password_input")
        passwordSequence(field, assertIncomplete = { confirm.assertIsNotEnabled() })
        field.assertDoesNotExist()
        confirm.performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(unlocked) }
    }

    @Test fun installationUnlockRequiresExactlyTheSavedLevelCount() =
        verifyHighFriction(HighFrictionChallengeType.UNBLOCK_INSTALLATION)

    @Test fun uninstallRequiresExactlyTheSavedLevelCount() =
        verifyHighFriction(HighFrictionChallengeType.UNINSTALL_LOCK_IN)

    @Test fun administratorDeactivationRequiresExactlyTheSavedLevelCount() =
        verifyHighFriction(HighFrictionChallengeType.DEACTIVATE_DEVICE_ADMIN)

    private fun verifyHighFriction(type: HighFrictionChallengeType) {
        var completed = false
        compose.setContent {
            LockInTheme {
                HighFrictionSecurityChallengeFlow(type, correctPassword = prefs.getTrustPassword(),
                    accentColor = Color.Red, onCancel = {}, onSuccess = { completed = true })
            }
        }
        val field = compose.onNodeWithTag("challenge_password_input")
        val phrase = compose.onNodeWithTag("stage2_phrase_input")
        phrase.assertIsNotEnabled()
        passwordSequence(field, submit = {
            compose.onNodeWithTag("verify_password_button").performScrollTo().performClick()
        }, assertIncomplete = { phrase.assertIsNotEnabled() })
        field.assertDoesNotExist()
        phrase.assertIsEnabled()
        compose.runOnIdle { assertFalse(completed) } // The required phrases must still be completed.
        // A wrong confirmation phrase must also reset the password stage.
        typeManually(phrase, "Esta frase está totalmente errada")
        compose.onNodeWithTag("verify_stage2_button").performScrollTo().performClick()
        phrase.assertIsNotEnabled()
        repeat(repetitions) { index ->
            typeManually(field, password)
            field.performImeAction()
            if (index < repetitions - 1) phrase.assertIsNotEnabled()
        }
        phrase.assertIsEnabled()
        val target = compose.onNodeWithTag("stage2_phrase_target").fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text.trim('\'')
        typeManually(phrase, target.uppercase(java.util.Locale.ROOT))
        compose.onNodeWithTag("verify_stage2_button").performScrollTo().performClick()
        if (type != HighFrictionChallengeType.UNBLOCK_INSTALLATION) {
            compose.mainClock.advanceTimeBy(3000)
            val finalTarget = compose.onNodeWithTag("stage4_phrase_target").fetchSemanticsNode()
                .config[SemanticsProperties.Text].single().text.trim('"')
            typeManually(compose.onNodeWithTag("stage4_phrase_input"), finalTarget.lowercase(java.util.Locale.ROOT))
            compose.onNodeWithTag("final_confirm_action_button").performScrollTo().performClick()
        }
        compose.runOnIdle { assertTrue(completed) }
    }

    @Test fun absoluteInstallationBlockRequiresExactlyTheSavedLevelCount() {
        compose.setContent {
            LockInTheme { AbsoluteInstallBlockDialog(false, {}, {}) }
        }
        val field = compose.onNode(hasSetTextAction())
        val nextStage = compose.onNodeWithText("ETAPA 2: DIGITE A FRASE MOTIVACIONAL COMPLETA")
        passwordSequence(field, assertIncomplete = { nextStage.assertDoesNotExist() })
        nextStage.assertExists()
        compose.onNodeWithText("Digite a frase acima").assertExists()
    }

    @Test fun appUnlockUsesSavedLevelAndPreservesMinorPhraseTypoTolerance() {
        var unlocked = false
        compose.setContent {
            LockInTheme {
                TrustUnlockScreen(prefs.getTrustPassword(), targetAppName = "App de teste",
                    onUnlockConfirmed = { unlocked = true }, onBack = {})
            }
        }
        val field = compose.onNodeWithTag("trust_unlock_password_input")
        val finalButton = compose.onNodeWithText("Confirmar Frase e Desbloquear")
        passwordSequence(field, assertIncomplete = { finalButton.assertDoesNotExist() })
        field.assertDoesNotExist()
        finalButton.assertExists()
        compose.runOnIdle { assertFalse(unlocked) }
        val phraseWithMinorTypo = level.requiredPhrase.replaceFirst("desbloquear", "desbloqiear")
            .replaceFirst("responsável", "responsavel")
        typeManually(compose.onNodeWithTag("trust_unlock_phrase_input"), phraseWithMinorTypo.uppercase(java.util.Locale.ROOT))
        finalButton.performClick()
        compose.runOnIdle { assertTrue(unlocked) }
    }
}
