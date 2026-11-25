package com.example.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Private DNS externo contorna o sinkhole local: precisa ser detectado durante bloqueio de sites. */
class IntegrityGuardDnsTest {

    @Test
    fun hostnameModeWithExternalResolverIsBypass() {
        assertTrue(IntegrityGuard.isDnsBypass("hostname", "dns.google"))
        assertTrue(IntegrityGuard.isDnsBypass("HOSTNAME", "cloudflare-dns.com"))
    }

    @Test
    fun automaticModesAreNotBypass() {
        assertFalse(IntegrityGuard.isDnsBypass("off", null))
        assertFalse(IntegrityGuard.isDnsBypass("opportunistic", null))
        assertFalse(IntegrityGuard.isDnsBypass(null, "dns.google"))
    }

    @Test
    fun hostnameModeWithoutSpecifierIsNotBypass() {
        assertFalse(IntegrityGuard.isDnsBypass("hostname", null))
        assertFalse(IntegrityGuard.isDnsBypass("hostname", "   "))
        assertFalse(IntegrityGuard.isDnsBypass("hostname", "off"))
    }
}