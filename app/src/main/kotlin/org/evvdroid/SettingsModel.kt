package org.evvdroid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: SettingsModel,
    onOpenSettings: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("Halo dunia! 😊 Selamat pagi Indonesia.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Indo TTS") },
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text("Pengaturan")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Teks yang akan dibaca",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6,
                        maxLines = 8
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { state.speakText(inputText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Baca")
                        }

                        OutlinedButton(
                            onClick = { state.stopSpeaking() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hentikan")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pengaturan cepat",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Baca emoji")
                        Switch(
                            checked = state.readEmoji,
                            onCheckedChange = { state.chooseReadEmoji(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sample rate")
                        Text("${state.sampleRateHz} Hz")
                    }

                    Slider(
                        value = state.sampleRateHz.toFloat(),
                        onValueChange = { state.chooseSampleRate(it.toInt()) },
                        valueRange = 11025f..22050f,
                        steps = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { state.chooseSampleRate(16000) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("16 kHz")
                        }

                        Button(
                            onClick = { state.chooseSampleRate(22050) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("22 kHz")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Contoh teks",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("Halo, saya sedang belajar TTS 😊")
                    Text("Sampai jumpa 👋")
                    Text("Hari ini saya senang sekali! ✅")
                    Text("Nomor 12345 dan emoji ❤️")
                }
            }
        }
    }
}
