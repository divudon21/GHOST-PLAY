package com.ghost.video.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghost.video.data.LoadingIndicatorStyle

/**
 * The app-wide loader selector. Ghost is intentionally the default and remains
 * the loader used by video buffering; the optional Material circular loader is
 * for library and update-screen loading states only.
 */
@Composable
fun AppLoadingIndicator(
    style: LoadingIndicatorStyle,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shapeColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    when (style) {
        LoadingIndicatorStyle.GHOST -> GhostLoadingIndicator(
            modifier = modifier,
            size = size,
            shapeColor = shapeColor,
            containerColor = containerColor
        )
        LoadingIndicatorStyle.MATERIAL_CIRCULAR -> CircularProgressIndicator(
            modifier = modifier.size(size),
            color = shapeColor,
            trackColor = containerColor,
            strokeWidth = 5.dp
        )
    }
}
