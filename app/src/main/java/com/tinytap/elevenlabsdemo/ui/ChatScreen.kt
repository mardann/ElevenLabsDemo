package com.tinytap.elevenlabsdemo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tinytap.elevenlabsdemo.R
import com.tinytap.elevenlabsdemo.audio.VoiceRecorder
import com.tinytap.elevenlabsdemo.data.model.ChatMessage
import com.tinytap.elevenlabsdemo.data.model.Sender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ChatScreen(viewModel: ChatUiModel, modifier: Modifier) {
    val messages by viewModel.messages.collectAsState()
    val context = LocalContext.current
    var hasMicPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var sessionActive by remember { mutableStateOf(false) }
    var agentBusy by remember { mutableStateOf(false) } // TODO: Set this based on agent state
    var isRecording by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        showPermissionDialog = !granted
    }

    // Check permission on start
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        hasMicPermission = granted
        showPermissionDialog = !granted
    }

    if (!hasMicPermission) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Microphone Required") },
            text = { Text("This app requires microphone access to function. Please grant permission.") },
            confirmButton = {
                Button(onClick = {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text("Grant Permission") }
            },
            dismissButton = {}
        )
        return
    }

    Column(modifier = modifier
        .fillMaxSize()
        .background(Color(0xFFF5F5F5))) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                ChatBubble(message)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Session toggle button
            Button(
                onClick = {
                    if (sessionActive) {
                        viewModel.disconnect()
                        sessionActive = false
                    } else {
                        viewModel.connect()
                        sessionActive = true
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = if (sessionActive) Color.Red else Color.Green),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (sessionActive) Icons.Default.Close else Icons.Default.PlayArrow,
                    contentDescription = if (sessionActive) "Stop Session" else "Start Session"
                )
            }
            // Microphone button
            val micEnabled = sessionActive && !agentBusy
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.pointerInput(micEnabled) {
                    Log.d("ChatScreen", "record pointerInput: micEnabled=$micEnabled, isRecording=$isRecording")
                    if (micEnabled) {
                        detectTapGestures(
                            onPress = {
                                Log.d("ChatScreen", "record onPress")
                                isRecording = true
                                VoiceRecorder.startRecording(viewModel.getUserInputAudioFormat()){
                                    coroutineScope.launch(Dispatchers.IO) {
                                        viewModel.sendAudioMessage(it)
                                    }
                                }
                                tryAwaitRelease()
                                Log.d("ChatScreen", "record onPress - released")
                                isRecording = false
                                VoiceRecorder.stopRecording()
//                                val base64 = VoiceRecorder.getBase64Audio()
//                                if (!base64.isNullOrBlank()) {
//                                    coroutineScope.launch(Dispatchers.IO) {
//                                        viewModel.sendAudioMessage(base64)
//                                    }
//                                }
                            }
                        )
                    }
                },) {

                Box (contentAlignment = Alignment.Center,
                    modifier = Modifier

                        .background(shape = CircleShape,
                           color =  when {
                               !micEnabled -> Color.Gray
                               isRecording -> Color.Blue
                               else -> Color(0xFF9976D2)
                           }
                        )
                        .size(72.dp)

                ) {
                    Icon(
                        painter = if (isRecording) painterResource(R.drawable.baseline_mic_off_24) else painterResource(R.drawable.baseline_mic_24),
                        contentDescription = if (isRecording) "Recording..." else "Record"
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.sender == Sender.USER) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (message.sender == Sender.USER) Color(0xFF1976D2) else Color(0xFFBDBDBD),
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = message.text,
                color = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    val fakeViewModel = object : ChatUiModel {
        override val messages: StateFlow<List<ChatMessage>> =
            MutableStateFlow(
                listOf(
                    ChatMessage("Hello, how can I help you?", Sender.BOT),
                    ChatMessage("What's the weather?", Sender.USER),
                    ChatMessage("It's sunny today!", Sender.BOT)
                )
            )
        override fun getUserInputAudioFormat() = "pcm_16000"
        override fun connect() {}
        override fun disconnect() {}
        override fun sendAudioMessage(base64: String) {}
    }
    ChatScreen(viewModel = fakeViewModel, modifier = Modifier.fillMaxSize())
}