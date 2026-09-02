package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.Appwrite
import io.appwrite.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DiagnosticItem(
    val service: String,
    val target: String,
    val isSuccess: Boolean,
    val message: String,
    val hint: String? = null
)

data class CloudDiagnosticResult(
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<DiagnosticItem>,
    val summary: String
)

class CloudDiagnosticManager(private val context: Context) {
    private val TAG = "CloudDiagnostic"

    suspend fun runDiagnostic(): CloudDiagnosticResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<DiagnosticItem>()

        // 1. Appwrite Client & Session
        try {
            if (!Appwrite.isInitialized) {
                Appwrite.init(context)
            }
            val sessionOk = Appwrite.ensureSession()
            if (sessionOk) {
                results.add(
                    DiagnosticItem(
                        service = "Appwrite Auth",
                        target = "Sessão Anônima / Usuário",
                        isSuccess = true,
                        message = "Conectado com sucesso no Appwrite NYC Cloud (Project: ${Appwrite.PROJECT_ID})"
                    )
                )
            } else {
                results.add(
                    DiagnosticItem(
                        service = "Appwrite Auth",
                        target = "Sessão Anônima",
                        isSuccess = false,
                        message = "Falha ao inicializar sessão de usuário.",
                        hint = "Verifique o Project ID '${Appwrite.PROJECT_ID}' no Appwrite."
                    )
                )
            }
        } catch (e: Throwable) {
            results.add(
                DiagnosticItem(
                    service = "Appwrite Auth",
                    target = "Inicialização",
                    isSuccess = false,
                    message = "Erro: ${e.message}",
                    hint = "Verifique a conexão de internet ou Endpoint do Appwrite."
                )
            )
        }

        // 2. Appwrite Database (Database: litoral_novelas, Collection: dramas)
        try {
            val db = Appwrite.databases
            if (db != null) {
                val response = db.listDocuments(
                    databaseId = Appwrite.DATABASE_ID,
                    collectionId = Appwrite.COLLECTION_DRAMAS,
                    queries = listOf(Query.limit(1))
                )
                results.add(
                    DiagnosticItem(
                        service = "Appwrite Database",
                        target = "DB: ${Appwrite.DATABASE_ID} / Col: ${Appwrite.COLLECTION_DRAMAS}",
                        isSuccess = true,
                        message = "Sucesso! Total de novelas cadastradas: ${response.total}"
                    )
                )
            } else {
                results.add(
                    DiagnosticItem(
                        service = "Appwrite Database",
                        target = "DB: ${Appwrite.DATABASE_ID}",
                        isSuccess = false,
                        message = "Serviço de Banco não inicializado.",
                        hint = "Verifique a conectividade de rede."
                    )
                )
            }
        } catch (e: Throwable) {
            val err = e.message ?: "Desconhecido"
            val hint = when {
                err.contains("404") || err.contains("not found", ignoreCase = true) ->
                    "Banco '${Appwrite.DATABASE_ID}' ou Coleção '${Appwrite.COLLECTION_DRAMAS}' não foi criada no Appwrite."
                err.contains("401") || err.contains("unauthorized", ignoreCase = true) || err.contains("permission", ignoreCase = true) ->
                    "Permissão negada na Coleção '${Appwrite.COLLECTION_DRAMAS}'. Vá em Databases > dramas > Settings > Permissions > adicione 'Any' (Create, Read, Update, Delete)."
                else -> "Erro retornado: $err"
            }
            results.add(
                DiagnosticItem(
                    service = "Appwrite Database",
                    target = "DB: ${Appwrite.DATABASE_ID} / Col: ${Appwrite.COLLECTION_DRAMAS}",
                    isSuccess = false,
                    message = "Erro: $err",
                    hint = hint
                )
            )
        }

        // 3. Appwrite Storage Bucket
        try {
            val st = Appwrite.storage
            if (st != null) {
                val candidateBuckets = listOf(Appwrite.BUCKET_MEDIA, "videos", "media", "covers", "default").distinct()
                var foundBucket: String? = null
                var totalFiles = 0L
                var lastErr: String? = null
                for (bucketCandidate in candidateBuckets) {
                    try {
                        val fileList = st.listFiles(
                            bucketId = bucketCandidate,
                            queries = listOf(Query.limit(1))
                        )
                        foundBucket = bucketCandidate
                        totalFiles = fileList.total
                        Appwrite.BUCKET_MEDIA = bucketCandidate
                        break
                    } catch (e: Throwable) {
                        lastErr = e.message
                    }
                }

                if (foundBucket != null) {
                    results.add(
                        DiagnosticItem(
                            service = "Appwrite Storage",
                            target = "Bucket: $foundBucket (Vídeos e Capas)",
                            isSuccess = true,
                            message = "Sucesso! Bucket único conectado ($totalFiles arquivos salvos)."
                        )
                    )
                } else {
                    val err = lastErr ?: "Bucket não encontrado"
                    val hint = when {
                        err.contains("401") || err.contains("unauthorized", ignoreCase = true) || err.contains("permission", ignoreCase = true) ->
                            "Permissão negada no Bucket. Vá em Storage > seu bucket > Settings > Permissions > adicione 'Any' (Create, Read, Update)."
                        else ->
                            "Nenhum bucket encontrado. Crie 1 único bucket em Storage > Create Bucket (ex: ID: 'videos' ou 'media') e adicione permissão 'Any'."
                    }
                    results.add(
                        DiagnosticItem(
                            service = "Appwrite Storage",
                            target = "Bucket: ${Appwrite.BUCKET_MEDIA}",
                            isSuccess = false,
                            message = "Erro: $err",
                            hint = hint
                        )
                    )
                }
            } else {
                results.add(
                    DiagnosticItem(
                        service = "Appwrite Storage",
                        target = "Bucket: ${Appwrite.BUCKET_MEDIA}",
                        isSuccess = false,
                        message = "Serviço de Storage não inicializado."
                    )
                )
            }
        } catch (e: Throwable) {
            results.add(
                DiagnosticItem(
                    service = "Appwrite Storage",
                    target = "Bucket: ${Appwrite.BUCKET_MEDIA}",
                    isSuccess = false,
                    message = "Falha no Storage: ${e.message}",
                    hint = "Crie 1 bucket no Appwrite com permissão 'Any'."
                )
            )
        }

        val successCount = results.count { it.isSuccess }
        val totalCount = results.size
        val summary = if (successCount == totalCount) {
            "Todos os serviços do Appwrite NYC Cloud estão 100% operacionais e autorizados!"
        } else {
            "$successCount de $totalCount serviços conectados. Verifique as orientações acima para liberar o acesso."
        }

        CloudDiagnosticResult(
            items = results,
            summary = summary
        )
    }
}
