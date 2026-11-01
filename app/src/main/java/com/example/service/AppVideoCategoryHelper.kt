package com.example.service

object AppVideoCategoryHelper {
    private val SHORT_VIDEO_PACKAGES = setOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.snapchat.android",
        "com.facebook.katana",
        "com.kwai.video",
        "com.kuaishou.nebula",
        "com.twitter.android",
        "com.reddit.frontpage"
    )

    private val VIDEO_PACKAGES = setOf(
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "tv.twitch.android.app",
        "com.hbo.hbonow",
        "com.wbd.stream",
        "org.videolan.vlc",
        "com.mxtech.videoplayer.ad",
        "com.google.android.videos",
        "com.crunchyroll.crunchyroll"
    )

    private val NON_VIDEO_EXCLUSIONS = setOf(
        "camera", "cam", "gallery", "clock", "calculator", "settings",
        "contacts", "dialer", "phone", "notes", "calendar", "files",
        "scanner", "recorder", "soundrecorder", "voice"
    )

    fun isExcludedFromVideo(packageName: String): Boolean {
        val p = packageName.lowercase()
        return NON_VIDEO_EXCLUSIONS.any { p.contains(it) } &&
                !p.contains("youtube") && !p.contains("instagram") &&
                !p.contains("tiktok") && !p.contains("kwai")
    }

    fun hasShortVideos(packageName: String): Boolean {
        if (isExcludedFromVideo(packageName)) return false
        val p = packageName.lowercase()
        return SHORT_VIDEO_PACKAGES.contains(p) ||
                p.contains("tiktok") || p.contains("kwai") ||
                p.contains("instagram") || p.contains("youtube") ||
                p.contains("reels") || p.contains("shorts")
    }

    /**
     * Apps that exclusively feature short-form videos (Reels, TikToks, Shorts feed only)
     * e.g. Instagram, TikTok, Kwai, Snapchat.
     */
    fun hasShortVideosOnly(packageName: String): Boolean {
        if (isExcludedFromVideo(packageName)) return false
        val p = packageName.lowercase()
        return (p.contains("instagram") || p.contains("tiktok") ||
                p.contains("musically") || p.contains("kwai") ||
                p.contains("kuaishou") || p.contains("snapchat") ||
                p.contains("trill")) && !p.contains("youtube")
    }

    /**
     * Apps that have BOTH short-form videos (Shorts/Reels) AND long-form videos
     * e.g. YouTube, Facebook, Reddit, Twitter/X.
     */
    fun hasBothShortAndLongVideos(packageName: String): Boolean {
        if (isExcludedFromVideo(packageName)) return false
        val p = packageName.lowercase()
        return p.contains("youtube") || p.contains("facebook") ||
                p.contains("katana") || p.contains("reddit") ||
                p.contains("twitter") || p.contains("x.com")
    }

    /**
     * Apps that only have long-form videos, movies, streams, or player content
     * e.g. Netflix, Disney+, Twitch, Prime Video, HBO Max, VLC, Crunchyroll.
     */
    fun hasLongVideosOnly(packageName: String): Boolean {
        if (isExcludedFromVideo(packageName)) return false
        val p = packageName.lowercase()
        return (VIDEO_PACKAGES.contains(p) || p.contains("netflix") ||
                p.contains("disney") || p.contains("twitch") ||
                p.contains("primevideo") || p.contains("hbomax") ||
                p.contains("max.com") || p.contains("crunchyroll") ||
                p.contains("vlc") || p.contains("videoplayer")) &&
                !hasShortVideos(packageName)
    }

    fun hasVideos(packageName: String): Boolean {
        if (isExcludedFromVideo(packageName)) return false
        val p = packageName.lowercase()
        return hasShortVideos(packageName) || VIDEO_PACKAGES.contains(p) ||
                p.contains("netflix") || p.contains("disney") ||
                p.contains("twitch") || p.contains("stream") ||
                p.contains("primevideo") || p.contains("hbomax") ||
                p.contains("max.com") || p.contains("crunchyroll") ||
                (p.contains("videoplayer") && !p.contains("camera"))
    }

    fun hasShortVideosForDomain(domain: String?): Boolean {
        if (domain.isNullOrBlank()) return false
        val d = domain.lowercase()
        return d.contains("youtube.com") || d.contains("youtu.be") ||
                d.contains("instagram.com") || d.contains("tiktok.com") ||
                d.contains("facebook.com") || d.contains("kwai.com") ||
                d.contains("twitter.com") || d.contains("x.com")
    }

    fun hasVideosForDomain(domain: String?): Boolean {
        if (domain.isNullOrBlank()) return false
        val d = domain.lowercase()
        return hasShortVideosForDomain(d) ||
                d.contains("netflix.com") || d.contains("twitch.tv") ||
                d.contains("vimeo.com") || d.contains("dailymotion.com") ||
                d.contains("disneyplus.com") || d.contains("primevideo.com")
    }

    fun isVideoPlatform(packageName: String?, domain: String? = null): Boolean {
        val pkgHasVideos = packageName?.let { hasVideos(it) } ?: false
        val domainHasVideos = hasVideosForDomain(domain)
        return pkgHasVideos || domainHasVideos
    }
}
