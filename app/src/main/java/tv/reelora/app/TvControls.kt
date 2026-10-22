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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.sp
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

// Read animated values in the layer: focus does not remeasure the lazy row.
@Composable
internal fun Modifier.tvFocusMotion(focused: Boolean, pressed: Boolean, lifted: Boolean): Modifier {
    val scale = animateFloatAsState(
        targetValue = if (pressed) .97f else if (focused && lifted) 1.08f else 1f,
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
    }
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
            Text(tr(title), color = Color.White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(tr(subtitle), color = SecondaryText, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                Text(tr(title), color = if (focused) FocusContent else Color.White, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(tr(subtitle), color = if (focused) FocusContent.copy(alpha = .7f) else SecondaryText, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        interactionSource = interaction,
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tr(title), color = ink, fontSize = 16.sp, lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(tr(it), color = secondary, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            value?.let { Text(tr(it), Modifier.widthIn(max = 108.dp), color = secondary, fontSize = 13.sp, lineHeight = 18.sp,
                textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Box(Modifier.width(40.dp).height(24.dp), contentAlignment = Alignment.Center) {
                if (checked != null) {
                    Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(
                        checkedTrackColor = if (focused) FocusContent else Accent,
                        checkedThumbColor = if (focused) FocusSurface else FocusContent,
                        uncheckedTrackColor = if (focused) FocusContent.copy(alpha = .16f) else Color.White.copy(alpha = .18f),
                        uncheckedThumbColor = if (focused) FocusContent else FocusSurface,
                        uncheckedBorderColor = Color.Transparent,
                    ))
                } else if (navigation) {
                    Icon(ChevronRightIcon, contentDescription = null, tint = secondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}


@Composable
internal fun InfoBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(ActionShape)
            .background(Background.copy(alpha = .88f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(tr(text), color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
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
        scale = ButtonDefaults.scale(focusedScale = if (navigation) 1f else 1.04f, pressedScale = .98f),
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
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!compact) trailingIcon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(16.dp)) }
            if (navigation) Box(Modifier.size(5.dp).clip(ActionShape)
                .background(if (!isSelected) Color.Transparent else if (focused) FocusContent else Color.White))
        }
    }
}

