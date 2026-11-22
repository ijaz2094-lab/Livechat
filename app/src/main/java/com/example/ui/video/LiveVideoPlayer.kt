package com.example.ui.video

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R

fun coverDrawableForKey(coverKey: String): Int = when (coverKey) {
    "acoustic" -> R.drawable.img_stream_acoustic_1795346073476
    "dance" -> R.drawable.img_stream_dance_1795346073544
    "gaming" -> R.drawable.img_stream_gaming_1795346073371
    "talkshow" -> R.drawable.img_stream_talkshow_1795346073063
    else -> R.drawable.img_stream_acoustic_1795346073476
}

@OptIn(UnstableApi::class)
@Composable
fun LiveVideoPlayer(
    streamUrl: String,
    coverKey: String,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isBuffering by remember(streamUrl) { mutableStateOf(true) }
    var playbackError by remember(streamUrl) { mutableStateOf(false) }

    val exoPlayer = remember(context, streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
            prepare()
        }
    }

    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playbackError = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0B0714))) {
        if (playbackError || streamUrl.isBlank()) {
            Image(
                painter = painterResource(id = coverDrawableForKey(coverKey)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x660B0714), Color(0x220B0714), Color(0xBB0B0714))
                        )
                    )
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        player = exoPlayer
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (isBuffering) {
                Image(
                    painter = painterResource(id = coverDrawableForKey(coverKey)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(42.dp)
                        .align(Alignment.Center),
                )
            }
        }
    }
}
