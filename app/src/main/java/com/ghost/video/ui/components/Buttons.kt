package com.ghost.video.ui.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ghost.video.R

/**
 * Ported from NextPlayer (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/core/ui/src/main/java/dev/anilbeesetti/nextplayer/core/ui/components/Buttons.kt
 */
@Composable
fun DoneButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    ) { Text(text = stringResource(R.string.done)) }
}

@Composable
fun CancelButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    ) { Text(text = stringResource(R.string.cancel)) }
}
