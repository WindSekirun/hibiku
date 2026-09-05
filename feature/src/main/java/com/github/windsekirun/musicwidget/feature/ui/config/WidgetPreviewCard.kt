package com.github.windsekirun.musicwidget.feature.ui.config

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.windsekirun.musicwidget.core.graphics.PaletteExtractor
import com.github.windsekirun.musicwidget.core.graphics.WidgetBitmapRenderer
import com.github.windsekirun.musicwidget.domain.model.MediaPlaybackState
import com.github.windsekirun.musicwidget.domain.model.WidgetConfig
import com.github.windsekirun.musicwidget.feature.R

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
    val ringSizePx = (80 * density).toInt().coerceAtLeast(64)

    // Render artwork ring bitmap
    val ringBitmap = remember(
        effectiveArtwork,
        effectiveProgress,
        effectiveIsPlaying,
        widgetConfig.ringStyle,
        accentColorInt,
        ringSizePx
    ) {
        WidgetBitmapRenderer.renderArtworkWithRing(
            artwork = effectiveArtwork,
            progress = effectiveProgress,
            isPlaying = effectiveIsPlaying,
            ringStyle = widgetConfig.ringStyle,
            ringColor = accentColorInt,
            trackColor = 0x33FFFFFF,
            sizePx = ringSizePx
        )
    }

    // Render blurred background bitmap if artwork present
    val bgBitmap = remember(effectiveArtwork, accentColorInt, density) {
        if (effectiveArtwork != null && !effectiveArtwork.isRecycled) {
            WidgetBitmapRenderer.renderBlurredBackground(
                artwork = effectiveArtwork,
                widthPx = (320 * density).toInt().coerceAtLeast(100),
                heightPx = (150 * density).toInt().coerceAtLeast(60),
                dimAlpha = 0.55f,
                cornerRadiusPx = 24f * density,
                strokeColor = (accentColorInt and 0x00FFFFFF) or 0x33000000
            )
        } else {
            null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xE61C1B1F)
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
                        shape = RoundedCornerShape(24.dp)
                    )
            )

            // Content row (4x2 widget preview representation)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular artwork with progress ring
                Image(
                    bitmap = ringBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_album_art),
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Information and Controls Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (widgetConfig.textVisible) {
                        Text(
                            text = effectiveTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = effectiveArtist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xBBFFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Mock controls row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Previous button
                        Surface(
                            shape = CircleShape,
                            color = Color(0x33FFFFFF),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_widget_prev),
                                    contentDescription = stringResource(R.string.cd_previous),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Play / Pause button
                        Surface(
                            shape = CircleShape,
                            color = Color(accentColorInt),
                            modifier = Modifier.size(40.dp)
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
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Next button
                        Surface(
                            shape = CircleShape,
                            color = Color(0x33FFFFFF),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_widget_next),
                                    contentDescription = stringResource(R.string.cd_next),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // "Live" or "Sample" status badge in top-right corner
            Surface(
                shape = RoundedCornerShape(topEnd = 24.dp, bottomStart = 8.dp),
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
