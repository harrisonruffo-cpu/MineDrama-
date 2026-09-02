package com.example.data.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object MediaStorageHelper {
    private const val TAG = "MediaStorageHelper"

    fun copyUriToLocalStorage(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val mediaDir = File(context.filesDir, "media_uploads")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }
            // Ensure proper file extension
            val safeName = if (fileName.contains(".")) fileName else "$fileName.mp4"
            val destination = File(mediaDir, safeName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            val absolutePath = destination.absolutePath
            Log.d(TAG, "Arquivo de mídia salvo localmente em: $absolutePath")
            absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Erro salvando arquivo local: ${e.message}", e)
            null
        }
    }
}
