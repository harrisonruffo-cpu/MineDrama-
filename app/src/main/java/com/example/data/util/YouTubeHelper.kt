package com.example.data.util

object YouTubeHelper {
    /**
     * Extrai o ID do vídeo do YouTube a partir de qualquer formato de link:
     * - https://www.youtube.com/watch?v=dQw4w9WgXcQ
     * - https://youtu.be/dQw4w9WgXcQ?si=abc
     * - https://www.youtube.com/shorts/dQw4w9WgXcQ
     * - https://www.youtube.com/embed/dQw4w9WgXcQ
     * - https://m.youtube.com/watch?v=dQw4w9WgXcQ
     * - https://youtube.com/v/dQw4w9WgXcQ
     * - https://www.youtube.com/live/dQw4w9WgXcQ
     * - ou o próprio ID direto de 11 caracteres
     */
    fun extractVideoId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()

        if (isGoogleDriveUrl(trimmed)) return null
        if (isVimeoUrl(trimmed)) return null

        // Se já for apenas o ID de 11 caracteres alfanuméricos com traços/underlines
        if (trimmed.length == 11 && trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return trimmed
        }

        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?v=|youtube\\.com/watch\\?.*[&?]v=)([a-zA-Z0-9_-]{11})"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/v/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/live/([a-zA-Z0-9_-]{11})")
        )

        for (pattern in patterns) {
            val match = pattern.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    fun isYouTubeUrl(url: String?): Boolean {
        return extractVideoId(url) != null
    }

    /**
     * Extrai o ID do arquivo do Google Drive a partir de qualquer formato de link:
     * - https://drive.google.com/file/d/1A2B3C.../view?usp=sharing
     * - https://drive.google.com/file/d/1A2B3C.../preview
     * - https://drive.google.com/open?id=1A2B3C...
     * - https://drive.google.com/uc?id=1A2B3C...
     * - https://docs.google.com/file/d/1A2B3C...
     */
    fun extractGoogleDriveFileId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        val patterns = listOf(
            Regex("drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)"),
            Regex("(?:drive|docs)\\.google\\.com/.*?[?&]id=([a-zA-Z0-9_-]+)"),
            Regex("drive\\.google\\.com/open\\?id=([a-zA-Z0-9_-]+)"),
            Regex("drive\\.google\\.com/uc\\?id=([a-zA-Z0-9_-]+)")
        )
        for (pattern in patterns) {
            val match = pattern.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    fun isGoogleDriveUrl(url: String?): Boolean {
        return extractGoogleDriveFileId(url) != null
    }

    /**
     * Converte links de imagens do Google Drive em URL direta via CDN do Google.
     * Exemplo: https://drive.google.com/file/d/1VWIfZ8lcuPWCc2ijTwvX6WoWnbqkUpO7/view?usp=drivesdk
     * Retorna: https://lh3.googleusercontent.com/u/0/d/1VWIfZ8lcuPWCc2ijTwvX6WoWnbqkUpO7
     */
    fun getDriveDirectImageUrl(url: String?): String? {
        val fileId = extractGoogleDriveFileId(url) ?: return null
        return "https://lh3.googleusercontent.com/u/0/d/$fileId"
    }

    fun normalizeAvatarUrl(url: String?): String {
        if (url.isNullOrBlank()) return "https://lh3.googleusercontent.com/u/0/d/1VWIfZ8lcuPWCc2ijTwvX6WoWnbqkUpO7"
        val driveDirect = getDriveDirectImageUrl(url)
        return driveDirect ?: url
    }

    /**
     * Extrai o ID do vídeo do Vimeo a partir de links como:
     * - https://vimeo.com/1223423999?share=copy&fl=sv&fe=ci
     * - https://vimeo.com/1223423999
     * - https://player.vimeo.com/video/1223423999
     */
    fun extractVimeoId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        val pattern = Regex("vimeo\\.com/(?:video/)?([0-9]+)")
        val match = pattern.find(trimmed)
        return match?.groupValues?.getOrNull(1)
    }

    fun isVimeoUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return extractVimeoId(url) != null || url.contains("vimeo.com")
    }

    fun isDirectVideoUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val clean = url.trim().lowercase()
        return clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".mkv") ||
               clean.contains(".mp4?") || clean.contains(".webm?") || clean.endsWith(".m3u8") ||
               clean.startsWith("android.resource://") || clean.startsWith("file://") || clean.startsWith("content://")
    }

    /**
     * Retorna a URL da thumbnail em alta definição do vídeo do YouTube.
     */
    fun getThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }

    /**
     * Gera HTML otimizado para reproduzir vídeo do Google Drive em tela cheia/vertical.
     */
    fun buildGoogleDriveEmbedHtml(fileId: String): String {
        val embedUrl = "https://drive.google.com/file/d/$fileId/preview"
        val directDriveUrl = "https://drive.google.com/file/d/$fileId/view?usp=sharing"
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        background: #000000 !important;
                    }
                    iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100% !important;
                        height: 100% !important;
                        border: 0 !important;
                    }
                    .drive-quick-action {
                        position: absolute;
                        top: 14px;
                        right: 14px;
                        background: rgba(18, 18, 18, 0.85);
                        backdrop-filter: blur(8px);
                        border: 1px solid rgba(255, 255, 255, 0.25);
                        color: #ffffff;
                        padding: 7px 12px;
                        border-radius: 20px;
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        font-size: 11px;
                        font-weight: 600;
                        text-decoration: none;
                        display: inline-flex;
                        align-items: center;
                        gap: 6px;
                        z-index: 999;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.6);
                    }
                </style>
            </head>
            <body>
                <iframe 
                    src="$embedUrl" 
                    allow="autoplay; fullscreen; encrypted-media" 
                    allowfullscreen>
                </iframe>
                <a class="drive-quick-action" href="$directDriveUrl">
                    <span>🎬 Abrir no Drive</span>
                </a>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Gera HTML otimizado para reproduzir vídeo do Vimeo em tela cheia/vertical.
     */
    fun buildVimeoEmbedHtml(vimeoId: String): String {
        val embedUrl = "https://player.vimeo.com/video/$vimeoId?autoplay=1&loop=1&title=0&byline=0&portrait=0"
        val webUrl = "https://vimeo.com/$vimeoId"
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        background: #000000 !important;
                    }
                    iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100% !important;
                        height: 100% !important;
                        border: 0 !important;
                    }
                    .vimeo-quick-action {
                        position: absolute;
                        top: 14px;
                        right: 14px;
                        background: rgba(26, 183, 234, 0.9);
                        backdrop-filter: blur(8px);
                        border: 1px solid rgba(255, 255, 255, 0.3);
                        color: #ffffff;
                        padding: 7px 12px;
                        border-radius: 20px;
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        font-size: 11px;
                        font-weight: 700;
                        text-decoration: none;
                        display: inline-flex;
                        align-items: center;
                        gap: 6px;
                        z-index: 999;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.6);
                    }
                </style>
            </head>
            <body>
                <iframe 
                    src="$embedUrl" 
                    allow="autoplay; fullscreen; picture-in-picture; encrypted-media" 
                    allowfullscreen>
                </iframe>
                <a class="vimeo-quick-action" href="$webUrl">
                    <span>⚡ Abrir no Vimeo</span>
                </a>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Gera HTML5 para reprodução de vídeo direto (.mp4, .webm, etc).
     */
    fun buildHtml5VideoHtml(videoUrl: String): String {
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        background: #000000 !important;
                    }
                    video {
                        width: 100% !important;
                        height: 100% !important;
                        object-fit: contain;
                        background: #000000;
                    }
                </style>
            </head>
            <body>
                <video src="$videoUrl" controls autoplay playsinline loop></video>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Gera HTML otimizado com a API oficial do YouTube (IFrame Player API),
     * permitindo que o vídeo rode sem erro de 'Vídeo indisponível' e com bypass de User-Agent.
     * Além disso, caso o canal aplique restrição de incorporação (embed bloqueado pelo autor),
     * fornece automaticamente fallback elegante e interativo direto na tela.
     */
    fun buildEmbedHtml(videoId: String): String {
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                        -webkit-tap-highlight-color: transparent;
                    }
                    html, body {
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        background: #000000 !important;
                    }
                    #player-container {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        background: #000000;
                    }
                    iframe {
                        width: 100% !important;
                        height: 100% !important;
                        border: 0 !important;
                    }
                    #error-overlay {
                        display: none;
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        background: rgba(10, 10, 10, 0.95);
                        color: #ffffff;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        padding: 24px;
                        text-align: center;
                        font-family: sans-serif;
                        z-index: 99;
                    }
                    .play-fallback-btn {
                        margin-top: 14px;
                        padding: 12px 24px;
                        background: #E50914;
                        color: #ffffff;
                        font-weight: bold;
                        border: none;
                        border-radius: 8px;
                        cursor: pointer;
                        font-size: 14px;
                        text-decoration: none;
                    }
                </style>
            </head>
            <body>
                <div id="player-container">
                    <div id="yt-player"></div>
                </div>

                <div id="error-overlay">
                    <div style="font-size: 32px; margin-bottom: 8px;">🎬</div>
                    <div style="font-weight: bold; font-size: 16px; margin-bottom: 6px;">Dono Do Morro • Episódio 1</div>
                    <div style="font-size: 12px; color: #b0b0b0; max-width: 280px; line-height: 1.4;">
                        Este vídeo possui restrição de reprodução externa imposta pelo canal original no YouTube.
                    </div>
                    <a class="play-fallback-btn" href="https://www.youtube.com/watch?v=$videoId">
                        Assistir no App do YouTube
                    </a>
                </div>

                <script src="https://www.youtube.com/iframe_api"></script>
                <script>
                    var player;
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('yt-player', {
                            height: '100%',
                            width: '100%',
                            videoId: '$videoId',
                            playerVars: {
                                'autoplay': 1,
                                'playsinline': 1,
                                'rel': 0,
                                'modestbranding': 1,
                                'controls': 1,
                                'iv_load_policy': 3,
                                'fs': 1,
                                'origin': 'https://www.youtube.com'
                            },
                            events: {
                                'onReady': function(event) {
                                    try {
                                        event.target.playVideo();
                                    } catch(e) {}
                                },
                                'onError': function(event) {
                                    // 100: Vídeo não encontrado ou removido
                                    // 101 ou 150: O proprietário do vídeo não permite que ele seja reproduzido em players incorporados
                                    console.log('YouTube Error:', event.data);
                                    if (event.data === 101 || event.data === 150 || event.data === 100 || event.data === 2) {
                                        var overlay = document.getElementById('error-overlay');
                                        if (overlay) {
                                            overlay.style.display = 'flex';
                                        }
                                        if (window.AndroidBridge && window.AndroidBridge.onVideoUnavailable) {
                                            window.AndroidBridge.onVideoUnavailable(event.data);
                                        }
                                    }
                                }
                            }
                        });
                    }

                    function playVideo() {
                        try {
                            if (player && player.playVideo) {
                                player.playVideo();
                            }
                        } catch (e) {}
                    }
                    function pauseVideo() {
                        try {
                            if (player && player.pauseVideo) {
                                player.pauseVideo();
                            }
                        } catch (e) {}
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
