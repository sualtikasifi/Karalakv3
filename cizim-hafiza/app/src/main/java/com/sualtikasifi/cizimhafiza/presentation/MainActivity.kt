package com.sualtikasifi.cizimhafiza.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.view.View
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.ads.AdManager
import com.sualtikasifi.cizimhafiza.ads.ConsentManager
import com.sualtikasifi.cizimhafiza.data.repository.GoogleSignInLauncher
import com.sualtikasifi.cizimhafiza.util.AutoBackupPublisher
import com.sualtikasifi.cizimhafiza.presentation.navigation.CizimHafizaNavGraph
import com.sualtikasifi.cizimhafiza.presentation.splash.BrandSplash
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme
import com.sualtikasifi.cizimhafiza.util.MusicPlayer
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Extends AppCompatActivity (not plain ComponentActivity) solely so
// AppCompatDelegate.setApplicationLocales() — the per-app language switch
// used by the Settings screen's language toggle — actually works; Compose
// still owns 100% of the visible UI/theming (see themes.xml's comment).
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var adManager: dagger.Lazy<AdManager>
    @Inject lateinit var consentManager: ConsentManager
    @Inject lateinit var musicPlayer: MusicPlayer
    @Inject lateinit var googleSignInLauncher: GoogleSignInLauncher
    @Inject lateinit var autoBackupPublisher: dagger.Lazy<AutoBackupPublisher>

    private var navController: NavHostController? = null

    private companion object {
        private const val TAG = "MainActivity"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate()/setContent — shows the branded
        // splash (see Theme.Karalak.Splash) until Compose draws its first
        // frame instead of a plain platform default screen.
        com.sualtikasifi.cizimhafiza.util.StartupTrace.mark("activity.onCreate begin")
        val brandSplashComing = savedInstanceState == null
        if (brandSplashComing) com.sualtikasifi.cizimhafiza.presentation.splash.SplashHandOff.reset()
        installSplashScreen().setOnExitAnimationListener { splash ->
            // No exit animation of its own: BrandSplash's first frame is this very picture (the round logo on the
            // field colour, in the same place), and BrandSplash turns it into the painted scene. So the system splash
            // simply stays up until that identical frame is drawn underneath it, then goes (SplashHandOff).
            //
            // Where the platform really drew the logo is read off its icon view, because skins differ in size.
            // getIconView() is @Nullable on API 31+ and androidx's `platformView.iconView!!` can throw (a hand-over
            // with no icon, e.g. the first launch after install), so it is only ever touched inside runCatching: a
            // cosmetic detail must never be able to fail a launch.
            if (!brandSplashComing) {
                runCatching { splash.remove() }
                return@setOnExitAnimationListener
            }
            runCatching {
                val icon = splash.iconView
                if (icon.width > 0) {
                    val at = IntArray(2)
                    icon.getLocationInWindow(at)
                    com.sualtikasifi.cizimhafiza.presentation.splash.SplashHandOff.iconBounds.value =
                        android.graphics.RectF(at[0].toFloat(), at[1].toFloat(), (at[0] + icon.width).toFloat(), (at[1] + icon.height).toFloat())
                }
            }
            com.sualtikasifi.cizimhafiza.presentation.splash.SplashHandOff.hold {
                runCatching { splash.remove() }.onFailure { Log.w(TAG, "Splash remove failed", it) }
            }
        }
        super.onCreate(savedInstanceState)
        com.sualtikasifi.cizimhafiza.util.StartupTrace.mark("activity.super.onCreate done (Hilt injected)")
        // Two earlier attempts at the language-switch black flash targeted
        // what a recreate LOOKED like (this line; the transition override
        // below; android:windowBackground in themes.xml) without noticing
        // neither ever stopped the recreate from happening — the actual
        // fix is AndroidManifest.xml's configChanges="locale" on this
        // Activity, which is what now stops Android from tearing this
        // window down at all. Left in place as a cheap backup for a
        // genuine cold start; harmless either way.
        window.setBackgroundDrawableResource(R.color.splash_background)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // Read once, here, rather than observed: the start destination is
        // fixed for the lifetime of this NavHost, and completing the
        // tutorial navigates away explicitly instead of re-deciding it.
        val tutorialCompleted = settingsRepository.tutorialCompleted
        val composeApp = {
        setContent {
            CizimHafizaTheme {
                // The app's cream page color, not Surface's default white:
                // this is what shows through anywhere a screen's own
                // background doesn't reach (behind the status bar during a
                // transition, for a frame on first draw), and white there
                // read as a seam against every page.
                // Hoisted so the page colour can wait: while the opening scene is up, the Surface is see-through, so the
                // scene already painted as the window's background shows through any frame in which BrandSplash itself
                // has not been drawn yet (it used to flash plain cream there).
                var brandSplashVisible by rememberSaveable { mutableStateOf(true) }
                Surface(
                    color = if (brandSplashVisible) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // The whole app is composed one frame AFTER the opening scene is on screen. Composing the nav graph
                    // (and decoding its first screen's pictures) inside the very first frame is what kept the plain
                    // brown system splash up for seconds before the scene could appear; now that frame holds only
                    // BrandSplash, so the scene shows almost at once and the app builds itself underneath while its
                    // loading bar fills. Saved, so a rotation or language change recreate does not wait again.
                    var appReady by rememberSaveable { mutableStateOf(false) }
                    // True once the app below has composed AND had a couple of frames to draw: only then may the opening
                    // scene be lifted (see BrandSplash's appReady).
                    var appDrawn by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        androidx.compose.runtime.withFrameNanos { }
                        com.sualtikasifi.cizimhafiza.util.StartupTrace.mark("nav graph starts composing")
                        appReady = true
                        androidx.compose.runtime.withFrameNanos { }
                        androidx.compose.runtime.withFrameNanos { }
                        appDrawn = true
                        com.sualtikasifi.cizimhafiza.util.StartupTrace.mark("app drawn, scene may lift")
                    }
                    if (appReady) CizimHafizaNavGraph(
                        onNavControllerReady = { navController = it },
                        tutorialCompleted = tutorialCompleted
                    )
                    com.sualtikasifi.cizimhafiza.presentation.common.XpFlyOverlay()
                    RequestNotificationPermissionOnce(settingsRepository)
                    com.sualtikasifi.cizimhafiza.presentation.common.UsernameFinalizeHost()
                    GoogleSignInLauncherHost(googleSignInLauncher)
                    StartMusicAfterFirstFrame(musicPlayer)
                    // Over the app, not instead of it: the nav graph above
                    // composes and draws underneath while this plays, so the
                    // opening costs no startup time. rememberSaveable, so a
                    // rotation or a language-change recreate does not replay
                    // it — only a genuinely cold start does.
                    if (brandSplashVisible) {
                        BrandSplash(appReady = appDrawn, onFinished = {
                            brandSplashVisible = false
                            // The scene served as the window's background only to be there for the very first frame.
                            window.setBackgroundDrawableResource(R.color.splash_background)
                            com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache.evict(R.drawable.splash_art)
                        })
                    }
                }
            }
        }
        }

        // The window's own background is the system splash's picture (logo on the field colour), so nothing between
        // the system splash and BrandSplash can show anything else. The app itself (consent, nav graph, everything) is composed
        // right AFTER that frame is on screen, not inside it: the stretch before the first frame is then only process,
        // Application and Activity start-up, no Compose work.
        if (savedInstanceState == null) {
            window.setBackgroundDrawableResource(R.drawable.splash_window)
            // Never leave the system splash up for good if BrandSplash cannot draw for some reason.
            window.decorView.postDelayed({ com.sualtikasifi.cizimhafiza.presentation.splash.SplashHandOff.brandSplashDrawn() }, 4_000)
        }
        var contentShown = false
        fun showContent() {
            if (contentShown) return
            contentShown = true
            com.sualtikasifi.cizimhafiza.util.StartupTrace.mark("scene on screen, composing the app")
            // Consent first, ads second — always in that order, and from an
            // Activity because UMP needs one to present its form. This used to
            // run unconditionally in the Application class, which meant ad
            // requests went out in the EEA before anyone had been asked, in
            // breach of both GDPR and AdMob's own policy. ensureConsent resolves
            // silently for players in regions with no form requirement.
            consentManager.ensureConsent(this) {
                adManager.get().initializeIfConsented(consentManager)
            }
            composeApp()
        }
        window.decorView.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                window.decorView.viewTreeObserver.removeOnPreDrawListener(this)
                com.sualtikasifi.cizimhafiza.util.StartupTrace.mark("first frame about to draw")
                // After this frame has been drawn.
                window.decorView.post { showContent() }
                return true
            }
        })
        // Safety net: never leave the app without content if that first frame is somehow never drawn.
        window.decorView.postDelayed({ showContent() }, 2000)
    }

    // With AndroidManifest.xml's configChanges="locale" now in place (see
    // its own comment for why that, and not this, is the actual fix for
    // the language-switch black flash), the OS no longer treats a locale
    // change as a reason to tear this Activity down at all, on any API
    // level — so AppCompatDelegate.setApplicationLocales() should no
    // longer have a live path to this override in the first place.
    // Left in place, transition-suppressing, as a defensive fallback in
    // case some future AppCompat version (or a caller elsewhere) calls
    // recreate() for an unrelated reason — never a reason to reintroduce
    // the flash this was originally written to fight.
    override fun recreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        super.recreate()
    }

    // The actual live-update mechanism for a language switch, now that
    // configChanges="locale|layoutDirection" (AndroidManifest.xml) keeps
    // Android from destroying this Activity over one: Compose's own
    // setContent machinery already recomposes stringResource() and every
    // other LocalConfiguration-derived read the instant this fires — see
    // ComposeView.onConfigurationChanged, which AppCompatActivity's default
    // implementation reaches on its own. Overridden only so that's written
    // down somewhere rather than left to look like an oversight; there is
    // nothing left to add by hand.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    // launchMode="singleTask" (see AndroidManifest.xml) means a deep-link tap
    // while the app is already running reuses this Activity instance and
    override fun onResume() {
        super.onResume()
        musicPlayer.onAppForegrounded()
    }

    override fun onPause() {
        super.onPause()
        // Not a media app: the soundtrack belongs to a game being looked at,
        // so it stops the moment the player leaves rather than playing on
        // over whatever they switched to.
        musicPlayer.onAppBackgrounded()
        // A safety net alongside AutoBackupPublisher's own debounced
        // trigger — catches a change (a cosmetic pick with no XP attached)
        // right before the player actually leaves, no-op if unlinked.
        autoBackupPublisher.get().backupNowIfLinked()
    }

    // arrives here instead of a fresh onCreate — so the new URI has to be
    // forwarded to the existing NavController by hand.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navController?.handleDeepLink(intent)
    }
}

