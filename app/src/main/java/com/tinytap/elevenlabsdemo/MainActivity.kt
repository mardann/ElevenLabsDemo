package com.tinytap.elevenlabsdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.tinytap.elevenlabsdemo.data.AGENT_ID_EDDIE
import com.tinytap.elevenlabsdemo.data.AGENT_ID_ROGER
import com.tinytap.elevenlabsdemo.ui.theme.ElevenLabsDemoTheme
import com.tinytap.elevenlabsdemo.ui.ChatScreen
import com.tinytap.elevenlabsdemo.ui.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ElevenLabsDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { pv: PaddingValues ->
                    val viewModel = ChatViewModel(agentId = AGENT_ID_EDDIE)
                    ChatScreen(viewModel = viewModel, Modifier.padding(pv))
                }
            }
        }
    }
}