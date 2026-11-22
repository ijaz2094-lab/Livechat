package com.example.ui.video

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs

enum class StudioFilter(val label: String, val tintBrush: Brush?) {
    NATURAL("Natural", null),
    WARM_GLOW(
        "Warm Glow",
        Brush.verticalGradient(
            listOf(Color(0x28FF7E5F), Color(0x18FEB47B), Color(0x222B1055))
        )
    ),
    NEON_POP(
        "Neon Pop",
        Brush.linearGradient(
            listOf(Color(0x28FF2E7E), Color(0x1800E5FF))
        )
    ),
    STAGE_NOIR(
        "Stage Indigo",
        Brush.verticalGradient(
            listOf(Color(0x381A0B3B), Color(0x180B0714), Color(0x40120824))
        )
    ),
}

@Composable
fun CameraBroadcasterPreview(
    useFrontCamera: Boolean,
    torchEnabled: Boolean,
    studioFilter: StudioFilter,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(boundCamera, torchEnabled, useFrontCamera) {
        val cam = boundCamera ?: return@LaunchedEffect
        if (cam.cameraInfo.hasFlashUnit()) {
            cam.cameraControl.enableTorch(torchEnabled && !useFrontCamera)
        }
    }

    DisposableEffect(lifecycleOwner, useFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var provider: ProcessCameraProvider? = null

        val listener = Runnable {
            try {
                provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val preferredSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                val selector = when {
                    provider?.hasCamera(preferredSelector) == true -> preferredSelector
                    provider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) == true -> CameraSelector.DEFAULT_BACK_CAMERA
                    provider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }
                provider?.unbindAll()
                if (selector != null) {
                    boundCamera = provider?.bindToLifecycle(lifecycleOwner, selector, preview)
                }
            } catch (e: Exception) {
                Log.w("CameraBroadcaster", "CameraX bind failed: ${e.message}", e)
            }
        }

        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            try {
                provider?.unbindAll()
            } catch (_: Exception) {
            }
            boundCamera = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        studioFilter.tintBrush?.let { brush ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush)
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun rememberMicrophoneLevel(
    hasAudioPermission: Boolean,
    micMuted: Boolean,
): Float {
    var normalizedLevel by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(hasAudioPermission, micMuted) {
        if (!hasAudioPermission || micMuted) {
            normalizedLevel = 0f
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val sampleRate = 16000
            val minBuffer = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBuffer <= 0) return@withContext

            var recorder: AudioRecord? = null
            try {
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuffer * 2,
                )
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    return@withContext
                }
                recorder.startRecording()
                val buffer = ShortArray(minBuffer / 2)
                while (isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var maxAmp = 0
                        for (i in 0 until read) {
                            val amp = abs(buffer[i].toInt())
                            if (amp > maxAmp) maxAmp = amp
                        }
                        normalizedLevel = (maxAmp / 12000f).coerceIn(0.05f, 1f)
                    }
                    delay(80L)
                }
            } catch (e: Exception) {
                Log.w("AudioMeter", "AudioRecord unavailable: ${e.message}")
            } finally {
                try {
                    recorder?.stop()
                } catch (_: Exception) {
                }
                try {
                    recorder?.release()
                } catch (_: Exception) {
                }
            }
        }
    }
    return normalizedLevel
}
