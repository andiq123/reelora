package tv.reelora.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults

private const val FocusShadowSteps = 7

/**
 * The single focus treatment for content: tiles, posters, and cast lift and cast a soft shadow.
 * Animated values are read in the layer and the draw pass, so focus never remeasures a lazy row.
 * The shadow is a stack of translucent rounded rectangles: soft, with no blur pass on TV GPUs.
 */
@Composable
internal fun Modifier.tvFocusMotion(
    focused: Boolean,
    pressed: Boolean,
    lifted: Boolean,
    surfaceHeight: Dp = Dp.Unspecified,
    corner: Dp = AppTileCorner,
    shadow: Boolean = true,
): Modifier {
    val scale = animateFloatAsState(
        targetValue = if (pressed) TvMotion.PressScale else if (focused && lifted) TvMotion.FocusScale else 1f,
        animationSpec = spring(dampingRatio = .86f, stiffness = 380f),
        label = "TV focus scale",
    )
    val lift = animateFloatAsState(
        targetValue = if (focused && lifted && !pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 380f),
        label = "TV focus lift",
    )
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        translationY = -2.dp.toPx() * lift.value
    }.drawBehind {
        val progress = lift.value
        if (!shadow || progress <= .01f) return@drawBehind
        val height = if (surfaceHeight.isSpecified) surfaceHeight.toPx() else size.height
        val step = 2.dp.toPx()
        val drop = 7.dp.toPx()
        val radius = corner.toPx()
        for (index in FocusShadowSteps downTo 1) {
            val spread = index * step
            drawRoundRect(
                Color.Black.copy(alpha = .09f * progress),
                topLeft = Offset(-spread, drop - spread),
                size = Size(size.width + spread * 2, height + spread * 2),
                cornerRadius = CornerRadius(radius + spread),
            )
        }
    }
}

/** The focus edge for artwork, drawn inside its shape so every card shares one rim. */
internal fun Modifier.tvFocusRim(color: () -> Color, shape: Shape, width: Dp = 3.dp): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    // The surface clips the outer half of the stroke, leaving a clean inner rim.
    val stroke = Stroke((width * 2).toPx())
    onDrawWithContent {
        drawContent()
        val edge = color()
        if (edge.alpha > 0f) drawOutline(outline, edge, style = stroke)
    }
}

private const val StarGlyph = "★ "

/** One metadata line with a single separator style and a real rating icon. */
@Composable
internal fun MetaLine(parts: List<String>, color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    val text = remember(parts) {
        buildAnnotatedString {
            parts.filter(String::isNotBlank).forEachIndexed { index, part ->
                if (index > 0) append(" · ")
                if (part.startsWith(StarGlyph)) {
                    appendInlineContent("rating", "★")
                    append(" ")
                    append(part.removePrefix(StarGlyph))
                } else append(part)
            }
        }
    }
    val inline = remember(color) {
        mapOf("rating" to InlineTextContent(Placeholder(.9.em, .9.em, PlaceholderVerticalAlign.TextCenter)) {
            Icon(StarIcon, contentDescription = null, tint = color, modifier = Modifier.fillMaxSize())
        })
    }
    Text(text, modifier, color = color, style = style, inlineContent = inline, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun DialogHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GapLarge)) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(tr(title), color = Color.White, style = TvType.Title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(tr(subtitle), color = SecondaryText, style = TvType.Footnote, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        action?.invoke()
    }
}


