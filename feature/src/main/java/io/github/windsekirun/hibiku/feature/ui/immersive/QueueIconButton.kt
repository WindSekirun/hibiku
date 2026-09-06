package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.windsekirun.hibiku.feature.R

@Composable
fun QueueIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true
) {
    val buttonBg = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val buttonBorder = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
    val iconTint = if (isDarkMode) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.85f)

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(buttonBg)
            .border(1.dp, buttonBorder, CircleShape)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_queue_list),
            contentDescription = "Playback Queue",
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(name = "Queue Icon Button Preview", showBackground = true, backgroundColor = 0xFF0B0C0E)
@Composable
fun QueueIconButtonPreview() {
    MaterialTheme {
        QueueIconButton(
            onClick = {}
        )
    }
}
