package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont

/**
 * What a painted window says in place of its list when there is nothing to pick: a gold badge (a lock, or whatever
 * [icon] is given), a heading and the reason in the game's own lettering, on a cream card framed in gold like the window
 * around it. Replaces the loose line of small red text on a blank card.
 */
@Composable
fun NoticeCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
    }
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFBEF), Color(0xFFFBE7BE))), shape)
            .border(2.5.dp, Brush.verticalGradient(listOf(Color(0xFFF3C45A), Color(0xFFD9962B))), shape)
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .softShadow()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFC95A), Color(0xFFF08A14))), CircleShape)
                .border(3.dp, Color.White, CircleShape)
        ) { icon() }
        Spacer(Modifier.height(14.dp))
        Text(
            title, color = Color(0xFF3B2314), fontFamily = DisplayFont, fontSize = 22.sp, lineHeight = 26.sp,
            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        if (message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                message, color = Color(0xFF7A5A40), fontFamily = DisplayFont, fontSize = 16.sp, lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * A short notice inside a painted window: a gold badge at the left, a bold heading and a line of explanation, on a soft
 * gold strip. For the one thing a player must know before acting (a change that can be made only once, a locked name).
 */
@Composable
fun InfoStrip(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = androidx.compose.material.icons.Icons.Filled.Info
) {
    val shape = RoundedCornerShape(18.dp)
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF6DC), Color(0xFFFCE7B8))), shape)
            .border(1.5.dp, Color(0xFFE2B866), shape)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFC95A), Color(0xFFF08A14))), CircleShape)
                .border(2.dp, Color.White, CircleShape)
        ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
        androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color(0xFF3B2314), fontFamily = DisplayFont, fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold)
            androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
            Text(message, color = Color(0xFF6B4A30), fontFamily = DisplayFont, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
