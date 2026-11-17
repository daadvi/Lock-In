package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Screen
import com.example.ui.theme.LocalCustomTextColor

/** Guia de primeira visita de uma área: o que ela é e como usar. */
data class ScreenGuide(
    val title: String,
    val objective: String,
    val howTo: List<String>
)

/** Persistência de "já vi esta área" em SharedPreferences (uma chave por tela). */
object FirstVisitHints {
    private const val PREFS = "lock_in_first_visit"
    private const val PREFIX = "visited_"

    fun wasVisited(context: Context, screen: Screen): Boolean =
        context.getSharedPreferences(PREFS, 0).getBoolean(PREFIX + screen.name, false)

    fun markVisited(context: Context, screen: Screen) {
        context.getSharedPreferences(PREFS, 0)
            .edit()
            .putBoolean(PREFIX + screen.name, true)
            .apply()
    }

    fun resetAll(context: Context) {
        context.getSharedPreferences(PREFS, 0).edit().clear().apply()
    }
}

fun guideFor(screen: Screen): ScreenGuide = when (screen) {
    Screen.DASHBOARD -> ScreenGuide(
        title = "SEU PAINEL",
        objective = "Aqui você vê o resumo do seu dia: o limite de tela do aparelho e o tempo restante de cada app, grupo e site.",
        howTo = listOf(
            "Ative \"LIMITE DIÁRIO ATIVO NO APARELHO\" para controlar o tempo de celular como um todo.",
            "Acompanhe o tempo restante nos cards de apps e grupos.",
            "Use o dock na parte de baixo para trocar de área."
        )
    )
    Screen.APP_SELECTION -> ScreenGuide(
        title = "APPS E LIMITES",
        objective = "Escolha quais apps recebem limite e como o tempo é contado, e defina os apps proibidos de instalar.",
        howTo = listOf(
            "Toque em um app para definir o modo de contagem: absoluto, só vídeos curtos ou vídeo longo.",
            "Crie grupos para que vários apps dividam a mesma cota de tempo.",
            "A lista de bloqueio de instalação proíbe instalar certos apps; o site oficial deles também é bloqueado."
        )
    )
    Screen.CLOCK_LIMIT -> ScreenGuide(
        title = "LIMITE POR RELÓGIO",
        objective = "Defina horários em que as restrições valem, além dos limites em minutos.",
        howTo = listOf(
            "Escolha os dias e o intervalo de início e fim.",
            "Confira o resumo antes de ativar.",
            "Um horário é diferente de um limite diário em minutos: os dois podem coexistir."
        )
    )
    Screen.WEB_LINKS -> ScreenGuide(
        title = "SITES BLOQUEADOS",
        objective = "Lista de sites bloqueados nos navegadores e na rede.",
        howTo = listOf(
            "Adicione os endereços que você quer restringir.",
            "Bloquear um app na blacklist de instalação adiciona o site oficial dele aqui automaticamente.",
            "A proteção de Rede (VPN local) reforça o bloqueio no nível de DNS."
        )
    )
    Screen.RESET_SETTINGS -> ScreenGuide(
        title = "RESET",
        objective = "Redefina preferências de aparência e alertas.",
        howTo = listOf(
            "O reset mexe em tema, alertas e pausa consciente; não mexe na senha nem no nível de lock.",
            "Os limites de tempo zeram sozinhos no reset diário (meia-noite)."
        )
    )
    Screen.STYLING -> ScreenGuide(
        title = "APARÊNCIA",
        objective = "Ajuste o visual do Lock In e da ilha flutuante.",
        howTo = listOf(
            "Escolha tema claro, escuro ou acompanhar o sistema.",
            "Configure a ilha flutuante: tamanho, posição e opacidade.",
            "Troque a cor de destaque do app inteiro."
        )
    )
    Screen.TRUST_UNLOCK -> ScreenGuide(
        title = "DESBLOQUEIO DE CONFIANÇA",
        objective = "Desbloqueio manual de um app que estourou o limite, com atrito proposital.",
        howTo = listOf(
            "Digite a senha a quantidade de vezes do seu nível de lock, sem errar.",
            "Digite a frase exatamente como pedida (maiúsculas ou minúsculas, sem erros). Você tem 3 chances.",
            "Cada desbloqueio consome uma das vagas diárias do seu nível."
        )
    )
    Screen.PERMISSIONS -> ScreenGuide(
        title = "PERMISSÕES",
        objective = "As permissões que fazem o Lock In funcionar de verdade.",
        howTo = listOf(
            "Acessibilidade, Acesso ao uso e Sobreposição são as essenciais.",
            "Conceda cada uma nas telas do Android e volte para cá.",
            "Se o celular bloquear, veja as instruções do seu fabricante nesta tela."
        )
    )
    Screen.STATISTICS -> ScreenGuide(
        title = "ESTATÍSTICAS",
        objective = "Seu uso registrado por período, com comparação e adesão.",
        howTo = listOf(
            "Selecione o período: dia, semana, mês ou ano.",
            "Use o filtro antes/depois da instalação para comparar.",
            "Acompanhe a streak e o indicador de limites respeitados."
        )
    )
    Screen.FOCUS_TIME -> ScreenGuide(
        title = "FOCO E ROTINAS",
        objective = "Trave o aparelho agora por alguns minutos ou programe bloqueios repetidos.",
        howTo = listOf(
            "\"BLOQUEAR DISPOSITIVO AGORA\" inicia uma sessão de foco imediata.",
            "Você pode liberar até 3 apps e o discador durante a sessão.",
            "Crie rotinas com dias e horários para o bloqueio acontecer sozinho."
        )
    )
    Screen.FRIENDS_PANEL -> ScreenGuide(
        title = "AMIGOS (PARTY)",
        objective = "Um grupo de até 20 amigos para acompanhar o foco juntos.",
        howTo = listOf(
            "Crie uma party e compartilhe o código, ou entre com o código de alguém.",
            "Veja o ranking de tempo de tela e a streak do grupo.",
            "Instalar apps bloqueados ou sair do grupo depende de aprovação de um amigo."
        )
    )
    Screen.SETTINGS -> ScreenGuide(
        title = "AJUSTES",
        objective = "As proteções do Lock In: senha, nível, bloqueios e conta.",
        howTo = listOf(
            "O nível de lock define quantas vezes a senha é pedida (1, 3 ou 5).",
            "O Bloqueio Absoluto é sempre ativo; no nível Obcecado nada sai da lista.",
            "Senha de confiança, desinstalação, banco, backup e tema ficam aqui."
        )
    )
}

/**
 * Card de primeira visita: aparece sozinho quando a pessoa entra em uma área que
 * ainda não tinha acessado, explica o objetivo e some ao tocar em ENTENDI.
 */
@Composable
fun FirstVisitHintOverlay(
    screen: Screen,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val guide = remember(screen) { guideFor(screen) }
    var visible by remember(screen) { mutableStateOf(!FirstVisitHints.wasVisited(context, screen)) }
    if (!visible) return

    val accent = LocalCustomTextColor.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.66f))
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                .padding(20.dp)
                .testTag("first_visit_hint"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = guide.title,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 2.sp,
                color = accent
            )
            Text(
                text = guide.objective,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "COMO USAR",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                guide.howTo.forEach { tip ->
                    Text(
                        text = "• $tip",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Button(
                onClick = {
                    FirstVisitHints.markVisited(context, screen)
                    visible = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("first_visit_hint_dismiss"),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("ENTENDI", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
            }
        }
    }
}