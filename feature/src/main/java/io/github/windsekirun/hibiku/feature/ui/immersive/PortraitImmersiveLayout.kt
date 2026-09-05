package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.feature.R
import io.github.windsekirun.hibiku.feature.data.SeekBarStyle

@Composable
fun PortraitImmersiveLayout(
    playbackState: MediaPlaybackState,
    currentShape: ImmersiveShapeStyle,
    seeBarStyle: SeekBarStyle,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenQueue: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth(),
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
                modifier = Modifier.size(310.dp),
                glowColor = accentColor,
                contentDescription = playbackState.title.ifEmpty { "Album Artwork" }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            SinglePassMarqueeText(
                text = playbackState.title.ifEmpty { "No Media Playing" },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = playbackState.artist.ifEmpty { "StandBy Mode" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (seeBarStyle == SeekBarStyle.WAVY) {
                SquigglySeekBar(
                    positionMs = playbackState.positionMs,
                    durationMs = playbackState.durationMs,
                    isPlaying = playbackState.isPlaying,
                    accentColor = accentColor,
                    onSeek = onSeek
                )
            } else {
                FluidWaveSeekBar(
                    positionMs = playbackState.positionMs,
                    durationMs = playbackState.durationMs,
                    isPlaying = playbackState.isPlaying,
                    accentColor = accentColor,
                    onSeek = onSeek
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScallopButton(
                    onClick = onSkipPrevious,
                    size = 54.dp,
                    backgroundColor = Color.White.copy(alpha = 0.12f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_widget_prev),
                        contentDescription = "Previous",
                        tint = Color.White,
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
                    backgroundColor = Color.White.copy(alpha = 0.12f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_widget_next),
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppSessionChip(
                    packageName = playbackState.packageName,
                    accentColor = accentColor
                )

                QueueIconButton(
                    accentColor = accentColor,
                    onClick = onOpenQueue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(
    name = "Immersive Player - Portrait",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
fun ImmersivePlayerPortraitPreview() {
    val sampleState = MediaPlaybackState(
        isPlaying = true,
        title = "SUPERNOVA",
        artist = "Shirakami Fubuki",
        positionMs = 85_000L,
        durationMs = 210_000L,
        packageName = "com.spotify.music"
    )

    MaterialTheme {
        PortraitImmersiveLayout(
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
