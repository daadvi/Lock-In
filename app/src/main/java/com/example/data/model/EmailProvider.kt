package com.example.data.model

enum class EmailProvider(
    val displayName: String,
    val domains: List<String>,
    val defaultDomain: String,
    val brandColorHex: String
) {
    GOOGLE(
        displayName = "Google",
        domains = listOf("gmail.com", "googlemail.com"),
        defaultDomain = "gmail.com",
        brandColorHex = "#EA4335"
    ),
    APPLE(
        displayName = "Apple ID",
        domains = listOf("icloud.com", "me.com", "mac.com", "apple.com"),
        defaultDomain = "icloud.com",
        brandColorHex = "#1C1C1E"
    ),
    HOTMAIL(
        displayName = "Hotmail",
        domains = listOf("hotmail.com", "hotmail.com.br", "hotmail.co.uk", "hotmail.es", "hotmail.fr"),
        defaultDomain = "hotmail.com",
        brandColorHex = "#0078D4"
    ),
    OUTLOOK(
        displayName = "Outlook",
        domains = listOf("outlook.com", "outlook.com.br", "live.com", "msn.com"),
        defaultDomain = "outlook.com",
        brandColorHex = "#0078D4"
    ),
    YAHOO(
        displayName = "Yahoo",
        domains = listOf("yahoo.com", "ymail.com", "yahoo.com.br", "yahoo.co.uk"),
        defaultDomain = "yahoo.com",
        brandColorHex = "#6001D2"
    ),
    PROTON(
        displayName = "Proton Mail",
        domains = listOf("proton.me", "protonmail.com"),
        defaultDomain = "proton.me",
        brandColorHex = "#6D4AFF"
    ),
    CUSTOM(
        displayName = "Outro E-mail",
        domains = emptyList(),
        defaultDomain = "",
        brandColorHex = "#FF5500"
    );

    companion object {
        fun fromEmail(email: String): EmailProvider {
            val domain = email.substringAfter("@", "").lowercase().trim()
            return entries.firstOrNull { it.domains.contains(domain) } ?: CUSTOM
        }
    }
}

data class AccountProfile(
    val email: String,
    val provider: EmailProvider,
    val displayName: String,
    val lastSyncEpoch: Long = 0L,
    val isSynced: Boolean = false,
    val createdAtEpoch: Long = System.currentTimeMillis()
)
