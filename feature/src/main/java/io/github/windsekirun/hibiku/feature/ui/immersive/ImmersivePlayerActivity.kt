package io.github.windsekirun.hibiku.feature.ui.immersive

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import io.github.windsekirun.hibiku.feature.data.SeekBarStyle
import io.github.windsekirun.hibiku.feature.data.WidgetPreferencesRepository

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

@OptIn(ExperimentalMaterial3Api::class)
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

    val context = LocalContext.current
    val repository = remember(context) { WidgetPreferencesRepository(context) }

    val defaultAccent = 0xFF5CB3FF.toInt()
    val accentColorInt = remember(playbackState.albumArt) {
        PaletteExtractor.extractAccentColor(playbackState.albumArt, defaultAccent)
    }
    val accentColor = Color(accentColorInt)

    var currentShape by rememberSaveable { mutableStateOf(repository.getImmersiveShape()) }
    var currentSeekBarStyle by rememberSaveable { mutableStateOf(repository.getSeekBarStyle()) }

    var isShapeBottomSheetOpen by rememberSaveable { mutableStateOf(false) }
    var isAudioBottomSheetOpen by rememberSaveable { mutableStateOf(false) }
    var isQueueBottomSheetOpen by rememberSaveable { mutableStateOf(false) }

    val darkBackground = Color(0xFF0B0C0E)
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()

    var dragYAmount by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .padding(safePadding)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragYAmount = 0f },
                    onDragEnd = {
                        if (dragYAmount > 120.dp.toPx()) {
                            onClose()
                        }
                        dragYAmount = 0f
                    },
                    onDragCancel = { dragYAmount = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragYAmount += dragAmount
                    }
                )
            }
    ) {
        // Top action bar with Close button and 3-dots MoreVert Menu button
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

            // 3-Dots More Menu Button (opens BottomSheet for shape & seekbar selection)
            IconButton(
                onClick = { isShapeBottomSheetOpen = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = Color.White
                )
            }
        }

        // Modal Bottom Sheet for Shape & SeekBar Style Selection
        if (isShapeBottomSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isShapeBottomSheetOpen = false },
                containerColor = Color(0xFF16181D),
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "앨범아트 모양 설정",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ImmersiveShapeStyle.entries.forEach { style ->
                                ShapePreviewCard(
                                    style = style,
                                    isSelected = style == currentShape,
                                    accentColor = accentColor,
                                    bitmap = playbackState.albumArt,
                                    onClick = {
                                        currentShape = style
                                        repository.saveImmersiveShape(style)
                                    }
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "재생바(SeekBar) 스타일 설정",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SeekBarStyle.entries.forEach { style ->
                                val isSelected = style == currentSeekBarStyle
                                val cardBg = if (isSelected) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
                                val border = if (isSelected) BorderStroke(1.5.dp, accentColor) else BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = cardBg,
                                    border = border,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            currentSeekBarStyle = style
                                            repository.saveSeekBarStyle(style)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = style.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modal Bottom Sheet for Audio Output Device Switcher
        if (isAudioBottomSheetOpen) {
            AudioOutputBottomSheet(
                accentColor = accentColor,
                onDismiss = { isAudioBottomSheetOpen = false }
            )
        }

        // Modal Bottom Sheet for Playback Queue Info
        if (isQueueBottomSheetOpen) {
            QueueBottomSheet(
                playbackState = playbackState,
                accentColor = accentColor,
                onDismiss = { isQueueBottomSheetOpen = false }
            )
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
                    seeBarStyle = currentSeekBarStyle,
                    accentColor = accentColor,
                    onPlayPause = onPlayPause,
                    onSkipPrevious = onSkipPrevious,
                    onSkipNext = onSkipNext,
                    onSeek = onSeek,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onOpenAudioOutput = { isAudioBottomSheetOpen = true },
                    onOpenQueue = { isQueueBottomSheetOpen = true }
                )
            } else {
                PortraitImmersiveLayout(
                    playbackState = playbackState,
                    currentShape = currentShape,
                    seeBarStyle = currentSeekBarStyle,
                    accentColor = accentColor,
                    onPlayPause = onPlayPause,
                    onSkipPrevious = onSkipPrevious,
                    onSkipNext = onSkipNext,
                    onSeek = onSeek,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onOpenAudioOutput = { isAudioBottomSheetOpen = true },
                    onOpenQueue = { isQueueBottomSheetOpen = true }
                )
            }
        }
    }
}

@Composable
fun LandscapeImmersiveLayout(
    playbackState: MediaPlaybackState,
    currentShape: ImmersiveShapeStyle,
    seeBarStyle: SeekBarStyle,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onOpenAudioOutput: () -> Unit = {},
    onOpenQueue: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Album Art with Ambient Glow (equal top and bottom margins)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Ambient radial glow behind album art
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

        // Right Column: Track metadata, seekbar, controls, output chip
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
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = playbackState.artist.ifEmpty { "StandBy Mode" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Controls Row: Shuffle - Prev - Play/Pause - Next - Repeat (centered)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
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
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom 3-part Row: [Audio Output Icon] --- [App Session Chip] --- [Queue Icon]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioOutputIconButton(
                    accentColor = accentColor,
                    onClick = onOpenAudioOutput
                )

                AppSessionChip(
                    packageName = playbackState.packageName,
                    accentColor = accentColor
                )

                QueueIconButton(
                    accentColor = accentColor,
                    onClick = onOpenQueue
                )
            }
        }
    }
}

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
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onOpenAudioOutput: () -> Unit = {},
    onOpenQueue: () -> Unit = {}
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

        // Bottom: Metadata, seekbar, controls, output chip
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

            // Bottom 3-part Row: [Audio Output Icon] --- [App Session Chip] --- [Queue Icon]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioOutputIconButton(
                    accentColor = accentColor,
                    onClick = onOpenAudioOutput
                )

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
fun AudioOutputIconButton(
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
            .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_widget_immersive),
            contentDescription = "Audio Output Switcher",
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AppSessionChip(
    packageName: String?,
    accentColor: Color,
    modifier: Modifier = Modifier
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

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
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
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

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
            painter = painterResource(id = R.drawable.ic_widget_next),
            contentDescription = "Playback Queue",
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputBottomSheet(
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val connectedDevices = remember(audioManager) {
        val outputs = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        outputs?.mapNotNull { dev ->
            val name = dev.productName.toString()
            if (name.isNotBlank()) name else null
        }?.distinct() ?: listOf("Phone Speaker")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16181D),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "오디오 출력 장치 선택",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                connectedDevices.forEach { devName ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
                                    putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                runCatching { context.startActivity(intent) }
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Text(
                                text = devName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    playbackState: MediaPlaybackState,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16181D),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "재생 큐 목록",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = playbackState.queueText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playbackState.title.ifEmpty { "No Media Playing" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = playbackState.artist.ifEmpty { "StandBy Mode" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
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
            seeBarStyle = SeekBarStyle.WAVY,
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
            seeBarStyle = SeekBarStyle.WAVY,
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

