package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DramaCrimsonBright

@Composable
fun DoubleTapHeartAnimation(
    show: Boolean,
    onAnimationEnd: () -> Unit
) {
    if (show) {
        val scale = remember { Animatable(0.2f) }
        val alpha = remember { Animatable(1f) }

        LaunchedEffect(Unit) {
            scale.animateTo(
                targetValue = 1.3f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 350)
            )
            onAnimationEnd()
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = DramaCrimsonBright,
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale.value)
                    .alpha(alpha.value)
            )
        }
    }
}
