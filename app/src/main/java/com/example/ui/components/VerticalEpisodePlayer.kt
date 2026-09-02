package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
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

    val driveFileId = remember(videoSource) {
        YouTubeHelper.extractGoogleDriveFileId(videoSource)
    }
    val isDrive = driveFileId != null

    val vimeoVideoId = remember(videoSource) {
        YouTubeHelper.extractVimeoId(videoSource)
    }
    val isVimeo = vimeoVideoId != null || YouTubeHelper.isVimeoUrl(videoSource)

    val isWebViewVideo = (isYouTube && youtubeVideoId != null) || isDrive || isVimeo

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Configuração robusta do ExoPlayer para streams diretos (MP4 / HLS)
    val exoPlayer = remember(videoSource, isWebViewVideo) {
        if (isWebViewVideo) {
            null
        } else {
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build()

                ExoPlayer.Builder(context)
                    .setAudioAttributes(audioAttributes, true)
                    .setWakeMode(C.WAKE_MODE_NETWORK)
                    .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    .build()
                    .apply {
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

                            override fun onRenderedFirstFrame() {
                                isVideoBuffering = false
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
                exoPlayer?.stop()
                exoPlayer?.release()
            } catch (_: Throwable) {}
        }
    }

    DisposableEffect(isYouTube, youtubeVideoId) {
        onDispose {
            try {
                webViewRef?.apply {
                    loadUrl("about:blank")
                    onPause()
                    pauseTimers()
                }
            } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(isPlaying, isPlayerPaused, isYouTube) {
        if (isYouTube) {
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
        // Thumbnail de fundo para transição suave
        val posterImage = remember(youtubeVideoId, drama.coverUrl) {
            if (drama.id.contains("dono_do_morro", ignoreCase = true) || drama.title.contains("Morro", ignoreCase = true)) {
                com.example.ui.util.AppImageResolver.resolve(drama.coverUrl)
            } else if (youtubeVideoId != null) {
                YouTubeHelper.getThumbnailUrl(youtubeVideoId)
            } else {
                com.example.ui.util.AppImageResolver.resolve(drama.coverUrl)
            }
        }

        if (isVideoBuffering) {
            AsyncImage(
                model = posterImage,
                contentDescription = drama.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isWebViewVideo) {
            // Renderização do YouTube ou Google Drive via WebView 100% interna sem sair do APK
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        setBackgroundColor(android.graphics.Color.BLACK)
                        isFocusable = true
                        isFocusableInTouchMode = true

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
                                isVideoBuffering = false
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val destUrl = request?.url?.toString() ?: ""
                                if (destUrl.contains("youtube.com/watch") || destUrl.contains("youtu.be/") || destUrl.contains("drive.google.com/file/d/") || destUrl.contains("accounts.google.com")) {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(destUrl))
                                        view?.context?.startActivity(intent)
                                        return true
                                    } catch (e: Exception) {
                                        return false
                                    }
                                }
                                if (destUrl.startsWith("intent:") || destUrl.contains("play.google.com") || destUrl.startsWith("market:")) {
                                    return true
                                }
                                return false
                            }
                        }

                        if (isDrive && driveFileId != null) {
                            val embedHtml = YouTubeHelper.buildGoogleDriveEmbedHtml(driveFileId)
                            loadDataWithBaseURL("https://drive.google.com", embedHtml, "text/html", "UTF-8", "https://drive.google.com")
                        } else if (isVimeo && vimeoVideoId != null) {
                            val embedHtml = YouTubeHelper.buildVimeoEmbedHtml(vimeoVideoId)
                            loadDataWithBaseURL("https://vimeo.com", embedHtml, "text/html", "UTF-8", "https://vimeo.com")
                        } else if (youtubeVideoId != null) {
                            val embedHtml = YouTubeHelper.buildEmbedHtml(youtubeVideoId)
                            loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", "https://www.youtube.com")
                        }
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Player de Vídeo ExoPlayer (MP4 / HLS)
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Camada de Gestos para Vídeos Diretos
        if (!isYouTube) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isYouTube, youtubeVideoId) {
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

        // Indicador de Qualidade HD / Série Exclusiva (Sem sair do APK)
        if (isYouTube && youtubeVideoId != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tv,
                        contentDescription = "Player Nativo",
                        tint = DramaCrimsonBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PLAYER INTERNO",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Overlay de Pausa
        if (isPlayerPaused) {
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

        // Indicador de Buffer
        if (isVideoBuffering) {
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

        // Informações da Novela
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

        // Ações Laterais
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Curtir
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

            // Episódios
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

            // Próximo
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
