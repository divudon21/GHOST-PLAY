package com.ghost.video.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghost.video.data.LoadingIndicatorStyle

/**
 * App-wide RoundedPolygon loading indicator backed directly by the official
 * Material 3 Expressive contained loading indicator API.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLoadingIndicator(
    style: LoadingIndicatorStyle,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shapeColor: Color = Color.Unspecified,
    containerColor: Color = Color.Unspecified
) {
    // Map shapeColor -> the spinning shape, containerColor -> the container
    // behind it. When unspecified, fall back to the theme defaults so existing
    // callers that pass no colors keep the standard look.
    val resolvedIndicator = if (shapeColor == Color.Unspecified) {
        LoadingIndicatorDefaults.containedIndicatorColor
    } else {
        shapeColor
    }
    val resolvedContainer = if (containerColor == Color.Unspecified) {
        LoadingIndicatorDefaults.containedContainerColor
    } else {
        containerColor
    }

    ContainedLoadingIndicator(
        modifier = modifier.size(size),
        indicatorColor = resolvedIndicator,
        containerColor = resolvedContainer,
    )
}
