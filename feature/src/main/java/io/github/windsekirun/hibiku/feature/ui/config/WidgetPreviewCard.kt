package io.github.windsekirun.hibiku.feature.ui.config

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.windsekirun.hibiku.core.graphics.PaletteExtractor
import io.github.windsekirun.hibiku.core.graphics.WidgetBitmapRenderer
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.domain.model.RingStyle
import io.github.windsekirun.hibiku.domain.model.WidgetConfig
import io.github.windsekirun.hibiku.feature.R

@Composable
fun WidgetPreviewCard(
    playbackState: MediaPlaybackState,
    widgetConfig: WidgetConfig,
    modifier: Modifier = Modifier
) {
    // Determine effective track data (use live track info or sample fallback if empty)
    val hasLiveTrack = playbackState.title.isNotBlank()
    val effectiveTitle = if (hasLiveTrack) {
        playbackState.title
    } else {
        stringResource(R.string.sample_track_title)
    }

    val effectiveArtist = if (hasLiveTrack) {
        playbackState.artist.ifBlank { stringResource(R.string.unknown_artist) }
    } else {
        stringResource(R.string.sample_track_artist)
    }

    val effectiveProgress = if (hasLiveTrack) playbackState.progress else 0.65f
    val effectiveIsPlaying = if (hasLiveTrack) playbackState.isPlaying else true
    val effectiveArtwork = playbackState.albumArt

    // Resolve accent color
    val defaultColorInt = remember(widgetConfig.borderColorHex) {
        parseColorIntOrNull(widgetConfig.borderColorHex) ?: 0xFFFFFFFF.toInt()
    }

    val accentColorInt = remember(widgetConfig.useDynamicColor, effectiveArtwork, defaultColorInt) {
        if (widgetConfig.useDynamicColor) {
            PaletteExtractor.extractAccentColor(effectiveArtwork, defaultColorInt)
        } else {
            defaultColorInt
        }
    }

    val density = LocalDensity.current.density
    // 실제 위젯 4x2: 최소 높이 110dp, 앨범아트 120dp
    // 설정 카드 높이 150dp 기준으로 비율 환산: 120 * (150/110) ≈ 106dp → padding 제외하면 106dp 적절
    val artworkSizeDp = 106.dp
    val ringSizePx = (artworkSizeDp.value * density).toInt().coerceAtLeast(64)

    // Render artwork border bitmap
    val ringBitmap = remember(
        effectiveArtwork,
        accentColorInt,
        ringSizePx
    ) {
        WidgetBitmapRenderer.renderArtworkWithBorder(
            artwork = effectiveArtwork,
            borderColor = accentColorInt,
            sizePx = ringSizePx
        )
    }

    // Render blurred background bitmap if artwork present and blur option is enabled
    val bgBitmap = remember(effectiveArtwork, accentColorInt, density, widgetConfig.useBlurBackground) {
        if (widgetConfig.useBlurBackground && effectiveArtwork != null && !effectiveArtwork.isRecycled) {
            WidgetBitmapRenderer.renderBlurredBackground(
                artwork = effectiveArtwork,
                widthPx = (320 * density).toInt().coerceAtLeast(100),
                heightPx = (150 * density).toInt().coerceAtLeast(60),
                dimAlpha = 0.55f,
                cornerRadiusPx = 24f * density,
                strokeColor = 0
            )
        } else {
            null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xE612141C)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            // Blurred background image if available
            if (bgBitmap != null) {
                Image(
                    bitmap = bgBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Subtle border overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        color = Color((accentColorInt and 0x00FFFFFF) or 0x44000000),
                        shape = RoundedCornerShape(32.dp)
                    )
            )

            // Content row (4x2 widget preview - 실제 위젯과 동일한 패딩/크기 적용)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular artwork with progress ring
                Image(
                    bitmap = ringBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_album_art),
                    modifier = Modifier.size(artworkSizeDp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Information and Controls Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (widgetConfig.textVisible) {
                        Text(
                            text = effectiveArtist.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xB3FFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = effectiveTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Mock controls row matching mockup
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Previous button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_widget_prev),
                                contentDescription = stringResource(R.string.cd_previous),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Play / Pause button (solid white circle + dark icon)
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val playIconRes = if (effectiveIsPlaying) {
                                    R.drawable.ic_widget_pause
                                } else {
                                    R.drawable.ic_widget_play
                                }
                                Icon(
                                    painter = painterResource(playIconRes),
                                    contentDescription = stringResource(
                                        if (effectiveIsPlaying) R.string.cd_pause else R.string.cd_play
                                    ),
                                    tint = Color(0xFF11111B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Next button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_widget_next),
                                contentDescription = stringResource(R.string.cd_next),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // "Live" or "Sample" status badge in top-right corner
            Surface(
                shape = RoundedCornerShape(topEnd = 32.dp, bottomStart = 8.dp),
                color = if (hasLiveTrack) Color(0x9910B981) else Color(0x66000000),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = if (hasLiveTrack) "LIVE" else "SAMPLE",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Compose UI Previews
// -------------------------------------------------------------------------

@Preview(name = "Widget Preview Card - Live Preview", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun WidgetPreviewCardPreview() {
    MaterialTheme {
        WidgetPreviewCard(
            playbackState = MediaPlaybackState(
                isPlaying = true,
                title = "SUPERNOVA",
                artist = "Shirakami Fubuki",
                positionMs = 120_000L,
                durationMs = 210_000L
            ),
            widgetConfig = WidgetConfig(
                ringStyle = RingStyle.SQUIGGLY_WAVE,
                borderColorHex = "#5CB3FF",
                useDynamicColor = true,
                textVisible = true
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

