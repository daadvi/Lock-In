package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.service.CloudPartyEvent
import com.example.service.FriendsPartyManager
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PartyAuthorizationTest {
    private lateinit var context: Context
    private val code = "LCK2-" + "C".repeat(64)
    private lateinit var myId: String

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        FriendsPartyManager.forceLeaveParty(context)
        myId = FriendsPartyManager.getMyUserId()
        setState(PartyState(code, hostUserId = myId, members = listOf(
            PartyMember(myId, "Eu"), PartyMember("peer", "Amigo"), PartyMember("other", "Outro")
        ), pendingInstallRequests = listOf(InstallApprovalRequest("install", myId, "Eu", "dummy.allowed", "Dummy")),
            pendingLeaveRequests = listOf(LeaveApprovalRequest("leave", myId, "Eu"))))
    }

    @Suppress("UNCHECKED_CAST")
    private fun setState(state: PartyState) {
        val field = FriendsPartyManager::class.java.getDeclaredField("_partyState").apply { isAccessible = true }
        (field.get(FriendsPartyManager) as MutableStateFlow<PartyState?>).value = state
    }

    private fun event(action: String, sender: String = "peer", payload: JSONObject) {
        FriendsPartyManager.processIncomingCloudEvent(context,
            CloudPartyEvent("dummy", System.currentTimeMillis(), action, sender, payload))
    }

    private fun resolution(id: String = "install", status: String = "APPROVED") =
        JSONObject().put("requestId", id).put("status", status).put("targetPackage", "dummy.injected")

    @Test fun `unsolicited unknown sender self and invalid status cannot unlock`() {
        event("ACTION_RESOLVE_INSTALL", payload = resolution("missing"))
        event("ACTION_RESOLVE_INSTALL", "unknown", resolution())
        event("ACTION_RESOLVE_INSTALL", myId, resolution())
        event("ACTION_RESOLVE_INSTALL", payload = resolution(status = "garbage"))
        assertFalse(FriendsPartyManager.isPackageTemporarilyAllowed("dummy.allowed"))
        assertFalse(FriendsPartyManager.isPackageTemporarilyAllowed("dummy.injected"))
        assertEquals(RequestStatus.PENDING, FriendsPartyManager.partyState.value!!.pendingInstallRequests.single().status)
        assertFalse(FriendsPartyManager.respondToInstallRequest(context, "install", true))
        assertFalse(FriendsPartyManager.respondToLeaveRequest(context, "leave", true))
    }

    @Test fun `approval binds stored package and replay cannot renew allowance`() {
        event("ACTION_RESOLVE_INSTALL", payload = resolution())
        assertTrue(FriendsPartyManager.isPackageTemporarilyAllowed("dummy.allowed"))
        assertFalse(FriendsPartyManager.isPackageTemporarilyAllowed("dummy.injected"))
        FriendsPartyManager.revokeTemporaryAllowance("dummy.allowed")
        event("ACTION_RESOLVE_INSTALL", payload = resolution())
        assertFalse(FriendsPartyManager.isPackageTemporarilyAllowed("dummy.allowed"))
        assertEquals("peer", FriendsPartyManager.partyState.value!!.pendingInstallRequests.single().resolvedById)
    }

    @Test fun `another members approval never grants allowance on this device`() {
        setState(FriendsPartyManager.partyState.value!!.copy(pendingInstallRequests = listOf(
            InstallApprovalRequest("install", "other", "Outro", "dummy.allowed", "Dummy"))))
        event("ACTION_RESOLVE_INSTALL", payload = resolution())
        assertFalse(FriendsPartyManager.isPackageTemporarilyAllowed("dummy.allowed"))
        assertEquals(RequestStatus.APPROVED, FriendsPartyManager.partyState.value!!.pendingInstallRequests.single().status)
    }

    @Test fun `leave resolution requires pending request and ignores supplied victim`() {
        event("ACTION_RESOLVE_LEAVE", payload = resolution("missing").put("leavingMemberId", myId))
        assertNotNull(FriendsPartyManager.partyState.value)
        event("ACTION_RESOLVE_LEAVE", payload = resolution("leave", "REJECTED"))
        assertEquals(RequestStatus.REJECTED, FriendsPartyManager.partyState.value!!.pendingLeaveRequests.single().status)
        event("ACTION_RESOLVE_LEAVE", payload = resolution("leave").put("leavingMemberId", myId))
        assertNotNull(FriendsPartyManager.partyState.value)
    }

    @Test fun `member sync cannot replace local identity or grow without limit`() {
        event("ACTION_SYNC_MEMBER", payload = JSONObject().put("userId", myId).put("displayName", "Spoof"))
        assertEquals("Eu", FriendsPartyManager.partyState.value!!.members.first { it.userId == myId }.displayName)
        repeat(40) { i ->
            event("ACTION_SYNC_MEMBER", "new$i", JSONObject().put("userId", "new$i"))
        }
        assertEquals(20, FriendsPartyManager.partyState.value!!.members.size)
    }

    @Test fun `privacy strips app list and exact time only from network serialization`() {
        val member = PartyMember(myId, "Eu", screenTimeSecondsToday = 900L,
            topApps = listOf(FriendAppUsage("dummy.private", "Private", 900)), shareDetailedScreenTime = false)
        val payload = FriendsPartyManager.serializeMemberForNetwork(member)
        assertEquals(0, payload.getJSONArray("topApps").length())
        assertEquals(0L, payload.getLong("screenTimeSecondsToday"))
        assertEquals(1, member.topApps.size)
        assertEquals(900L, member.screenTimeSecondsToday)
    }

    @Test fun `malformed message does not prevent next valid message`() {
        event("ACTION_SYNC_MEMBER", payload = JSONObject().put("userId", "peer").put("topApps", org.json.JSONArray().put(3)))
        event("ACTION_RESOLVE_INSTALL", payload = resolution())
        assertTrue(FriendsPartyManager.isPackageTemporarilyAllowed("dummy.allowed"))
    }

    @Test fun `request sender must match and duplicate cannot reset resolved status`() {
        val request = JSONObject().put("requestId", "remote").put("requesterId", myId)
            .put("targetPackage", "dummy.remote").put("status", "PENDING")
        event("ACTION_REQUEST_INSTALL", payload = request)
        assertEquals(1, FriendsPartyManager.partyState.value!!.pendingInstallRequests.size)
        request.put("requesterId", "peer")
        event("ACTION_REQUEST_INSTALL", payload = request)
        assertTrue(FriendsPartyManager.respondToInstallRequest(context, "remote", false))
        event("ACTION_REQUEST_INSTALL", payload = request)
        assertEquals(RequestStatus.REJECTED, FriendsPartyManager.partyState.value!!.pendingInstallRequests.first { it.requestId == "remote" }.status)
    }
    @Test fun `resolved history stays bounded without preventing future requests`() {
        val previous = (1..100).map { InstallApprovalRequest("old-$it", myId, "Owner", "dummy.old", "Old", status = RequestStatus.REJECTED) }
        setState(FriendsPartyManager.partyState.value!!.copy(pendingInstallRequests = previous))
        assertNotNull(FriendsPartyManager.requestInstallApproval(context, "dummy.new", "New"))
        val requests = FriendsPartyManager.partyState.value!!.pendingInstallRequests
        assertEquals(100, requests.size)
        assertEquals(1, requests.count { it.status == RequestStatus.PENDING })
    }

}
