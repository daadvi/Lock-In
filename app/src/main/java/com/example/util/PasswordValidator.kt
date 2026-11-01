package com.example.util

object PasswordValidator {

    fun getValidationError(password: String): String? {
        if (password.length < 12) {
            return "A senha deve ter no mínimo 12 caracteres."
        }
        if (!password.any { it.isUpperCase() }) {
            return "A senha deve conter pelo menos 1 letra maiúscula (A-Z)."
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            return "A senha deve conter pelo menos 1 caractere especial (ex: @, #, $, !, %, &)."
        }
        return null
    }

    fun hasMinLength(password: String): Boolean = password.length >= 12

    fun hasUppercase(password: String): Boolean = password.any { it.isUpperCase() }

    fun hasSpecialSymbol(password: String): Boolean = password.any { !it.isLetterOrDigit() }

    fun isValid(password: String): Boolean = getValidationError(password) == null
}
