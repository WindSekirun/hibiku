package io.github.windsekirun.hibiku

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import io.github.windsekirun.hibiku.feature.ui.immersive.ImmersivePlayerActivity
import io.github.windsekirun.hibiku.ui.MainAppScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    surfaceVariant = Color(0xFF2B2B2B),
                    primary = Color(0xFF5CB3FF),
                    onPrimary = Color.Black
                )
            ) {
                MainAppScreen(
                    onOpenNotificationSettings = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    },
                    onLaunchImmersivePlayer = {
                        val intent = Intent(this, ImmersivePlayerActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    },
                    onHideIcon = {
                        packageManager.setComponentEnabledSetting(
                            ComponentName(this, MainActivity::class.java),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                        finish()
                    }
                )
            }
        }
    }
}
