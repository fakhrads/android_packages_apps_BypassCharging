/*
 * SPDX-FileCopyrightText: 2026 Fakhri Adi Saputra
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.fakhrads.bypasscharging.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Haptic feedback through the view, which needs no permission. */
@Composable
fun rememberHaptic(): (Int) -> Unit {
    val view = LocalView.current
    return remember(view) { { type: Int -> view.performHapticFeedback(type) } }
}

object Haptic {
    const val CLICK = HapticFeedbackConstants.CONTEXT_CLICK
    const val TOGGLE_ON = HapticFeedbackConstants.TOGGLE_ON
    const val TOGGLE_OFF = HapticFeedbackConstants.TOGGLE_OFF
    const val TICK = HapticFeedbackConstants.CLOCK_TICK
    const val CONFIRM = HapticFeedbackConstants.CONFIRM
}

/** Shrinks a little while pressed, like the tiles in LunarisDolby. */
@Composable
fun Modifier.squishable(enabled: Boolean = true, scaleDown: Float = 0.94f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleDown else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "squish"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    when (awaitPointerEvent().type) {
                        PointerEventType.Press -> pressed = true
                        PointerEventType.Release, PointerEventType.Exit -> pressed = false
                    }
                }
            }
        }
}

/** A titled card with an icon badge, the main building block of every screen. */
@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
fun SettingSwitch(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val haptic = rememberHaptic()
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (checked && enabled) colors.primaryContainer.copy(alpha = 0.5f)
        else colors.surfaceVariant.copy(alpha = 0.6f),
        label = "switch_row"
    )
    val toggle = { value: Boolean ->
        haptic(if (value) Haptic.TOGGLE_ON else Haptic.TOGGLE_OFF)
        onCheckedChange(value)
    }
    Surface(
        onClick = { toggle(!checked) },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        shape = MaterialTheme.shapes.large,
        color = container
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (checked) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = toggle,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

/** A slider over whole numbers from [min] to [max] in steps of [step]. */
@Composable
fun SettingSlider(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    enabled: Boolean = true,
    valueLabel: (Int) -> String = { it.toString() }
) {
    val haptic = rememberHaptic()
    // Follows the finger while dragging; the caller is told about whole steps only
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging?.let { snap(it, min, step) } ?: value
    Column(modifier = modifier
        .fillMaxWidth()
        .alpha(if (enabled) 1f else 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = valueLabel(shown),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        Slider(
            value = dragging ?: value.toFloat(),
            enabled = enabled,
            onValueChange = { raw ->
                val before = shown
                dragging = raw
                val now = snap(raw, min, step)
                if (now != before) {
                    haptic(Haptic.TICK)
                    onValueChange(now)
                }
            },
            onValueChangeFinished = { dragging = null },
            valueRange = min.toFloat()..max.toFloat(),
            steps = ((max - min) / step - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

private fun snap(raw: Float, min: Int, step: Int) = min + ((raw - min) / step).roundToInt() * step

data class Choice<T>(
    val value: T,
    val title: String,
    val summary: String? = null,
    val icon: ImageVector? = null
)

/** One selectable tile per option, the selected one highlighted (radio buttons, restyled). */
@Composable
fun <T> ChoiceList(
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = rememberHaptic()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        choices.forEach { choice ->
            val isSelected = choice.value == selected
            val colors = MaterialTheme.colorScheme
            val container by animateColorAsState(
                if (isSelected) colors.primaryContainer else colors.surfaceVariant.copy(alpha = 0.6f),
                label = "choice"
            )
            Surface(
                onClick = {
                    if (!isSelected) {
                        haptic(Haptic.CLICK)
                        onSelect(choice.value)
                    }
                },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .squishable(enabled),
                shape = if (isSelected) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
                color = container,
                border = if (isSelected) BorderStroke(2.dp, colors.primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    choice.icon?.let {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) colors.primary
                                    else colors.onSurfaceVariant.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = choice.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.onPrimaryContainer else colors.onSurface
                        )
                        if (!choice.summary.isNullOrEmpty()) {
                            Text(
                                text = choice.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) colors.onPrimaryContainer.copy(alpha = 0.8f)
                                else colors.onSurfaceVariant
                            )
                        }
                    }
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.primary
                        )
                    }
                }
            }
        }
    }
}

