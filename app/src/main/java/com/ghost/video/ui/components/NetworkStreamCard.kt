package com.ghost.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Network stream card — ported from NextPlayer's official UI.
 *
 * Source (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/network/src/main/java/dev/anilbeesetti/nextplayer/feature/network/screens/list/NetworkScreen.kt
 *
 * `NetworkStreamCard`: a rounded surface at 1 dp elevation holding the shared
 * outlined text field and a trailing right-aligned `Button`. No bespoke
 * animation, no chip row, no trailing icon cluster — the platform ripple is the
 * only feedback, exactly as upstream.
 *
 * Two deliberate departures, requested by the app owner: the title and
 * description rows are not drawn (the card only appears after the link button is
 * tapped), and the whole card is revealed with `AnimatedVisibility` rather than
 * being permanently on screen.
 */
@Composable
fun NetworkStreamCard(
    modifier: Modifier = Modifier,
    placeholder: String,
    actionLabel: String,
    url: String,
    onUrlChange: (String) -> Unit,
    onOpenStream: () -> Unit,
    enabled: Boolean,
) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(24.dp))
            // NextPlayer uses surfaceColorAtElevation(1.dp); this project's theme
            // exposes the same tonal step as surfaceContainerHigh.
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Title and description are intentionally not rendered: the card is only
        // shown after the user taps the link button, so the heading explained
        // itself twice.
        NextOutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions onGo@{
                if (!enabled) return@onGo
                onOpenStream()
            },
        )
        Button(
            onClick = onOpenStream,
            enabled = enabled,
            modifier = Modifier.align(Alignment.End),
        ) { Text(text = actionLabel) }
    }
}

/**
 * NextPlayer's `NextOutlinedTextField`.
 *
 * Source (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/core/ui/src/main/java/dev/anilbeesetti/nextplayer/core/ui/components/NextOutlinedTextField.kt
 *
 * The touch-device code path of the original is reproduced verbatim (the-only
 * difference being that this app has no TV focus plumbing): a plain
 * `OutlinedTextField` with the same parameter surface the Network screen passes.
 */
@Composable
fun NextOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    shape: Shape = OutlinedTextFieldDefaults.shape,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = label,
        placeholder = placeholder,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        shape = shape,
    )
}
