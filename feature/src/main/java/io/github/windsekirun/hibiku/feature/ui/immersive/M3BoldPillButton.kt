package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.windsekirun.hibiku.feature.R

@Composable
fun M3BoldPillButton(
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 104.dp,
    height: Dp = 60.dp
) {
    val iconRes = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(percent = 50), spotColor = accentColor)
            .clip(RoundedCornerShape(percent = 50))
            .background(accentColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color(0xFF0B0C0E),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Preview(name = "M3 Bold Pill Button Preview", showBackground = true, backgroundColor = 0xFF0B0C0E)
@Composable
fun M3BoldPillButtonPreview() {
    MaterialTheme {
        M3BoldPillButton(
            isPlaying = true,
            accentColor = Color(0xFF5CB3FF),
            onClick = {}
        )
    }
}
