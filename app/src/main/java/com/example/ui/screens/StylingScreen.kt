package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.service.FloatingIslandService
import com.example.ui.components.DotMatrixPattern
import com.example.ui.components.FloatingIslandCapsule
import com.example.ui.components.InteractiveIslandCanvas
import com.example.ui.components.NothingCard
import com.example.ui.theme.CmfBorder
import com.example.ui.theme.CmfDark
import com.example.ui.theme.CmfLightGray
import com.example.ui.theme.CmfMutedText
import com.example.ui.theme.CmfWhite
import com.example.ui.theme.LocalCustomTextColor

data class PaletteColor(val hex: String, val label: String)

@Composable
fun StylingScreen(
    currentTextColorHex: String,
    currentIslandSize: String,
    currentIslandOpacity: Float,
    currentPosXRatio: Float,
    currentPosYRatio: Float,
    onSaveTextColorHex: (String) -> Unit,
    onSaveIslandSettings: (size: String, opacity: Float, posX: Float, posY: Float) -> Unit,
    onBack: () -> Unit
) {
    val accentColor = LocalCustomTextColor.current
    val context = LocalContext.current

    val colorPresets = listOf(
        PaletteColor("#FF5500", "CMF Laranja (Padrão)"),
        PaletteColor("#E53935", "Vermelho Fogo"),
        PaletteColor("#000000", "Preto Monocromático"),
        PaletteColor("#2979FF", "Azul Elétrico"),
        PaletteColor("#00C853", "Verde Foco"),
        PaletteColor("#AA00FF", "Roxo Profundo")
    )

    var selectedSize by remember { mutableStateOf(currentIslandSize) }
    var selectedOpacity by remember { mutableFloatStateOf(currentIslandOpacity) }
    var selectedPosX by remember { mutableFloatStateOf(currentPosXRatio) }
    var selectedPosY by remember { mutableFloatStateOf(currentPosYRatio) }

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
                        text = "ESTILIZAÇÃO & ILHA FLUTUANTE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = CmfDark,
                        letterSpacing = 1.sp
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Accent Color Card
                    item {
                        NothingCard(
                            title = "COR DOS TEXTOS & DESTAQUES",
                            badge = "CMF AESTHETIC"
                        ) {
                            Column {
                                Text(
                                    text = "Por padrão, os fundos são brancos e os textos são laranja CMF. Escolha sua cor de preferência:",
                                    fontSize = 12.sp,
                                    color = CmfMutedText,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    colorPresets.forEach { item ->
                                        val isSelected = currentTextColorHex.equals(item.hex, ignoreCase = true)
                                        val c = Color(item.hex.toColorInt())
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) CmfDark else CmfBorder,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    onSaveTextColorHex(item.hex)
                                                }
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selecionado",
                                                    tint = if (item.hex == "#000000") Color.White else Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Island Size Card
                    item {
                        NothingCard(
                            title = "TAMANHO DA ILHA FLUTUANTE",
                            badge = selectedSize
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("SMALL" to "Pequena", "MEDIUM" to "Média (Padrão)", "LARGE" to "Grande").forEach { (key, label) ->
                                    val isSelected = selectedSize == key
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) accentColor else CmfLightGray)
                                            .border(1.dp, if (isSelected) accentColor else CmfBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedSize = key
                                                onSaveIslandSettings(key, selectedOpacity, selectedPosX, selectedPosY)
                                            }
                                            .padding(vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isSelected) Color.White else CmfDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Opacity Slider
                    item {
                        NothingCard(
                            title = "TRANSPARÊNCIA DA ILHA",
                            badge = "${(selectedOpacity * 100).toInt()}%"
                        ) {
                            Column {
                                Slider(
                                    value = selectedOpacity,
                                    onValueChange = {
                                        selectedOpacity = it
                                        onSaveIslandSettings(selectedSize, it, selectedPosX, selectedPosY)
                                    },
                                    valueRange = 0.2f..1.0f,
                                    steps = 15,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor,
                                        inactiveTrackColor = CmfLightGray
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("20% (Muito sutil)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CmfMutedText)
                                    Text("100% (Sólida)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CmfMutedText)
                                }
                            }
                        }
                    }

                    // Interactive Drag Positioning
                    item {
                        NothingCard(
                            title = "POSICIONAMENTO LIVRE NA TELA",
                            badge = "ARRASTE"
                        ) {
                            Column {
                                Text(
                                    text = "Arraste a ilha no quadro abaixo para definir onde ela aparecerá sobre os seus aplicativos:",
                                    fontSize = 11.sp,
                                    color = CmfMutedText,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                InteractiveIslandCanvas(
                                    sizeMode = selectedSize,
                                    opacity = selectedOpacity,
                                    posXRatio = selectedPosX,
                                    posYRatio = selectedPosY,
                                    onPositionChanged = { rx, ry ->
                                        selectedPosX = rx
                                        selectedPosY = ry
                                        onSaveIslandSettings(selectedSize, selectedOpacity, rx, ry)
                                    }
                                )
                            }
                        }
                    }

                    // Floating Island Live Preview on Screen
                    item {
                        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Settings.canDrawOverlays(context)
                        } else true

                        NothingCard(
                            title = "PRÉ-VISUALIZAÇÃO DA ILHA FLUTUANTE",
                            badge = if (hasOverlay) "SOBREPOSIÇÃO OK" else "PERMISSÃO PENDENTE"
                        ) {
                            Column {
                                Text(
                                    text = "Ative a ilha flutuante para que ela apareça na tela do seu celular agora mesmo. Você pode arrastá-la, minimizar e tocar nela para retornar ao app.",
                                    fontSize = 11.sp,
                                    color = CmfMutedText,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (!hasOverlay) {
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                try {
                                                    val intent = Intent(
                                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                        Uri.parse("package:${context.packageName}")
                                                    )
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                                    context.startActivity(intent)
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                                    ) {
                                        Text("Conceder Permissão de Sobreposição", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                FloatingIslandService.startTestIsland(context)
                                            },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Exibir Ilha na Tela", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Button(
                                            onClick = {
                                                FloatingIslandService.stopTestIsland(context)
                                            },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CmfLightGray)
                                        ) {
                                            Text("Ocultar Ilha", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CmfDark)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
