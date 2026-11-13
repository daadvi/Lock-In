package com.example.data.model

enum class LockLevel(
    val id: String,
    val levelNumber: Int,
    val title: String,
    val description: String,
    val passwordRepeatCount: Int,
    val requiredPhrase: String,
    val maxDailyUnlocksPerApp: Int,
    val badgeLabel: String
) {
    BASIC(
        id = "BASIC",
        levelNumber = 1,
        title = "Básico",
        description = "Este nível de bloqueio é para aqueles que não querem mudar, ou então para aquele que querem testar o app antes de aumentar o nível de Lock. Para desbloquear os apps, você só precisa digitar a senha uma vez (sem errar) e digitar uma frase curta (sem errar). Você só pode desbloquear apps 3 vezes no dia para cada app. Para tirar um app da lista de bloqueio de instalação, você só precisa digitar a senha uma vez e digitar uma frase curta (não pode errar em ambos).",
        passwordRepeatCount = 1,
        requiredPhrase = "Quero desbloquear este aplicativo",
        maxDailyUnlocksPerApp = 3,
        badgeLabel = "NÍVEL 1"
    ),
    DETERMINED(
        id = "DETERMINED",
        levelNumber = 2,
        title = "Determinado",
        description = "Este nível de bloqueio já é para aqueles que tem determinação e querem mudar de verdade! Para desbloquear os apps, você deve digitar a senha 3 vezes (sem errar) e digitar uma frase (sem errar). Você só pode desbloquear apps 2 vezes no dia para cada app. Para tirar um app da lista de bloqueio de instalação, você só precisa digitar a senha 3 vezes e digitar uma frase (não pode errar em ambos).",
        passwordRepeatCount = 3,
        requiredPhrase = "Eu reconheço que desbloquear este aplicativo quebra meu foco e compromisso com o meu futuro",
        maxDailyUnlocksPerApp = 2,
        badgeLabel = "NÍVEL 2"
    ),
    DEV_DREAM(
        id = "DEV_DREAM",
        levelNumber = 3,
        title = "Obcecado",
        description = "O Criador deste app só criou o Lock In por causa desse modo de bloqueio. Este é para aqueles que não estão só determinado pela mudança, mas sim obcecados por ela. É para aquele que tem força de vontade inesgotável. Para desbloquear os apps você deve digitar a senha 5 vezes (sem errar) e digitar uma frase (sem errar). Para tirar um app da lista de bloqueio de instalação, você precisa digitar a senha 5 vezes e digitar uma frase (não pode errar em ambos).",
        passwordRepeatCount = 5,
        requiredPhrase = "Eu sou o único responsável pelos meus resultados e tenho força de vontade inesgotável para superar distrações",
        maxDailyUnlocksPerApp = 1,
        badgeLabel = "NÍVEL 3"
    );

    companion object {
        fun fromId(id: String?): LockLevel {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: BASIC
        }

        fun fromNumber(levelNumber: Int): LockLevel {
            return entries.firstOrNull { it.levelNumber == levelNumber } ?: BASIC
        }
    }
}
