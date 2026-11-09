package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.data.local.dao.DrawingResultDao
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository
import com.sualtikasifi.cizimhafiza.domain.repository.AuthState
import com.sualtikasifi.cizimhafiza.presentation.reports.console.AssistantViewModel
import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.AacEncoder
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.Pcm
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.PcmOps
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.Placed
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.SpeechSynth
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.ElevenLabs
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.VoiceBank
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.VoiceModel
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.VoiceSpec
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotSprites
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotTimeline
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotDirector
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotMode
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotRenderer
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotSource
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotChest
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotScript
import com.sualtikasifi.cizimhafiza.domain.model.Coupon
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.domain.model.CouponExistsException
import com.sualtikasifi.cizimhafiza.domain.repository.CouponRepository
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.ClipAudio
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.CardLayout
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.ClipFrames
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.LibraryItem
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDirector
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealEnding
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealOpening
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.SocialPlatform
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.TimedWord
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.VideoPost
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMix
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealHowto
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMusic
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.VoicedLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealImages
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealProject
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealRenderer
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealStore
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RecentDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import com.sualtikasifi.cizimhafiza.data.local.dao.WordDao
import com.sualtikasifi.cizimhafiza.domain.repository.ModerationRepository
import com.sualtikasifi.cizimhafiza.util.GameConstants
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/** The studio's video formats. */
enum class StudioFormat(val title: String, val blurb: String) {
    Forgot("Ödüllü Karalak Düellosu", "Çizenin bile tanıyamadığı çizim; doğru cevap kupon kodu"),
    RealLife("Gerçek Olsaydı", "Çizimler gerçek hayata dönüşüyor")
}

/** Who gives Karalak its voice: the phone's own text-to-speech, or ElevenLabs. */
enum class VoiceEngine { Phone, ElevenLabs }

/** ElevenLabs settings as the panel shows them; the key itself never leaves SharedPreferences. */
data class ElevenUi(
    val keySet: Boolean = false,
    /** The key's last characters, so the panel can show which one is in. */
    val keyHint: String = "",
    val voiceId: String = ElevenLabs.PRESETS.first().id,
    val model: VoiceModel = VoiceModel.V3,
    /** 0 creative · 0.5 natural · 1 robust. */
    val stability: Float = 0.5f,
    /** v3 only: say each line the way Karalak's face says it ("[laughs]", "[surprised]"…). */
    val tags: Boolean = true,
    /** The language of the videos: what the assistant writes in and what the voice speaks. */
    val lang: StudioLang = StudioLang.Tr,
    /** The account's own voices, when the key may list them. */
    val voices: List<ElevenLabs.Voice> = emptyList(),
    val status: String? = null,
    val busy: Boolean = false,
    /** The current script's lines already voiced with these settings, the ones still to voice, and what those would cost. */
    val ready: Int = 0,
    val missing: Int = 0,
    val missingCredits: Int = 0,
    /** "Seslendiriliyor 3/14" while voicing. */
    val progress: String? = null,
    /** A line not yet voiced that the person wants to hear: asks before spending credits on it. */
    val confirm: SpokenLine? = null
) {
    val spec: VoiceSpec get() = VoiceSpec(voiceId, model, stability.toDouble(), tags, lang)
}

/** Which spoken line of the script an edit is about. */
sealed interface LineRef {
    data object ForgotHook : LineRef
    data class ForgotGuess(val index: Int) : LineRef
    data object ForgotAsk : LineRef
    data object ForgotCode : LineRef
    data object ForgotOutro : LineRef
}

