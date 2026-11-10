package com.example.service

import java.nio.ByteBuffer
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class DnsPacketTest {
    private fun query(type: Int = 1): ByteArray {
        val name = byteArrayOf(7) + "example".toByteArray() + byteArrayOf(3) + "com".toByteArray() + byteArrayOf(0)
        return ByteBuffer.allocate(12 + name.size + 4).apply {
            putShort(123); putShort(0x0100); putShort(1)
            putShort(0); putShort(0); putShort(0)
            put(name); putShort(type.toShort()); putShort(1)
        }.array()
    }
    private fun packet(): ByteArray {
        val dns = query()
        return ByteBuffer.allocate(28 + dns.size).apply {
            put(0, 0x45); putShort(2, capacity().toShort()); put(9, 17)
            putShort(20, 1234); putShort(22, 53); putShort(24, (8 + dns.size).toShort())
            position(28); put(dns)
        }.array()
    }
    @Test fun acceptsIpv4DnsAndBothAddressFamilies() {
        assertTrue(DnsPacket.isDnsQuery(packet(), packet().size))
        for (type in listOf(1, 28)) {
            val q = query(type)
            assertEquals("example.com", DnsPacket.question(q, 0, q.size)?.domain)
            assertEquals(q.size, DnsPacket.question(q, 0, q.size)?.end)
        }
    }
    @Test fun rejectsTruncationFragmentsAndInvalidLengths() {
        val valid = packet()
        for (length in 0 until valid.size) assertFalse(DnsPacket.isDnsQuery(valid, length))
        for (offset in listOf(0, 2, 6, 9, 22, 24)) {
            val corrupt = valid.clone(); corrupt[offset] = 0x7f
            assertFalse("offset $offset", DnsPacket.isDnsQuery(corrupt, corrupt.size))
        }
    }
    @Test fun rejectsCompressedQuestionsMissingTerminatorAndResponses() {
        val q = query()
        val compressed = q.clone(); compressed[12] = 0xc0.toByte()
        assertNull(DnsPacket.question(compressed, 0, compressed.size))
        val response = q.clone(); response[2] = 0x81.toByte()
        assertNull(DnsPacket.question(response, 0, response.size))
        assertNull(DnsPacket.question(q, 0, q.size - 1))
        assertNull(DnsPacket.question(q, Int.MAX_VALUE, q.size))
    }
    @Test fun malformedTrafficNeverThrows() {
        val random = Random(123)
        repeat(10_000) {
            val bytes = random.nextBytes(random.nextInt(0, 512))
            DnsPacket.isDnsQuery(bytes, bytes.size)
            DnsPacket.question(bytes, random.nextInt(-1, bytes.size + 2), random.nextInt(0, 600))
        }
    }
}
