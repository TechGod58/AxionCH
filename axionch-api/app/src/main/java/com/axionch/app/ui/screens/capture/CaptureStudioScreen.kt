package com.axionch.app.ui.screens.capture

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.media3.effect.Media3Effect
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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

private const val TAG = "CaptureStudio"

private data class RealtimeFilterPreset(
    val id: String,
    val name: String,
    val vibe: String,
    val effects: List<Effect>
)

private val REALTIME_FILTER_PRESETS = listOf(
    RealtimeFilterPreset(
        id = "clean",
        name = "Clean",
        vibe = "Natural camera look with no color transform.",
        effects = emptyList()
    ),
    RealtimeFilterPreset(
        id = "bright-pop",
        name = "Bright Pop",
        vibe = "Punchier contrast with social-ready color.",
        effects = listOf(
            Brightness(0.08f),
            Contrast(0.18f),
            RgbAdjustment.Builder()
                .setRedScale(1.04f)
                .setGreenScale(1.04f)
                .setBlueScale(1.06f)
                .build()
        )
    ),
    RealtimeFilterPreset(
        id = "mono",
        name = "Mono Dramatic",
        vibe = "High-impact grayscale storytelling look.",
        effects = listOf(
            RgbFilter.createGrayscaleFilter(),
            Contrast(0.24f)
        )
    ),
    RealtimeFilterPreset(
        id = "sepia",
        name = "Sepia Story",
        vibe = "Warm nostalgic tone for lifestyle content.",
        effects = listOf(
            Contrast(0.10f),
            RgbAdjustment.Builder()
                .setRedScale(1.18f)
                .setGreenScale(1.05f)
                .setBlueScale(0.82f)
                .build()
        )
    ),
    RealtimeFilterPreset(
        id = "cool-crisp",
        name = "Cool Crisp",
        vibe = "Cooler blues with a modern creator feel.",
        effects = listOf(
            Contrast(0.14f),
            RgbAdjustment.Builder()
                .setRedScale(0.94f)
                .setGreenScale(1.00f)
                .setBlueScale(1.14f)
                .build()
        )
    )
)

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun createVideoOutputOptions(context: Context): MediaStoreOutputOptions {
    val stamp = System.currentTimeMillis()
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "creators_hub_capture_$stamp")
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/CreatorsHub"
            )
        }
    }
    return MediaStoreOutputOptions.Builder(
        context.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).setContentValues(values).build()
}

@Composable
fun CaptureStudioScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    var hasCameraPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.CAMERA))
    }
    var hasAudioPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.RECORD_AUDIO))
    }
    var permissionPrompted by rememberSaveable { mutableStateOf(false) }

    var selectedPreset by remember { mutableStateOf(REALTIME_FILTER_PRESETS.first()) }
    var selectedCamera by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var recordAudio by rememberSaveable { mutableStateOf(true) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var recordingSeconds by remember { mutableStateOf(0L) }
    var savedVideoUri by remember { mutableStateOf("") }
    var statusText by remember {
        mutableStateOf("Grant camera access to start true realtime filtered recording.")
    }

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
            Log.e(TAG, "Media3 GPU pipeline error", throwable)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] == true
        hasAudioPermission = result[Manifest.permission.RECORD_AUDIO] == true
        statusText = if (hasCameraPermission) {
            "Permissions ready. Choose a filter and start recording."
        } else {
            "Camera permission is required to record."
        }
    }

    LaunchedEffect(hasCameraPermission, permissionPrompted) {
        if (!hasCameraPermission && !permissionPrompted) {
            permissionPrompted = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    LaunchedEffect(selectedPreset) {
        runCatching {
            media3Effect.setEffects(selectedPreset.effects)
            cameraController.setEffects(setOf(media3Effect))
        }.onFailure {
            statusText = "Failed to apply GPU filter: ${it.message}"
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
        onDispose {
            media3Effect.close()
        }
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
                    statusText = "Recording live with ${selectedPreset.name} filter..."
                }
                is VideoRecordEvent.Status -> {
                    recordingSeconds = event.recordingStats.recordedDurationNanos / 1_000_000_000L
                }
                is VideoRecordEvent.Finalize -> {
                    activeRecording = null
                    if (event.hasError()) {
                        statusText = "Recording failed: ${event.error}"
                    } else {
                        val uri = event.outputResults.outputUri.toString()
                        savedVideoUri = uri
                        statusText = "Saved filtered video to MediaStore."
                    }
                }
            }
        }
        try {
            val recording = cameraController.startRecording(
                outputOptions,
                audioConfig,
                mainExecutor,
                listener
            )
            activeRecording = recording
            statusText = if (recordAudio && !hasAudioPermission) {
                "Recording started with mic OFF (audio permission denied)."
            } else {
                "Recording started."
            }
        } catch (t: Throwable) {
            statusText = "Could not start recording: ${t.message}"
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Capture Studio", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                Text(
                    "Realtime CameraX preview + GPU filter pipeline for recording inside the app.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    controller = cameraController
                                }
                            },
                            update = { previewView ->
                                previewView.controller = cameraController
                            }
                        )
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.CAMERA,
                                        Manifest.permission.RECORD_AUDIO
                                    )
                                )
                            }
                        ) {
                            Text("Request Camera/Mic Permissions")
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
                            Text("Flip Camera")
                        }
                        Button(
                            onClick = { recordAudio = !recordAudio },
                            enabled = activeRecording == null
                        ) {
                            Text(if (recordAudio) "Mic: ON" else "Mic: OFF")
                        }
                        Button(
                            onClick = {
                                if (activeRecording == null) startRecording() else stopRecording()
                            },
                            enabled = hasCameraPermission
                        ) {
                            Text(if (activeRecording == null) "Start Recording" else "Stop Recording")
                        }
                    }
                }
            }
            item {
                Text("Realtime Filters", style = MaterialTheme.typography.titleMedium)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(REALTIME_FILTER_PRESETS) { preset ->
                        Button(
                            onClick = { selectedPreset = preset },
                            enabled = activeRecording == null || selectedPreset.id == preset.id
                        ) {
                            Text(if (selectedPreset.id == preset.id) "Selected: ${preset.name}" else preset.name)
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Status")
                        Text(statusText, style = MaterialTheme.typography.bodySmall)
                        Text("Filter vibe: ${selectedPreset.vibe}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Recording duration: ${
                                String.format(
                                    Locale.US,
                                    "%02d:%02d",
                                    recordingSeconds / 60L,
                                    recordingSeconds % 60L
                                )
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (savedVideoUri.isNotBlank()) {
                            Text("Saved video URI: $savedVideoUri", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
