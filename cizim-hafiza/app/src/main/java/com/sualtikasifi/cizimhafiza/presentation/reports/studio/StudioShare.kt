package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.PostLink
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.SocialPlatform
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import java.io.File

// Putting a finished video or picture into the clipboard or another app.

internal fun postText(caption: String, hashtags: String, platform: SocialPlatform? = null, lang: StudioLang = StudioLang.Tr): String {
    val text = listOf(caption.trim(), hashtags.trim()).filter { it.isNotEmpty() }.joinToString("\n\n")
    // the viewer is sent to the app's Google Play page: by the link itself where the platform makes it tappable, by "link in bio" elsewhere
    return if (platform != null) PostLink.withFooter(text, platform, lang) else text
}

internal fun copy(context: Context, text: String) {
    runCatching {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak", text))
        Toast.makeText(context, "Metin kopyalandı", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Hands a picture to ChatGPT or Gemini, with the prompt in the message and on the clipboard to paste beside it.
 * Gemini's app is tried directly first; the share sheet is the way out when it is not installed.
 */
internal fun shareImage(context: Context, file: File, prompt: String, target: ShareTarget) {
    runCatching {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak", prompt))
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, prompt)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val title = if (target == ShareTarget.Gemini) "Gemini'ye gönder" else "ChatGPT'ye gönder"
        var started = false
        if (target == ShareTarget.Gemini) {
            started = runCatching {
                context.startActivity(Intent(send).setPackage(GEMINI_PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
        }
        if (!started) context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val hint = if (target == ShareTarget.Gemini) "İstem panoya kopyalandı; Gemini'de Video'yu seçip gönder" else "İstem panoya kopyalandı; ChatGPT'de yapıştır"
        Toast.makeText(context, hint, Toast.LENGTH_LONG).show()
    }.onFailure { Toast.makeText(context, "Paylaşılamadı: ${it.message}", Toast.LENGTH_LONG).show() }
}

internal const val GEMINI_PACKAGE = "com.google.android.apps.bard"

/** Hands several pictures to the share sheet as one swipe-through post (Instagram, WhatsApp…), post text on the clipboard. */
internal fun shareImages(context: Context, files: List<File>, text: String) {
    runCatching {
        if (text.isNotEmpty()) (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak", text))
        val uris = ArrayList(files.map { FileProvider.getUriForFile(context, context.packageName + ".fileprovider", it) })
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            if (text.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Görselleri paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        if (text.isNotEmpty()) Toast.makeText(context, "Açıklama panoya kopyalandı", Toast.LENGTH_SHORT).show()
    }.onFailure { Toast.makeText(context, "Paylaşılamadı: ${it.message}", Toast.LENGTH_LONG).show() }
}

/** Hands the video to the system share sheet (Instagram, TikTok, WhatsApp…), with the post text on the clipboard. */
internal fun share(context: Context, file: File, caption: String, hashtags: String, platform: SocialPlatform? = null, lang: StudioLang = StudioLang.Tr) {
    runCatching {
        val text = postText(caption, hashtags, platform, lang)
        if (text.isNotEmpty()) {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Karalak", text))
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (text.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Videoyu paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        if (text.isNotEmpty()) Toast.makeText(context, "Açıklama panoya kopyalandı", Toast.LENGTH_SHORT).show()
    }.onFailure { Toast.makeText(context, "Paylaşılamadı: ${it.message}", Toast.LENGTH_LONG).show() }
}
