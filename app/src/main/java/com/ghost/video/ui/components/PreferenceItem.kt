package com.ghost.video.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Ported from NextPlayer (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/core/ui/src/main/java/dev/anilbeesetti/nextplayer/core/ui/components/PreferenceItem.kt
 * https://github.com/anilbeesetti/nextplayer/blob/main/core/ui/src/main/java/dev/anilbeesetti/nextplayer/core/ui/components/ClickablePreferenceItem.kt
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferenceItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    trailingContent: @Composable () -> Unit = {},
) {
    NextSegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        leadingContent = icon?.let {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        supportingContent = description?.let { { Text(text = description) } },
        content = { Text(text = title) },
        trailingContent = trailingContent,
    )
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun ClickablePreferenceItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
) {
    PreferenceItem(
        title = title,
        description = description,
        icon = icon,
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        onLongClick = onLongClick,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
    )
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun SelectablePreference(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
) {
    NextSegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        content = {
            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium.copy(
                    textDecoration = if (selected) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    },
                ),
            )
        },
        supportingContent = {
            description?.let {
                Text(
                    text = it,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (selected) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                    ),
                )
            }
        },
        trailingContent = {
            Checkbox(
                modifier = Modifier.semantics { contentDescription = title },
                checked = selected,
                onCheckedChange = null,
            )
        },
    )
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun SingleSelectablePreference(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
) {
    NextSegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        content = {
            Text(
                text = title,
                maxLines = 1,
            )
        },
        supportingContent = {
            description?.let {
                Text(
                    text = it,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
            )
        },
    )
}
