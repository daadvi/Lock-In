package com.example.ui.screens
import com.example.ui.theme.BlockyFontFamily

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.NothingCard
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor

data class ResetOption(
    val hours: Int,
    val title: String,
    val description: String
)

@Composable
fun ResetSettingsScreen(
    currentIntervalHours: Int,
    onSaveInterval: (Int) -> Unit,
    onManualResetAll: () -> Unit,
    onBack: () -> Unit
) {
    val accentColor = LocalCustomTextColor.current

    val options = listOf(
        ResetOption(24, "A cada 1 dia (24h) - Padrão", "Padrão: Todos os dias às 00:00 o saldo de tempo de tela volta integralmente."),
        ResetOption(2, "A cada 2 horas", "Ao esgotar o tempo, o celular bloqueia por 2 horas até o timer reiniciar."),
        ResetOption(4, "A cada 4 horas", "Pausa de 4 horas de descanso antes do timer reiniciar."),
        ResetOption(6, "A cada 6 horas", "Recarga a cada 6 horas após o tempo esgotar."),
        ResetOption(12, "A cada 12 horas", "Duas recargas diárias: ideal para meio-dia e meia-noite."),
        ResetOption(48, "A cada 2 dias (48h)", "Para metas extremas: saldo restaurado a cada 2 dias.")
    )

    val standardHours = listOf(24, 2, 4, 6, 12, 48)
    var selectedHours by remember { mutableIntStateOf(currentIntervalHours) }
    var customHoursInput by remember {
        mutableStateOf(if (currentIntervalHours !in standardHours) currentIntervalHours.toString() else "")
    }
    var isCustomSelected by remember {
        mutableStateOf(currentIntervalHours !in standardHours)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DotMatrixPattern(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = CmfDark
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INTERVALO DE RESET DOS TIMERS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = BlockyFontFamily,
                        color = CmfDark,
                        letterSpacing = 1.sp
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Explanatory Card (Literal translation of user request)
                    item {
                        NothingCard(
                            title = "REGRA DE RESET DO TEMPO DE TELA",
                            badge = "IMPORTANTE"
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Se você usou o celular até o tempo de tela acabar (ex: 23 minutos), o telefone só poderá ser usado novamente após o tempo de reset (ex: 2 horas, padrão 1 dia), pois o timer vai reiniciar.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CmfDark,
                                        lineHeight = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Mesmo que você não zere o saldo de tempo no dia, ao final do ciclo de reset o seu limite total é restaurado integralmente.",
                                    fontSize = 11.sp,
                                    color = CmfMutedText,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "ESCOLHA A FREQUÊNCIA DE RESET:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BlockyFontFamily,
                            color = CmfMutedText
                        )
                    }

                    options.forEach { opt ->
                        item {
                            val isSelected = !isCustomSelected && selectedHours == opt.hours
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CmfWhite)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) accentColor else CmfBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        isCustomSelected = false
                                        selectedHours = opt.hours
                                        onSaveInterval(opt.hours)
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        isCustomSelected = false
                                        selectedHours = opt.hours
                                        onSaveInterval(opt.hours)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = accentColor,
                                        unselectedColor = CmfMutedText
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = opt.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CmfDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = opt.description,
                                        fontSize = 11.sp,
                                        color = CmfMutedText,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Custom Interval Option
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CmfWhite)
                                .border(
                                    width = if (isCustomSelected) 1.5.dp else 1.dp,
                                    color = if (isCustomSelected) accentColor else CmfBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    isCustomSelected = true
                                }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isCustomSelected,
                                    onClick = {
                                        isCustomSelected = true
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = accentColor,
                                        unselectedColor = CmfMutedText
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Personalizado (a cada X horas)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CmfDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Defina exatamente de quantas em quantas horas os timers devem ser restaurados.",
                                        fontSize = 11.sp,
                                        color = CmfMutedText,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            if (isCustomSelected) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customHoursInput,
                                        onValueChange = { input: String ->
                                            val filtered = input.filter { it.isDigit() }
                                            customHoursInput = filtered
                                            val h = filtered.toIntOrNull()
                                            if (h != null && h > 0) {
                                                selectedHours = h
                                                onSaveInterval(h)
                                            }
                                        },
                                        placeholder = { Text(text = "Ex: 8 ou 36") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                        ),
                                        trailingIcon = {
                                            Text(
                                                text = "horas",
                                                fontFamily = BlockyFontFamily,
                                                fontSize = 12.sp,
                                                color = CmfMutedText,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("custom_reset_hours_input"),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Immediate Manual Reset button for testing
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onManualResetAll,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("manual_reset_all_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CmfLightGray,
                                contentColor = CmfDark
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Restaurar Todos os Timers Agora",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
