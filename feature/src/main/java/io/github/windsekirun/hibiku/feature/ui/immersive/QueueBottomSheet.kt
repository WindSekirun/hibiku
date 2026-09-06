package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.windsekirun.hibiku.domain.model.MediaPlaybackState
import io.github.windsekirun.hibiku.domain.repository.MediaPlaybackRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    playbackState: MediaPlaybackState,
    accentColor: Color,
    isDarkMode: Boolean = true,
    onDismiss: () -> Unit
) {
    val primaryTextColor = if (isDarkMode) Color.White else Color(0xFF111111)
    val secondaryTextColor = if (isDarkMode) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.6f)
    val items = playbackState.queueItems
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val targetSheetHeight = (screenHeightDp * 0.60f).coerceAtLeast(320.dp)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lazyListState = rememberLazyListState()

    val noOverscrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val isAtTop = lazyListState.firstVisibleItemIndex == 0 &&
                        lazyListState.firstVisibleItemScrollOffset == 0

                if (isAtTop && available.y > 0f) {
                    return Offset.Zero
                }

                return available
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDarkMode) Color(0xFF16181D) else Color(0xFFFFFFFF),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(targetSheetHeight)
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
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
                    color = primaryTextColor
                )

                Text(
                    text = playbackState.queueText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            if (items.isNotEmpty()) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(noOverscrollConnection),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(items) { index, queueItem ->
                        val isCurrent = (index == playbackState.queueIndex - 1) ||
                                (queueItem.title == playbackState.title)
                        val defaultBorder = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
                        val cardBorder = if (isCurrent) BorderStroke(1.5.dp, accentColor) else BorderStroke(1.dp, defaultBorder)
                        val defaultCardBg = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
                        val cardBg = if (isCurrent) accentColor.copy(alpha = 0.15f) else defaultCardBg

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = cardBg,
                            border = cardBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    MediaPlaybackRepository.skipToQueueItem(queueItem.queueId)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) accentColor else primaryTextColor.copy(alpha = 0.5f),
                                    modifier = Modifier.width(28.dp),
                                    textAlign = TextAlign.Center
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = queueItem.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = primaryTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (queueItem.artist.isNotBlank()) {
                                        Text(
                                            text = queueItem.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = secondaryTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isCurrent) {
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
            } else {
                val defaultCardBg = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = defaultCardBg,
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
                                color = primaryTextColor
                            )
                            Text(
                                text = playbackState.artist.ifEmpty { "StandBy Mode" },
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryTextColor
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
}

@Preview(name = "Queue Bottom Sheet Preview", showBackground = true)
@Composable
fun QueueBottomSheetPreview() {
    MaterialTheme {
        QueueBottomSheet(
            playbackState = MediaPlaybackState(
                isPlaying = true,
                title = "SUPERNOVA",
                artist = "Shirakami Fubuki"
            ),
            accentColor = Color(0xFF5CB3FF),
            onDismiss = {}
        )
    }
}
