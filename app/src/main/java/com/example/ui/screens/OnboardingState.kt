package com.example.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.data.model.LockLevel

/** Keep progress across activity recreation without putting the password in saved-state bundles. */
class OnboardingState : ViewModel() {
    val step = mutableStateOf(OnboardingStep.EMAIL_AUTH)
    val name = mutableStateOf("")
    val password = mutableStateOf("")
    val lockLevel = mutableStateOf(LockLevel.DETERMINED)

    override fun onCleared() {
        password.value = ""
    }
}
