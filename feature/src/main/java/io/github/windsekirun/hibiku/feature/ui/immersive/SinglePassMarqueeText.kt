package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SinglePassMarqueeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    key(text) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = textAlign,
            modifier = modifier.basicMarquee(
                iterations = 1,
                repeatDelayMillis = 1000,
                initialDelayMillis = 800
            )
        )
    }
}

@Preview(name = "Single Pass Marquee Text Preview", showBackground = true, backgroundColor = 0xFF0B0C0E)
@Composable
fun SinglePassMarqueeTextPreview() {
    MaterialTheme {
        SinglePassMarqueeText(
            text = "SUPERNOVA - SHIRAKAMI FUBUKI",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = Color.White
        )
    }
}
