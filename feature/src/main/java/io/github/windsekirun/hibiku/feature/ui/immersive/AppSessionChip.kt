package io.github.windsekirun.hibiku.feature.ui.immersive

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppSessionChip(
    packageName: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true
) {
    val context = LocalContext.current
    val appName = remember(packageName) {
        if (packageName.isNullOrBlank()) {
            "Music Player"
        } else when {
            packageName.contains("spotify", ignoreCase = true) -> "Spotify"
            packageName.contains("youtube", ignoreCase = true) -> "YouTube Music"
            packageName.contains("apple", ignoreCase = true) -> "Apple Music"
            else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    val onClickApp = remember(packageName, context) {
        {
            if (!packageName.isNullOrBlank()) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(launchIntent) }
                }
            }
        }
    }

    val chipBg = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val textColor = if (isDarkMode) Color.White else Color(0xFF111111)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(chipBg)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(enabled = !packageName.isNullOrBlank(), onClick = onClickApp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = appName,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(name = "App Session Chip Preview", showBackground = true, backgroundColor = 0xFF0B0C0E)
@Composable
fun AppSessionChipPreview() {
    MaterialTheme {
        AppSessionChip(
            packageName = "com.spotify.music",
            accentColor = Color(0xFF1DB954)
        )
    }
}
