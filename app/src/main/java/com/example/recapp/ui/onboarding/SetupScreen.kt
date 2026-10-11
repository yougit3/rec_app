package com.example.recapp.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.recapp.util.AccessibilityUtil

data class SetupState(
    val requiredPermissionsGranted: Boolean,
    val optionalPermissionsGranted: Boolean,
    val accessibilityEnabled: Boolean,
) {
    val isComplete: Boolean get() = requiredPermissionsGranted && accessibilityEnabled

    companion object {
        val REQUIRED = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE)
        val OPTIONAL = buildList {
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }

        fun from(context: Context): SetupState {
            fun granted(p: String) = context.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
            return SetupState(
                requiredPermissionsGranted = REQUIRED.all(::granted),
                optionalPermissionsGranted = OPTIONAL.all(::granted),
                accessibilityEnabled = AccessibilityUtil.isServiceEnabled(context),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    state: SetupState,
    onPermissionsResult: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { onPermissionsResult() }

    Scaffold(topBar = { TopAppBar(title = { Text("初期設定") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("録音についての注意", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "通話を録音する際は、事前に相手へ録音することを伝えてください。" +
                            "録音データの扱いは関係する法令に従ってください。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "この方式では自分の声は確実に録音されますが、相手の声は端末によって小さい、" +
                            "または録音されないことがあります。スピーカーホンにすると確実に録音できます。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            SetupStep(
                title = "1. 必須の権限",
                description = "マイク・電話の状態",
                done = state.requiredPermissionsGranted,
                actionLabel = "許可する",
                onAction = { permissionLauncher.launch((SetupState.REQUIRED + SetupState.OPTIONAL).toTypedArray()) },
            )
            SetupStep(
                title = "2. 任意の権限",
                description = "通話履歴・連絡先（相手の番号・名前の記録）、通知（録音中の表示）",
                done = state.optionalPermissionsGranted,
                actionLabel = "許可する",
                onAction = { permissionLauncher.launch(SetupState.OPTIONAL.toTypedArray()) },
            )
            SetupStep(
                title = "3. アクセシビリティサービス",
                description = "設定 → ユーザー補助 → 「通話録音サービス」をオンにしてください。" +
                    "通話中の録音にはこの設定が必要です。",
                done = state.accessibilityEnabled,
                actionLabel = "設定を開く",
                onAction = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )

            Spacer(Modifier.size(8.dp))
            Button(onClick = onDone, enabled = state.isComplete, modifier = Modifier.fillMaxWidth()) {
                Text("完了")
            }
        }
    }
}

@Composable
private fun SetupStep(
    title: String,
    description: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (done) "完了" else "未完了",
                tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            if (!done) OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
