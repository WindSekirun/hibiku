package io.github.windsekirun.hibiku.feature.ui.immersive

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.windsekirun.hibiku.core.graphics.PaletteExtractor
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository
import io.github.windsekirun.hibiku.feature.R

class ImmersivePlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full-sensor rotation override: freely rotate portrait and landscape based on physical sensor
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

        // Keep screen on for standby clock / music display
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Edge-to-edge fullscreen
        enableEdgeToEdge()

        setContent {
            val playbackState by MediaPlaybackRepository.playbackState.collectAsState()

            ImmersivePlayerScreen(
                playbackState = playbackState,
                onClose = { finish() },
                onPlayPause = { MediaPlaybackRepository.playPause() },
                onSkipPrevious = { MediaPlaybackRepository.skipToPrevious() },
                onSkipNext = { MediaPlaybackRepository.skipToNext() },
                onSeek = { MediaPlaybackRepository.seekTo(it) },
                onToggleShuffle = { MediaPlaybackRepository.toggleShuffle() },
                onToggleRepeat = { MediaPlaybackRepository.toggleRepeat() }
            )
        }
    }
}

@Composable
fun ImmersivePlayerScreen(
    playbackState: MediaPlaybackState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val defaultAccent = 0xFF5CB3FF.toInt()
    val accentColorInt = remember(playbackState.albumArt) {
        PaletteExtractor.extractAccentColor(playbackState.albumArt, defaultAccent)
    }
    val accentColor = Color(accentColorInt)

    var currentShape by rememberSaveable { mutableStateOf(ImmersiveShapeStyle.SCALLOP) }

    val darkBackground = Color(0xFF0B0C0E)
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .padding(safePadding)
    ) {
        // Top action bar with Close button and Shape style chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Shape Selector Chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ImmersiveShapeStyle.entries.forEach { style ->
                    val isSelected = style == currentShape
                    val chipBg = if (isSelected) accentColor else Color.White.copy(alpha = 0.1f)
                    val textColor = if (isSelected) Color(0xFF0B0C0E) else Color.White

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(chipBg)
                            .clickable { currentShape = style }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }
        }

        // Responsive Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp)
        ) {
            if (isLandscape) {
                LandscapeImmersiveLayout(
                    playbackState = playbackState,
                    currentShape = currentShape,
                    accentColor = accentColor,
                    onPlayPause = onPlayPause,
                    onSkipPrevious = onSkipPrevious,
                    onSkipNext = onSkipNext,
                    onSeek = onSeek,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat
                )
            } else {
                PortraitImmersiveLayout(
                    playbackState = playbackState,
                    currentShape = currentShape,
                    accentColor = accentColor,
                    onPlayPause = onPlayPause,
                    onSkipPrevious = onSkipPrevious,
                    onSkipNext = onSkipNext,
                    onSeek = onSeek,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat
                )
            }
        }
    }
}

@Composable
fun LandscapeImmersiveLayout(
    playbackState: MediaPlaybackState,
    currentShape: ImmersiveShapeStyle,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Album Art with Ambient Glow
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Ambient radial glow behind album art
            Box(
                modifier = Modifier
                    .size(340.dp)
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
                modifier = Modifier.size(280.dp),
                glowColor = accentColor,
                contentDescription = playbackState.title.ifEmpty { "Album Artwork" }
            )
        }

        // Right Column: Track metadata, squiggly seekbar, controls, output chip
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = playbackState.title.ifEmpty { "No Media Playing" },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = playbackState.artist.ifEmpty { "StandBy Mode" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            SquigglySeekBar(
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                isPlaying = playbackState.isPlaying,
                accentColor = accentColor,
                onSeek = onSeek
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Controls Row: Shuffle - Prev - Play/Pause - Next - Repeat - OutputChip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle toggle button
                PlaybackModeToggleButton(
                    isActive = playbackState.isShuffleEnabled,
                    iconRes = R.drawable.ic_shuffle,
                    contentDescription = "Shuffle",
                    accentColor = accentColor,
                    onClick = onToggleShuffle
                )

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

                // Repeat toggle button (off / all / one)
                val repeatIconRes = if (playbackState.repeatMode == 2) R.drawable.ic_repeat_one else R.drawable.ic_repeat
                PlaybackModeToggleButton(
                    isActive = playbackState.isRepeatEnabled,
                    iconRes = repeatIconRes,
                    contentDescription = "Repeat",
                    accentColor = accentColor,
                    onClick = onToggleRepeat
                )

                Spacer(modifier = Modifier.weight(1f))

                // Output chip
                AudioOutputChip(packageName = playbackState.packageName, accentColor = accentColor)
            }
        }
    }
}

