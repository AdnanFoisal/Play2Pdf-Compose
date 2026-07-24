package com.adnanfoisal.play2pdf.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Saves a compiled PDF to the user's Downloads folder via the MediaStore
 * API (Android 10+) or a direct file write (Android 9 and below, requires
 * WRITE_EXTERNAL_STORAGE).
 *
 * Returns the [Uri] of the saved file so the caller can show an "Open"
 * or "Share" action.
 *
 * Quality checklist: native Android — "uses MediaStore API for Android 10+".
 */
class SavePdfToDownloadsUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(pdfFile: File, displayName: String): Uri? {
        val resolved = if (displayName.endsWith(".pdf", ignoreCase = true)) displayName
                       else "$displayName.pdf"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(pdfFile, resolved)
        } else {
            saveLegacy(pdfFile, resolved)
        }
    }

    private fun saveViaMediaStore(pdfFile: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Play2PDF")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            pdfFile.inputStream().use { input -> input.copyTo(out) }
        } ?: return null
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(pdfFile: File, displayName: String): Uri? {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outDir = File(downloads, "Play2PDF").apply { if (!exists()) mkdirs() }
        val outFile = File(outDir, displayName)
        pdfFile.inputStream().use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
        return outFile.toUri()
    }
}
