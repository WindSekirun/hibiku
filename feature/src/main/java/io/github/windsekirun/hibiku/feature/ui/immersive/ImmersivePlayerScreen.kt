package io.github.windsekirun.hibiku.feature.ui.immersive

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.windsekirun.hibiku.core.graphics.PaletteExtractor
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.feature.data.ImmersiveThemeMode
import io.github.windsekirun.hibiku.feature.data.SeekBarStyle
import io.github.windsekirun.hibiku.feature.data.WidgetPreferencesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersivePlayerScreen(
    playbackState: MediaPlaybackState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val repository = remember(context) { WidgetPreferencesRepository(context) }

    val defaultAccentInt = 0xFF5CB3FF.toInt()
    val art = playbackState.albumArt
    val extractedAccentInt = remember(art) {
        if (art != null && !art.isRecycled) {
            PaletteExtractor.extractAccentColor(art, defaultAccentInt)
        } else {
            null
        }
    }

    var lastValidAccentInt by remember { mutableIntStateOf(defaultAccentInt) }
    if (extractedAccentInt != null) {
        lastValidAccentInt = extractedAccentInt
    }

    val accentColor by animateColorAsState(
        targetValue = Color(lastValidAccentInt),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "AccentColorAnimation"
    )

    var currentShape by rememberSaveable { mutableStateOf(repository.getImmersiveShape()) }
    var currentSeekBarStyle by rememberSaveable { mutableStateOf(repository.getSeekBarStyle()) }
    var currentThemeMode by rememberSaveable { mutableStateOf(repository.getImmersiveThemeMode()) }
    var isAutoLaunchCharging by rememberSaveable { mutableStateOf(repository.isAutoLaunchOnChargingEnabled()) }

    val isSystemDark = isSystemInDarkTheme()
    val isDarkMode = when (currentThemeMode) {
        ImmersiveThemeMode.DARK -> true
        ImmersiveThemeMode.LIGHT -> false
        ImmersiveThemeMode.SYSTEM -> isSystemDark
    }

    val backgroundColor = if (isDarkMode) Color(0xFF0B0C0E) else Color(0xFFF5F6F8)
    val primaryTextColor = if (isDarkMode) Color.White else Color(0xFF111111)
    val topBarIconBg = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)

    var isShapeBottomSheetOpen by rememberSaveable { mutableStateOf(false) }
    var isQueueBottomSheetOpen by rememberSaveable { mutableStateOf(false) }

    val safePadding = WindowInsets.safeDrawing.asPaddingValues()

    var dragYAmount by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragYAmount.coerceAtLeast(0f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SwipeToDismissOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = animatedOffsetY
                alpha = (1f - (animatedOffsetY / 1000f)).coerceIn(0.2f, 1f)
            }
            .background(backgroundColor)
            .padding(safePadding)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragYAmount = 0f },
                    onDragEnd = {
                        if (dragYAmount > 120.dp.toPx()) {
                            onClose()
                        } else {
                            dragYAmount = 0f
                        }
                    },
                    onDragCancel = { dragYAmount = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragYAmount = (dragYAmount + dragAmount).coerceAtLeast(0f)
                    }
                )
            }
    ) {
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
                    .background(topBarIconBg)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = primaryTextColor
                )
            }

            IconButton(
                onClick = { isShapeBottomSheetOpen = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(topBarIconBg)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = primaryTextColor
                )
            }
        }

        if (isShapeBottomSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isShapeBottomSheetOpen = false },
                containerColor = if (isDarkMode) Color(0xFF16181D) else Color(0xFFFFFFFF),
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "앨범아트 모양 설정",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
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
                            color = primaryTextColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SeekBarStyle.entries.forEach { style ->
                                val isSelected = style == currentSeekBarStyle
                                val cardBg = if (isSelected) accentColor.copy(alpha = 0.25f) else (if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                val border = if (isSelected) BorderStroke(1.5.dp, accentColor) else BorderStroke(1.dp, if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f))

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
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = style.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = primaryTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "테마 모드 설정",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImmersiveThemeMode.entries.forEach { mode ->
                                val isSelected = mode == currentThemeMode
                                val cardBg = if (isSelected) accentColor.copy(alpha = 0.25f) else (if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                val border = if (isSelected) BorderStroke(1.5.dp, accentColor) else BorderStroke(1.dp, if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f))

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = cardBg,
                                    border = border,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            currentThemeMode = mode
                                            repository.saveImmersiveThemeMode(mode)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = primaryTextColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = !isAutoLaunchCharging
                                    isAutoLaunchCharging = next
                                    repository.saveAutoLaunchOnCharging(next)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "충전기 연결 시 스탠바이 자동 실행",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryTextColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "충전기를 연결하면 스탠바이 이머시브 모드를 자동으로 오픈합니다.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = primaryTextColor.copy(alpha = 0.65f)
                                )
                            }
                            Switch(
                                checked = isAutoLaunchCharging,
                                onCheckedChange = { enabled ->
                                    isAutoLaunchCharging = enabled
                                    repository.saveAutoLaunchOnCharging(enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accentColor
                                )
                            )
                        }
                    }
                }
            }
        }

        if (isQueueBottomSheetOpen) {
            QueueBottomSheet(
                playbackState = playbackState,
                accentColor = accentColor,
                onDismiss = { isQueueBottomSheetOpen = false }
            )
        }

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
                    onOpenQueue = { isQueueBottomSheetOpen = true }
                )
            }
        }
    }
}

@Preview(name = "Immersive Player Screen Preview", showBackground = true)
@Composable
fun ImmersivePlayerScreenPreview() {
    MaterialTheme {
        ImmersivePlayerScreen(
            playbackState = MediaPlaybackState(
                isPlaying = true,
                title = "SUPERNOVA",
                artist = "Shirakami Fubuki",
                positionMs = 85_000L,
                durationMs = 210_000L,
                packageName = "com.spotify.music"
            ),
            onClose = {},
            onPlayPause = {},
            onSkipPrevious = {},
            onSkipNext = {},
            onSeek = {}
        )
    }
}
