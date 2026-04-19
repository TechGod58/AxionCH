package com.axionch.app.ui.screens.media

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraEffect
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Effect
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.navigation.NavController
import java.util.Locale
import androidx.camera.core.CameraSelector
import androidx.camera.media3.effect.Media3Effect

private const val TAG = "RealtimeCaptureScreen"

private data class FilterGraphConfig(
    val mono: Boolean = false,
    val brightness: Float = 0.0f,
    val contrast: Float = 0.0f,
    val saturation: Float = 1.0f,
    val warmth: Float = 0.0f
)

private fun buildLiveFilterGraph(config: FilterGraphConfig): List<Effect> {
    val effects = mutableListOf<Effect>()
    if (config.mono) {
        effects += RgbFilter.createGrayscaleFilter()
    }
    if (kotlin.math.abs(config.brightness) > 0.001f) {
        effects += Brightness(config.brightness)
    }
    if (kotlin.math.abs(config.contrast) > 0.001f) {
        effects += Contrast(config.contrast)
    }
    val redScale = (1.0f + (config.warmth * 0.16f)) * config.saturation
    val blueScale = (1.0f - (config.warmth * 0.16f)) * config.saturation
    val greenScale = config.saturation
    effects += RgbAdjustment.Builder()
        .setRedScale(redScale.coerceIn(0.65f, 1.55f))
        .setGreenScale(greenScale.coerceIn(0.65f, 1.55f))
        .setBlueScale(blueScale.coerceIn(0.65f, 1.55f))
        .build()
    return effects
}

private fun createVideoOutputOptions(context: android.content.Context): MediaStoreOutputOptions {
    val stamp = System.currentTimeMillis()
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "creators_hub_rt_$stamp")
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/CreatorsHub")
        }
    }
    return MediaStoreOutputOptions.Builder(
        context.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).setContentValues(values).build()
}

