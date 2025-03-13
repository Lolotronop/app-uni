package com.andronncollmider.argus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView

const val TAG = "VideoFeed"

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoFeed(
    modifier: Modifier = Modifier,
    uri: String,
    objects: MutableList<DetectedObject>?,
    selectedObject: Int,
    displayNames: Boolean
) {
    // FIXME: this is the biggest hack of all time
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 20.dp, max = 230.dp)
//            .height(220.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        //                .background(MaterialTheme.colorScheme.error),
    ) {
        val textMeasurer = rememberTextMeasurer()

        val intervals = remember { FloatArray(2) }
        intervals[0] = 10f
        intervals[1] = 4f
        var phase by remember { mutableFloatStateOf(0f) }

        PlayerScreen(uri)

        android.util.Log.d(TAG, "VideoFeed: ${objects?.size}")

        Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
            objects?.forEachIndexed { index, obj ->
                val x1 = obj.x1 * size.width
                val y1 = obj.y1 * size.height
                val x2 = obj.x2 * size.width
                val y2 = obj.y2 * size.height

                val path = Path()
                path.moveTo(x1, y1)
                path.lineTo(x2, y1)
                path.lineTo(x2, y2)
                path.lineTo(x1, y2)
                path.lineTo(x1, y1)
                path.close()

                if (index == selectedObject) {
                    val effect = PathEffect.dashPathEffect(intervals = intervals, phase = phase)
                    drawPath(path, obj.color, style = Stroke(width = 5f, pathEffect = effect))
                    drawRect(obj.color.copy(0.2f), Offset(x1, y1), Size(x2 - x1, y2 - y1))
                    phase = (phase + 0.3f) % 14
                } else {
                    drawPath(path, obj.color, style = Stroke(width = 3f))
                }
                val measuredText =
                    textMeasurer.measure(AnnotatedString(obj.text), TextStyle(fontSize = 14.sp))
                val height = y2 - y1
                var offset = Offset(x1, y1 - measuredText.size.height)
                if (y1 - measuredText.size.height < 14) {
                    offset = Offset(x1, y1 + height)
                }
                if (displayNames) {
                    drawRect(Color.White, offset, measuredText.size.toSize())
                    drawText(measuredText, Color.Black, offset)
                }
            }
        })
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(uri: String) {
    val context = LocalContext.current
    val source: MediaSource = remember(key1 = uri) {
        RtspMediaSource.Factory().setForceUseRtpTcp(true).createMediaSource(MediaItem.fromUri(uri))
    }
    val exoPlayer = remember(key1 = uri) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            setMediaSource(source)
            prepare()
        }
    }
    exoPlayer.volume = 0f
    Log.i("HHH", "----Video player created")
    Log.d("HHH", "${exoPlayer.videoSize.height.dp}")
    VideoSurface(modifier = Modifier.fillMaxWidth(), exoPlayer = exoPlayer)
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoSurface(modifier: Modifier = Modifier, exoPlayer: ExoPlayer) {
    // TODO: test if this actually works
    AndroidView(factory = { context ->
        PlayerView(context).apply {
            player = exoPlayer
        }
    }, modifier = Modifier.fillMaxWidth())
//    AndroidExternalSurface(modifier = modifier, onInit = {
//        onSurface { surface, _, _ ->
//            exoPlayer.setVideoSurface(surface)
//            exoPlayer.play()
//            Log.i("HHH", "------CREATED SURFACE")
//            surface.onDestroyed {
//                exoPlayer.setVideoSurface(null)
//                Log.i("HHH", "------Destroyed")
//            }
//        }
//    })
}

