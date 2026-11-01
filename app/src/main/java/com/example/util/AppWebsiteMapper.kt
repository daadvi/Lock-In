package com.example.util

import com.example.data.local.LockInDao
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.RestrictedWebsiteEntity

/**
 * Maps Android app package names to their official website domains and vice-versa,
 * ensuring that whenever an app has a time limit, its corresponding website automatically
 * inherits the exact same time limit and state.
 */
object AppWebsiteMapper {

    private val packageToDomainsMap = mapOf(
        "com.google.android.youtube" to listOf("youtube.com", "m.youtube.com", "youtu.be"),
        "com.instagram.android" to listOf("instagram.com"),
        "com.zhiliaoapp.musically" to listOf("tiktok.com"),
        "com.ss.android.ugc.trill" to listOf("tiktok.com"),
        "com.facebook.katana" to listOf("facebook.com", "m.facebook.com", "fb.com"),
        "com.facebook.lite" to listOf("facebook.com", "m.facebook.com"),
        "com.twitter.android" to listOf("x.com", "twitter.com"),
        "com.reddit.frontpage" to listOf("reddit.com"),
        "tv.twitch.android.app" to listOf("twitch.tv"),
        "com.netflix.mediaclient" to listOf("netflix.com"),
        "com.pinterest" to listOf("pinterest.com"),
        "com.spotify.music" to listOf("spotify.com", "open.spotify.com"),
        "com.discord" to listOf("discord.com"),
        "com.linkedin.android" to listOf("linkedin.com"),
        "com.whatsapp" to listOf("whatsapp.com", "web.whatsapp.com"),
        "org.telegram.messenger" to listOf("telegram.org", "web.telegram.org"),
        "com.instagram.barcelona" to listOf("threads.net"),
        "com.roblox.client" to listOf("roblox.com"),
        "com.kwai.video" to listOf("kwai.com"),
        "com.snapchat.android" to listOf("snapchat.com"),
        "com.amazon.mShop.android.shopping" to listOf("amazon.com", "amazon.com.br"),
        "com.shopee.br" to listOf("shopee.com.br", "shopee.com"),
        "com.mercadolibre" to listOf("mercadolivre.com.br", "mercadolibre.com"),
        "com.shein.fastfashion" to listOf("shein.com"),
        "com.aliexpress" to listOf("aliexpress.com"),
        "com.openai.chatgpt" to listOf("chatgpt.com", "openai.com"),
        "com.google.android.apps.bard" to listOf("gemini.google.com"),
        "com.chess" to listOf("chess.com"),
        "com.duolingo" to listOf("duolingo.com")
    )

    /**
     * Resolves all known domains for a given package and app name.
     */
    fun getWebsitesForApp(packageName: String, appName: String): List<String> {
        val known = packageToDomainsMap[packageName]
        if (!known.isNullOrEmpty()) {
            return known
        }

        val results = mutableListOf<String>()
        val lowerPkg = packageName.lowercase()
        val lowerName = appName.lowercase().replace(" ", "").replace("-", "")

        for ((pkg, domains) in packageToDomainsMap) {
            if (lowerPkg.contains(pkg) || domains.any { lowerPkg.contains(it.substringBefore(".")) }) {
                results.addAll(domains)
            }
        }

        if (results.isEmpty() && lowerName.length >= 3) {
            // Heuristic deduction: e.g. "roblox" -> roblox.com
            val candidate = "$lowerName.com"
            results.add(candidate)
        }

        return results.distinct()
    }

    /**
     * Returns the primary canonical domain for an app, or null if unknown.
     */
    fun getPrimaryDomainForApp(packageName: String, appName: String): String? {
        val list = getWebsitesForApp(packageName, appName)
        return list.firstOrNull()
    }

    /**
     * Finds a monitored app that matches a given URL or browser domain.
     */
    fun findAppForDomain(apps: Collection<RestrictedAppEntity>, domainOrUrl: String?): RestrictedAppEntity? {
        if (domainOrUrl.isNullOrBlank()) return null
        val clean = domainOrUrl.lowercase()
            .replace("https://", "")
            .replace("http://", "")
            .replace("www.", "")
            .split("/")
            .firstOrNull() ?: return null

        return apps.firstOrNull { app ->
            val domains = getWebsitesForApp(app.packageName, app.appName)
            domains.any { clean.contains(it) || it.contains(clean) } ||
                    (app.associatedWebsites.isNotBlank() && app.associatedWebsites.lowercase().contains(clean)) ||
                    clean.contains(app.appName.lowercase().replace(" ", ""))
        }
    }

    /**
     * Synchronizes website entities in the Room database with an app's limit.
     * If the app is limited, its corresponding website automatically gets the same limit.
     */
    suspend fun syncWebsitesForApp(repository: LockInDao, app: RestrictedAppEntity) {
        val domains = getWebsitesForApp(app.packageName, app.appName)
        if (domains.isEmpty()) return

        val existingWebsites = repository.getAllWebsitesList()
        val primaryDomain = domains.first()

        if (app.isLimited && app.totalLimitMinutes > 0) {
            val matchingWebsite = existingWebsites.firstOrNull { it.domain.equals(primaryDomain, ignoreCase = true) }
            val limitM = app.totalLimitMinutes
            val remainingSec = app.totalAvailableSeconds
            val isBlocked = app.isBlocked || app.isLimitDepleted

            if (matchingWebsite != null) {
                val updated = matchingWebsite.copy(
                    limitMinutes = limitM,
                    remainingSeconds = remainingSec,
                    linkedPackageName = app.packageName,
                    isBlocked = isBlocked
                )
                repository.updateWebsite(updated)
            } else {
                val newWebsite = RestrictedWebsiteEntity(
                    domain = primaryDomain,
                    linkedPackageName = app.packageName,
                    limitMinutes = limitM,
                    remainingSeconds = remainingSec,
                    isBlocked = isBlocked
                )
                repository.insertWebsite(newWebsite)
            }
        } else {
            // App limit removed or disabled: remove linked website
            val matchingWebsite = existingWebsites.firstOrNull {
                it.linkedPackageName == app.packageName || it.domain.equals(primaryDomain, ignoreCase = true)
            }
            if (matchingWebsite != null && matchingWebsite.linkedPackageName == app.packageName) {
                repository.deleteWebsite(matchingWebsite)
            }
        }
    }

    /**
     * Batch synchronizes all restricted apps with their corresponding websites.
     */
    suspend fun syncAllAppsAndWebsites(repository: LockInDao) {
        val allApps = repository.getAllAppsList()
        for (app in allApps) {
            syncWebsitesForApp(repository, app)
        }
    }
}
