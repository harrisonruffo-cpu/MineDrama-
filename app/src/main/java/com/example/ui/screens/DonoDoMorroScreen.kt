package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.util.DonoDoMorroManager
import com.example.data.util.YouTubeHelper
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DramaCrimson
import com.example.ui.theme.DramaCrimsonBright
import com.example.ui.theme.DramaGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.AppImageResolver

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DonoDoMorroScreen(
    donoDoMorroManager: DonoDoMorroManager,
    onPlayInFeed: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LocalContext.current
    val currentEp1Link by donoDoMorroManager.ep1Link.collectAsState()
    val currentEp1AltLink by donoDoMorroManager.ep1AltLink.collectAsState()
    val activePlayerNumber by donoDoMorroManager.activePlayer.collectAsState()

    val drama = remember(currentEp1Link, currentEp1AltLink, activePlayerNumber) {
        donoDoMorroManager.getDrama(activePlayerNumber)
    }

    val activeEp1Link = if (activePlayerNumber == 2) currentEp1AltLink else currentEp1Link

    var isPlayerActive by remember { mutableStateOf(false) }
    var showLinkEditDialog by remember { mutableStateOf(false) }
    var tempLinkInput1 by remember { mutableStateOf(currentEp1Link) }
    var tempLinkInput2 by remember { mutableStateOf(currentEp1AltLink) }
    var isBuffering by remember { mutableStateOf(true) }
    var isLiked by remember { mutableStateOf(true) }
    var likesCount by remember { mutableLongStateOf(drama.likesCount) }

    val vimeoId = remember(activeEp1Link) { YouTubeHelper.extractVimeoId(activeEp1Link) }
    val isVimeo = vimeoId != null || YouTubeHelper.isVimeoUrl(activeEp1Link)
    val driveId = remember(activeEp1Link) { YouTubeHelper.extractGoogleDriveFileId(activeEp1Link) }
    val isDrive = driveId != null
    val isDirect = remember(activeEp1Link) { YouTubeHelper.isDirectVideoUrl(activeEp1Link) }
    val videoId = remember(activeEp1Link) {
        YouTubeHelper.extractVideoId(activeEp1Link) ?: "dQw4w9WgXcQ"
    }

    val scrollState = rememberScrollState()

    // Diálogo para atualizar/configurar os links do Episódio 1 (Player 1 e Player 2 Alternativo)
    if (showLinkEditDialog) {
        AlertDialog(
            onDismissRequest = { showLinkEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = DramaCrimsonBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gerenciar Players do Ep. 1", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Configure os links para os dois players alternativos. Ambos rodam direto dentro do APK sem sair do app.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text("Player 1 Principal (Google Drive / YouTube):", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempLinkInput1,
                        onValueChange = { tempLinkInput1 = it },
                        placeholder = { Text("Link Google Drive ou YouTube", color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = DramaCrimson,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Player 2 Alternativo (Vimeo / MP4):", color = Color(0xFF1AB7EA), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempLinkInput2,
                        onValueChange = { tempLinkInput2 = it },
                        placeholder = { Text("https://vimeo.com/...", color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Color(0xFF1AB7EA),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        donoDoMorroManager.updateEpisode1Link(tempLinkInput1)
                        donoDoMorroManager.updateEpisode1AltLink(tempLinkInput2)
                        showLinkEditDialog = false
                        isPlayerActive = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Salvar e Assistir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLinkEditDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", color = TextPrimary)
                }
            },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(14.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
    ) {
        // ÁREA SUPERIOR: PLAYER DO YOUTUBE (OU CAPA INTERATIVA CAMUFLADA)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.Black)
        ) {
            if (isPlayerActive) {
                // PLAYER DO YOUTUBE EM TELA CHEIA DENTRO DO APP (NUNCA SAI DO APK)
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            setBackgroundColor(android.graphics.Color.BLACK)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                allowFileAccess = true
                                allowContentAccess = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                setSupportZoom(false)
                                builtInZoomControls = false
                                displayZoomControls = false
                                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                            }
                            android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webChromeClient = object : WebChromeClient() {
                                override fun getDefaultVideoPoster(): Bitmap? {
                                    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isBuffering = false
                                    // Injeta CSS para ocultar elementos de saída do YouTube
                                    view?.evaluateJavascript("""
                                        var s = document.createElement('style');
                                        s.innerHTML = '.ytp-youtube-button, .ytp-share-button, .ytp-watermark { display: none !important; }';
                                        document.head.appendChild(s);
                                    """.trimIndent(), null)
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val dest = request?.url?.toString() ?: ""
                                    if (dest.contains("youtube.com/watch") || dest.contains("youtu.be/") || dest.contains("drive.google.com/file/d/") || dest.contains("accounts.google.com")) {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(dest))
                                            view?.context?.startActivity(intent)
                                            return true
                                        } catch (e: Exception) {
                                            return false
                                        }
                                    }
                                    if (dest.startsWith("intent:") || dest.contains("play.google.com") || dest.startsWith("market:")) {
                                        return true // Bloqueia abertura de loja externa
                                    }
                                    return false
                                }
                            }
                            if (isVimeo && vimeoId != null) {
                                val vimeoHtml = YouTubeHelper.buildVimeoEmbedHtml(vimeoId)
                                loadDataWithBaseURL("https://vimeo.com", vimeoHtml, "text/html", "UTF-8", "https://vimeo.com")
                            } else if (isDrive && driveId != null) {
                                val driveHtml = YouTubeHelper.buildGoogleDriveEmbedHtml(driveId)
                                loadDataWithBaseURL("https://drive.google.com", driveHtml, "text/html", "UTF-8", "https://drive.google.com")
                            } else if (isDirect) {
                                val directHtml = YouTubeHelper.buildHtml5VideoHtml(activeEp1Link)
                                loadDataWithBaseURL(null, directHtml, "text/html", "UTF-8", null)
                            } else {
                                val embedHtml = YouTubeHelper.buildEmbedHtml(videoId)
                                loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", "https://www.youtube.com")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isBuffering) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DramaCrimsonBright)
                    }
                }

                // Indicador do Player Ativo com botão de troca rápida
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clickable {
                            val nextPlayer = if (activePlayerNumber == 1) 2 else 1
                            donoDoMorroManager.setActivePlayer(nextPlayer)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (activePlayerNumber == 2) Icons.Filled.Bolt else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (activePlayerNumber == 2) Color(0xFF1AB7EA) else DramaCrimsonBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (activePlayerNumber == 2) "Player 2: Vimeo" else "Player 1: Drive",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• Trocar",
                            color = DramaGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Botão para fechar o player e voltar à capa
                IconButton(
                    onClick = { isPlayerActive = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar Player", tint = Color.White)
                }
            } else {
                // CAPA DA NOVELA COM LINK CAMUFLADO: CLICAR DÁ PLAY DIRETO NO EPISÓDIO 1
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            // Link camuflado: ao clicar na capa da novela, inicia a reprodução do episódio 1!
                            isPlayerActive = true
                        }
                ) {
                    AsyncImage(
                        model = AppImageResolver.resolve(drama.bannerUrl.ifBlank { drama.coverUrl }),
                        contentDescription = "Capa Dono Do Morro",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradientes cinematográficos
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent,
                                        DarkBackground
                                    )
                                )
                            )
                    )

                    // Selo de Destaque Oficial
                    Surface(
                        color = DramaCrimson,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Whatshot, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SÉRIE EXCLUSIVA LITORAL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Botão Central de Play Camuflado na Capa
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .shadow(16.dp, CircleShape)
                            .background(DramaCrimson.copy(alpha = 0.95f), CircleShape)
                            .border(2.dp, DramaGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Assistir Dono Do Morro",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Legenda na parte inferior da capa
                    Text(
                        text = "Toque na capa para assistir o Episódio 1",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }
            }
        }

        // DETALHES DA NOVELA: TÍTULO, SINOPSE E BOTÃO DE PLAY
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = drama.title,
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "MALVADÃO - DJ RUFFO",
                        color = DramaGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Botão de Like
                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        likesCount = if (isLiked) likesCount + 1 else likesCount - 1
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Curtir",
                        tint = if (isLiked) DramaCrimson else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges informativas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFF1E2330),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "4K ULTRA HD",
                        color = DramaGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = Color(0xFF263238),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "⭐ 5.0 (980k views)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = Color(0xFF37474F),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "4 Episódios",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SINOPSE EXATA PEDIDA PELO USUÁRIO
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = DramaCrimsonBright, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sinopse",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = drama.description,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOTÃO 1: PLAYER PRINCIPAL DO EPISÓDIO 1 (GOOGLE DRIVE / YOUTUBE)
            Button(
                onClick = {
                    donoDoMorroManager.setActivePlayer(1)
                    isPlayerActive = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = DramaCrimson),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Assistir Episódio 1 (Player 1)",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BOTÃO 2: ASSISTIR 2 ALTERNATIVO (VIMEO HD) - EXATAMENTE O PEDIDO PELO USUÁRIO
            Button(
                onClick = {
                    donoDoMorroManager.setActivePlayer(2)
                    isPlayerActive = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1AB7EA) // Azul oficial vibrante Vimeo
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Assistir 2 Alternativo (Vimeo HD)",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SELETOR RÁPIDO DE PLAYERS (CHIPS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (activePlayerNumber == 1) DramaCrimson.copy(alpha = 0.25f) else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (activePlayerNumber == 1) DramaCrimson else Color.Transparent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            donoDoMorroManager.setActivePlayer(1)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (activePlayerNumber == 1) DramaCrimsonBright else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Player 1: Drive",
                            color = if (activePlayerNumber == 1) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (activePlayerNumber == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Surface(
                    color = if (activePlayerNumber == 2) Color(0xFF1AB7EA).copy(alpha = 0.25f) else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (activePlayerNumber == 2) Color(0xFF1AB7EA) else Color.Transparent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            donoDoMorroManager.setActivePlayer(2)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = if (activePlayerNumber == 2) Color(0xFF1AB7EA) else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Player 2: Vimeo",
                            color = if (activePlayerNumber == 2) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (activePlayerNumber == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botão secundário para assistir no modo Feed Vertical (Reels / TikTok)
            OutlinedButton(
                onClick = { onPlayInFeed(0) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.ViewStream, contentDescription = null, tint = DramaCrimsonBright)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Assistir no Modo Feed Vertical", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botão para gerenciar/colar link dos Players do Episódio 1
            TextButton(
                onClick = {
                    tempLinkInput1 = currentEp1Link
                    tempLinkInput2 = currentEp1AltLink
                    showLinkEditDialog = true
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Configurar / Trocar Links dos Players (Drive / Vimeo)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LISTA DE EPISÓDIOS DA NOVELA
            Text(
                text = "Episódios da Série",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            drama.episodes.forEachIndexed { index, ep ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (index == 0) Color(0xFF26191B) else DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (index == 0) {
                                isPlayerActive = true
                            } else {
                                onPlayInFeed(index)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail do episódio
                        Box(
                            modifier = Modifier
                                .size(width = 70.dp, height = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = AppImageResolver.resolve(drama.coverUrl),
                                contentDescription = ep.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayCircleFilled,
                                    contentDescription = null,
                                    tint = if (index == 0) DramaCrimsonBright else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = ep.title,
                                    color = if (index == 0) DramaCrimsonBright else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (index == 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = DramaCrimson,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "PRONTO",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (index == 0) {
                                    if (activePlayerNumber == 2) "Player 2: Vimeo HD ativo • Direto no APK"
                                    else "Player 1: Drive ativo • Direto no APK"
                                } else "Lançamento Exclusivo",
                                color = if (index == 0 && activePlayerNumber == 2) Color(0xFF1AB7EA) else TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
