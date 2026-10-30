package com.librecuts.rust.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewPlayer(
    videoUri: String?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    onPositionChanged: (Long) -> Unit,
    onPlaybackEnded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(videoUri) {
        if (!videoUri.isNullOrEmpty()) {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // Playback loop with real-time playhead update
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
            while (isActive) {
                onPositionChanged(exoPlayer.currentPosition)
                delay(40) // Smooth ~25fps scrubber update
            }
        } else {
            exoPlayer.pause()
        }
    }

    // Scrubbing synchronization
    LaunchedEffect(currentPositionMs) {
        if (Math.abs(exoPlayer.currentPosition - currentPositionMs) > 120) {
            exoPlayer.seekTo(currentPositionMs)
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
