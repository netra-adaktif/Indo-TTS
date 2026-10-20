@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import kotlin.math.roundToInt

@Composable
fun PremiumSettingsScreen(state: SettingsModel) {
    var voiceMenuExpanded by remember { mutableStateOf(false) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var punctuationMenuExpanded by remember { mutableStateOf(false) }
    var numberModeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan TTS") },
                actions = {
                    TextButton(onClick = { state.say() }) {
                        Text("Pratinjau")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
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
                        text = "Profil suara",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Suara")
                        Button(onClick = { voiceMenuExpanded = true }) {
                            Text(Eci.PRESET_NAMES.getOrNull(state.voice) ?: "Reed")
                        }
                        DropdownMenu(
                            expanded = voiceMenuExpanded,
                            onDismissRequest = { voiceMenuExpanded = false }
                        ) {
                            Eci.PRESET_NAMES.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        state.chooseVoice(index)
                                        voiceMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (state.languages.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bahasa pratinjau")
                            Button(onClick = { languageMenuExpanded = true }) {
                                Text(state.languageNames.getOrNull(state.language) ?: "English")
                            }
                            DropdownMenu(
                                expanded = languageMenuExpanded,
                                onDismissRequest = { languageMenuExpanded = false }
                            ) {
                                state.languageNames.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            state.chooseLanguage(index)
                                            languageMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Jenis kelamin")
                        Text(
                            text = if (state.gender == 0) "Laki-laki" else "Perempuan",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Slider(
                        value = state.gender.toFloat(),
                        onValueChange = { state.setShape(Eci.VOICE_GENDER, it.roundToInt()) },
                        valueRange = 0f..1f,
                        steps = 1
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Karakter suara",
                        style = MaterialTheme.typography.titleMedium
                    )

                    SettingsSliderRow(
                        title = "Kecepatan",
                        value = state.percentOf(Eci.VOICE_SPEED),
                        onValueChange = { state.setPercent(Eci.VOICE_SPEED, it) }
                    )
                    SettingsSliderRow(
                        title = "Nada",
                        value = state.percentOf(Eci.VOICE_PITCH),
                        onValueChange = { state.setPercent(Eci.VOICE_PITCH, it) }
                    )
                    SettingsSliderRow(
                        title = "Infleksi",
                        value = state.percentOf(Eci.VOICE_INFLECTION),
                        onValueChange = { state.setPercent(Eci.VOICE_INFLECTION, it) }
                    )
                    SettingsSliderRow(
                        title = "Ukuran kepala",
                        value = state.percentOf(Eci.VOICE_HEAD_SIZE),
                        onValueChange = { state.setPercent(Eci.VOICE_HEAD_SIZE, it) }
                    )
                    SettingsSliderRow(
                        title = "Kekasaran",
                        value = state.percentOf(Eci.VOICE_ROUGHNESS),
                        onValueChange = { state.setPercent(Eci.VOICE_ROUGHNESS, it) }
                    )
                    SettingsSliderRow(
                        title = "Kelembutan",
                        value = state.percentOf(Eci.VOICE_BREATHINESS),
                        onValueChange = { state.setPercent(Eci.VOICE_BREATHINESS, it) }
                    )
                    SettingsSliderRow(
                        title = "Volume",
                        value = state.percentOf(Eci.VOICE_VOLUME),
                        onValueChange = { state.setPercent(Eci.VOICE_VOLUME, it) }
                    )
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
                        text = "Pembicaraan",
                        style = MaterialTheme.typography.titleMedium
                    )

                    SettingsToggleRow(
                        title = "Prediksi frasa",
                        checked = state.phrasePrediction,
                        onCheckedChange = state::choosePhrasePrediction
                    )
                    SettingsToggleRow(
                        title = "Perluas singkatan",
                        checked = state.abbreviations,
                        onCheckedChange = state::chooseAbbreviations
                    )
                    SettingsToggleRow(
                        title = "Baca emoji",
                        checked = state.readEmoji,
                        onCheckedChange = state::chooseReadEmoji
                    )
                    SettingsToggleRow(
                        title = "Proses angka",
                        checked = state.processNumbers,
                        onCheckedChange = state::chooseProcessNumbers
                    )

                    if (state.processNumbers) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gaya baca angka")
                            Button(onClick = { numberModeMenuExpanded = true }) {
                                Text(
                                    when (state.numberMode) {
                                        Numbers.MODE_DEFAULT -> "Normal"
                                        Numbers.MODE_DIGITS -> "Digit"
                                        Numbers.MODE_PAIRS -> "Pasangan"
                                        Numbers.MODE_TRIPLETS -> "Tiga serangkai"
                                        Numbers.MODE_WORDS -> "Kata"
                                        else -> "Normal"
                                    }
                                )
                            }
                            DropdownMenu(
                                expanded = numberModeMenuExpanded,
                                onDismissRequest = { numberModeMenuExpanded = false }
                            ) {
                                listOf(
                                    "Normal" to Numbers.MODE_DEFAULT,
                                    "Digit" to Numbers.MODE_DIGITS,
                                    "Pasangan" to Numbers.MODE_PAIRS,
                                    "Tiga serangkai" to Numbers.MODE_TRIPLETS,
                                    "Kata" to Numbers.MODE_WORDS
                                ).forEach { (label, mode) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            state.chooseNumberMode(mode)
                                            numberModeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    SettingsToggleRow(
                        title = "Baca waktu secara alami",
                        checked = state.readTimeNaturally,
                        onCheckedChange = state::chooseReadTimeNaturally
                    )
                    SettingsToggleRow(
                        title = "Baca angka Romawi",
                        checked = state.readRomanNumerals,
                        onCheckedChange = state::chooseReadRomanNumerals
                    )
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
                        text = "Tanda baca",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pelafalan tanda baca")
                        Button(onClick = { punctuationMenuExpanded = true }) {
                            Text(
                                when (state.punctuationLevel) {
                                    0 -> "Tidak ada"
                                    1 -> "Beberapa"
                                    2 -> "Sebagian besar"
                                    3 -> "Semua"
                                    else -> "Tidak ada"
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = punctuationMenuExpanded,
                            onDismissRequest = { punctuationMenuExpanded = false }
                        ) {
                            listOf(
                                "Tidak ada" to 0,
                                "Beberapa" to 1,
                                "Sebagian besar" to 2,
                                "Semua" to 3
                            ).forEach { (label, value) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        state.choosePunctuationLevel(value)
                                        punctuationMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    SettingsToggleRow(
                        title = "Baca tanda baca penuh",
                        checked = state.readPunctuation,
                        onCheckedChange = state::chooseReadPunctuation
                    )
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
                        text = "Audio",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kecepatan sampel")
                        Text("${state.sampleRateHz} Hz")
                    }

                    Slider(
                        value = state.sampleRateHz.toFloat(),
                        onValueChange = { state.chooseSampleRate(it.roundToInt()) },
                        valueRange = 8000f..24000f,
                        steps = 13
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { state.chooseSampleRate(11025) },
                            modifier = Modifier.weight(1f)
                        ) { Text("11 kHz") }
                        OutlinedButton(
                            onClick = { state.chooseSampleRate(16000) },
                            modifier = Modifier.weight(1f)
                        ) { Text("16 kHz") }
                        OutlinedButton(
                            onClick = { state.chooseSampleRate(22050) },
                            modifier = Modifier.weight(1f)
                        ) { Text("22 kHz") }
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Kamus",
                        style = MaterialTheme.typography.titleMedium
                    )

                    val mainName = state.dictionaryName(Eci.DICT_MAIN)
                    val rootName = state.dictionaryName(Eci.DICT_ROOT)
                    val abbrevName = state.dictionaryName(Eci.DICT_ABBREVIATION)
                    val emojiName = state.dictionaryName(Eci.DICT_EMOJI)

                    Text("Utama: $mainName")
                    Text("Root: $rootName")
                    Text("Singkatan: $abbrevName")
                    Text("Emoji: $emojiName")
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
                        text = "Aksi",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { state.say() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Coba suara")
                        }

                        OutlinedButton(
                            onClick = { state.resetVoice() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset bawaan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSliderRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title)
            Text("$value%")
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..100f,
            steps = 99
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