/** "Çizen Bile Bilemedi": the chosen drawing, what its drawer wrote, how the code is given, what it gives, and the script. */
data class ForgotUi(
    val drawingId: Long? = null,
    /** Where the drawing is picked from: this phone's own, or the pool of the players' (the same one "Gerçek Olsaydı" reads). */
    val source: ForgotSource = ForgotSource.Phone,
    val script: ForgotScript? = null,
    val mode: ForgotMode = ForgotMode.Word,
    /** What the code gives, all set by hand: gold, XP and one chest (or none). */
    val chest: ForgotChest = ForgotChest.Gold,
    val gold: Int = 0,
    val xp: Int = 0,
    /** How many accounts may use the code, and the date and time it stops working (device time zone). */
    val maxUses: Int = 100,
    val expiresAtMillis: Long = ForgotCopy.defaultExpiry(),
    /** The code written out on the end card in [ForgotMode.Shown]; a fresh one on demand. */
    val shownCode: String = CouponCodes.generate(),
    val writing: Boolean = false,
    val writeError: String? = null,
    /** The draft this work is saved as (see ForgotDraft), the drawing it holds (kept whole, so it is found even when the lists have changed), and whether it is "Tamamlandı". */
    val draftId: Long? = null,
    val draftDrawing: StudioDrawing? = null,
    val done: Boolean = false,
    /** The finished video kept with the draft, and the script it was made from. */
    val draftVideo: String? = null,
    val draftVideoFingerprint: Int = 0,
    /** How many finished videos' lines the assistant is shown as examples. */
    val learned: Int = 0,
    /** The app's music under the video (a [RealMusic] track's name; null: none) and how loud it is before it steps back under Karalak's voice. Kept on this phone. */
    val music: String? = null,
    val musicVolume: Float = com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMix.DEFAULT_VOLUME,
    /** The opening line alone being rewritten ("yenile"), why it failed, how many were asked for (they take turns between kinds), and the ones already tried. */
    val hookWriting: Boolean = false,
    val hookError: String? = null,
    val hookTurn: Int = 0,
    val hookTried: List<String> = emptyList(),
    val couponBusy: Boolean = false,
    val couponStatus: String? = null,
    /** How many of the phone's drawings the list shows: [FORGOT_PAGE] at first, that many more each time "Daha fazla" is pressed. */
    val shown: Int = FORGOT_PAGE,
    /** The wording of the on-screen texts: one picked by hand, or null to let the three take turns from video to video. */
    val styleChoice: ForgotStyle? = null
)

/** "Gerçek Olsaydı": the pool's latest drawings, and the project (the chosen drawings of different words) on this phone. */
data class RealUi(
    /** Finished pieces kept on this phone (picture, clip, card details), newest first. */
    val library: List<LibraryItem> = emptyList(),
    /** Library pieces ticked to be made into a video, in the order they were ticked. */
    val librarySelected: List<String> = emptyList(),
    /** The library piece open in the viewer. */
    val libraryView: String? = null,
    val libraryShown: Boolean = false,
    val libraryBytes: Long = 0L,
    val recent: List<RecentDrawing> = emptyList(),
    val loadingRecent: Boolean = false,
    val recentError: String? = null,
    val project: RealProject? = null,
    val writing: Boolean = false,
    val writeError: String? = null,
    /** The drawing whose video scene ideas are being written, and why the last try failed. */
    val pitching: String? = null,
    val pitchError: String? = null,
    /** Which of Karalak's lines the assistant is writing ideas for. */
    val lineWriting: RealLine? = null,
    val lineErrors: Map<RealLine, String> = emptyMap(),
    /** The line being voiced in ElevenLabs right now. */
    val lineVoicing: Pair<RealLine, String>? = null,
    /** The video drawing's ideas already voiced with the current voice settings (paid for, kept on this phone), per line. */
    val lineVoiced: Map<RealLine, Set<String>> = emptyMap(),
    /** The music track playing on the speaker to choose from, if any. */
    val musicPlaying: String? = null,
    /** The drawing whose picture, clip or reaction is being worked on. */
    val busy: String? = null,
    /** Starting over while the project has pictures in it: asks before throwing them away. */
    val confirmReset: Boolean = false,
    /** Where the Karalak card sits on the video and how big it is, kept on this phone. */
    val card: CardLayout = CardLayout(),
    /** The app's promo video (one, kept on this phone) and its length; whether it closes the videos is [ending]'s. */
    val promo: File? = null,
    val promoMs: Long = 0L,
    val promoBusy: Boolean = false,
    /** What closes every video: a short end card (the default), the promo video, or nothing after Karalak's closing. */
    val ending: RealEnding = RealEnding.Card,
    /** The video that shows how the game is played (shipped in the app, no sound) and its length; shown before the end card while [howtoOn]. */
    val howto: File? = null,
    val howtoMs: Long = 0L,
    val howtoOn: Boolean = true,
    /** Whether the app's music is mixed into the video; off, it is left out so a trending sound can be added in the app it is posted to. */
    val musicOn: Boolean = true,
    /** The music the person added from their own files (names, see RealMusic), after the app's own tracks. */
    val customMusic: List<String> = emptyList(),
    /** How many of the ready-made closing lines are voiced with the current settings, and how many voices are kept for good. */
    val endVoiced: Int = 0,
    val endKept: Int = 0,
    /** Where the video is going to be posted: what its post text ends with. */
    val platform: SocialPlatform = SocialPlatform.TikTok,
    /** A picture ready to hand to ChatGPT or Gemini, with the prompt to paste. */
    val share: RealShare? = null,
    /** The swipe-through post: each drawing followed by what it became, ready for the share sheet. */
    val carousel: List<File>? = null,
    /** Which part of "Gerçek Olsaydı" is open, which of its seven steps, and which of the voices' five; kept on this phone. */
    val tab: RealTab = RealTab.Prepare,
    val step: Int = 1,
    val voiceStep: Int = 1
)

