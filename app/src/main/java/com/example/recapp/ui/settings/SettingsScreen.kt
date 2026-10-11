package com.example.recapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.recapp.RecApp
import com.example.recapp.data.Settings
import com.example.recapp.recorder.AudioSourceOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenSetup: () -> Unit) {
    val repository = (LocalContext.current.applicationContext as RecApp).settingsRepository
    val settings by repository.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text("通話を自動録音") },
                supportingContent = { Text("オフにすると通話を録音しません") },
                trailingContent = {
                    Switch(
                        checked = settings.autoRecord,
                        onCheckedChange = { scope.launch { repository.setAutoRecord(it) } },
                    )
                },
            )
            ListItem(
                headlineContent = { Text("短い録音を削除") },
                supportingContent = { Text("2秒未満の録音は保存しません") },
                trailingContent = {
                    Switch(
                        checked = settings.deleteShortRecordings,
                        onCheckedChange = { scope.launch { repository.setDeleteShortRecordings(it) } },
                    )
                },
            )
            HorizontalDivider()
            Text(
                "録音の音源",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            )
            Text(
                "相手の声が録音されない場合は別の音源を試してください。使えない音源は自動で次の候補に切り替わります。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            AudioSourceOption.entries.forEach { option ->
                ListItem(
                    modifier = Modifier.selectable(
                        selected = settings.audioSource == option,
                        onClick = { scope.launch { repository.setAudioSource(option) } },
                    ),
                    headlineContent = { Text(option.label) },
                    leadingContent = { RadioButton(selected = settings.audioSource == option, onClick = null) },
                )
            }
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenSetup),
                headlineContent = { Text("権限とアクセシビリティの確認") },
            )
        }
    }
}
