package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioLang
import kotlinx.serialization.Serializable
import java.net.URLEncoder

/**
 * Where a video is posted, and what that place does with a link in the post text: TikTok and Instagram do not make
 * one clickable (people tap the link in the profile), YouTube Shorts does.
 */
enum class SocialPlatform(val label: String, val clickableLink: Boolean) {
    TikTok("TikTok", false),
    Instagram("Instagram", false),
    YouTube("YouTube Shorts", true);

    companion object {
        fun of(name: String?): SocialPlatform = entries.find { it.name == name } ?: TikTok
    }
}

/** The tracked link to the app's Google Play page, and the line that sends the viewer to it. */
object PostLink {
    const val CAMPAIGN = "gercek_olsaydi"
    private const val PLAY = "https://play.google.com/store/apps/details?id=com.sualtikasifi.cizimhafiza"

    /**
     * The Play page with a `referrer` that Google Play Console reports as the install's source (Acquisition → UTM): the
     * platform it was posted on, and the campaign. [content], when given, tells one video from another (only a link in
     * the post text can carry it, so it is for YouTube; a profile link is per account).
     */
    fun url(platform: SocialPlatform, content: String? = null): String {
        val params = buildList {
            add("utm_source" to platform.name.lowercase())
            add("utm_medium" to "social")
            add("utm_campaign" to CAMPAIGN)
            if (!content.isNullOrBlank()) add("utm_content" to content.trim().take(40))
        }.joinToString("&") { (k, v) -> "$k=${enc(v)}" }
        return "$PLAY&referrer=${enc(params)}"
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** "Link profilde" where the platform makes no link clickable; the tracked link itself where it does. */
    fun footer(platform: SocialPlatform, lang: StudioLang = StudioLang.Tr, content: String? = null): String =
        if (platform.clickableLink) "${download(lang)}: ${url(platform, content)}" else bio(lang)

    private fun bio(lang: StudioLang) = when (lang) {
        StudioLang.Tr -> "Link profilde 👆"
        StudioLang.En -> "Link in bio 👆"
        StudioLang.Pt -> "Link na bio 👆"
        StudioLang.Es -> "Enlace en la bio 👆"
    }

    private fun download(lang: StudioLang) = when (lang) {
        StudioLang.Tr -> "Karalak'ı Google Play'den indir"
        StudioLang.En -> "Get Karalak on Google Play"
        StudioLang.Pt -> "Baixe o Karalak no Google Play"
        StudioLang.Es -> "Descarga Karalak en Google Play"
    }

    /** The post text with the footer on its last line (once: text that already has the link or the line is left as it is). */
    fun withFooter(text: String, platform: SocialPlatform, lang: StudioLang = StudioLang.Tr, content: String? = null): String {
        val footer = footer(platform, lang, content)
        val body = text.trim()
        if (body.contains(footer) || body.contains(PLAY)) return body
        return listOf(body, footer).filter { it.isNotEmpty() }.joinToString("\n\n")
    }
}

/**
 * One time a video was posted, with how it did. The numbers come from the platform's own analytics, typed in by hand a
 * day or two later; [hook] is kept as it was said, since the hook is the thing worth comparing.
 */
@Serializable
data class VideoPost(
    val id: String,
    val platform: String,
    val postedAt: Long,
    val hook: String = "",
    val opening: String = RealOpening.Classic.name,
    val views: Int = 0,
    /** Of those who started it, the share (0–100) still watching at the 3 s mark. */
    val hold3s: Int = 0,
    /** The share (0–100) who watched to the end. */
    val finished: Int = 0,
    val shares: Int = 0,
    val installs: Int = 0
) {
    val social: SocialPlatform get() = SocialPlatform.of(platform)

    /** Whether any result has been typed in yet. */
    val measured: Boolean get() = views > 0 || hold3s > 0 || finished > 0 || shares > 0 || installs > 0
}

/** What the posts say about the hooks: the numbers, added up, so a hook can be judged by more than one video. */
object PostStats {
    /** One hook, with every post it was used in. */
    data class HookResult(val hook: String, val posts: Int, val views: Int, val hold3s: Int, val finished: Int, val installs: Int)

    /** Fewest views a post needs before its percentages count; a handful of views says nothing. */
    const val MIN_VIEWS = 100

    /**
     * The hooks, best first: by how many were still watching at 3 s (weighted by views, over the posts that have
     * enough views to count), then by installs. A hook with no counted post is left out.
     */
    fun bestHooks(posts: List<VideoPost>): List<HookResult> =
        posts.filter { it.hook.isNotBlank() && it.views >= MIN_VIEWS }
            .groupBy { it.hook.trim().lowercase() }
            .map { (_, group) ->
                val views = group.sumOf { it.views }
                HookResult(
                    hook = group.first().hook.trim(),
                    posts = group.size,
                    views = views,
                    hold3s = (group.sumOf { it.hold3s.toLong() * it.views } / views).toInt(),
                    finished = (group.sumOf { it.finished.toLong() * it.views } / views).toInt(),
                    installs = group.sumOf { it.installs }
                )
            }
            .sortedWith(compareByDescending<HookResult> { it.hold3s }.thenByDescending { it.installs })

    /** Installs per thousand views of one platform's counted posts, or null with too few views to say. */
    fun installsPerThousand(posts: List<VideoPost>, platform: SocialPlatform): Int? {
        val of = posts.filter { it.social == platform && it.views > 0 }
        val views = of.sumOf { it.views.toLong() }
        if (views < MIN_VIEWS) return null
        return (of.sumOf { it.installs.toLong() } * 1000 / views).toInt()
    }
}