@Composable
internal fun AppOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    navigation: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = ControlShape
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().zIndex(if (focused) 1f else 0f),
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(
            containerColor = Color.White.copy(alpha = .065f),
            focusedContainerColor = FocusSurface,
            pressedContainerColor = FocusSurface.copy(alpha = .88f),
        ),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = .98f),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border(BorderStroke(1.dp, FocusSurface), shape = shape),
        ),
        interactionSource = interaction,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, tint = if (focused) FocusContent else Color.White.copy(alpha = .9f), modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tr(title), color = if (focused) FocusContent else Color.White, style = TvType.Body, fontWeight = FontWeight.SemiBold)
                Text(tr(subtitle), color = if (focused) FocusContent.copy(alpha = .7f) else SecondaryText, style = TvType.Footnote, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (navigation) Icon(ChevronRightIcon, contentDescription = null, tint = if (focused) FocusContent.copy(alpha = .65f) else SecondaryText, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    subtitle: String? = null,
    checked: Boolean? = null,
    navigation: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ink = if (focused) FocusContent else Color.White
    val secondary = if (focused) FocusContent.copy(alpha = .7f) else SecondaryText
    val state = checked?.let { tr(if (it) "On" else "Off") }
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics {
            if (checked != null) {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                stateDescription = state.orEmpty()
            }
        },
        shape = ButtonDefaults.shape(shape = ControlShape),
        colors = ButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = .075f), contentColor = Color.White,
            focusedContainerColor = FocusSurface, focusedContentColor = FocusContent,
            pressedContainerColor = FocusSurface.copy(alpha = .9f), pressedContentColor = FocusContent,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = .98f),
        border = ButtonDefaults.border(border = Border.None, focusedBorder = Border.None),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        interactionSource = interaction,
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tr(title), color = ink, style = TvType.Body,
                    fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(tr(it), color = secondary, style = TvType.Caption, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            value?.let { Text(tr(it), Modifier.widthIn(max = 108.dp), color = secondary, style = TvType.Footnote,
                textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            // Values, chevrons, and switches all finish on the same right edge.
            if (checked != null || navigation) Box(Modifier.height(24.dp), contentAlignment = Alignment.CenterEnd) {
                if (checked != null) {
                    Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(
                        checkedTrackColor = if (focused) FocusContent else Accent,
                        checkedThumbColor = if (focused) FocusSurface else FocusContent,
                        uncheckedTrackColor = if (focused) FocusContent.copy(alpha = .16f) else Color.White.copy(alpha = .18f),
                        uncheckedThumbColor = if (focused) FocusContent else FocusSurface,
                        uncheckedBorderColor = Color.Transparent,
                    ))
                } else {
                    Icon(ChevronRightIcon, contentDescription = null, tint = secondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}


@Composable
internal fun InfoBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    val label = tr(text)
    Row(
        modifier
            .clip(ActionShape)
            .background(Background.copy(alpha = .88f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (label.startsWith("◷ ")) Icon(ClockIcon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        if (label.startsWith("● ")) Box(Modifier.size(6.dp).clip(ActionShape).background(color))
        Text(label.removePrefix("◷ ").removePrefix("● "), color = color, style = TvType.Caption, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}


internal enum class ActionRole { Secondary, Primary, Navigation }

@Composable
internal fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    role: ActionRole = ActionRole.Secondary,
    trailingIcon: ImageVector? = null,
    iconOnly: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val navigation = role == ActionRole.Navigation
    val compact = iconOnly && icon != null && !navigation
    val primary = role == ActionRole.Primary
    val label = tr(text)
    val shape = if (navigation) ControlShape else ActionShape
    // Center within explicit widths while keeping ordinary actions sized to their content.
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier.width(IntrinsicSize.Max).onFocusChanged { if (it.isFocused) onFocused() }.then(if (compact) Modifier.size(44.dp) else Modifier.heightIn(min = 44.dp))
            .zIndex(if (focused) 1f else 0f).semantics {
                if (navigation) selected = isSelected
                if (compact) contentDescription = label
            },
        shape = ButtonDefaults.shape(shape = shape),
        colors = ButtonDefaults.colors(
            containerColor = when {
                primary -> FocusSurface.copy(alpha = .9f)
                isSelected -> SelectedSurface
                navigation -> Color.Transparent
                else -> ActionSurface
            },
            contentColor = if (primary) FocusContent else Color.White.copy(alpha = .94f),
            focusedContainerColor = FocusSurface,
            focusedContentColor = FocusContent,
            pressedContainerColor = FocusSurface.copy(alpha = .88f),
            pressedContentColor = FocusContent,
            disabledContainerColor = ControlSurface.copy(alpha = .5f),
            disabledContentColor = SecondaryText.copy(alpha = .5f),
        ),
        scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = .98f),
        border = ButtonDefaults.border(
            border = if (primary || navigation) Border.None else Border(BorderStroke(1.dp, SubtleBorder), shape = shape),
            focusedBorder = Border(BorderStroke(1.dp, FocusSurface), shape = shape),
        ),
        contentPadding = if (compact) PaddingValues(0.dp) else PaddingValues(horizontal = if (navigation) 12.dp else 24.dp, vertical = 12.dp),
        interactionSource = interaction,
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = if (compact) 44.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, if (navigation) Alignment.Start else Alignment.CenterHorizontally)) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) }
            if (!compact) Text(label, modifier = if (navigation) Modifier.weight(1f) else Modifier,
                fontWeight = FontWeight.SemiBold, style = TvType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!compact) trailingIcon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(16.dp)) }
        }
    }
}