@Composable
fun PortraitImmersiveLayout(
    playbackState: MediaPlaybackState,
    currentShape: ImmersiveShapeStyle,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: Album Art with Ambient Glow
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glow
            Box(
                modifier = Modifier
                    .size(320.dp)
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
                modifier = Modifier.size(260.dp),
                glowColor = accentColor,
                contentDescription = playbackState.title.ifEmpty { "Album Artwork" }
            )
        }

        // Bottom: Metadata, seekbar, controls, output chip
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = playbackState.title.ifEmpty { "No Media Playing" },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

            SquigglySeekBar(
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                isPlaying = playbackState.isPlaying,
                accentColor = accentColor,
                onSeek = onSeek
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Controls Row: Shuffle - Prev - Play/Pause - Next - Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaybackModeToggleButton(
                    isActive = playbackState.isShuffleEnabled,
                    iconRes = R.drawable.ic_shuffle,
                    contentDescription = "Shuffle",
                    accentColor = accentColor,
                    onClick = onToggleShuffle
                )

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

                val repeatIconRes = if (playbackState.repeatMode == 2) R.drawable.ic_repeat_one else R.drawable.ic_repeat
                PlaybackModeToggleButton(
                    isActive = playbackState.isRepeatEnabled,
                    iconRes = repeatIconRes,
                    contentDescription = "Repeat",
                    accentColor = accentColor,
                    onClick = onToggleRepeat
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            AudioOutputChip(packageName = playbackState.packageName, accentColor = accentColor)

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun PlaybackModeToggleButton(
    isActive: Boolean,
    iconRes: Int,
    contentDescription: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) accentColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f)
    val iconTint = if (isActive) accentColor else Color.White.copy(alpha = 0.7f)
    val borderModifier = if (isActive) {
        Modifier.border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape)
    } else {
        Modifier
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(borderModifier)
            .background(backgroundColor)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun AudioOutputChip(
    packageName: String?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val displayText = when {
        packageName.isNullOrBlank() -> "Phone Speaker"
        packageName.contains("spotify", ignoreCase = true) -> "Spotify"
        packageName.contains("youtube", ignoreCase = true) -> "YouTube Music"
        packageName.contains("apple", ignoreCase = true) -> "Apple Music"
        else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium
        )
    }
}

// -------------------------------------------------------------------------
// Compose UI Previews (StandBy Portrait & Landscape)
// -------------------------------------------------------------------------

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
        packageName = "com.spotify.music",
        isShuffleEnabled = true,
        repeatMode = 1
    )

    MaterialTheme {
        PortraitImmersiveLayout(
            playbackState = sampleState,
            currentShape = ImmersiveShapeStyle.SCALLOP,
            accentColor = Color(0xFF5CB3FF),
            onPlayPause = {},
            onSkipPrevious = {},
            onSkipNext = {},
            onSeek = {},
            onToggleShuffle = {},
            onToggleRepeat = {}
        )
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
        packageName = "com.spotify.music",
        isShuffleEnabled = true,
        repeatMode = 1
    )

    MaterialTheme {
        LandscapeImmersiveLayout(
            playbackState = sampleState,
            currentShape = ImmersiveShapeStyle.SCALLOP,
            accentColor = Color(0xFF5CB3FF),
            onPlayPause = {},
            onSkipPrevious = {},
            onSkipNext = {},
            onSeek = {},
            onToggleShuffle = {},
            onToggleRepeat = {}
        )
    }
}

