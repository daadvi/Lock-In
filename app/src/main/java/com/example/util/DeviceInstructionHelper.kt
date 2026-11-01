package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.lang.reflect.Method

enum class DeviceFamily(val displayName: String, val badge: String) {
    SAMSUNG("Samsung (One UI)", "SAMSUNG"),
    XIAOMI_HYPEROS("Xiaomi / Redmi / POCO (HyperOS)", "HYPEROS"),
    XIAOMI_MIUI("Xiaomi / Redmi / POCO (MIUI)", "MIUI"),
    MOTOROLA("Motorola (Hello UI / MyUX)", "MOTOROLA"),
    REALME("Realme / Oppo / OnePlus", "REALME / COLOROS"),
    GENERIC("Android", "DISPOSITIVO")
}

data class DeviceInstructionProfile(
    val family: DeviceFamily,
    val deviceName: String,
    val recentAppsTitle: String,
    val recentAppsSteps: List<String>,
    val autoStartTitle: String,
    val autoStartSections: List<InstructionSection>,
    val essentialBatteryTitle: String,
    val essentialBatteryTip: String
)

data class InstructionSection(
    val title: String,
    val steps: List<String>
)

object DeviceInstructionHelper {

    fun detectDeviceFamily(): DeviceFamily {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val brand = Build.BRAND?.lowercase() ?: ""
        val product = Build.PRODUCT?.lowercase() ?: ""

        if (manufacturer.contains("samsung") || brand.contains("samsung")) {
            return DeviceFamily.SAMSUNG
        }

        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ||
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")) {
            val miuiVersion = getMiuiOrHyperOsVersion() ?: ""
            val isHyperOs = miuiVersion.contains("OS", ignoreCase = true) ||
                    miuiVersion.contains("Hyper", ignoreCase = true) ||
                    Build.VERSION.SDK_INT >= 34
            return if (isHyperOs) DeviceFamily.XIAOMI_HYPEROS else DeviceFamily.XIAOMI_MIUI
        }

        if (manufacturer.contains("motorola") || brand.contains("motorola") || brand.contains("moto")) {
            return DeviceFamily.MOTOROLA
        }

        if (manufacturer.contains("realme") || manufacturer.contains("oppo") || manufacturer.contains("oneplus") ||
            brand.contains("realme") || brand.contains("oppo") || brand.contains("oneplus")) {
            return DeviceFamily.REALME
        }

