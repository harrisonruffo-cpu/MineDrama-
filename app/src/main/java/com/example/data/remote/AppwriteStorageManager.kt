package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.Appwrite
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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

            val candidateBuckets = listOf(Appwrite.BUCKET_MEDIA, "videos", "media", "default").distinct()
            var directUrl: String? = null

            for (bucket in candidateBuckets) {
                try {
                    val fileResult = st.createFile(
                        bucketId = bucket,
                        fileId = ID.unique(),
                        file = inputFile
                    )
                    directUrl = Appwrite.getFileViewUrl(bucket, fileResult.id)
                    Appwrite.BUCKET_MEDIA = bucket
                    Log.d(TAG, "Vídeo enviado com sucesso para o bucket '$bucket' do Appwrite: $directUrl")
                    break
                } catch (bucketErr: Throwable) {
                    Log.w(TAG, "Falha ao enviar vídeo no bucket '$bucket': ${bucketErr.message}")
                }
            }

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
            val st = Appwrite.storage
            Appwrite.ensureSession()

            val tempFile = createTempFileFromUri(uri, "upload_cover_$fileName")
            var directUrl: String? = null

            if (st != null) {
                val inputFile = InputFile.fromFile(tempFile)
                val candidateBuckets = listOf(Appwrite.BUCKET_MEDIA, "covers", "media", "videos", "default").distinct()

                for (bucket in candidateBuckets) {
                    try {
                        val fileResult = st.createFile(
                            bucketId = bucket,
                            fileId = ID.unique(),
                            file = inputFile
                        )
                        directUrl = Appwrite.getFileViewUrl(bucket, fileResult.id)
                        Appwrite.BUCKET_MEDIA = bucket
                        Log.d(TAG, "Capa enviada com sucesso para o bucket '$bucket': $directUrl")
                        break
                    } catch (bucketErr: Throwable) {
                        Log.w(TAG, "Falha ao enviar capa no bucket '$bucket': ${bucketErr.message}")
                    }
                }
            }

            // Se o upload no bucket do Appwrite não concluiu, gera uma data URI persistente da imagem
            // para que a capa viaje nos metadados da nuvem e NUNCA dependa de arquivos locais temporários!
            if (directUrl == null) {
                directUrl = generateCompressedCloudImage(tempFile)
            }

            tempFile.delete()
            directUrl
        } catch (e: Throwable) {
            Log.e(TAG, "Falha no upload da capa para o Appwrite: ${e.message}", e)
            null
        }
    }

    private fun generateCompressedCloudImage(file: File): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val maxDim = 480
            val scale = (maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
            val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)

            val outStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 75, outStream)
            val base64 = Base64.encodeToString(outStream.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao gerar imagem otimizada para nuvem: ${e.message}")
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
