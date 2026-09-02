package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.Drama
import com.example.data.model.Episode
import com.example.data.util.VideoUrlResolver
import com.example.data.util.YouTubeHelper
import com.example.ui.theme.DramaCrimson
import com.example.ui.theme.DramaCrimsonBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@SuppressLint("SetJavaScriptEnabled")
@OptIn(UnstableApi::class)
@Composable
fun VerticalEpisodePlayer(
    drama: Drama,
    episode: Episode,
    isPlaying: Boolean,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onOpenEpisodesSheet: () -> Unit,
    onNextEpisode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showHeartAnimation by remember { mutableStateOf(false) }
    var isVideoBuffering by remember { mutableStateOf(true) }
    var isPlayerPaused by remember { mutableStateOf(false) }

    val videoSource = remember(episode) {
        if (!episode.localUri.isNullOrBlank()) {
            episode.localUri
        } else {
            VideoUrlResolver.resolve(episode.videoUrl)
        }
    }

    val isYouTube = remember(videoSource) {
        YouTubeHelper.isYouTubeUrl(videoSource)
    }

    val youtubeVideoId = remember(videoSource) {
        YouTubeHelper.extractVideoId(videoSource)
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val exoPlayer = remember(videoSource, isYouTube) {
        if (isYouTube) {
            null
        } else {
            try {
                ExoPlayer.Builder(context).build().apply {
                    if (!videoSource.isNullOrBlank()) {
                        val mediaItem = MediaItem.fromUri(Uri.parse(videoSource))
                        setMediaItem(mediaItem)
                        repeatMode = Player.REPEAT_MODE_ONE
                        prepare()
                        playWhenReady = isPlaying
                    }
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            isVideoBuffering = state == Player.STATE_BUFFERING
                        }
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            isVideoBuffering = false
                        }
                    })
                }
            } catch (e: Throwable) {
                null
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                exoPlayer?.release()
            } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(isPlaying, isPlayerPaused, isYouTube) {
        if (isYouTube) {
            isVideoBuffering = false
            try {
                if (isPlaying && !isPlayerPaused) {
                    webViewRef?.evaluateJavascript("playVideo();", null)
                } else {
                    webViewRef?.evaluateJavascript("pauseVideo();", null)
                }
            } catch (_: Throwable) {}
        } else {
            try {
                exoPlayer?.playWhenReady = isPlaying && !isPlayerPaused
            } catch (_: Throwable) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isYouTube && youtubeVideoId != null) {
            // Player Integrado de YouTube com Aceleração por Hardware ativa (evita tela preta)
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        setBackgroundColor(android.graphics.Color.BLACK)

                        webChromeClient = object : WebChromeClient() {
                            override fun getDefaultVideoPoster(): Bitmap? {
                                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                return false
                            }
                        }

                        val embedHtml = YouTubeHelper.buildEmbedHtml(youtubeVideoId)
                        loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", "https://www.youtube.com")
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Player de Vídeo ExoPlayer (MP4 / Streams Locais ou Remotos)
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                showHeartAnimation = true
                                if (!isLiked) {
                                    onToggleLike()
                                }
                            },
                            onTap = {
                                isPlayerPaused = !isPlayerPaused
                                exoPlayer?.playWhenReady = !isPlayerPaused
                            }
                        )
                    }
            )
        }

        // Overlay de Pausa (para ExoPlayer)
        if (isPlayerPaused && !isYouTube) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Pausado",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }

        // Loading do Vídeo
        if (isVideoBuffering && !isYouTube) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = DramaCrimsonBright,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // Coração animado de Double Tap
        DoubleTapHeartAnimation(
            show = showHeartAnimation,
            onAnimationEnd = { showHeartAnimation = false }
        )

        // Gradiente Inferior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                    )
                )
        )

        // Informações da Novela (Inferior Esquerdo)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 80.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DramaCrimson
                ) {
                    Text(
                        text = drama.genre,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EP ${episode.episodeNumber}/${drama.totalEpisodes}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = drama.title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = episode.title,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            if (drama.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = drama.description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }
        }

        // Ações Laterais (Inferior Direito)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Botão de Curtir
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Curtir",
                    tint = if (isLiked) DramaCrimsonBright else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Lista de Episódios
            IconButton(
                onClick = onOpenEpisodesSheet,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoLibrary,
                    contentDescription = "Episódios",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Próximo Episódio
            IconButton(
                onClick = onNextEpisode,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Próximo",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
