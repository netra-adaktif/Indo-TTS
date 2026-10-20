package org.evvdroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

class MainActivity : ComponentActivity() {
    private var model: SettingsModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = SettingsModel(this)

        setContent {
            MaterialTheme {
                Surface {
                    MainScreen(
                        state = model!!,
                        onOpenSettings = {
                            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                        }
                    )
                }
            }
        }
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
