package io.github.windsekirun.hibiku.feature.ui.config

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import io.github.windsekirun.hibiku.domain.model.WidgetConfig
import io.github.windsekirun.hibiku.feature.data.WidgetPreferencesRepository
import io.github.windsekirun.hibiku.feature.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetConfigurationActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var repository: WidgetPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        repository = WidgetPreferencesRepository(this)
        val initialConfig = repository.loadConfig(appWidgetId)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    surfaceVariant = Color(0xFF2D2D2D),
                    primary = Color(0xFF5CB3FF),
                    onPrimary = Color.Black
                )
            ) {
                WidgetConfigurationScreen(
                    initialConfig = initialConfig,
                    onSave = { config ->
                        saveAndApply(config)
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }

    private fun saveAndApply(config: WidgetConfig) {
        repository.saveConfig(appWidgetId, config)
        val appContext = applicationContext
        CoroutineScope(Dispatchers.Main.immediate).launch {
            WidgetUpdateHelper.updateAllWidgetsSuspend(appContext)
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
