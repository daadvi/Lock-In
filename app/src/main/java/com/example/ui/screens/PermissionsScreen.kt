package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.service.FloatingIslandService
import com.example.ui.components.PermissionSetup
import com.example.util.AppPermission

@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
                Text("Permissões do Lock In", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            }
            PermissionSetup(
                modifier = Modifier.weight(1f),
                onPermissionsChanged = { state ->
                    if (state.isGranted(AppPermission.OVERLAY)) FloatingIslandService.ensureStarted(context)
                }
            )
        }
    }
}