        return DeviceFamily.GENERIC
    }

    private fun getMiuiOrHyperOsVersion(): String? {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod: Method = systemPropertiesClass.getMethod("get", String::class.java)
            val miuiName = getMethod.invoke(null, "ro.miui.ui.version.name") as? String
            val miuiCode = getMethod.invoke(null, "ro.miui.ui.version.code") as? String
            if (!miuiName.isNullOrEmpty()) {
                if (!miuiCode.isNullOrEmpty()) "$miuiName ($miuiCode)" else miuiName
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun getProfile(): DeviceInstructionProfile {
        val family = detectDeviceFamily()
        val model = Build.MODEL ?: "Dispositivo"
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"

        return when (family) {
            DeviceFamily.SAMSUNG -> DeviceInstructionProfile(
                family = DeviceFamily.SAMSUNG,
                deviceName = "$manufacturer $model",
                recentAppsTitle = "Como manter o app ativo",
                recentAppsSteps = listOf(
                    "Abra a tela de 'Aplicativos Recentes' (botão de 3 barras ou deslize de baixo para cima e segure).",
                    "Toque no ícone do nosso aplicativo no topo da miniatura.",
                    "Selecione a opção 'Manter aberto'.",
                    "Um ícone de cadeado aparecerá na miniatura, impedindo o fechamento ao clicar em 'Fechar tudo'."
                ),
                autoStartTitle = "Permitir início automático no seu Samsung (One UI)",
                autoStartSections = listOf(
                    InstructionSection(
                        title = "Para sistemas mais recentes (One UI 6 ou superior):",
                        steps = listOf(
                            "Abra as Configurações do seu celular.",
                            "Acesse Assistência do aparelho > Bateria.",
                            "Toque em Limites de uso em segundo plano > Aplicativos nunca suspensos.",
                            "Toque no ícone de + no canto superior direito e adicione este aplicativo."
                        )
                    ),
                    InstructionSection(
                        title = "Para sistemas anteriores (One UI 5 ou inferior):",
                        steps = listOf(
                            "Vá em Configurações > Assistência do aparelho e bateria > Bateria.",
                            "Toque em Limites de uso em segundo plano > Aplicativos nunca suspensos e adicione o app."
                        )
                    )
                ),
                essentialBatteryTitle = "Ajuste de energia essencial (Todos os Samsung):",
                essentialBatteryTip = "Volte para Configurações > Aplicativos > selecione este app > Bateria > altere para Não restrito."
            )

            DeviceFamily.XIAOMI_HYPEROS -> DeviceInstructionProfile(
                family = DeviceFamily.XIAOMI_HYPEROS,
                deviceName = "$manufacturer $model (HyperOS)",
                recentAppsTitle = "Como manter o app ativo no HyperOS",
                recentAppsSteps = listOf(
                    "Abra o aplicativo nativo 'Segurança' (ícone de escudo verde da Xiaomi).",
                    "Toque na engrenagem de 'Configurações' no canto superior direito.",
                    "Vá em 'Otimização de velocidade' e depois em 'Travar apps'.",
                    "Ative a chave ao lado do nosso aplicativo para fixar o cadeado.",
                    "Para garantir, vá em 'Configurações' > 'Bateria' > aba 'Bateria' > 'Economia de bateria de apps' > selecione nosso app e marque 'Sem restrições'."
                ),
                autoStartTitle = "Ativar início automático na Xiaomi (MIUI / HyperOS 1)",
                autoStartSections = listOf(
                    InstructionSection(
                        title = "Para o novo HyperOS 1:",
                        steps = listOf(
                            "Abra as Configurações do seu aparelho.",
                            "Vá em Apps (ou Aplicativos) > Permissões.",
                            "Selecione Início automático em segundo plano.",
                            "Localize este aplicativo na lista e ative a chave ao lado dele."
                        )
                    ),
                    InstructionSection(
                        title = "Para sistemas anteriores (MIUI 14 ou inferior):",
                        steps = listOf(
                            "Vá em Configurações > Apps > Gerenciar Apps.",
                            "Selecione este aplicativo na lista e ative a chave ao lado de Início automático."
                        )
                    )
                ),
                essentialBatteryTitle = "Ajuste de energia essencial (Todos os Xiaomi):",
                essentialBatteryTip = "Na tela de informações deste app (em Gerenciar Apps), toque em Economia de bateria e escolha Nenhuma restrição."
            )

            DeviceFamily.XIAOMI_MIUI -> DeviceInstructionProfile(
                family = DeviceFamily.XIAOMI_MIUI,
                deviceName = "$manufacturer $model (MIUI)",
                recentAppsTitle = "Como manter o app ativo na MIUI",
                recentAppsSteps = listOf(
                    "Abra a tela de 'Aplicativos Recentes'.",
                    "Toque e mantenha pressionada a janela do nosso aplicativo por 2 segundos.",
                    "No menu flutuante que aparecer, toque no ícone de 'Cadeado'.",
                    "Vá em 'Configurações' > 'Bateria' > 'Economia de bateria de apps' > selecione nosso app e mude para 'Sem restrições'."
                ),
                autoStartTitle = "Ativar início automático na Xiaomi (MIUI / HyperOS 1)",
                autoStartSections = listOf(
                    InstructionSection(
                        title = "Para o novo HyperOS 1:",
                        steps = listOf(
                            "Abra as Configurações do seu aparelho.",
                            "Vá em Apps (ou Aplicativos) > Permissões.",
                            "Selecione Início automático em segundo plano.",
                            "Localize este aplicativo na lista e ative a chave ao lado dele."
                        )
                    ),
                    InstructionSection(
                        title = "Para sistemas anteriores (MIUI 14 ou inferior):",
                        steps = listOf(
                            "Vá em Configurações > Apps > Gerenciar Apps.",
                            "Selecione este aplicativo na lista e ative a chave ao lado de Início automático."
                        )
                    )
                ),
                essentialBatteryTitle = "Ajuste de energia essencial (Todos os Xiaomi):",
                essentialBatteryTip = "Na tela de informações deste app (em Gerenciar Apps), toque em Economia de bateria e escolha Nenhuma restrição."
            )

            DeviceFamily.MOTOROLA -> DeviceInstructionProfile(
                family = DeviceFamily.MOTOROLA,
                deviceName = "$manufacturer $model",
                recentAppsTitle = "Evitar fechamento automático",
                recentAppsSteps = listOf(
                    "Abra a tela de 'Aplicativos Recentes'.",
                    "Toque no ícone do nosso app (acima da miniatura da tela).",
                    "Toque na opção 'Manter aberto' ou 'Fixar' (se disponível na sua versão).",
                    "Para evitar que o sistema derrube o app, vá em 'Configurações' > 'Bateria' > 'Uso da bateria' > selecione nosso app e mude para 'Sem restrições' (ou 'Não otimizar')."
                ),
                autoStartTitle = "Configurar otimização de inicialização na Motorola (Hello UI / MyUX)",
                autoStartSections = listOf(
                    InstructionSection(
                        title = "Para sistemas recentes (Hello UI / Android 14+):",
                        steps = listOf(
                            "Abra as Configurações do smartphone.",
                            "Acesse Segurança e privacidade > Privacidade > Gerenciador de permissões.",
                            "Procure por Início automático ou Atividade em segundo plano e conceda a permissão a este app."
                        )
                    ),
                    InstructionSection(
                        title = "Para sistemas anteriores (MyUX):",
                        steps = listOf(
                            "Vá em Configurações > Sistema > Desempenho > Início inteligente dos apps e ative a chave deste app."
                        )
                    )
                ),
                essentialBatteryTitle = "Ajuste de energia essencial (Todos os Motorola):",
                essentialBatteryTip = "Volte para Configurações > Apps > Ver todos os apps > selecione este app > Bateria (ou Uso da bateria do app) > altere para Não restrito."
            )

            DeviceFamily.REALME -> DeviceInstructionProfile(
                family = DeviceFamily.REALME,
                deviceName = "$manufacturer $model",
                recentAppsTitle = "Manter em segundo plano",
                recentAppsSteps = listOf(
                    "Abra a tela de 'Aplicativos Recentes'.",
                    "Toque nos três pontos verticais no canto superior direito da miniatura do app.",
                    "Selecione a opção 'Bloquear'.",
                    "O app exibirá um cadeado e será ignorado pelo botão 'Fechar tudo'."
                ),
                autoStartTitle = "Configurar inicialização em segundo plano (Realme UI / ColorOS / OxygenOS)",
                autoStartSections = listOf(
                    InstructionSection(
                        title = "Para versões recentes (Android 14 / ColorOS 14):",
                        steps = listOf(
                            "Abra as Configurações do aparelho.",
                            "Vá em Aplicativos > Início automático do aplicativo.",
                            "Ative a permissão para este aplicativo."
                        )
                    ),
                    InstructionSection(
                        title = "Para versões anteriores:",
                        steps = listOf(
                            "Vá em Configurações > Aplicativos > Inicialização automática e ative o app.",
                            "Acesse Configurações > Bateria > Gerenciamento de energia de apps > selecione este app > ative Permitir atividade em segundo plano."
                        )
                    )
                ),
                essentialBatteryTitle = "Ajuste de energia essencial:",
                essentialBatteryTip = "Volte para Configurações > Bateria > Mais configurações > Otimizar uso da bateria > selecione este app > marque Não otimizar."
            )

            DeviceFamily.GENERIC -> DeviceInstructionProfile(
                family = DeviceFamily.GENERIC,
                deviceName = "$manufacturer $model",
                recentAppsTitle = "Como manter o app ativo",
                recentAppsSteps = listOf(
                    "Abra a tela de 'Aplicativos Recentes'.",
                    "Toque no ícone do nosso aplicativo no topo da tela.",
                    "Selecione 'Fixar' ou 'Bloquear'.",
                    "O aplicativo permanecerá ativo em segundo plano e não será fechado ao limpar a memória."
                ),
                autoStartTitle = "Configurar início em segundo plano e bateria",
                autoStartSections = listOf(
                    InstructionSection(
                        title = "Configuração de Bateria e Início:",
                        steps = listOf(
                            "Abra as Configurações do seu aparelho.",
                            "Acesse Aplicativos > selecione Lock In > Bateria.",
                            "Altere para 'Não restrito' (ou 'Sem restrições')."
                        )
                    )
                ),
                essentialBatteryTitle = "Ajuste essencial:",
                essentialBatteryTip = "Desative a otimização de bateria para o Lock In para garantir proteção 24h sem interrupção do sistema."
            )
        }
    }

    fun openDeviceOptimizationSettings(context: Context): Boolean {
        val family = detectDeviceFamily()
        val pkg = context.packageName

        return try {
            when (family) {
                DeviceFamily.SAMSUNG -> {
                    val intent = Intent().setClassName(
                        "com.android.settings",
                        "com.android.settings.Settings\$AppBatteryUsageActivity"
                    ).apply {
                        putExtra("package_name", pkg)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                        true
                    } catch (_: Exception) {
                        openAppDetailsSettings(context)
                    }
                }
                DeviceFamily.XIAOMI_HYPEROS, DeviceFamily.XIAOMI_MIUI -> {
                    XiaomiCompatibilityHelper.openAutostartSettings(context)
                }
                DeviceFamily.MOTOROLA -> {
                    val intent = Intent().apply {
                        setClassName("com.motorola.motocare", "com.motorola.motocare.MainActivity")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                        true
                    } catch (_: Exception) {
                        openAppDetailsSettings(context)
                    }
                }
                DeviceFamily.REALME -> {
                    val intents = listOf(
                        Intent().apply {
                            setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                        Intent().apply {
                            setClassName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                    var opened = false
                    for (i in intents) {
                        try {
                            context.startActivity(i)
                            opened = true
                            break
                        } catch (_: Exception) {}
                    }
                    if (!opened) openAppDetailsSettings(context) else true
                }
                DeviceFamily.GENERIC -> {
                    openAppDetailsSettings(context)
                }
            }
        } catch (_: Exception) {
            openAppDetailsSettings(context)
        }
    }

    fun openAppDetailsSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
