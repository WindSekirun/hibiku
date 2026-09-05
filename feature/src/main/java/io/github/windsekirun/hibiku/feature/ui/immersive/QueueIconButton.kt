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
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_queue_list),
            contentDescription = "Playback Queue",
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(name = "Queue Icon Button Preview", showBackground = true, backgroundColor = 0xFF0B0C0E)
@Composable
fun QueueIconButtonPreview() {
    MaterialTheme {
        QueueIconButton(
            accentColor = Color(0xFF5CB3FF),
            onClick = {}
        )
    }
}
