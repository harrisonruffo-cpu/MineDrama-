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
     * Retorna a URL da thumbnail em alta definição do vídeo do YouTube.
     */
    fun getThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }

    /**
     * Gera HTML otimizado com a YouTube IFrame API e aceleração por hardware e viewport vertical.
     * Corrige problemas de renderização onde apenas o áudio era reproduzido sem imagem de vídeo no WebView.
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
                    }
                    html, body {
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        background: #000000 !important;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .video-wrapper {
                        position: relative;
                        width: 100vw;
                        height: 100vh;
                        overflow: hidden;
                        background: #000000;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    #player {
                        width: 100%;
                        height: 100%;
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        border: none;
                    }
                </style>
            </head>
            <body>
                <div class="video-wrapper">
                    <div id="player"></div>
                </div>

                <script>
                    var tag = document.createElement('script');
                    tag.src = "https://www.youtube.com/iframe_api";
                    var firstScriptTag = document.getElementsByTagName('script')[0];
                    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                    var ytPlayer;
                    function onYouTubeIframeAPIReady() {
                        ytPlayer = new YT.Player('player', {
                            videoId: '$videoId',
                            playerVars: {
                                'autoplay': 1,
                                'playsinline': 1,
                                'controls': 1,
                                'rel': 0,
                                'modestbranding': 1,
                                'fs': 0,
                                'loop': 1,
                                'playlist': '$videoId',
                                'enablejsapi': 1,
                                'iv_load_policy': 3,
                                'origin': 'https://www.youtube.com'
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange
                            }
                        });
                    }

                    function onPlayerReady(event) {
                        try {
                            event.target.playVideo();
                        } catch (e) {}
                    }

                    function onPlayerStateChange(event) {
                        // Estado do player atualizado
                    }

                    function playVideo() {
                        try {
                            if (ytPlayer && typeof ytPlayer.playVideo === 'function') {
                                ytPlayer.playVideo();
                            }
                        } catch (e) {}
                    }

                    function pauseVideo() {
                        try {
                            if (ytPlayer && typeof ytPlayer.pauseVideo === 'function') {
                                ytPlayer.pauseVideo();
                            }
                        } catch (e) {}
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
