package com.example.ui.util

import com.example.R

/**
 * Utilitário para resolução de capas e banners locais e remotos.
 * Garante que a capa oficial da série 'Dono Do Morro' seja sempre carregada
 * em altíssima definição a partir dos drawables nativos do app.
 */
object AppImageResolver {
    fun resolve(url: String?): Any {
        if (url.isNullOrBlank()) {
            return R.drawable.dono_do_morro_cover
        }
        val lower = url.lowercase()
        if (lower.contains("dono_do_morro_banner") || lower.contains("morro_banner")) {
            return R.drawable.dono_do_morro_banner
        }
        if (lower.contains("dono_do_morro") || lower.contains("dono do morro") || lower.contains("malvadao")) {
            return R.drawable.dono_do_morro_cover
        }
        return url
    }
}
