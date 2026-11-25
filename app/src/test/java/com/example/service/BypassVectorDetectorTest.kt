package com.example.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BypassVectorDetectorTest {

    @Test
    fun browsersAreAlwaysVectors() {
        assertTrue(BypassVectorDetector.isBypassVector("com.android.chrome", isBrowser = true))
        assertTrue(BypassVectorDetector.isBypassVector("org.mozilla.firefox", isBrowser = true))
    }

    @Test
    fun vpnAndDnsClientsAreVectors() {
        assertTrue(BypassVectorDetector.isBypassVector("com.wireguard.android"))
        assertTrue(BypassVectorDetector.isBypassVector("com.github.kr328.clash"))
        assertTrue(BypassVectorDetector.isBypassVector("de.blinkt.openvpn"))
        assertTrue(BypassVectorDetector.isBypassVector("com.cloudflare.onedotonedotonedotone"))
    }

    @Test
    fun clonersAndDualAppsAreVectors() {
        assertTrue(BypassVectorDetector.isBypassVector("com.lbe.parallel.intl"))
        assertTrue(BypassVectorDetector.isBypassVector("com.appcloner.cloner"))
    }

    @Test
    fun ordinaryAppsAreNotVectors() {
        assertFalse(BypassVectorDetector.isBypassVector("com.instagram.android"))
        assertFalse(BypassVectorDetector.isBypassVector("com.example.allowed"))
        assertFalse(BypassVectorDetector.isBypassVector(null))
        assertFalse(BypassVectorDetector.isBypassVector("  "))
    }

    @Test
    fun platformPackagesAreNeverFlagged() {
        assertFalse(BypassVectorDetector.isBypassVector("com.android.providers.downloads"))
        assertFalse(BypassVectorDetector.isBypassVector("android"))
    }
}