enum class RealTab { Prepare, Library, Settings }

enum class ShareTarget { ChatGpt, Gemini }

class RealShare(val file: File, val prompt: String, val target: ShareTarget = ShareTarget.ChatGpt)

data class StudioState(
    val format: StudioFormat = StudioFormat.RealLife,
    val loading: Boolean = true,
    /** This phone's own drawings. */
    val drawings: List<StudioDrawing> = emptyList(),
    val mascotPoses: Int = 0,
    val mascotLoading: Boolean = true,
    val aiCost: Double? = null,
    /** The model that wrote the last one (a fallback may have stepped in for the chosen one). */
    val aiModel: String? = null,
    /** The model that reads the drawings in "Gerçek Olsaydı" (an OpenRouter id), kept on this phone. */
    val analysisModel: String = OpenRouterClient.ANALYSIS_MODEL,
    val caption: String = "",
    val hashtags: String = DEFAULT_TAGS,
    val voiceOn: Boolean = true,
    val voiceEngine: VoiceEngine = VoiceEngine.Phone,
    val eleven: ElevenUi = ElevenUi(),
    val voicePitch: Float = 1.2f,
    val voiceRate: Float = 1.05f,
    val voiceError: String? = null,
    val preview: ImageBitmap? = null,
    val previewMs: Long = 0L,
    val totalMs: Long = 0L,
    val rendering: Boolean = false,
    val renderStage: String = "",
    val progress: Float = 0f,
    val video: File? = null,
    val renderError: String? = null,
    val message: String? = null,
    val forgot: ForgotUi = ForgotUi(),
    val real: RealUi = RealUi(),
    /** The log of videos made on this phone, newest first (see [PublishLog]). */
    val publishLog: List<PublishEntry> = emptyList(),
    /** What making videos costs, as OpenRouter and ElevenLabs report it. */
    val spend: SpendUi = SpendUi(),
    /** Counts up each time a draft is opened from the log, so the panel scrolls back to the top. */
    val openedDraft: Int = 0
) {
    /** The language the videos are written and spoken in. */
    val lang: StudioLang get() = eleven.lang

    val ready: Boolean get() = !rendering && when (format) {
        StudioFormat.Forgot -> forgot.script != null
        StudioFormat.RealLife -> real.project?.shown?.isNotEmpty() == true
    }

    /**
     * The drawing a Düello script was made from: one of this phone's own (a row number), or one of the pool's (a negative id,
     * see [RecentDrawing.studioId]); null when it is in neither any more.
     */
    fun forgotDrawing(id: Long?): StudioDrawing? =
        if (id == null) null
        else drawings.find { it.id == id } ?: real.recent.firstOrNull { it.studioId == id }?.toStudio() ?: forgot.draftDrawing?.takeIf { it.id == id }
}

/** The most accounts one code may be given to. */
internal const val MAX_COUPON_USES = 1_000_000

/** The drawing list of "Çizen Bile Bilemedi" grows by this many at a time (three rows of three). */
const val FORGOT_PAGE = 9

internal const val DEFAULT_TAGS = "#çizimoyunu #karalak #yapayzeka #oyunönerisi #çizim"
