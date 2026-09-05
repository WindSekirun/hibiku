package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.windsekirun.hibiku.feature.R

@Composable
fun ScallopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.12f),
    contentColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(ScallopShape(12))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Transparent,
            contentColor = contentColor
        ) {
            content()
        }
    }
}

@Preview(name = "Scallop Button Preview", showBackground = true, backgroundColor = 0xFF0B0C0E)
@Composable
fun ScallopButtonPreview() {
    MaterialTheme {
        ScallopButton(onClick = {}) {
            Icon(
                painter = painterResource(id = R.drawable.ic_widget_next),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
