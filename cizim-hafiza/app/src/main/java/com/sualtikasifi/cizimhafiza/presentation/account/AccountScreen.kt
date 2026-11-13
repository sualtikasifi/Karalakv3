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
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sinkWith
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sunkenFace
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedBackButton
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.graphicsLayer
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
    var showRename by remember { mutableStateOf(false) }

    // The page is one painted scene (bg_account: the sign, the profile card with its bar, the three figure cards, the
    // account ribbon and strip, the two buttons - all left empty). Everything that changes is laid over it by the
    // picture's own coordinates, so every box below is a rectangle of that picture.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val unit = minOf(maxWidth / ArtW, maxHeight / ArtH)
        val offX = (maxWidth - unit * ArtW) / 2
        val offY = (maxHeight - unit * ArtH) / 2
        val us = unit.value
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * us / fontScale0).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(offX + unit * x0, offY + unit * y0).requiredSize(unit * (x1 - x0), unit * (y1 - y0))

        val online = com.sualtikasifi.cizimhafiza.presentation.common.LocalOnline.current
        val scene = cachedPainterResource(R.drawable.bg_account)
        // The painted buttons (sign out, delete) cave in under the finger: their own pixels are drawn again, shrunk, inside their frame
        // (see sunkenFace), and the words dip together with them.
        val sceneBitmap = remember { runCatching { BackdropCache.get(context.resources, R.drawable.bg_account) }.getOrNull() }
        val pxX = (sceneBitmap?.width ?: 1) / ArtW
        val pxY = (sceneBitmap?.height ?: 1) / ArtH
        val outSink = com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink(0.94f)
        val delSink = com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink(0.95f)
        if (offX > 1.dp || offY > 1.dp) {
            Image(scene, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(20.dp))
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }
        Image(scene, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = box(0f, 0f, ArtW, ArtH))

        // The painted back arrow in the corner.
        Box(
            box(14f, 66f, 138f, 172f)
                .pressable(pressedScale = 0.92f, onClick = onBack)
                .a11yButton(stringResource(R.string.cd_back))
        )

        // ---- The sign ------------------------------------------------------------------------------------------
        LetteredText(
            text = stringResource(R.string.account_title),
            size = fs(104f),
            fill = Color.White,
            outline = Color(0xFF5A2815),
            modifier = box(215f, 301f, 625f, 401f),
            minScale = 0.5f,
            title = true
        )

        // ---- Profile card --------------------------------------------------------------------------------------
        val name = uiState.nickname.ifBlank {
            linked?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.account_guest_badge)
        }
        Box(box(70f, 490f, 306f, 734f), contentAlignment = Alignment.Center) {
            LevelAvatar(
                level = uiState.level,
                frame = uiState.frame,
                size = (214f * us).dp,
                photo = com.sualtikasifi.cizimhafiza.presentation.common.avatarPhotoOf(linked?.photoUrl)
            )
        }
        FitText(
            text = name,
            style = PaintedStyle(color = PageInk, fontSize = fs(58f), textAlign = TextAlign.Start),
            maxLines = 1,
            minScale = 0.5f,
            contentAlignment = Alignment.CenterStart,
            modifier = box(335f, 504f, 682f, 566f)
        )
        // The painted pencil button: rename (once, before Google is linked), or the reason it cannot be.
        Box(
            box(692f, 505f, 765f, 578f)
                .pressable(pressedScale = 0.9f, onClick = { showRename = true })
                .a11yButton(stringResource(R.string.account_nickname_label)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = Color(0xFFD9701F), modifier = Modifier.size(unit * 38f))
        }
        // The rank on its painted pill.
        val tier = com.sualtikasifi.cizimhafiza.domain.model.LevelTier.forLevel(uiState.level)
        Row(
            box(330f, 563f, 537f, 605f).padding(horizontal = unit * 14f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(tier.rank.emoji, fontSize = fs(28f))
            Spacer(Modifier.size(unit * 8f))
            FitText(
                text = stringResource(tier.rank.nameRes),
                style = PaintedStyle(color = Color(0xFF5A3418), fontSize = fs(33f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                maxLines = 1,
                minScale = 0.55f,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        // Who the profile belongs to. The painted pill is green (a linked Google account); a guest's is covered by a plain one.
        // A linked pill has two lines, so a long address has the whole width of its own line: it shrinks, and when that is not
        // enough the middle of the part before the @ is cut, never the domain.
        if (linked == null) {
            Box(box(330f, 622f, 712f, 712f).clip(RoundedCornerShape(unit * 45f)).background(Color(0xFFF1E2C6)))
        }
        Row(
            box(330f, 622f, 712f, 712f).padding(horizontal = unit * 26f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (linked != null) Icons.Filled.Check else Icons.Filled.Person,
                contentDescription = null,
                tint = if (linked != null) Color(0xFF2E9B3E) else Color(0xFF8A6A50),
                modifier = Modifier.size(unit * 46f)
            )
            Spacer(Modifier.size(unit * 14f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                FitText(
                    text = if (linked != null) stringResource(R.string.account_google_linked) else stringResource(R.string.account_guest_badge),
                    style = PaintedStyle(color = PageInk, fontSize = fs(33f), textAlign = TextAlign.Start),
                    maxLines = 1,
                    minScale = 0.6f,
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth().height(unit * 38f)
                )
                val email = linked?.email
                if (!email.isNullOrBlank()) {
                    FitText(
                        text = shortenEmail(email, 30),
                        style = PaintedStyle(color = Color(0xFF4F3A28), fontSize = fs(27f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Start),
                        maxLines = 1,
                        minScale = 0.65f,
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth().height(unit * 32f)
                    )
                }
            }
        }

        // The XP bar: the painted one is only a frame; its track and fill are drawn here so the fill follows the real progress.
        val fraction by androidx.compose.animation.core.animateFloatAsState(
            progress.progressFraction.coerceIn(0f, 1f),
            androidx.compose.animation.core.tween(700),
            label = "xpBar"
        )
        Box(box(196f, 777f, 747f, 820f).clip(RoundedCornerShape(unit * 21f)).background(Color(0xFF5F2C10))) {
            val f = fraction.coerceIn(0.06f, 1f)
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight(f)
                        .background(
                            Brush.verticalGradient(
                                0f to Color(0xFFFCEC9C), 0.2f to Color(0xFFFCCD15), 0.5f to Color(0xFFFEBA17),
                                0.66f to Color(0xFFFE9701), 1f to Color(0xFFF36501)
                            )
                        )
                )
                if (f < 0.995f) {
                    Box(Modifier.fillMaxHeight().size(width = unit * 4f, height = unit * 43f).background(Color(0xFF8A3206)))
                    Spacer(Modifier.weight(1f - f))
                }
            }
            // The figure is centred on the whole bar, in white with a dark edge so it reads on the orange and on the dark part.
            Text(
                text = if (progress.isMaxLevel) stringResource(R.string.account_xp_max)
                else stringResource(R.string.account_xp_progress, progress.xpIntoLevel, progress.xpForThisLevel),
                style = PaintedStyle(
                    color = Color.White, fontSize = fs(30f), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                ).copy(shadow = androidx.compose.ui.graphics.Shadow(Color(0xDD3A1A08), androidx.compose.ui.geometry.Offset(0f, 2f), 5f)),
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        // The level, on the painted star.
        LetteredText(
            text = "${uiState.level}",
            size = fs(54f),
            outline = Color(0xFF5A2815),
            modifier = box(98f, 758f, 182f, 834f),
            minScale = 0.5f
        )

        // ---- The three figures ---------------------------------------------------------------------------------
        val stats = listOf(
            Triple(number.format(uiState.levelProgress.totalXp), stringResource(R.string.account_stat_xp), R.drawable.ac_icon_star),
            Triple(number.format(uiState.gamesPlayed), stringResource(R.string.account_stat_games), R.drawable.ac_icon_games),
            Triple(number.format(uiState.bestStreak), stringResource(R.string.account_stat_streak), R.drawable.ac_icon_cal)
        )
        val centres = listOf(166f, 416.5f, 669f)
        stats.forEachIndexed { i, (value, label, icon) ->
            val cx = centres[i]
            Image(
                painterResource(icon),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = box(cx - 58f, 926f, cx + 58f, 998f)
            )
            FitText(
                text = value,
                style = PaintedStyle(color = PageInk, fontSize = fs(56f), textAlign = TextAlign.Center),
                maxLines = 1,
                minScale = 0.5f,
                modifier = box(cx - 100f, 998f, cx + 100f, 1048f)
            )
            FitText(
                text = label,
                style = PaintedStyle(color = PageInk, fontSize = fs(26f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = fs(28f)),
                maxLines = 2,
                minScale = 0.6f,
                modifier = box(cx - 70f, 1046f, cx + 70f, 1104f)
            )
        }

        // ---- Account and backup --------------------------------------------------------------------------------
        Row(
            box(168f, 1166f, 668f, 1234f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Cloud, contentDescription = null, tint = Color.White, modifier = Modifier.size(unit * 54f))
            Spacer(Modifier.size(unit * 12f))
            LetteredText(
                text = stringResource(R.string.account_section_account),
                size = fs(52f),
                outline = Color(0xFF3A0F5E),
                modifier = Modifier.weight(1f, fill = false).height(unit * 68f),
                minScale = 0.5f
            )
        }
        val backedUp = uiState.lastBackupAtMillis != null
        val saved = uiState.isSignedIn && backedUp
        // The strip's painted tick and cloud say "saved": only true for a signed-in account that has been backed up, so for
        // anyone else they are covered in the strip's own colour and what is really the case is shown instead.
        if (!saved) {
            Box(box(70f, 1286f, 146f, 1364f).clip(CircleShape).background(Color(0xFFFCEACB)), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (uiState.isSignedIn) Icons.Filled.CloudSync else Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = Color(0xFF8A6A50),
                    modifier = Modifier.size(unit * 48f)
                )
            }
            Box(box(630f, 1266f, 778f, 1388f).clip(RoundedCornerShape(unit * 22f)).background(Brush.verticalGradient(listOf(Color(0xFFFCEDCF), Color(0xFFFCE8C9)))))
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
        // The two lines are one block, centred on the strip (1275..1385) between the tick and the cloud.
        Column(
            box(156f, 1277f, 628f, 1383f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            FitText(
                text = strip1,
                style = PaintedStyle(color = PageInk, fontSize = fs(37f), textAlign = TextAlign.Start),
                maxLines = if (strip2 == null) 2 else 1,
                minScale = 0.55f,
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth().height(unit * (if (strip2 == null) 90f else 52f))
            )
            if (strip2 != null) {
                FitText(
                    text = strip2,
                    style = PaintedStyle(color = Color(0xFF6B4A30), fontSize = fs(30f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start),
                    maxLines = 1,
                    minScale = 0.55f,
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth().height(unit * 40f)
                )
            }
        }

        // ---- Sign out / sign in --------------------------------------------------------------------------------
        when {
            uiState.isBusy -> Box(box(150f, 1462f, 685f, 1590f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp), color = Color.White)
            }
            uiState.isSignedIn -> Box(
                box(150f, 1462f, 685f, 1590f)
                    .sunkenFace(outSink, sceneBitmap, 150f * pxX, 1462f * pxY, 535f * pxX, 128f * pxY, (15f * us).dp, (50f * us).dp)
                    .clickable(interactionSource = outSink.source, indication = null, onClick = viewModel::promptSignOut)
                    .a11yButton(stringResource(R.string.account_sign_out)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    Modifier.sinkWith(outSink, 0.5f, 0.5f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(unit * 64f))
                    Spacer(Modifier.size(unit * 16f))
                    LetteredText(
                        stringResource(R.string.account_sign_out), fs(66f), outline = Color(0xFF8A3A00),
                        modifier = Modifier.height(unit * 90f).weight(1f, fill = false), minScale = 0.5f
                    )
                }
            }
            // A guest: Google's own white button sits over the painted orange one (it covers it to the rim).
            else -> Box(box(141f, 1453f, 694f, 1599f), contentAlignment = Alignment.Center) {
                GoogleSignInButton(
                    onClick = com.sualtikasifi.cizimhafiza.presentation.common.onlineOnly(viewModel::signIn),
                    modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (online) 1f else 0.55f },
                    height = unit * 146f,
                    corner = unit * 73f
                )
            }
        }

        // ---- Delete --------------------------------------------------------------------------------------------
        // Offered whether or not a Google account is signed in: an anonymous player still has a uid with a profile, a
        // friends list and a league entry under it, and Play's requirement is about the data, not about how the
        // account was created. The painted pill carries the bin; the words are laid beside it.
        Box(
            box(157f, 1610f, 673f, 1705f)
                .graphicsLayer { alpha = if (online) 1f else 0.55f }
                .sunkenFace(delSink, sceneBitmap, 157f * pxX, 1610f * pxY, 516f * pxX, 95f * pxY, (9f * us).dp, (38f * us).dp)
                .clickable(
                    interactionSource = delSink.source, indication = null,
                    onClick = com.sualtikasifi.cizimhafiza.presentation.common.onlineOnly(viewModel::promptDeleteAccount)
                )
                .a11yButton(stringResource(R.string.account_delete_action)),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isDeleting) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else {
                FitText(
                    text = stringResource(R.string.account_delete_action),
                    style = PaintedStyle(color = Color(0xFFC62828), fontSize = fs(37f), textAlign = TextAlign.Center),
                    maxLines = 1,
                    minScale = 0.55f,
                    modifier = Modifier.sinkWith(delSink, 0.5f, 0.5f).padding(start = unit * 125f, end = unit * 17f).fillMaxWidth().height(unit * 68f)
                )
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
                modifier = box(60f, 1728f, 776f, 1810f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF2FFF6E4))
                    .padding(horizontal = 10.dp)
            )
        }
    }

    if (showRename) {
        val editable = viewModel.nicknameEditable
        LaunchedEffect(uiState.nicknameSaveState) {
            if (uiState.nicknameSaveState == NicknameSaveState.Saved) showRename = false
        }
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialog(
            title = stringResource(R.string.nickname_edit_title),
            onDismiss = { showRename = false },
            buttons = {
                if (editable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton(
                            text = stringResource(R.string.nickname_edit_cancel),
                            onClick = { showRename = false },
                            primary = false,
                            modifier = Modifier.weight(1f)
                        )
                        com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton(
                            text = stringResource(R.string.nickname_edit_save),
                            onClick = viewModel::saveNickname,
                            enabled = uiState.canSaveNickname,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton(
                        text = stringResource(R.string.close),
                        onClick = { showRename = false }
                    )
                }
            }
        ) {
            if (editable) {
                com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialogText(stringResource(R.string.nickname_edit_warning))
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = uiState.nicknameDraft,
                    onValueChange = viewModel::setNicknameDraft,
                    placeholder = stringResource(R.string.nickname_edit_hint),
                    centered = true,
                    modifier = Modifier.fillMaxWidth()
                )
                uiState.nicknameError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialogText(stringResource(R.string.account_nickname_locked))
            }
        }
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


private const val ArtW = 836f
private const val ArtH = 1882f
private val PageInk = Color(0xFF3B2314)

/**
 * [email] cut to about [max] characters without touching the domain: "very.long.address.here@gmail.com" becomes
 * "very.long.addr…@gmail.com". An address that already fits is returned as it is.
 */
internal fun shortenEmail(email: String, max: Int): String {
    if (email.length <= max) return email
    val at = email.lastIndexOf('@')
    if (at <= 0) return email.take(max - 1) + "…"
    val domain = email.substring(at)
    val keep = (max - domain.length - 1).coerceAtLeast(6)
    return if (at <= keep) email else email.take(keep) + "…" + domain
}

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
