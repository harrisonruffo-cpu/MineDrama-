package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Drama
import com.example.data.model.Episode
import com.example.ui.theme.*
import com.example.ui.viewmodel.DramaViewModel
import com.example.ui.viewmodel.VideoUploadViewModel

@Composable
fun PublishDramaScreen(
    viewModel: DramaViewModel,
    uploadViewModel: VideoUploadViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allDramas by viewModel.allDramas.collectAsState()
    val isPublishing by viewModel.isPublishing.collectAsState()
    val diagnosticResult by viewModel.diagnosticResult.collectAsState()
    val isRunningDiagnostic by viewModel.isRunningDiagnostic.collectAsState()

    val isUploading by uploadViewModel.isUploading.collectAsState()
    val uploadStatusMessage by uploadViewModel.uploadStatusMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Novo Drama, 1: Meus Vídeos / Gerenciar
    var showDiagnosticDialog by remember { mutableStateOf(false) }

    // Form states
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Romance") }
    var coverUrl by remember { mutableStateOf("") }
    val episodeList = remember { mutableStateListOf<Episode>() }

    // Dialog rename states
    var dramaToRename by remember { mutableStateOf<Drama?>(null) }
    var newDramaTitle by remember { mutableStateOf("") }

    // Dialog YouTube Episode states
    var showYouTubeDialog by remember { mutableStateOf(false) }
    var youtubeTitleInput by remember { mutableStateOf("") }
    var youtubeUrlInput by remember { mutableStateOf("") }

    // Pickers
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val epNum = episodeList.size + 1
            uploadViewModel.uploadVideo(uri, "ep_$epNum.mp4") { cloudUrl, localPath ->
                val chosenSource = cloudUrl ?: (localPath ?: uri.toString())
                episodeList.add(
                    Episode(
                        id = "temp_ep_$epNum",
                        episodeNumber = epNum,
                        title = "Episódio $epNum",
                        videoUrl = chosenSource,
                        localUri = localPath ?: uri.toString()
                    )
                )
                Toast.makeText(context, "Vídeo adicionado com sucesso!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadViewModel.uploadCover(uri, "cover.jpg") { url ->
                if (url != null) {
                    coverUrl = url
                    Toast.makeText(context, "Capa carregada!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Modal de Diagnóstico
    if (showDiagnosticDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BugReport, contentDescription = null, tint = DramaCrimsonBright, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diagnóstico da Nuvem (NYC)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isRunningDiagnostic) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = DramaCrimsonBright)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Testando conexão com Appwrite NYC...", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    } else if (diagnosticResult != null) {
                        val report = diagnosticResult!!
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (report.items.all { it.isSuccess }) Color(0xFF1B382B) else Color(0xFF38231B)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = report.summary,
                                color = if (report.items.all { it.isSuccess }) Color(0xFF81C784) else Color(0xFFFFB74D),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        report.items.forEach { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighlight.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (item.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                            contentDescription = null,
                                            tint = if (item.isSuccess) Color(0xFF4CAF50) else Color(0xFFE53935),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(item.service, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(item.message, color = TextSecondary, fontSize = 12.sp)
                                    if (!item.hint.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("💡 Dica: ${item.hint}", color = DramaGold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Clique em 'Iniciar Teste' para verificar a comunicação com o Appwrite e armazenamento.", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.runCloudDiagnostic() },
                    colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                    enabled = !isRunningDiagnostic
                ) {
                    Text(if (diagnosticResult == null) "Iniciar Teste" else "Repetir Teste")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiagnosticDialog = false }) {
                    Text("Fechar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Modal Renomear
    if (dramaToRename != null) {
        AlertDialog(
            onDismissRequest = { dramaToRename = null },
            title = { Text("Renomear Novela", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newDramaTitle,
                    onValueChange = { newDramaTitle = it },
                    label = { Text("Novo Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDramaTitle.isNotBlank()) {
                            viewModel.updateDramaTitle(dramaToRename!!.id, newDramaTitle)
                            dramaToRename = null
                            Toast.makeText(context, "Título atualizado!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { dramaToRename = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    if (showYouTubeDialog) {
        AlertDialog(
            onDismissRequest = { showYouTubeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SmartDisplay, contentDescription = null, tint = DramaCrimsonBright)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar via YouTube / Link", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Insira o link de um vídeo do YouTube, Shorts ou link direto de vídeo.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = youtubeTitleInput,
                        onValueChange = { youtubeTitleInput = it },
                        label = { Text("Título do Episódio") },
                        placeholder = { Text("Ex: Episódio ${episodeList.size + 1}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = youtubeUrlInput,
                        onValueChange = { youtubeUrlInput = it },
                        label = { Text("Link YouTube / Vídeo") },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (youtubeUrlInput.isNotBlank()) {
                            val epNum = episodeList.size + 1
                            val finalTitle = youtubeTitleInput.ifBlank { "Episódio $epNum" }
                            val cleanUrl = youtubeUrlInput.trim()
                            episodeList.add(
                                Episode(
                                    id = "temp_ep_$epNum",
                                    episodeNumber = epNum,
                                    title = finalTitle,
                                    videoUrl = cleanUrl,
                                    localUri = ""
                                )
                            )
                            youtubeTitleInput = ""
                            youtubeUrlInput = ""
                            showYouTubeDialog = false
                            Toast.makeText(context, "Episódio do YouTube adicionado!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                    enabled = youtubeUrlInput.isNotBlank()
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showYouTubeDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Estúdio de Publicação", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = {
                    showDiagnosticDialog = true
                    if (diagnosticResult == null) viewModel.runCloudDiagnostic()
                }
            ) {
                Icon(Icons.Filled.BugReport, contentDescription = "Diagnóstico", tint = DramaCrimsonBright)
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = DramaCrimson
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Novo Drama", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Gerenciar / Minhas Novelas", fontWeight = FontWeight.SemiBold) }
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Card Diagnóstico Rápido
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDiagnosticDialog = true
                                if (diagnosticResult == null) viewModel.runCloudDiagnostic()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.BugReport, contentDescription = null, tint = DramaCrimsonBright)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Diagnóstico da Nuvem (NYC)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Toque para verificar status de conexão", color = TextSecondary, fontSize = 11.sp)
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título da Novela") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Sinopse / Descrição") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { coverPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Capa", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { videoPickerLauncher.launch("video/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                            modifier = Modifier.weight(1.1f),
                            enabled = !isUploading
                        ) {
                            Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Vídeo MP4", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                youtubeTitleInput = "Episódio ${episodeList.size + 1}"
                                showYouTubeDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC4302B)),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Icon(Icons.Filled.SmartDisplay, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ YouTube", fontSize = 11.sp)
                        }
                    }
                }

                if (isUploading) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(uploadStatusMessage ?: "Processando upload...", color = DramaGold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(color = DramaCrimsonBright, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                if (coverUrl.isNotBlank()) {
                    item {
                        Text("Capa Selecionada:", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = "Capa",
                            modifier = Modifier
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                item {
                    Text("Episódios Adicionados (${episodeList.size}):", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                itemsIndexed(episodeList) { idx, ep ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("EP ${ep.episodeNumber}:", color = DramaGold, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ep.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (ep.videoUrl.contains("youtu")) "YouTube Link" else "Arquivo de Vídeo",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { episodeList.removeAt(idx) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remover", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (title.isNotBlank() && episodeList.isNotEmpty()) {
                                viewModel.publishDrama(
                                    title = title,
                                    description = description,
                                    genre = genre,
                                    coverUrl = coverUrl.ifBlank { "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?auto=format&fit=crop&w=600&q=80" },
                                    bannerUrl = coverUrl.ifBlank { "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?auto=format&fit=crop&w=1200&q=80" },
                                    episodes = episodeList.toList()
                                ) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Novela publicada e salva com sucesso!", Toast.LENGTH_LONG).show()
                                        title = ""
                                        description = ""
                                        coverUrl = ""
                                        episodeList.clear()
                                        selectedTab = 1
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Informe o título e adicione pelo menos 1 episódio (MP4 ou YouTube)!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isPublishing
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Salvar e Publicar Novela", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Aba 1: Gerenciar e Renomear
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (allDramas.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhum drama disponível para gerenciar.", color = TextSecondary)
                        }
                    }
                } else {
                    itemsIndexed(allDramas) { _, drama ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = drama.coverUrl.ifBlank { drama.bannerUrl },
                                    contentDescription = drama.title,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(drama.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${drama.genre} • ${drama.totalEpisodes} episódios", color = TextSecondary, fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = {
                                        dramaToRename = drama
                                        newDramaTitle = drama.title
                                    }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Renomear", tint = DramaGold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