/**
 * Asks for the POST_NOTIFICATIONS runtime permission (API 33+) exactly once,
 * as soon as an install first opens — never nags again afterward.
 * The Settings screen's "Bildirimler" toggle offers a way to (re-)request it
 * later for anyone who dismissed this or wants to turn notifications on
 * after having turned them off.
 */
@Composable
private fun RequestNotificationPermissionOnce(settingsRepository: SettingsRepository) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> settingsRepository.setNotificationsEnabled(granted) }

    LaunchedEffect(Unit) {
        if (settingsRepository.notificationPermissionRequested) return@LaunchedEffect
        // Asked the moment a fresh install first opens, before the player has started anything, so it never
        // interrupts a game. Not tied to anything the player does afterwards: it used to wait for a finished
        // game, which made it fire right after a Google sign-in restored an old account's game count on a reinstall.
        settingsRepository.notificationPermissionRequested = true
        val alreadyGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!alreadyGranted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * Starts the soundtrack after Compose's first frame instead of directly in
 * onCreate.
 *
 * [MusicPlayer.start] launches a coroutine that immediately collects two
 * hot StateFlows and, on their first (already-available) value, calls
 * [android.media.MediaPlayer.create] to decode the first track's header —
 * a documented synchronous/blocking call, tens of milliseconds long. Called
 * directly in onCreate (as this used to be), that decode ran on the main
 * thread BEFORE setContent()'s first frame was even composed, delaying the
 * branded splash's own first draw by exactly that long on every cold start.
 * A LaunchedEffect still runs on the main thread (MediaPlayer needs a
 * Looper to deliver its completion/error callbacks, which a plain
 * background-dispatcher thread does not have) but only once composition
 * has produced a frame to hand off to, so the decode can no longer block
 * the first frame that never gets the chance to appear late.
 */
@Composable
private fun StartMusicAfterFirstFrame(musicPlayer: MusicPlayer) {
    LaunchedEffect(Unit) { musicPlayer.start() }
}

/**
 * Registers this Activity instance's launcher with [googleSignInLauncher] —
 * see that class for why the bridge exists at all. `registerForActivityResult`
 * has to run before the Activity reaches STARTED, which composition already
 * guarantees here the same way it does for [RequestNotificationPermissionOnce]
 * above; unbinding on dispose stops a torn-down Activity's dead launcher
 * reference from lingering in a singleton that outlives it.
 */
@Composable
private fun GoogleSignInLauncherHost(googleSignInLauncher: GoogleSignInLauncher) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> googleSignInLauncher.onResult(result) }

    DisposableEffect(launcher) {
        googleSignInLauncher.bind(launcher)
        onDispose { googleSignInLauncher.unbind(launcher) }
    }
}
