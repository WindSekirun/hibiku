package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.feature.R
import io.github.windsekirun.hibiku.feature.data.SeekBarStyle

@Composable
fun LandscapeImmersiveLayout(
    playbackState: MediaPlaybackState,
    currentShape: ImmersiveShapeStyle,
    seeBarStyle: SeekBarStyle,
    accentColor: Color,
    isDarkMode: Boolean = true,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenQueue: () -> Unit = {}
) {
    val primaryTextColor = if (isDarkMode) Color.White else Color(0xFF111111)
    val secondaryTextColor = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.65f)
    val buttonBgColor = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    val buttonContentColor = if (isDarkMode) Color.White else Color(0xFF111111)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(360.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.40f),
                                accentColor.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            ArtisticAlbumArt(
                bitmap = playbackState.albumArt,
                shape = currentShape,
                modifier = Modifier.size(290.dp),
                glowColor = accentColor,
                contentDescription = playbackState.title.ifEmpty { "Album Artwork" }
            )
        }

        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            SinglePassMarqueeText(
                text = playbackState.title.ifEmpty { "No Media Playing" },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = primaryTextColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = playbackState.artist.ifEmpty { "StandBy Mode" },
                style = MaterialTheme.typography.titleMedium,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (seeBarStyle == SeekBarStyle.WAVY) {
                SquigglySeekBar(
                    positionMs = playbackState.positionMs,
                    durationMs = playbackState.durationMs,
                    isPlaying = playbackState.isPlaying,
                    accentColor = accentColor,
                    isDarkMode = isDarkMode,
                    onSeek = onSeek
                )
            } else {
                FluidWaveSeekBar(
                    positionMs = playbackState.positionMs,
                    durationMs = playbackState.durationMs,
                    isPlaying = playbackState.isPlaying,
                    accentColor = accentColor,
                    isDarkMode = isDarkMode,
                    onSeek = onSeek
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScallopButton(
                    onClick = onSkipPrevious,
                    size = 54.dp,
                    backgroundColor = buttonBgColor
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_widget_prev),
                        contentDescription = "Previous",
                        tint = buttonContentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                M3BoldPillButton(
                    isPlaying = playbackState.isPlaying,
                    accentColor = accentColor,
                    onClick = onPlayPause,
                    width = 104.dp,
                    height = 56.dp
                )

                ScallopButton(
                    onClick = onSkipNext,
                    size = 54.dp,
                    backgroundColor = buttonBgColor
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_widget_next),
                        contentDescription = "Next",
                        tint = buttonContentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppSessionChip(
                    packageName = playbackState.packageName,
                    accentColor = accentColor,
                    isDarkMode = isDarkMode
                )

                QueueIconButton(
                    accentColor = accentColor,
                    isDarkMode = isDarkMode,
                    onClick = onOpenQueue
                )
            }
        }
    }
}

@Preview(
    name = "Immersive Player - Landscape StandBy",
    widthDp = 844,
    heightDp = 390,
    showBackground = true
)
@Composable
fun ImmersivePlayerLandscapePreview() {
    val sampleState = MediaPlaybackState(
        isPlaying = true,
        title = "SUPERNOVA",
        artist = "Shirakami Fubuki",
        positionMs = 85_000L,
        durationMs = 210_000L,
        packageName = "com.spotify.music"
    )

    MaterialTheme {
        LandscapeImmersiveLayout(
            playbackState = sampleState,
            currentShape = ImmersiveShapeStyle.SCALLOP,
            seeBarStyle = SeekBarStyle.WAVY,
            accentColor = Color(0xFF5CB3FF),
            onPlayPause = {},
            onSkipPrevious = {},
            onSkipNext = {},
            onSeek = {}
        )
    }
}
