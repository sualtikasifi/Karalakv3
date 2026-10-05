package com.sualtikasifi.cizimhafiza.presentation.account

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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .screenBackground()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(TopActionsClearance))

                ProfileHeader(uiState)
                Spacer(modifier = Modifier.height(10.dp))
                StatsRow(uiState)
                Spacer(modifier = Modifier.height(10.dp))
                AccountCard(
                    uiState = uiState,
                    onSignIn = viewModel::signIn,
                    onSignOut = viewModel::promptSignOut
                )

                uiState.message?.let { message ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = message.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.tokens.success,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                uiState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = message.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Offered whether or not a Google account is signed in: an
                // anonymous player still has a uid with a profile, a friends
                // list and a league entry under it, and Play's requirement is
                // about the data, not about how the account was created.
                Spacer(modifier = Modifier.height(24.dp))
                DangerZone(isDeleting = uiState.isDeleting, onDelete = viewModel::promptDeleteAccount)
                Spacer(modifier = Modifier.height(16.dp))
            }
            ScreenTopActions(onBack = onBack, title = stringResource(R.string.account_title), modifier = Modifier.align(Alignment.TopStart))
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

/**
 * The top of the page: the player's frame and picture, their name, and the level / rank line that turns over
 * every five seconds (the same label the league table and the lobby use), with progress to the next level.
 */
