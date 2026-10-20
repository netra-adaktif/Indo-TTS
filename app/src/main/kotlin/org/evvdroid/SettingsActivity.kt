package org.evvdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

class SettingsActivity : ComponentActivity() {

    private var model: SettingsModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = SettingsModel(this).also { model = it }
        setContent {
            MaterialTheme {
                Surface {
                    PremiumSettingsScreen(state)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model?.refreshStability()
    }

    override fun onStop() {
        super.onStop()
        model?.stopSpeaking()
    }

    override fun onDestroy() {
        super.onDestroy()
        model?.close()
        model = null
    }
}
