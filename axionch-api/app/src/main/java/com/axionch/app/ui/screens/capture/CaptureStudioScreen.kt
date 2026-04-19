package com.axionch.app.ui.screens.capture

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "CaptureStudio"
private const val CAPTURE_AV_PROFILE_PREFS = "capture_av_profiles"
private const val CAPTURE_AV_PROFILE_KEY = "profiles_json"

private data class RealtimeFilterPreset(
    val id: String,
    val name: String,
    val vibe: String,
    val effects: List<Effect>
)

private data class LiveFilterGraphState(
    val mono: Boolean = false,
    val brightness: Float = 0.0f,
    val contrast: Float = 0.0f,
    val saturation: Float = 1.0f,
    val warmth: Float = 0.0f
)

private data class CaptureAvProfile(
    val name: String,
    val cameraFacing: String,
    val recordAudio: Boolean,
    val previewFormat: String,
    val outputDeviceId: Int?,
    val outputVolume: Float
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

private fun buildLiveFilterGraphEffects(graph: LiveFilterGraphState): List<Effect> {
    val effects = mutableListOf<Effect>()
    if (graph.mono) {
        effects += RgbFilter.createGrayscaleFilter()
    }
    if (kotlin.math.abs(graph.brightness) > 0.001f) {
        effects += Brightness(graph.brightness)
    }
    if (kotlin.math.abs(graph.contrast) > 0.001f) {
        effects += Contrast(graph.contrast)
    }
    val redScale = (1.0f + (graph.warmth * 0.16f)) * graph.saturation
    val blueScale = (1.0f - (graph.warmth * 0.16f)) * graph.saturation
    val greenScale = graph.saturation
    effects += RgbAdjustment.Builder()
        .setRedScale(redScale.coerceIn(0.65f, 1.55f))
        .setGreenScale(greenScale.coerceIn(0.65f, 1.55f))
        .setBlueScale(blueScale.coerceIn(0.65f, 1.55f))
        .build()
    return effects
}

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

private fun AudioDeviceInfo.displayName(): String {
    val product = productName?.toString()?.trim().orEmpty()
    return if (product.isBlank()) "Device $id" else "$product (#$id)"
}

private object CaptureAvProfileStore {
    fun load(context: Context): List<CaptureAvProfile> {
        val prefs = context.getSharedPreferences(CAPTURE_AV_PROFILE_PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(CAPTURE_AV_PROFILE_KEY, "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val profiles = mutableListOf<CaptureAvProfile>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val name = obj.optString("name").trim()
            if (name.isBlank()) continue
            val cameraFacing = obj.optString("cameraFacing", "back")
            val recordAudio = obj.optBoolean("recordAudio", true)
            val previewFormat = obj.optString("previewFormat", "phone")
            val outputDeviceId = if (obj.has("outputDeviceId") && !obj.isNull("outputDeviceId")) {
                obj.optInt("outputDeviceId")
            } else {
                null
            }
            val outputVolume = obj.optDouble("outputVolume", 0.5).toFloat().coerceIn(0f, 1f)
            profiles += CaptureAvProfile(
                name = name,
                cameraFacing = cameraFacing,
                recordAudio = recordAudio,
                previewFormat = previewFormat,
                outputDeviceId = outputDeviceId,
                outputVolume = outputVolume
            )
        }
        return profiles
    }

    fun save(context: Context, profiles: List<CaptureAvProfile>) {
        val array = JSONArray()
        profiles.forEach { profile ->
            val obj = JSONObject().apply {
                put("name", profile.name)
                put("cameraFacing", profile.cameraFacing)
                put("recordAudio", profile.recordAudio)
                put("previewFormat", profile.previewFormat)
                if (profile.outputDeviceId != null) {
                    put("outputDeviceId", profile.outputDeviceId)
                } else {
                    put("outputDeviceId", JSONObject.NULL)
                }
                put("outputVolume", profile.outputVolume.coerceIn(0f, 1f).toDouble())
            }
            array.put(obj)
        }
        context.getSharedPreferences(CAPTURE_AV_PROFILE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CAPTURE_AV_PROFILE_KEY, array.toString())
            .apply()
    }
}

@Composable
fun CaptureStudioScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val scope = rememberCoroutineScope()
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var hasCameraPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.CAMERA))
    }
    var hasAudioPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.RECORD_AUDIO))
    }
    var permissionPrompted by rememberSaveable { mutableStateOf(false) }

    var selectedPreset by remember { mutableStateOf(REALTIME_FILTER_PRESETS.first()) }
    var useCustomGraph by rememberSaveable { mutableStateOf(false) }
    var customGraph by remember { mutableStateOf(LiveFilterGraphState()) }
    var selectedCamera by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var recordAudio by rememberSaveable { mutableStateOf(true) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var recordingSeconds by remember { mutableStateOf(0L) }
    var savedVideoUri by remember { mutableStateOf("") }
    var statusText by remember {
        mutableStateOf("Grant camera access to start true realtime filtered recording.")
    }
    var previewFormat by rememberSaveable { mutableStateOf("phone") }
    var outputDevices by remember { mutableStateOf<List<AudioDeviceInfo>>(emptyList()) }
    var selectedOutputDeviceId by rememberSaveable { mutableStateOf<Int?>(null) }
    var outputVolume by remember { mutableStateOf(0.5f) }
    var micLevel by remember { mutableStateOf(0f) }
    var micMeterJob by remember { mutableStateOf<Job?>(null) }
    var avProfileNameInput by rememberSaveable { mutableStateOf("") }
    var avProfiles by remember(context) { mutableStateOf(CaptureAvProfileStore.load(context)) }
    var selectedAvProfileName by rememberSaveable { mutableStateOf("") }
    var avProfileStatus by remember { mutableStateOf("") }

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

    LaunchedEffect(selectedPreset, useCustomGraph, customGraph) {
        runCatching {
            val effects = if (useCustomGraph) {
                buildLiveFilterGraphEffects(customGraph)
            } else {
                selectedPreset.effects
            }
            media3Effect.setEffects(effects)
            cameraController.setEffects(setOf(media3Effect))
        }.onFailure {
            statusText = "Failed to apply GPU filter: ${it.message}"
        }
    }

    fun refreshOutputDevices() {
        val available = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        outputDevices = available
        if (selectedOutputDeviceId == null && available.isNotEmpty()) {
            selectedOutputDeviceId = available.first().id
        }
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        outputVolume = (current.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
    }

    fun setMusicVolume(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        outputVolume = clamped
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val desired = (clamped * maxVol).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, desired, 0)
    }

    fun currentCameraFacing(): String {
        return if (selectedCamera == CameraSelector.DEFAULT_FRONT_CAMERA) "front" else "back"
    }

    fun stopMicMeter() {
        micMeterJob?.cancel()
        micMeterJob = null
        micLevel = 0f
    }

    fun startMicMeter() {
        if (micMeterJob != null) return
        if (!hasAudioPermission) {
            statusText = "Record audio permission is required for mic meter."
            return
        }
        micMeterJob = scope.launch(Dispatchers.IO) {
            runCatching {
                val sampleRate = 44_100
                val minSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(2_048)
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    minSize
                )
                recorder.startRecording()
                val sampleBuffer = ShortArray(minSize / 2)
                while (isActive) {
                    val read = recorder.read(sampleBuffer, 0, sampleBuffer.size, AudioRecord.READ_BLOCKING)
                    if (read > 0) {
                        var peak = 0f
                        for (index in 0 until read) {
                            val normalized = abs(sampleBuffer[index].toInt()) / 32768f
                            if (normalized > peak) peak = normalized
                        }
                        withContext(Dispatchers.Main) {
                            micLevel = (micLevel * 0.65f) + (peak * 0.35f)
                        }
                    }
                    delay(35)
                }
                recorder.stop()
                recorder.release()
            }.onFailure {
                withContext(Dispatchers.Main) {
                    statusText = "Mic meter error: ${it.message}"
                }
            }
            withContext(Dispatchers.Main) {
                micMeterJob = null
                micLevel = 0f
            }
        }
    }

    fun saveCurrentAvProfile() {
        val profileName = avProfileNameInput.trim()
        if (profileName.isBlank()) {
            avProfileStatus = "Enter a profile name to save."
            return
        }
        val profile = CaptureAvProfile(
            name = profileName,
            cameraFacing = currentCameraFacing(),
            recordAudio = recordAudio,
            previewFormat = if (previewFormat == "desktop") "desktop" else "phone",
            outputDeviceId = selectedOutputDeviceId,
            outputVolume = outputVolume.coerceIn(0f, 1f)
        )
        val updated = (avProfiles.filterNot { it.name.equals(profileName, ignoreCase = true) } + profile)
            .sortedBy { it.name.lowercase(Locale.US) }
        avProfiles = updated
        CaptureAvProfileStore.save(context, updated)
        selectedAvProfileName = profile.name
        avProfileStatus = "Saved AV profile '${profile.name}'."
    }

    fun applyAvProfile(profile: CaptureAvProfile) {
        selectedCamera = if (profile.cameraFacing == "front") {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        cameraController.cameraSelector = selectedCamera
        recordAudio = profile.recordAudio
        previewFormat = if (profile.previewFormat == "desktop") "desktop" else "phone"
        selectedOutputDeviceId = profile.outputDeviceId
        setMusicVolume(profile.outputVolume)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && profile.outputDeviceId != null) {
            val selectedDevice = outputDevices.firstOrNull { it.id == profile.outputDeviceId }
            if (selectedDevice != null) {
                audioManager.setCommunicationDevice(selectedDevice)
            }
        }
        selectedAvProfileName = profile.name
        avProfileStatus = "Loaded AV profile '${profile.name}'."
    }

    fun deleteAvProfile(profileName: String) {
        val updated = avProfiles.filterNot { it.name.equals(profileName, ignoreCase = true) }
        avProfiles = updated
        CaptureAvProfileStore.save(context, updated)
        if (selectedAvProfileName.equals(profileName, ignoreCase = true)) {
            selectedAvProfileName = ""
        }
        avProfileStatus = "Deleted AV profile '$profileName'."
    }

    DisposableEffect(lifecycleOwner, hasCameraPermission) {
        if (hasCameraPermission) {
            cameraController.bindToLifecycle(lifecycleOwner)
            cameraController.cameraSelector = selectedCamera
        } else {
            cameraController.unbind()
        }
        onDispose {
            stopMicMeter()
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

    LaunchedEffect(Unit) {
        refreshOutputDevices()
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
                        val previewHeight = if (previewFormat == "phone") 360.dp else 240.dp
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(previewHeight),
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    scaleType = if (previewFormat == "phone") {
                                        PreviewView.ScaleType.FILL_CENTER
                                    } else {
                                        PreviewView.ScaleType.FIT_CENTER
                                    }
                                    controller = cameraController
                                }
                            },
                            update = { previewView ->
                                previewView.scaleType = if (previewFormat == "phone") {
                                    PreviewView.ScaleType.FILL_CENTER
                                } else {
                                    PreviewView.ScaleType.FIT_CENTER
                                }
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
                            onClick = { previewFormat = if (previewFormat == "phone") "desktop" else "phone" },
                            enabled = activeRecording == null
                        ) {
                            Text(if (previewFormat == "phone") "Preview: Phone 9:16" else "Preview: Desktop 16:9")
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Audio/Video Settings", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Sections for video, microphone, and speaker/output routing.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Video", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    selectedCamera = CameraSelector.DEFAULT_BACK_CAMERA
                                    cameraController.cameraSelector = selectedCamera
                                },
                                enabled = hasCameraPermission && activeRecording == null
                            ) {
                                Text(if (selectedCamera == CameraSelector.DEFAULT_BACK_CAMERA) "Back*" else "Back")
                            }
                            Button(
                                onClick = {
                                    selectedCamera = CameraSelector.DEFAULT_FRONT_CAMERA
                                    cameraController.cameraSelector = selectedCamera
                                },
                                enabled = hasCameraPermission && activeRecording == null
                            ) {
                                Text(if (selectedCamera == CameraSelector.DEFAULT_FRONT_CAMERA) "Front*" else "Front")
                            }
                        }

                        Text("Microphone", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = ::startMicMeter, enabled = micMeterJob == null) { Text("Start Mic Meter") }
                            Button(onClick = ::stopMicMeter, enabled = micMeterJob != null) { Text("Stop Mic Meter") }
                        }
                        LinearProgressIndicator(progress = { micLevel.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text("Mic level: ${(micLevel * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)

                        Text("Speakers / Output", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = ::refreshOutputDevices) { Text("Refresh Outputs") }
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val selectedDevice = outputDevices.firstOrNull { it.id == selectedOutputDeviceId }
                                        if (selectedDevice != null) {
                                            audioManager.setCommunicationDevice(selectedDevice)
                                            statusText = "Requested output route: ${selectedDevice.displayName()}"
                                        }
                                    } else {
                                        statusText = "Direct output route selection needs Android 12+."
                                    }
                                }
                            ) {
                                Text("Route Output")
                            }
                        }
                        if (outputDevices.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(outputDevices) { device ->
                                    Button(onClick = { selectedOutputDeviceId = device.id }) {
                                        Text(
                                            if (selectedOutputDeviceId == device.id) {
                                                "${device.displayName()}*"
                                            } else {
                                                device.displayName()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Text("Output volume: ${(outputVolume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = outputVolume,
                            onValueChange = { setMusicVolume(it) },
                            valueRange = 0f..1f
                        )
                        LinearProgressIndicator(progress = { outputVolume.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())

                        Text("Save AV Profile", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = avProfileNameInput,
                            onValueChange = { avProfileNameInput = it },
                            label = { Text("Profile Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = ::saveCurrentAvProfile) {
                            Text("Save / Update Profile")
                        }
                        if (avProfiles.isEmpty()) {
                            Text(
                                "No AV profiles yet. Save one for one-tap switching.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(avProfiles) { profile ->
                                    Button(onClick = { applyAvProfile(profile) }) {
                                        Text(
                                            if (selectedAvProfileName.equals(profile.name, ignoreCase = true)) {
                                                "${profile.name}*"
                                            } else {
                                                profile.name
                                            }
                                        )
                                    }
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(avProfiles) { profile ->
                                    Button(onClick = { deleteAvProfile(profile.name) }) {
                                        Text("Delete ${profile.name}")
                                    }
                                }
                            }
                        }
                        if (avProfileStatus.isNotBlank()) {
                            Text(avProfileStatus, style = MaterialTheme.typography.bodySmall)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { useCustomGraph = !useCustomGraph },
                        enabled = activeRecording == null
                    ) {
                        Text(if (useCustomGraph) "Graph Mode: On" else "Graph Mode: Presets")
                    }
                }
            }
            if (useCustomGraph) {
                item {
                    Text("Custom Graph Nodes", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    Text("Brightness: ${"%.2f".format(Locale.US, customGraph.brightness)}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = customGraph.brightness,
                        onValueChange = { customGraph = customGraph.copy(brightness = it) },
                        valueRange = -0.20f..0.30f
                    )
                }
                item {
                    Text("Contrast: ${"%.2f".format(Locale.US, customGraph.contrast)}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = customGraph.contrast,
                        onValueChange = { customGraph = customGraph.copy(contrast = it) },
                        valueRange = -0.30f..0.40f
                    )
                }
                item {
                    Text("Saturation: ${"%.2f".format(Locale.US, customGraph.saturation)}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = customGraph.saturation,
                        onValueChange = { customGraph = customGraph.copy(saturation = it) },
                        valueRange = 0.60f..1.50f
                    )
                }
                item {
                    Text("Warmth: ${"%.2f".format(Locale.US, customGraph.warmth)}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = customGraph.warmth,
                        onValueChange = { customGraph = customGraph.copy(warmth = it) },
                        valueRange = -1.0f..1.0f
                    )
                }
                item {
                    Button(
                        onClick = { customGraph = customGraph.copy(mono = !customGraph.mono) },
                        enabled = activeRecording == null
                    ) {
                        Text(if (customGraph.mono) "Monochrome: On" else "Monochrome: Off")
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
                        Text(
                            if (useCustomGraph) {
                                "Filter graph: custom nodes (brightness/contrast/saturation/warmth)"
                            } else {
                                "Filter vibe: ${selectedPreset.vibe}"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
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
