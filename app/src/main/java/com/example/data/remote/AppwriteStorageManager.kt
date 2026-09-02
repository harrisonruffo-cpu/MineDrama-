package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.Appwrite
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AppwriteStorageManager(private val context: Context) {
    private val TAG = "AppwriteStorage"

    suspend fun uploadVideo(
        uri: Uri,
        fileName: String,
        onProgress: (Float) -> Unit = {}
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (!Appwrite.isInitialized) {
                Appwrite.init(context)
            }
            val st = Appwrite.storage ?: return@withContext null
            Appwrite.ensureSession()

            val tempFile = createTempFileFromUri(uri, "upload_video_$fileName")
            val inputFile = InputFile.fromFile(tempFile)
            val targetBucket = Appwrite.BUCKET_MEDIA

            val fileResult = st.createFile(
                bucketId = targetBucket,
                fileId = ID.unique(),
                file = inputFile
            )

            val directUrl = Appwrite.getFileViewUrl(targetBucket, fileResult.id)
            Log.d(TAG, "Upload de vídeo com sucesso no Appwrite: $directUrl")
            tempFile.delete()
            directUrl
        } catch (e: Throwable) {
            Log.e(TAG, "Falha no upload do vídeo para o Appwrite: ${e.message}", e)
            null
        }
    }

    suspend fun uploadCover(
        uri: Uri,
        fileName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (!Appwrite.isInitialized) {
                Appwrite.init(context)
            }
            val st = Appwrite.storage ?: return@withContext null
            Appwrite.ensureSession()

            val tempFile = createTempFileFromUri(uri, "upload_cover_$fileName")
            val inputFile = InputFile.fromFile(tempFile)
            val targetBucket = Appwrite.BUCKET_MEDIA

            val fileResult = st.createFile(
                bucketId = targetBucket,
                fileId = ID.unique(),
                file = inputFile
            )

            val directUrl = Appwrite.getFileViewUrl(targetBucket, fileResult.id)
            Log.d(TAG, "Upload de capa com sucesso no Appwrite: $directUrl")
            tempFile.delete()
            directUrl
        } catch (e: Throwable) {
            Log.e(TAG, "Falha no upload da capa para o Appwrite: ${e.message}", e)
            null
        }
    }

    private fun createTempFileFromUri(uri: Uri, prefix: String): File {
        val extension = if (uri.toString().contains(".mp4", ignoreCase = true)) ".mp4" else ".tmp"
        val tempFile = File.createTempFile(prefix, extension, context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}
