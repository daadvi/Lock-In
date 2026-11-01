package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RequestStatus
import com.example.service.FriendsPartyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FriendsPartyTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        FriendsPartyManager.init(context)
        FriendsPartyManager.forceLeaveParty(context)
    }

    @Test
    fun `test creating a party generates code and registers host with stats`() {
        assertFalse(FriendsPartyManager.isJoined.value)

        val code = FriendsPartyManager.createParty(context, "Equipe Foco Total")

        assertTrue(code.startsWith("LCK-"))
        assertTrue(FriendsPartyManager.isJoined.value)

        val state = FriendsPartyManager.partyState.value
        assertNotNull(state)
        assertEquals(code, state?.partyCode)
        assertEquals(1, state?.members?.size)

        val host = state?.members?.first()
        assertNotNull(host)
        assertTrue(host?.isHost == true)
        assertTrue(host?.respectPercentage ?: 0 >= 70)
        assertTrue(host?.topApps?.isNotEmpty() == true)
    }

    @Test
    fun `test joining party with code adds friend and creates multi-user connection`() {
        val joined = FriendsPartyManager.joinParty(context, "LCK-9999")
        assertTrue(joined)
        assertTrue(FriendsPartyManager.isJoined.value)

        FriendsPartyManager.addSimulatedFriend(context, "Marcos")

        val state = FriendsPartyManager.partyState.value
        assertNotNull(state)
        assertEquals("LCK-9999", state?.partyCode)
        // Has buddy Marcos + joining user
        assertTrue((state?.members?.size ?: 0) >= 2)

        val buddy = state?.members?.find { it.displayName == "Marcos" }
        assertNotNull(buddy)
        assertTrue((buddy?.respectPercentage ?: 0) > 90)
    }

    @Test
    fun `test blacklisted install request requires friend approval`() {
        FriendsPartyManager.createParty(context)
        FriendsPartyManager.addSimulatedFriend(context, "Lucas")

        // Initially Instagram is not allowed
        assertFalse(FriendsPartyManager.isPackageTemporarilyAllowed("com.instagram.android"))

        // Request approval
        val req = FriendsPartyManager.requestInstallApproval(
            context = context,
            targetPackage = "com.instagram.android",
            targetAppName = "Instagram"
        )
        assertNotNull(req)
        assertEquals(RequestStatus.PENDING, req?.status)
        assertTrue(FriendsPartyManager.hasPendingInstallRequest())

        // Still not allowed while pending
        assertFalse(FriendsPartyManager.isPackageTemporarilyAllowed("com.instagram.android"))

        // Friend Lucas approves
        val resolved = FriendsPartyManager.respondToInstallRequest(context, req!!.requestId, approved = true)
        assertTrue(resolved)

        // Now package is temporarily allowed!
        assertTrue(FriendsPartyManager.isPackageTemporarilyAllowed("com.instagram.android"))
        assertFalse(FriendsPartyManager.hasPendingInstallRequest())
    }

    @Test
    fun `test friend rejecting install keeps app blocked`() {
        FriendsPartyManager.createParty(context)
        FriendsPartyManager.addSimulatedFriend(context, "Lucas")

        val req = FriendsPartyManager.requestInstallApproval(
            context = context,
            targetPackage = "com.zhiliaoapp.musically",
            targetAppName = "TikTok"
        )
        assertNotNull(req)

        // Friend Lucas rejects
        FriendsPartyManager.respondToInstallRequest(context, req!!.requestId, approved = false)

        // Must remain NOT allowed
        assertFalse(FriendsPartyManager.isPackageTemporarilyAllowed("com.zhiliaoapp.musically"))
        assertFalse(FriendsPartyManager.hasPendingInstallRequest())
    }

    @Test
    fun `test user cannot leave party while install request is pending`() {
        FriendsPartyManager.createParty(context)
        FriendsPartyManager.addSimulatedFriend(context, "Lucas")

        // Create pending install request
        FriendsPartyManager.requestInstallApproval(
            context = context,
            targetPackage = "com.zhiliaoapp.musically",
            targetAppName = "TikTok"
        )
        assertTrue(FriendsPartyManager.hasPendingInstallRequest())

        // User attempts to leave party
        val (allowed, message) = FriendsPartyManager.requestLeaveParty(context)

        // Must be STRICTLY BLOCKED!
        assertFalse(allowed)
        assertTrue(message.contains("Não é permitido sair do grupo enquanto houver uma solicitação de instalação pendente"))
        assertTrue(FriendsPartyManager.isJoined.value)

        // Once the request is resolved (e.g. rejected by friend)
        val state = FriendsPartyManager.partyState.value!!
        val pending = state.pendingInstallRequests.first()
        FriendsPartyManager.respondToInstallRequest(context, pending.requestId, approved = false)

        assertFalse(FriendsPartyManager.hasPendingInstallRequest())
    }

    @Test
    fun `test unpairing requires peer approval from another member`() {
        FriendsPartyManager.createParty(context)
        FriendsPartyManager.addSimulatedFriend(context, "Lucas")
        assertEquals(2, FriendsPartyManager.partyState.value?.members?.size)

        // User requests to leave / unpair
        val (allowed, msg) = FriendsPartyManager.requestLeaveParty(context)
        // Since there is another member, user cannot just vanish without approval
        assertFalse(allowed)
        assertTrue(FriendsPartyManager.hasPendingLeaveRequest())
        assertTrue(FriendsPartyManager.isJoined.value)

        val leaveReq = FriendsPartyManager.partyState.value?.pendingLeaveRequests?.first()
        assertNotNull(leaveReq)
        assertEquals(RequestStatus.PENDING, leaveReq?.status)

        // Friend Lucas approves the leave request
        FriendsPartyManager.respondToLeaveRequest(context, leaveReq!!.requestId, approved = true)

        // Now user is unpaired!
        assertFalse(FriendsPartyManager.isJoined.value)
    }

    @Test
    fun `test unpairing rejection keeps user in party`() {
        FriendsPartyManager.createParty(context)
        FriendsPartyManager.addSimulatedFriend(context, "Beatriz")

        FriendsPartyManager.requestLeaveParty(context)
        assertTrue(FriendsPartyManager.hasPendingLeaveRequest())

        val leaveReq = FriendsPartyManager.partyState.value?.pendingLeaveRequests?.first()
        assertNotNull(leaveReq)

        // Friend Beatriz rejects the leave request
        FriendsPartyManager.respondToLeaveRequest(context, leaveReq!!.requestId, approved = false)

        // User remains in party
        assertTrue(FriendsPartyManager.isJoined.value)
        assertFalse(FriendsPartyManager.hasPendingLeaveRequest())
    }

    @Test
    fun `test members share screen time and adherence statistics`() {
        FriendsPartyManager.joinParty(context, "LCK-5555")
        FriendsPartyManager.addSimulatedFriend(
            context = context,
            name = "Beatriz",
            screenTimeSec = 4200L,
            limitMin = 120,
            respectPct = 96,
            streakDays = 8
        )

        val state = FriendsPartyManager.partyState.value
        assertNotNull(state)
        assertEquals(2, state?.members?.size)

        val beatriz = state?.members?.find { it.displayName == "Beatriz" }
        assertNotNull(beatriz)
        assertEquals(4200L, beatriz?.screenTimeSecondsToday)
        assertEquals(120, beatriz?.screenTimeLimitMinutes)
        assertEquals(96, beatriz?.respectPercentage)
        assertEquals(8, beatriz?.streakDays)
    }

    @Test
    fun `test code normalization converts 4 digits to LCK prefix`() {
        assertEquals("LCK-1234", FriendsPartyManager.normalizeCode("1234"))
        assertEquals("LCK-9999", FriendsPartyManager.normalizeCode("LCK-9999"))
        assertEquals("LCK-5678", FriendsPartyManager.normalizeCode("  lck-5678  "))
        assertEquals("LCK-4321", FriendsPartyManager.normalizeCode("4321"))
    }

    @Test
    fun `test temporary permission expires after duration`() {
        FriendsPartyManager.createParty(context)
        FriendsPartyManager.addSimulatedFriend(context, "Rafael")

        val req = FriendsPartyManager.requestInstallApproval(
            context = context,
            targetPackage = "com.snapchat.android",
            targetAppName = "Snapchat"
        )
        assertNotNull(req)

        // Approve with 15 minutes limit
        val approved = FriendsPartyManager.respondToInstallRequest(
            context = context,
            requestId = req!!.requestId,
            approved = true
        )
        assertTrue(approved)
        assertTrue(FriendsPartyManager.isPackageTemporarilyAllowed("com.snapchat.android"))

        // Revoke / simulate expiry by setting timestamp in the past
        FriendsPartyManager.getTemporarilyAllowedPackagesMap()["com.snapchat.android"] = System.currentTimeMillis() - 1000L
        assertFalse(FriendsPartyManager.isPackageTemporarilyAllowed("com.snapchat.android"))

        // Also test direct revocation
        FriendsPartyManager.getTemporarilyAllowedPackagesMap()["com.snapchat.android"] = System.currentTimeMillis() + 60000L
        assertTrue(FriendsPartyManager.isPackageTemporarilyAllowed("com.snapchat.android"))
        FriendsPartyManager.revokeTemporaryAllowance("com.snapchat.android")
        assertFalse(FriendsPartyManager.isPackageTemporarilyAllowed("com.snapchat.android"))
    }

    @Test
    fun `test solo party host can leave party immediately without peer approval`() {
        FriendsPartyManager.createParty(context, "Meu Grupo Individual")
        assertEquals(1, FriendsPartyManager.partyState.value?.members?.size)

        // Since there is only 1 member (the user), leave request is immediately granted
        val (allowed, msg) = FriendsPartyManager.requestLeaveParty(context)
        assertTrue(allowed)
        assertFalse(FriendsPartyManager.isJoined.value)
    }

    @Test
    fun `test shared group time entity packages and conflict logic`() {
        val group = com.example.data.local.AppGroupEntity(
            name = "Redes Sociais",
            packageNamesCsv = "com.instagram.android, com.zhiliaoapp.musically, com.twitter.android",
            limitMinutes = 30,
            remainingSeconds = 30 * 60L
        )

        assertEquals(3, group.getPackageList().size)
        assertTrue(group.containsPackage("com.instagram.android"))
        assertTrue(group.containsPackage("com.zhiliaoapp.musically"))
        assertTrue(group.containsPackage("com.twitter.android"))
        assertFalse(group.containsPackage("com.google.android.youtube"))
    }
}

