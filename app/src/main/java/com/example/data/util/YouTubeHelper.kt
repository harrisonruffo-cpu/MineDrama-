package com.example.data.util

object YouTubeHelper {
    /**
     * Extrai o ID de um vídeo do YouTube a partir de qualquer formato de link:
     * - https://www.youtube.com/watch?v=dQw4w9WgXcQ
     * - https://youtu.be/dQw4w9WgXcQ
     * - https://www.youtube.com/shorts/dQw4w9WgXcQ
     * - https://www.youtube.com/embed/dQw4w9WgXcQ
     * - https://m.youtube.com/watch?v=dQw4w9WgXcQ
     */
    fun extractVideoId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()

        // Se já for apenas o ID de 11 caracteres
        if (trimmed.length == 11 && trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return trimmed
        }

        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?v=|youtube\\.com/watch\\?.*&v=)([a-zA-Z0-9_-]{11})"),
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
     * Gera HTML completo e compatível para exibição e renderização fluida no WebView,
     * evitando o problema comum de tela preta com reprodução apenas de áudio.
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
                        background: #000000;
                    }
                    html, body {
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        background-color: #000000;
                    }
                    .player-container {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        background: #000000;
                    }
                    iframe {
                        width: 100%;
                        height: 100%;
                        border: 0;
                    }
                </style>
            </head>
            <body>
                <div class="player-container">
                    <iframe
                        id="ytplayer"
                        type="text/html"
                        src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&controls=1&enablejsapi=1&rel=0&modestbranding=1&fs=1&origin=https://www.youtube.com"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                        allowfullscreen>
                    </iframe>
                </div>
                <script>
                    // Manter tela ativa e gerenciar playback
                    var player = document.getElementById('ytplayer');
                    function playVideo() {
                        if (player && player.contentWindow) {
                            player.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
                        }
                    }
                    function pauseVideo() {
                        if (player && player.contentWindow) {
                            player.contentWindow.postMessage('{"event":"command","func":"pauseVideo","args":""}', '*');
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