/** A row that opens another screen. */
@Composable
fun NavRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null
) {
    val haptic = rememberHaptic()
    Surface(
        onClick = {
            haptic(Haptic.CLICK)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .squishable(scaleDown = 0.97f),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                leading != null -> {
                    leading()
                    Spacer(modifier = Modifier.width(12.dp))
                }
                icon != null -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Small print under a card's controls. */
@Composable
fun FooterText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun StatusChip(icon: ImageVector, label: String, contentColor: Color) {
    Surface(
        shape = CircleShape,
        color = contentColor.copy(alpha = 0.12f),
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

data class HeroStat(val label: String, val value: String)

/**
 * The gradient card at the top of a screen: a headline, a line under it, an optional switch,
 * stat boxes and chips. Bright while [active], muted otherwise.
 */
@Composable
fun HeroCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    stats: List<HeroStat> = emptyList(),
    chips: List<Pair<ImageVector, String>> = emptyList(),
    checked: Boolean? = null,
    switchEnabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    val haptic = rememberHaptic()
    val colors = MaterialTheme.colorScheme
    val start by animateColorAsState(
        if (active) colors.primaryContainer else colors.surfaceContainerHighest, label = "hero_start"
    )
    val end by animateColorAsState(
        if (active) colors.tertiaryContainer else colors.surfaceContainerHigh, label = "hero_end"
    )
    val content = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(start, end)))
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(content.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = content)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = content.copy(alpha = 0.8f)
                    )
                }
                if (checked != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = checked,
                        enabled = switchEnabled,
                        onCheckedChange = {
                            haptic(Haptic.CONFIRM)
                            onCheckedChange(it)
                        },
                        thumbContent = {
                            Icon(
                                imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            if (stats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.forEach { stat ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = content.copy(alpha = 0.10f),
                            contentColor = content
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(
                                    text = stat.value,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stat.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = content.copy(alpha = 0.75f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            if (chips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { (chipIcon, label) -> StatusChip(chipIcon, label, content) }
                }
            }
        }
    }
}

data class FeatureTileItem(
    val icon: ImageVector,
    val title: String,
    val checked: Boolean,
    val onToggle: (Boolean) -> Unit
)

/** Two column grid of toggle tiles, laid out without a nested lazy list. */
@Composable
fun FeatureTileGrid(
    title: String,
    tiles: List<FeatureTileItem>,
    onLabel: String,
    offLabel: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(title)
        tiles.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { tile ->
                    FeatureTile(tile, onLabel, offLabel, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FeatureTile(
    tile: FeatureTileItem,
    onLabel: String,
    offLabel: String,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHaptic()
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (tile.checked) colors.primaryContainer else colors.surfaceBright, label = "tile"
    )
    val content = if (tile.checked) colors.onPrimaryContainer else colors.onSurface
    val iconContainer by animateColorAsState(
        if (tile.checked) colors.primary else colors.surfaceContainerHighest, label = "tile_icon"
    )
    Surface(
        onClick = {
            haptic(if (tile.checked) Haptic.TOGGLE_OFF else Haptic.TOGGLE_ON)
            tile.onToggle(!tile.checked)
        },
        modifier = modifier
            .squishable()
            .height(128.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = content
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = null,
                    tint = if (tile.checked) colors.onPrimary else colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = tile.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (tile.checked) onLabel else offLabel,
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 8.dp, bottom = 12.dp)
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    icon: ImageVector,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, shape = MaterialTheme.shapes.medium) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = MaterialTheme.shapes.medium) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

data class NavItem(val route: String, val icon: ImageVector, val label: String)

/** The pill at the bottom of the main screens, the same toolbar LunarisDolby uses. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingNavToolbar(
    items: List<NavItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHaptic()
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                toolbarContainerColor = colors.primaryContainer,
                toolbarContentColor = colors.onPrimaryContainer
            ),
            modifier = Modifier
                .padding(vertical = FloatingToolbarDefaults.ScreenOffset)
                .shadow(
                    elevation = 16.dp,
                    shape = MaterialTheme.shapes.extraLarge,
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
        ) {
            items.forEach { item ->
                val selected = item.route == currentRoute
                ToggleButton(
                    checked = selected,
                    onCheckedChange = {
                        if (!selected) {
                            haptic(Haptic.CLICK)
                            onNavigate(item.route)
                        }
                    },
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                        checkedContainerColor = colors.primary,
                        checkedContentColor = colors.onPrimary
                    ),
                    shapes = ToggleButtonDefaults.shapes(CircleShape, CircleShape, CircleShape),
                    modifier = Modifier.height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.animateContentSize(
                            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                        AnimatedVisibility(
                            visible = selected,
                            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                        ) {
                            Text(
                                text = item.label,
                                fontSize = 16.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A main screen: big title, a list of cards and the floating toolbar over a fade. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScaffold(
    title: String,
    navItems: List<NavItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    actions: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val cutout = WindowInsets.displayCutout.asPaddingValues()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
            FloatingNavToolbar(
                items = navItems,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = cutout.calculateStartPadding(layoutDirection),
                        end = cutout.calculateEndPadding(layoutDirection),
                        bottom = padding.calculateBottomPadding()
                    )
            )
        }
    }
}

/** A screen opened from a main screen, with a back arrow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            header?.invoke()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}