@Composable
private fun ProfileHeader(uiState: AccountUiState) {
    val linked = uiState.authState as? AuthState.Linked
    val name = uiState.nickname.ifBlank {
        linked?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.account_guest_badge)
    }
    val progress = uiState.levelProgress
    RaisedCard(corner = 28.dp, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primaryContainer, Color.Transparent)
                        )
                    )
            )
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevelAvatar(
                        level = uiState.level,
                        frame = uiState.frame,
                        size = 76.dp,
                        photo = com.sualtikasifi.cizimhafiza.presentation.common.avatarPhotoOf(linked?.photoUrl)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        RankLevelLabel(level = uiState.level, bullet = false)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusPill(linked = linked)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress.progressFraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (progress.isMaxLevel) stringResource(R.string.account_xp_max)
                        else stringResource(R.string.account_xp_progress, progress.xpIntoLevel, progress.xpForThisLevel),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!progress.isMaxLevel) {
                        Text(
                            text = stringResource(R.string.account_xp_to_next, progress.xpToNextLevel),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** "Linked with Google · address" or "Guest account": who this profile belongs to, in one line. */
@Composable
private fun StatusPill(linked: AuthState.Linked?) {
    val isLinked = linked != null
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isLinked) AppTheme.tokens.successContainer else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isLinked) Icons.Filled.Check else Icons.Filled.Person,
            contentDescription = null,
            tint = if (isLinked) AppTheme.tokens.success else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = if (isLinked) {
                listOfNotNull(stringResource(R.string.account_google_linked), linked?.email).joinToString(" · ")
            } else {
                stringResource(R.string.account_guest_badge)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Three figures worth glancing at: total XP, games played, best streak. */
@Composable
private fun StatsRow(uiState: AccountUiState) {
    val number = remember { java.text.NumberFormat.getIntegerInstance() }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile(
            value = number.format(uiState.levelProgress.totalXp),
            label = stringResource(R.string.account_stat_xp),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = number.format(uiState.gamesPlayed),
            label = stringResource(R.string.account_stat_games),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = number.format(uiState.bestStreak),
            label = stringResource(R.string.account_stat_streak),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    RaisedCard(corner = 18.dp, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/** Small section heading used on the page's cards: an icon well and a title. */
@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconWell(icon = icon, size = 32.dp)
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Sign-in state and backup in one card: who the profile is tied to, whether progress is safe, and the single
 * action that fits (sign in, or sign out).
 */
@Composable
private fun AccountCard(uiState: AccountUiState, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    RaisedCard(corner = 22.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            SectionHeader(Icons.Filled.CloudSync, stringResource(R.string.account_section_account))
            Spacer(modifier = Modifier.height(8.dp))
            when {
                uiState.isSignedIn -> SyncStatusRow(lastBackupAtMillis = uiState.lastBackupAtMillis)
                uiState.isGoogleSignInConfigured -> Text(
                    text = stringResource(R.string.account_guest_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Text(
                    text = stringResource(R.string.account_not_configured_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (uiState.isSignedIn || uiState.isGoogleSignInConfigured) {
                Spacer(modifier = Modifier.height(10.dp))
                if (uiState.isBusy) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(30.dp))
                    }
                } else if (uiState.isSignedIn) {
                    SecondaryButton(
                        text = stringResource(R.string.account_sign_out),
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    GoogleSignInButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** Account deletion, set apart at the bottom so it is never one tap from something harmless. */
@Composable
private fun DangerZone(isDeleting: Boolean, onDelete: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isDeleting) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        } else {
            Text(
                text = stringResource(R.string.account_delete_action),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * States the sync guarantee in the one place a player would look for it —
 * replacing the two buttons that used to imply syncing was their job.
 *
 * The two states are told apart deliberately. This row used to show the
 * green "kaydedildi" tick unconditionally, so an account whose progress had
 * never once reached the cloud was still reassured that it had — which is
 * the exact false comfort behind the account that was lost. A backup that
 * has not happened yet now looks like one that has not happened yet.
 */
@Composable
private fun SyncStatusRow(lastBackupAtMillis: Long?) {
    val backedUp = lastBackupAtMillis != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (backedUp) Icons.Filled.CloudDone else Icons.Filled.CloudSync,
            contentDescription = null,
            tint = if (backedUp) AppTheme.tokens.success else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column {
            Text(
                text = stringResource(
                    if (backedUp) R.string.account_sync_on else R.string.account_sync_pending
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = lastBackupAtMillis?.let {
                    stringResource(R.string.account_last_backup_format, rememberBackupTimestamp(it))
                } ?: stringResource(R.string.account_never_backed_up),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Shown signed in or out: an anonymous player has a nickname too (it is
 * what friends and league tables already show).
 *
 * Explicitly saved, unlike the Oda Kur/Koda Katıl fields it used to copy.
 * Writing on every keystroke meant clearing the field wrote a BLANK name,
 * and a blank name is exactly what util.ProfileNameSynchronizer refills
 * from the Google account — so deleting your name put the old one straight
 * back, mid-deletion. A name that only leaves the screen when the player
 * says so has no such window, and it also gives the write somewhere to
 * report from: this is the one field in the app that also travels to two
 * servers (see AccountViewModel.saveNickname).
 */
@Composable
private fun NicknameCard(
    editable: Boolean,
    draft: String,
    canSave: Boolean,
    saveState: NicknameSaveState,
    error: Int?,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit
) {
    RaisedCard(corner = 22.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            SectionHeader(Icons.Filled.Person, stringResource(R.string.account_section_username))
            Spacer(modifier = Modifier.height(12.dp))
            AppTextField(
                value = draft,
                onValueChange = onDraftChange,
                enabled = editable,
                label = stringResource(R.string.account_nickname_label),
                placeholder = stringResource(R.string.account_nickname_hint),
                // Autocorrect off is not cosmetic here. With it on, the IME
                // keeps a composing region over the whole word, and backspace
                // deletes that region rather than a character — which is why
                // clearing this field wiped a word at a time. A nickname is
                // not a dictionary word anyway, so there was never anything
                // for autocorrect to usefully do.
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { if (canSave) onSave() }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (error != null) {
                Text(
                    text = stringResource(error),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (editable) {
                Text(
                    text = stringResource(R.string.username_change_taken_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                NicknameSaveButton(canSave = canSave, saveState = saveState, onSave = onSave)
            } else {
                Text(
                    text = stringResource(R.string.nickname_locked_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * One button carrying all three states, rather than a button plus a
 * separate toast: the confirmation belongs where the action was, and a
 * message that appears somewhere else is a message that gets missed.
 */
@Composable
private fun NicknameSaveButton(canSave: Boolean, saveState: NicknameSaveState, onSave: () -> Unit) {
    val saved = saveState == NicknameSaveState.Saved
    // Animated rather than swapped so the button does not jump between
    // states — it settles into the confirmation and back out of it.
    val face by animateColorAsState(
        targetValue = if (saved) AppTheme.tokens.success else MaterialTheme.colorScheme.primary,
        animationSpec = tween(320),
        label = "nickname_save_face"
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when (saveState) {
            NicknameSaveState.Saving -> CircularProgressIndicator(modifier = Modifier.size(26.dp))
            else -> PrimaryButton(
                text = stringResource(
                    if (saved) R.string.account_nickname_saved else R.string.account_nickname_save
                ),
                icon = if (saved) Icons.Filled.Check else Icons.Filled.Save,
                onClick = onSave,
                // Stays visible once saved so the confirmation has something
                // to sit on; there is simply nothing left to save.
                enabled = canSave,
                height = 50.dp,
                face = face,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
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
