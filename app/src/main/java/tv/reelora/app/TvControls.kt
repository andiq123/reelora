package tv.reelora.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
            Text(tr(title), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tr(subtitle), color = SecondaryText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            containerColor = ControlSurface,
            focusedContainerColor = FocusSurface,
            pressedContainerColor = FocusSurface.copy(alpha = .88f),
        ),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = .99f),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border(BorderStroke(1.dp, FocusSurface), shape = shape),
        ),
        interactionSource = interaction,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(icon, contentDescription = null, tint = if (focused) FocusContent else SecondaryText, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tr(title), color = if (focused) FocusContent else Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(tr(subtitle), color = if (focused) FocusContent.copy(alpha = .7f) else SecondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}


@Composable
internal fun InfoBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Background.copy(alpha = .88f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(tr(text), color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}


@Composable
internal fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocused() }
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        shape = ButtonDefaults.shape(shape = ControlShape),
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) SelectedSurface else ControlSurface,
            contentColor = if (isSelected) Accent else Color.White.copy(alpha = .9f),
            focusedContainerColor = FocusSurface,
            focusedContentColor = FocusContent,
            pressedContainerColor = FocusSurface.copy(alpha = .88f),
            pressedContentColor = FocusContent,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.02f, pressedScale = .99f),
        border = ButtonDefaults.border(
            border = Border.None,
            focusedBorder = Border(BorderStroke(1.dp, FocusSurface), shape = ControlShape),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
        interactionSource = interaction,
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(tr(text), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

