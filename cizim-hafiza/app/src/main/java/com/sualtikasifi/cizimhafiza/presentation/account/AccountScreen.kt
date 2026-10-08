package com.sualtikasifi.cizimhafiza.presentation.account

import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedBackButton
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.sceneIn
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.repository.AuthState
import com.sualtikasifi.cizimhafiza.presentation.common.AppTextField
import com.sualtikasifi.cizimhafiza.presentation.common.GoogleSignInButton
import com.sualtikasifi.cizimhafiza.presentation.common.IconWell
import com.sualtikasifi.cizimhafiza.presentation.common.LevelAvatar
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.RankLevelLabel
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance
import com.sualtikasifi.cizimhafiza.presentation.common.raisedSurface
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.util.AppRestarter
import com.sualtikasifi.cizimhafiza.util.asString
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * One profile, one pair of buttons: sign in, or sign out.
 *
 * Backing up is not offered as an action because it is not one — it runs
 * on its own (see util.AutoBackupPublisher) and the card below reports
 * when it last did, which is the only part a player actually needs. The
 * old "Şimdi Yedekle"/"Yedeği Geri Yükle"/"Hesap Değiştir" trio is gone
 * for the same reason: each was a way to put the device into a state where
 * the profile on screen and the account it belonged to had drifted apart.
 */
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Deletion only. Signing in and out change the profile in place now —
    // see AccountUiState.restartRequired for what made that safe, and why
    // deleting the account is still the one case that cannot be.
    LaunchedEffect(uiState.restartRequired) {
        if (uiState.restartRequired) AppRestarter.restart(context)
    }

    val linked = uiState.authState as? AuthState.Linked
    val progress = uiState.levelProgress
    val number = remember { java.text.NumberFormat.getIntegerInstance() }

    // The page is the workshop picture (bg_account) with the cards painted into it; everything that changes — the
    // name, the figures, the bar, the buttons — is laid over it by the picture's own coordinates.
    BoxWithConstraints(modifier = Modifier.fillMaxSize().sceneIn()) {
        val unit = minOf(maxWidth / ArtW, maxHeight / ArtH)
        val offX = (maxWidth - unit * ArtW) / 2
        val offY = (maxHeight - unit * ArtH) / 2
        val us = unit.value
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * us / fontScale0).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(offX + unit * x0, offY + unit * y0).requiredSize(unit * (x1 - x0), unit * (y1 - y0))

        val scene = cachedPainterResource(R.drawable.bg_account)
        if (offX > 1.dp || offY > 1.dp) {
            Image(scene, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(20.dp))
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }
        Image(scene, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = box(0f, 0f, ArtW, ArtH))

        // ---- The sign ------------------------------------------------------------------------------------------
        LetteredText(
            text = stringResource(R.string.account_title),
            size = fs(104f),
            fill = Color(0xFFFFC21F),
            outline = Color(0xFF5A2815),
            modifier = box(270f, 325f, 546f, 445f),
            minScale = 0.5f
        )

        // ---- Profile card --------------------------------------------------------------------------------------
        val name = uiState.nickname.ifBlank {
            linked?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.account_guest_badge)
        }
        Box(box(92f, 545f, 242f, 695f), contentAlignment = Alignment.Center) {
            LevelAvatar(
                level = uiState.level,
                frame = uiState.frame,
                size = (150f * us).dp,
                photo = com.sualtikasifi.cizimhafiza.presentation.common.avatarPhotoOf(linked?.photoUrl)
            )
        }
        FitText(
            text = name,
            style = PaintedStyle(color = PageInk, fontSize = fs(48f), textAlign = TextAlign.Start),
            maxLines = 1,
            minScale = 0.55f,
            contentAlignment = Alignment.CenterStart,
            modifier = box(268f, 540f, 770f, 600f)
        )
        Box(box(268f, 604f, 770f, 648f), contentAlignment = Alignment.CenterStart) {
            RankLevelLabel(level = uiState.level, bullet = false)
        }
        // Who the profile belongs to: a green pill with a tick for a linked Google account, a plain one for a guest.
        Row(
            box(268f, 654f, 774f, 712f)
                .clip(CircleShape)
                .background(if (linked != null) Color(0xFFDDEEDD) else Color(0xFFEFE3CF))
                .padding(horizontal = (18f * us).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((10f * us).dp)
        ) {
            Icon(
                imageVector = if (linked != null) Icons.Filled.Check else Icons.Filled.Person,
                contentDescription = null,
                tint = if (linked != null) AppTheme.tokens.success else Color(0xFF8A6A50),
                modifier = Modifier.size((30f * us).dp)
            )
            FitText(
                text = if (linked != null) {
                    listOfNotNull(stringResource(R.string.account_google_linked), linked.email).joinToString(" · ")
                } else {
                    stringResource(R.string.account_guest_badge)
                },
                style = PaintedStyle(color = PageInk, fontSize = fs(27f), textAlign = TextAlign.Start),
                maxLines = 1,
                minScale = 0.6f,
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight(1f)
            )
        }
        // The level star and the bar it heads.
        Box(box(76f, 706f, 156f, 786f), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF8A4E12), modifier = Modifier.fillMaxSize())
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB627), modifier = Modifier.fillMaxSize(0.84f))
            LetteredText(uiState.level.toString(), fs(30f), outline = Color(0xFF5A2815), modifier = Modifier.padding(top = (6f * us).dp))
        }
        Box(
            box(166f, 726f, 736f, 762f)
                .clip(CircleShape)
                .background(Color(0xFF4A2A18))
        ) {
            val fraction by androidx.compose.animation.core.animateFloatAsState(
                progress.progressFraction.coerceIn(0f, 1f),
                androidx.compose.animation.core.tween(700),
                label = "xpBar"
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceAtLeast(0.05f))
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFC04A), Color(0xFFF58A1F))))
            )
            // The figure sits inside the bar, at its start, in white with a dark edge so it reads on the orange and on the dark part.
            Text(
                text = if (progress.isMaxLevel) stringResource(R.string.account_xp_max)
                else stringResource(R.string.account_xp_progress, progress.xpIntoLevel, progress.xpForThisLevel),
                style = PaintedStyle(
                    color = Color.White, fontSize = fs(26f), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Start
                ).copy(shadow = androidx.compose.ui.graphics.Shadow(Color(0xCC3A1A08), androidx.compose.ui.geometry.Offset(0f, 2f), 4f)),
                maxLines = 1,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = (16f * us).dp)
            )
        }

        // ---- The three figures ---------------------------------------------------------------------------------
        val stats = listOf(
            Triple(number.format(uiState.levelProgress.totalXp), stringResource(R.string.account_stat_xp), 24f),
            Triple(number.format(uiState.gamesPlayed), stringResource(R.string.account_stat_games), 294f),
            Triple(number.format(uiState.bestStreak), stringResource(R.string.account_stat_streak), 566f)
        )
        stats.forEach { (value, label, x) ->
            FitText(
                text = value,
                style = PaintedStyle(color = PageInk, fontSize = fs(46f), textAlign = TextAlign.Center),
                maxLines = 1,
                minScale = 0.5f,
                modifier = box(x + 24f, 922f, x + 228f, 974f)
            )
            FitText(
                text = label,
                style = PaintedStyle(color = PageInk, fontSize = fs(25f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                maxLines = 1,
                minScale = 0.55f,
                modifier = box(x + 16f, 970f, x + 236f, 1002f)
            )
        }

        // ---- Account and backup --------------------------------------------------------------------------------
        FitText(
            text = stringResource(R.string.account_section_account),
            style = PaintedStyle(color = PageInk, fontSize = fs(44f), textAlign = TextAlign.Start),
            maxLines = 1,
            minScale = 0.55f,
            contentAlignment = Alignment.CenterStart,
            modifier = box(190f, 1066f, 740f, 1134f)
        )
        val backedUp = uiState.lastBackupAtMillis != null
        // The strip's painted tick says "saved": only true for a signed-in account that has been backed up, so for
        // anyone else a disc in the strip's own colour covers it and shows what is really the case.
        if (!(uiState.isSignedIn && backedUp)) {
            Box(
                box(88f, 1160f, 148f, 1220f)
                    .clip(CircleShape)
                    .background(Color(0xFFD9E5D0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.isSignedIn) Icons.Filled.CloudSync else Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = Color(0xFF8A6A50),
                    modifier = Modifier.size((38f * us).dp)
                )
            }
        }
        val strip1: String
        val strip2: String?
        when {
            uiState.isSignedIn -> {
                strip1 = stringResource(if (backedUp) R.string.account_sync_on else R.string.account_sync_pending)
                strip2 = uiState.lastBackupAtMillis?.let {
                    stringResource(R.string.account_last_backup_format, rememberBackupTimestamp(it))
                } ?: stringResource(R.string.account_never_backed_up)
            }
            uiState.isGoogleSignInConfigured -> { strip1 = stringResource(R.string.account_guest_hint); strip2 = null }
            else -> { strip1 = stringResource(R.string.account_not_configured_message); strip2 = null }
        }
        FitText(
            text = strip1,
            style = PaintedStyle(color = PageInk, fontSize = fs(31f), textAlign = TextAlign.Start),
            maxLines = if (strip2 == null) 2 else 1,
            minScale = 0.55f,
            contentAlignment = Alignment.CenterStart,
            modifier = if (strip2 == null) box(166f, 1158f, 742f, 1222f) else box(166f, 1158f, 742f, 1198f)
        )
        if (strip2 != null) {
            FitText(
                text = strip2,
                style = PaintedStyle(color = Color(0xFF7A5A40), fontSize = fs(25f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start),
                maxLines = 1,
                minScale = 0.55f,
                contentAlignment = Alignment.CenterStart,
                modifier = box(166f, 1196f, 742f, 1228f)
            )
        }

        // ---- Sign out / sign in --------------------------------------------------------------------------------
        when {
            uiState.isBusy -> Box(box(120f, 1292f, 720f, 1460f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            }
            uiState.isSignedIn -> Box(
                box(120f, 1292f, 720f, 1292f + 172f)
                    .pressable(pressedScale = 0.95f, onClick = viewModel::promptSignOut)
                    .a11yButton(stringResource(R.string.account_sign_out)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.account_btn), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                // The lettering is centred on the button itself; the icon waits at its start.
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = (46f * us).dp).size((56f * us).dp)
                )
                LetteredText(
                    stringResource(R.string.account_sign_out), fs(56f), outline = Color(0xFF8A3A00),
                    modifier = Modifier.fillMaxWidth(0.62f).height((86f * us).dp), minScale = 0.5f
                )
            }
            uiState.isGoogleSignInConfigured -> Box(box(130f, 1318f, 710f, 1440f), contentAlignment = Alignment.Center) {
                GoogleSignInButton(onClick = viewModel::signIn, modifier = Modifier.fillMaxWidth())
            }
        }

        // ---- Delete --------------------------------------------------------------------------------------------
        // Offered whether or not a Google account is signed in: an anonymous player still has a uid with a profile, a
        // friends list and a league entry under it, and Play's requirement is about the data, not about how the
        // account was created.
        Box(box(120f, 1490f, 720f, 1570f), contentAlignment = Alignment.Center) {
            if (uiState.isDeleting) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else {
                Row(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFFF6EE))
                        .border((4f * us).dp, Color(0xFFD63A2E), RoundedCornerShape(50))
                        .pressable(pressedScale = 0.96f, onClick = viewModel::promptDeleteAccount)
                        .a11yButton(stringResource(R.string.account_delete_action)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFD63A2E), modifier = Modifier.size((40f * us).dp))
                    Spacer(Modifier.size((12f * us).dp))
                    FitText(
                        text = stringResource(R.string.account_delete_action),
                        style = PaintedStyle(color = Color(0xFFD63A2E), fontSize = fs(31f)),
                        maxLines = 1,
                        minScale = 0.55f,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }

        // ---- Saved / failed ------------------------------------------------------------------------------------
        val note = uiState.message?.asString() to uiState.errorMessage?.asString()
        (note.first ?: note.second)?.let { text ->
            FitText(
                text = text,
                style = PaintedStyle(color = if (note.first != null) AppTheme.tokens.success else MaterialTheme.colorScheme.error, fontSize = fs(30f), textAlign = TextAlign.Center),
                maxLines = 2,
                minScale = 0.6f,
                modifier = box(60f, 1590f, 780f, 1670f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF2FFF6E4))
                    .padding(horizontal = 10.dp)
            )
        }

        PaintedBackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart))
    }

    if (uiState.showSignOutPrompt) {
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedConfirmDialog(
            title = stringResource(R.string.account_sign_out_title),
            message = stringResource(R.string.account_sign_out_message),
            confirmText = stringResource(R.string.account_sign_out_confirm),
            dismissText = stringResource(R.string.account_sign_out_cancel),
            onConfirm = viewModel::signOut,
            onDismiss = viewModel::dismissSignOutPrompt
        )
    }

    if (uiState.showDeletePrompt) {
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedConfirmDialog(
            title = stringResource(R.string.account_delete_title),
            message = stringResource(R.string.account_delete_message),
            confirmText = stringResource(R.string.account_delete_continue),
            dismissText = stringResource(R.string.account_delete_cancel),
            destructive = true,
            onConfirm = viewModel::confirmDeleteFirstStep,
            onDismiss = viewModel::dismissDeletePrompt
        )
    }

    // The second, separate confirmation. Different wording and the safe choice on the prominent side, so the
    // two taps cannot be made on autopilot.
    if (uiState.showDeleteFinalPrompt) {
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialog(
            title = stringResource(R.string.account_delete_final_title),
            onDismiss = viewModel::dismissDeletePrompt,
            buttons = {
                androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton(
                        text = stringResource(R.string.account_delete_final_confirm),
                        onClick = viewModel::deleteAccount,
                        primary = false,
                        danger = true,
                        modifier = Modifier.weight(1f)
                    )
                    com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton(
                        text = stringResource(R.string.account_delete_final_keep),
                        onClick = viewModel::dismissDeletePrompt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        ) {
            com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialogText(stringResource(R.string.account_delete_final_message))
        }
    }

    // Feedback is one-shot: clear it once shown for long enough to read,
    // so navigating back to this screen later doesn't resurface a stale
    // message from a previous visit.
    LaunchedEffect(uiState.message, uiState.errorMessage) {
        if (uiState.message != null || uiState.errorMessage != null) {
            kotlinx.coroutines.delay(4_000)
            viewModel.dismissMessages()
        }
    }
}


private const val ArtW = 841f
private const val ArtH = 1870f
private val PageInk = Color(0xFF3B2314)

/**
 * "bugün 14:32" for a backup from today, "dün 14:32" for yesterday, the
 * full date before that.
 *
 * The absolute date was technically correct and read like a receipt. The
 * question this line answers is "is my progress safe right now?", and for
 * a backup made minutes ago the answer is far clearer as "bugün".
 *
 * Remembered on the timestamp because a SimpleDateFormat is not cheap to
 * build and this recomposes with the rest of the card.
 */
@Composable
private fun rememberBackupTimestamp(millis: Long): String {
    val timeOnly = remember(millis) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis)) }
    val daysAgo = remember(millis) {
        val day = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        LocalDate.now().toEpochDay() - day.toEpochDay()
    }
    val fullDate = remember(millis) { SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis)) }
    return when (daysAgo) {
        0L -> stringResource(R.string.account_backup_today, timeOnly)
        1L -> stringResource(R.string.account_backup_yesterday, timeOnly)
        else -> fullDate
    }
}
