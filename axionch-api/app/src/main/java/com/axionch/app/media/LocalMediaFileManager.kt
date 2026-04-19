package com.axionch.app.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LocalMediaKind(
    val collectionFolder: String,
    val filenamePrefix: String,
    val defaultExtension: String,
    val defaultMimeType: String
) {
    VIDEO(
        collectionFolder = Environment.DIRECTORY_MOVIES,
        filenamePrefix = "creators_hub_video",
        defaultExtension = "mp4",
        defaultMimeType = "video/mp4"
    ),
    IMAGE(
        collectionFolder = Environment.DIRECTORY_PICTURES,
        filenamePrefix = "creators_hub_image",
        defaultExtension = "jpg",
        defaultMimeType = "image/jpeg"
    ),
    AUDIO(
        collectionFolder = Environment.DIRECTORY_MUSIC,
        filenamePrefix = "creators_hub_audio",
        defaultExtension = "m4a",
        defaultMimeType = "audio/mp4"
    )
}

data class ImportedLocalMedia(
    val localPath: String,
    val displayName: String,
    val mimeType: String?
)

object LocalMediaFileManager {
    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun importToAppStorage(
        context: Context,
        sourceUri: Uri,
        kind: LocalMediaKind
    ): ImportedLocalMedia {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(sourceUri)
        val sourceName = queryDisplayName(context, sourceUri)
        val extension = resolveExtension(sourceName, mimeType, kind.defaultExtension)
        val filename = buildFileName(kind.filenamePrefix, extension)
        val baseDir = context.getExternalFilesDir(kind.collectionFolder) ?: context.filesDir
        val importDir = File(baseDir, "imports").apply { mkdirs() }
        val targetFile = File(importDir, filename)

        resolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Unable to open source media stream." }
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val displayLabel = sourceName.ifBlank { targetFile.name }
        return ImportedLocalMedia(
            localPath = targetFile.absolutePath,
            displayName = displayLabel,
            mimeType = mimeType
        )
    }

    fun exportLocalFileToMediaStore(
        context: Context,
        sourcePath: String,
        kind: LocalMediaKind,
        preferredDisplayName: String? = null
    ): Uri {
        val sourceFile = File(sourcePath)
        require(sourceFile.exists()) { "Source file not found: $sourcePath" }
        require(sourceFile.isFile) { "Source path is not a file: $sourcePath" }

        val extension = sourceFile.extension.ifBlank { kind.defaultExtension }
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.lowercase(Locale.US))
            ?: kind.defaultMimeType
        val displayName = buildDisplayName(preferredDisplayName, sourceFile.nameWithoutExtension, extension)
        val contentUri = when (kind) {
            LocalMediaKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            LocalMediaKind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            LocalMediaKind.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${kind.collectionFolder}/CreatorsHub"
                )
            }
        }

        val resolver = context.contentResolver
        val outputUri = resolver.insert(contentUri, values)
            ?: error("Failed to allocate MediaStore output.")

        resolver.openOutputStream(outputUri).use { output ->
            requireNotNull(output) { "Unable to open MediaStore output stream." }
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
        return outputUri
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    return cursor.getString(index).orEmpty()
                }
            }
        }
        return ""
    }

    private fun resolveExtension(sourceName: String, mimeType: String?, fallback: String): String {
        val fromName = sourceName.substringAfterLast('.', "").ifBlank { "" }
        if (fromName.isNotBlank()) return fromName.lowercase(Locale.US)
        val fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType.orEmpty()).orEmpty()
        if (fromMime.isNotBlank()) return fromMime.lowercase(Locale.US)
        return fallback
    }

    private fun buildFileName(prefix: String, extension: String): String {
        val stamp = timestampFormat.format(Date())
        return "${prefix}_$stamp.$extension"
    }

    private fun buildDisplayName(
        preferredDisplayName: String?,
        sourceWithoutExt: String,
        extension: String
    ): String {
        val trimmed = preferredDisplayName?.trim().orEmpty()
        if (trimmed.isNotBlank()) {
            return if (trimmed.endsWith(".$extension", ignoreCase = true)) trimmed else "$trimmed.$extension"
        }
        val stamp = timestampFormat.format(Date())
        return "${sourceWithoutExt}_export_$stamp.$extension"
    }
}

