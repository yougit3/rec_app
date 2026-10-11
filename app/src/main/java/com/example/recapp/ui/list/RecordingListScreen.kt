package com.example.recapp.ui.list

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recapp.data.CallDirection
import com.example.recapp.data.Recording
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingListScreen(
    onOpenSettings: () -> Unit,
    viewModel: RecordingListViewModel = viewModel(),
) {
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val playingId by viewModel.playingId.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Recording?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("録音一覧") },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "設定") }
                },
            )
        },
    ) { padding ->
        if (recordings.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("録音はまだありません。\n通話すると自動で録音されます。", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(recordings, key = { it.id }) { recording ->
                    RecordingRow(
                        recording = recording,
                        playing = playingId == recording.id,
                        onTogglePlay = { viewModel.togglePlay(recording) },
                        onShare = { share(context, recording) },
                        onDelete = { pendingDelete = recording },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    pendingDelete?.let { recording ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("録音を削除") },
            text = { Text("この録音を削除しますか？元に戻せません。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(recording)
                    pendingDelete = null
                }) { Text("削除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("キャンセル") } },
        )
    }
}

@Composable
private fun RecordingRow(
    recording: Recording,
    playing: Boolean,
    onTogglePlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val title = recording.contactName ?: recording.phoneNumber ?: "不明な番号"
    val date = DateUtils.formatDateTime(
        context,
        recording.startedAt,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR,
    )
    val duration = DateUtils.formatElapsedTime(recording.durationMs / 1000)
    ListItem(
        leadingContent = {
            val (icon, label) = when (recording.direction) {
                CallDirection.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to "着信"
                CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to "発信"
                CallDirection.UNKNOWN -> Icons.Filled.Call to "通話"
            }
            Icon(icon, contentDescription = label)
        },
        headlineContent = { Text(title) },
        supportingContent = { Text("$date ・ $duration") },
        trailingContent = {
            androidx.compose.foundation.layout.Row {
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "停止" else "再生",
                    )
                }
                IconButton(onClick = onShare) { Icon(Icons.Filled.Share, contentDescription = "共有") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "削除") }
            }
        },
    )
}

private fun share(context: Context, recording: Recording) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(recording.filePath))
    val intent = Intent(Intent.ACTION_SEND)
        .setType("audio/mp4")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "録音を共有"))
}
