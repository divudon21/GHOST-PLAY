package com.ghost.video.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CapsuleSwitcherOption(
    val label: String,
    val icon: ImageVector
)

// ═══════════════════════════════════════════════════════════════════════════
//  Lightweight capsule switcher
//
//  Uses the exact visual treatment of the Home / Audio / Settings capsule:
//   • Same 56dp track, 5dp inner padding and circular geometry.
//   • Active app-palette colour with matching on-primary content.
//   • No slide animation, shadow, physics, drag, haptics or gradient work.
// ═══════════════════════════════════════════════════════════════════════════

/** Palette of flat colours shared by every capsule card. */
private data class CapsuleColors(
    val cardBg: Color,
    val iconBadgeBg: Color,
    val iconBadgeColor: Color,
    val labelColor: Color,
    val trackBg: Color,
    val trackBorder: Color,
    val thumbBg: Color,
    val activeColor: Color,
    val inactiveColor: Color
)

@Composable
private fun rememberCapsuleColors(): CapsuleColors {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scheme = MaterialTheme.colorScheme
    return CapsuleColors(
        // Use Material theme roles instead of fixed white/grey values so the
        // complete capsule card follows the selected app palette.
        cardBg = scheme.surface,
        iconBadgeBg = scheme.primaryContainer,
        iconBadgeColor = scheme.onPrimaryContainer,
        labelColor = scheme.onSurface,
        trackBg = scheme.primaryContainer.copy(alpha = if (isDark) 0.32f else 0.48f),
        trackBorder = scheme.primary.copy(alpha = if (isDark) 0.22f else 0.18f),
        thumbBg = scheme.primary,
        activeColor = scheme.onPrimary,
        inactiveColor = scheme.onSurfaceVariant.copy(alpha = if (isDark) 0.62f else 0.58f)
    )
}

/**
 * Generic capsule switcher card. Label + icon on top, a solid segmented pill below
 * that works with any number of options. Used for View Layout, Orientation,
 * Decoder, Thumbnail strategy, Subtitle font, etc.
 */
@Composable
fun CapsuleSwitcherCard(
    title: String,
    titleIcon: ImageVector,
    options: List<CapsuleSwitcherOption>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    val c = rememberCapsuleColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(c.cardBg)
            .border(1.dp, c.trackBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CapsuleHeader(title = title, icon = titleIcon, colors = c)
            Spacer(modifier = Modifier.height(16.dp))
            CapsuleTrack(
                itemCount = options.size,
                selectedIndex = selectedIndex,
                colors = c,
                onSelected = onSelected
            ) { index ->
                CapsuleSegmentContent(
                    icon = options[index].icon,
                    label = options[index].label,
                    isActive = selectedIndex == index,
                    colors = c
                )
            }
        }
    }
}

/** Shared label row (icon badge + title) used by every capsule card. */
@Composable
private fun CapsuleHeader(title: String, icon: ImageVector, colors: CapsuleColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.iconBadgeBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.iconBadgeColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = colors.labelColor,
            letterSpacing = (-0.01).em
        )
    }
}

/**
 * Solid segmented track matching the main bottom capsule. Selection switches
 * immediately, avoiding the same uneven slide motion removed from bottom nav.
 */
@Composable
private fun CapsuleTrack(
    itemCount: Int,
    selectedIndex: Int,
    colors: CapsuleColors,
    onSelected: (Int) -> Unit,
    segment: @Composable (index: Int) -> Unit
) {
    val trackHeight = 56.dp
    val pad = 5.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(CircleShape)
            .background(colors.trackBg)
            .border(1.dp, colors.trackBorder, CircleShape)
            .padding(pad)
    ) {
        val segWidth = maxWidth / itemCount
        val safeIndex = selectedIndex.coerceIn(0, itemCount - 1)
        val thumbOffset = segWidth * safeIndex

        // Flat selected segment with no slide or elevation.
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(colors.thumbBg)
        )

        // Segments on top.
        Row(modifier = Modifier.matchParentSize()) {
            for (i in 0 until itemCount) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { if (selectedIndex != i) onSelected(i) },
                    contentAlignment = Alignment.Center
                ) {
                    segment(i)
                }
            }
        }
    }
}

/** Icon + label content for a segment; only the tint animates (cheap). */
@Composable
private fun CapsuleSegmentContent(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    colors: CapsuleColors
) {
    val tint by animateColorAsState(
        targetValue = if (isActive) colors.activeColor else colors.inactiveColor,
        animationSpec = tween(180),
        label = "segTint"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            maxLines = 1
        )
    }
}