private fun hasPermission(context: android.content.Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun RealtimeCaptureScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var hasCameraPermission by remember { mutableStateOf(hasPermission(context, Manifest.permission.CAMERA)) }
    var hasAudioPermission by remember { mutableStateOf(hasPermission(context, Manifest.permission.RECORD_AUDIO)) }
    var statusText by remember { mutableStateOf("Grant permissions to start CameraX + GPU recording.") }
    var selectedCamera by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var recordAudio by remember { mutableStateOf(true) }
    var recordingSeconds by remember { mutableStateOf(0L) }
    var savedVideoUri by remember { mutableStateOf("") }
    var filterGraph by remember { mutableStateOf(FilterGraphConfig()) }

    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.VIDEO_CAPTURE)
        }
    }
    val media3Effect = remember(context, mainExecutor) {
        Media3Effect(
            context,
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
            mainExecutor
        ) { throwable ->
            Log.e(TAG, "Live filter graph failure", throwable)
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] == true
        hasAudioPermission = result[Manifest.permission.RECORD_AUDIO] == true
        statusText = if (hasCameraPermission) {
            "Camera ready. Live filter graph is active for preview + encoded video."
        } else {
            "Camera permission is required."
        }
    }

    LaunchedEffect(filterGraph) {
        runCatching {
            media3Effect.setEffects(buildLiveFilterGraph(filterGraph))
            cameraController.setEffects(setOf(media3Effect))
        }.onFailure {
            statusText = "GPU graph apply failed: ${it.message}"
        }
    }

    DisposableEffect(lifecycleOwner, hasCameraPermission) {
        if (hasCameraPermission) {
            cameraController.bindToLifecycle(lifecycleOwner)
            cameraController.cameraSelector = selectedCamera
        } else {
            cameraController.unbind()
        }
        onDispose {
            activeRecording?.stop()
            activeRecording = null
            cameraController.unbind()
        }
    }

    DisposableEffect(media3Effect) {
        onDispose { media3Effect.close() }
    }

    fun startRecording() {
        if (!hasCameraPermission) {
            statusText = "Camera permission is required before recording."
            return
        }
        val outputOptions = createVideoOutputOptions(context)
        val audioConfig = if (recordAudio && hasAudioPermission) {
            AudioConfig.create(true)
        } else {
            AudioConfig.AUDIO_DISABLED
        }
        val listener = Consumer<VideoRecordEvent> { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    recordingSeconds = 0L
                    statusText = "Recording with live GPU graph..."
                }
                is VideoRecordEvent.Status -> {
                    recordingSeconds = event.recordingStats.recordedDurationNanos / 1_000_000_000L
                }
                is VideoRecordEvent.Finalize -> {
                    activeRecording = null
                    if (event.hasError()) {
                        statusText = "Recording failed: ${event.error}"
                    } else {
                        savedVideoUri = event.outputResults.outputUri.toString()
                        statusText = "Saved filtered recording."
                    }
                }
            }
        }
        runCatching {
            activeRecording = cameraController.startRecording(
                outputOptions,
                audioConfig,
                mainExecutor,
                listener
            )
        }.onFailure {
            statusText = "Could not start recording: ${it.message}"
        }
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
        statusText = "Stopping recording..."
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Realtime Capture", style = MaterialTheme.typography.headlineSmall)
            }
            item {
                Text(
                    "True CameraX + GPU pipeline with a live filter graph applied to preview and recording output.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            controller = cameraController
                        }
                    },
                    update = { preview ->
                        preview.controller = cameraController
                    }
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        permissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO
                            )
                        )
                    }) {
                        Text("Permissions")
                    }
                    Button(
                        onClick = {
                            selectedCamera = if (selectedCamera == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                            cameraController.cameraSelector = selectedCamera
                        },
                        enabled = hasCameraPermission && activeRecording == null
                    ) {
                        Text(if (selectedCamera == CameraSelector.DEFAULT_BACK_CAMERA) "Back" else "Front")
                    }
                    Button(onClick = { recordAudio = !recordAudio }, enabled = activeRecording == null) {
                        Text(if (recordAudio) "Mic On" else "Mic Off")
                    }
                    Button(onClick = { if (activeRecording == null) startRecording() else stopRecording() }, enabled = hasCameraPermission) {
                        Text(if (activeRecording == null) "Record" else "Stop")
                    }
                }
            }
            item {
                Text("Live Filter Graph", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Button(
                    onClick = { filterGraph = filterGraph.copy(mono = !filterGraph.mono) },
                    enabled = activeRecording == null
                ) {
                    Text(if (filterGraph.mono) "Mono: ON" else "Mono: OFF")
                }
            }
            item {
                Text("Brightness: ${"%.2f".format(Locale.US, filterGraph.brightness)}")
                Slider(
                    value = filterGraph.brightness,
                    onValueChange = { filterGraph = filterGraph.copy(brightness = it) },
                    valueRange = -0.20f..0.30f
                )
            }
            item {
                Text("Contrast: ${"%.2f".format(Locale.US, filterGraph.contrast)}")
                Slider(
                    value = filterGraph.contrast,
                    onValueChange = { filterGraph = filterGraph.copy(contrast = it) },
                    valueRange = -0.30f..0.40f
                )
            }
            item {
                Text("Saturation: ${"%.2f".format(Locale.US, filterGraph.saturation)}")
                Slider(
                    value = filterGraph.saturation,
                    onValueChange = { filterGraph = filterGraph.copy(saturation = it) },
                    valueRange = 0.60f..1.50f
                )
            }
            item {
                Text("Warmth: ${"%.2f".format(Locale.US, filterGraph.warmth)}")
                Slider(
                    value = filterGraph.warmth,
                    onValueChange = { filterGraph = filterGraph.copy(warmth = it) },
                    valueRange = -1.0f..1.0f
                )
            }
            item {
                Text(statusText, style = MaterialTheme.typography.bodySmall)
            }
            item {
                Text(
                    "Duration: ${
                        String.format(
                            Locale.US,
                            "%02d:%02d",
                            recordingSeconds / 60L,
                            recordingSeconds % 60L
                        )
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (savedVideoUri.isNotBlank()) {
                item {
                    Text("Saved URI: $savedVideoUri", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Button(onClick = { navController.popBackStack() }) { Text("Back") }
            }
        }
    }
}
